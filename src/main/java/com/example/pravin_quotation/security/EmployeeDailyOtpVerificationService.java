package com.example.pravin_quotation.security;

import com.example.pravin_quotation.model.EmployeeDailyOtpVerification;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.EmployeeDailyOtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Service
public class EmployeeDailyOtpVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDailyOtpVerificationService.class);

    public static final LocalTime WINDOW_START_TIME_IST = LocalTime.of(9, 0, 0);  // 9:00 AM IST
    public static final LocalTime WINDOW_END_TIME_IST = LocalTime.of(20, 0, 0);    // 8:00 PM IST

    private final EmployeeDailyOtpVerificationRepository repository;
    private final EmployeeSessionService employeeSessionService;

    public EmployeeDailyOtpVerificationService(
            EmployeeDailyOtpVerificationRepository repository,
            EmployeeSessionService employeeSessionService
    ) {
        this.repository = repository;
        this.employeeSessionService = employeeSessionService;
    }

    /**
     * Checks if current time in Asia/Kolkata is within the daily window: 9:00 AM to 8:00 PM IST.
     */
    public boolean isWithinDailyOtpWindow() {
        LocalTime time = employeeSessionService.nowIst().toLocalTime();
        return !time.isBefore(WINDOW_START_TIME_IST) && time.isBefore(WINDOW_END_TIME_IST);
    }

    /**
     * Checks if the given employee has a verified OTP record for today within the 9:00 AM - 8:00 PM IST window.
     * Outside the daily window (e.g. at or after 8:00 PM, or before 9:00 AM), returns false so a fresh OTP is required.
     */
    @Transactional(readOnly = true)
    public boolean isDailyOtpVerified(Long employeeId) {
        if (employeeId == null) {
            return false;
        }

        // OTP reuse is strictly permitted ONLY during 9:00 AM - 8:00 PM IST
        if (!isWithinDailyOtpWindow()) {
            return false;
        }

        LocalDate todayIst = employeeSessionService.nowIst().toLocalDate();
        return repository.existsByUserIdAndVerificationDateAndActiveTrue(employeeId, todayIst);
    }

    /**
     * Saves a successful OTP verification record for the employee for today's working window.
     */
    @Transactional
    public void recordSuccessfulVerification(User employee) {
        if (employee == null || employee.getId() == null) {
            return;
        }

        // Only record daily verification when within the 9:00 AM - 8:00 PM IST window
        if (!isWithinDailyOtpWindow()) {
            log.info("OTP verified for employee [{}] outside daily window (after-hours/early); no daily reuse record created.",
                    employee.getId());
            return;
        }

        ZonedDateTime nowIst = employeeSessionService.nowIst();
        LocalDate todayIst = nowIst.toLocalDate();

        if (repository.existsByUserIdAndVerificationDateAndActiveTrue(employee.getId(), todayIst)) {
            log.debug("Employee [{}] already has active daily OTP verification for [{}]", employee.getId(), todayIst);
            return;
        }

        try {
            EmployeeDailyOtpVerification record = new EmployeeDailyOtpVerification(
                    employee,
                    todayIst,
                    nowIst.toLocalDateTime()
            );
            repository.save(record);
            log.info("Saved daily OTP verification record for employee [{}] on [{}] at [{}]",
                    employee.getId(), todayIst, nowIst.toLocalDateTime());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent daily OTP verification insert for employee [{}] on [{}]: {}",
                    employee.getId(), todayIst, ex.getMessage());
        }
    }
}

