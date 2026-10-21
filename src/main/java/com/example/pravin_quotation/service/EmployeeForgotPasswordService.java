package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.OtpEmailService;
import com.example.pravin_quotation.security.OtpService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmployeeForgotPasswordService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeForgotPasswordService.class);

    public static final String SESSION_EMPLOYEE_ID = "PW_RESET_EMPLOYEE_ID";
    public static final String SESSION_EMPLOYEE_NAME = "PW_RESET_EMPLOYEE_NAME";
    public static final String SESSION_EMPLOYEE_IDENTIFIER = "PW_RESET_EMPLOYEE_IDENTIFIER";
    public static final String SESSION_OTP_HASH = "PW_RESET_OTP_HASH";
    public static final String SESSION_OTP_EXPIRY = "PW_RESET_OTP_EXPIRY";
    public static final String SESSION_OTP_ATTEMPTS = "PW_RESET_OTP_ATTEMPTS";
    public static final String SESSION_LAST_SENT = "PW_RESET_LAST_SENT";
    public static final String SESSION_VERIFIED = "PW_RESET_VERIFIED";
    public static final String SESSION_VERIFIED_EMPLOYEE_ID = "PW_RESET_VERIFIED_EMPLOYEE_ID";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final OtpEmailService otpEmailService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeForgotPasswordService(
            UserRepository userRepository,
            OtpService otpService,
            OtpEmailService otpEmailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.otpEmailService = otpEmailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Finds an employee account by ID (if numeric) or email.
     * Enforces that the user has Role.EMPLOYEE and is active.
     */
    public User findEligibleEmployee(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID or Email is required.");
        }

        String trimmed = identifier.trim();
        Optional<User> userOpt = Optional.empty();

        if (trimmed.matches("^\\d+$")) {
            try {
                Long id = Long.parseLong(trimmed);
                userOpt = userRepository.findById(id);
            } catch (NumberFormatException ignored) {
            }
        }

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(trimmed.toLowerCase());
        }

        User user = userOpt.orElseThrow(() ->
                new IllegalArgumentException("No employee account found with the provided Employee ID / Email."));

        if (user.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Password reset via this page is only available for Employee accounts.");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("This employee account is inactive. Please contact your administrator.");
        }

        return user;
    }

    /**
     * Dynamically finds the configured active Super Admin email.
     * Never hardcodes the address.
     */
    public String getAdminEmail() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.SUPER_ADMIN)
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .map(User::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active Super Admin email is configured in the system."));
    }

    /**
     * Initiates the password reset process for an employee.
     * Generates a 6-digit OTP, securely stores the hash in session with a 5-minute expiry,
     * and sends the OTP to the configured dynamic Super Admin email.
     */
    public void initiateReset(HttpSession session, String identifier) {
        User employee = findEligibleEmployee(identifier);

        LocalDateTime lastSent = (LocalDateTime) session.getAttribute(SESSION_LAST_SENT);
        if (lastSent != null && !otpService.isResendAllowed(lastSent)) {
            throw new IllegalStateException("Please wait before requesting another OTP.");
        }

        String rawOtp = otpService.generateOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);
        LocalDateTime expiry = otpService.getExpiryTime();

        session.setAttribute(SESSION_EMPLOYEE_ID, employee.getId());
        session.setAttribute(SESSION_EMPLOYEE_NAME, employee.getName());
        session.setAttribute(SESSION_EMPLOYEE_IDENTIFIER, identifier.trim());
        session.setAttribute(SESSION_OTP_HASH, hashedOtp);
        session.setAttribute(SESSION_OTP_EXPIRY, expiry);
        session.setAttribute(SESSION_OTP_ATTEMPTS, 0);
        session.setAttribute(SESSION_LAST_SENT, LocalDateTime.now());
        session.removeAttribute(SESSION_VERIFIED);
        session.removeAttribute(SESSION_VERIFIED_EMPLOYEE_ID);

        String adminEmail = getAdminEmail();
        otpEmailService.sendPasswordResetOtp(adminEmail, employee.getName(), identifier.trim(), rawOtp);
        log.info("Password reset OTP initiated for employee ID [{}]", employee.getId());
    }

    /**
     * Resends a new OTP to the configured Super Admin email subject to cooldown.
     */
    public void resendOtp(HttpSession session) {
        Long employeeId = (Long) session.getAttribute(SESSION_EMPLOYEE_ID);
        if (employeeId == null) {
            throw new IllegalStateException("No active password reset request found. Please enter your Employee ID first.");
        }

        LocalDateTime lastSent = (LocalDateTime) session.getAttribute(SESSION_LAST_SENT);
        if (lastSent != null && !otpService.isResendAllowed(lastSent)) {
            throw new IllegalStateException("Please wait 60 seconds before requesting another OTP.");
        }

        User employee = userRepository.findById(employeeId).orElseThrow(() ->
                new IllegalStateException("Employee account not found."));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new IllegalStateException("Employee account is inactive.");
        }

        String rawOtp = otpService.generateOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);
        LocalDateTime expiry = otpService.getExpiryTime();

        session.setAttribute(SESSION_OTP_HASH, hashedOtp);
        session.setAttribute(SESSION_OTP_EXPIRY, expiry);
        session.setAttribute(SESSION_OTP_ATTEMPTS, 0);
        session.setAttribute(SESSION_LAST_SENT, LocalDateTime.now());

        String adminEmail = getAdminEmail();
        String identifier = (String) session.getAttribute(SESSION_EMPLOYEE_IDENTIFIER);
        if (identifier == null) {
            identifier = employee.getEmail();
        }

        otpEmailService.sendPasswordResetOtp(adminEmail, employee.getName(), identifier, rawOtp);
        log.info("Password reset OTP resent for employee ID [{}]", employee.getId());
    }

    /**
     * Verifies the OTP submitted by the user.
     * Enforces expiry and max verification attempts.
     * Immediately invalidates the OTP upon successful verification.
     */
    public void verifyOtp(HttpSession session, String enteredOtp) {
        Long employeeId = (Long) session.getAttribute(SESSION_EMPLOYEE_ID);
        String hashedOtp = (String) session.getAttribute(SESSION_OTP_HASH);
        LocalDateTime expiry = (LocalDateTime) session.getAttribute(SESSION_OTP_EXPIRY);
        Integer attempts = (Integer) session.getAttribute(SESSION_OTP_ATTEMPTS);

        if (employeeId == null || hashedOtp == null || expiry == null) {
            throw new IllegalStateException("No active password reset session found. Please enter your Employee ID first.");
        }

        if (attempts == null) {
            attempts = 0;
        }

        if (otpService.isAttemptsExceeded(attempts)) {
            throw new IllegalStateException("Maximum verification attempts reached. Please request a new OTP.");
        }

        if (otpService.isExpired(expiry)) {
            throw new IllegalStateException("OTP has expired. Please request a new OTP.");
        }

        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter the 6-digit OTP.");
        }

        if (!passwordEncoder.matches(enteredOtp.trim(), hashedOtp)) {
            attempts++;
            session.setAttribute(SESSION_OTP_ATTEMPTS, attempts);
            int remaining = otpService.getMaxAttempts() - attempts;
            if (remaining <= 0) {
                throw new IllegalStateException("Maximum verification attempts reached. Please request a new OTP.");
            } else {
                throw new IllegalArgumentException("Invalid OTP. " + remaining + " attempt(s) remaining.");
            }
        }

        // Invalidate OTP hash and expiry immediately to prevent replay
        session.removeAttribute(SESSION_OTP_HASH);
        session.removeAttribute(SESSION_OTP_EXPIRY);
        session.removeAttribute(SESSION_OTP_ATTEMPTS);

        // Mark reset as verified for this employee
        session.setAttribute(SESSION_VERIFIED, true);
        session.setAttribute(SESSION_VERIFIED_EMPLOYEE_ID, employeeId);
        log.info("Password reset OTP successfully verified for employee ID [{}]", employeeId);
    }

    /**
     * Updates the password for the verified employee account.
     * Validates confirmation match and password policy before persisting with BCrypt hash.
     */
    @Transactional
    public void resetPassword(HttpSession session, String newPassword, String confirmPassword) {
        Boolean verified = (Boolean) session.getAttribute(SESSION_VERIFIED);
        Long verifiedEmployeeId = (Long) session.getAttribute(SESSION_VERIFIED_EMPLOYEE_ID);

        if (!Boolean.TRUE.equals(verified) || verifiedEmployeeId == null) {
            throw new IllegalStateException("Unauthorized. Please verify the OTP before resetting your password.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password is required.");
        }

        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        boolean hasLetter = newPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("Password must contain at least one letter and one number.");
        }

        User employee = userRepository.findById(verifiedEmployeeId).orElseThrow(() ->
                new IllegalStateException("Employee account not found."));

        if (employee.getRole() != Role.EMPLOYEE) {
            throw new IllegalStateException("Target account is not an employee.");
        }

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new IllegalStateException("Employee account is inactive.");
        }

        // Save new hashed password
        employee.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(employee);
        log.info("Password successfully updated for employee ID [{}]", verifiedEmployeeId);

        // Clear all reset session attributes
        clearResetSession(session);
    }

    public boolean isResetInitiated(HttpSession session) {
        return session.getAttribute(SESSION_EMPLOYEE_ID) != null;
    }

    public boolean isResetVerified(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SESSION_VERIFIED))
                && session.getAttribute(SESSION_VERIFIED_EMPLOYEE_ID) != null;
    }

    public String getResetEmployeeName(HttpSession session) {
        String name = (String) session.getAttribute(SESSION_EMPLOYEE_NAME);
        return name != null ? name : "Employee";
    }

    public void clearResetSession(HttpSession session) {
        session.removeAttribute(SESSION_EMPLOYEE_ID);
        session.removeAttribute(SESSION_EMPLOYEE_NAME);
        session.removeAttribute(SESSION_EMPLOYEE_IDENTIFIER);
        session.removeAttribute(SESSION_OTP_HASH);
        session.removeAttribute(SESSION_OTP_EXPIRY);
        session.removeAttribute(SESSION_OTP_ATTEMPTS);
        session.removeAttribute(SESSION_LAST_SENT);
        session.removeAttribute(SESSION_VERIFIED);
        session.removeAttribute(SESSION_VERIFIED_EMPLOYEE_ID);
    }
}
