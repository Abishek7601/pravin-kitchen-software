package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class EmployeeDashboardService {

    private final QuotationRepository quotationRepository;

    public EmployeeDashboardService(QuotationRepository quotationRepository) {
        this.quotationRepository = quotationRepository;
    }

    // =========================================================
    // 1. LAST 60 DAYS QUOTATIONS COUNT
    // =========================================================

    public long getLast60DaysQuotationCount(Long employeeId) {
        return getLast60DaysQuotationCount(employeeId, LocalDate.now());
    }

    public long getLast60DaysQuotationCount(Long employeeId, LocalDate today) {
        if (employeeId == null || today == null) {
            return 0;
        }

        LocalDate startDate = today.minusDays(59);

        return quotationRepository.countByEmployeeIdAndQuotationDateBetween(
                employeeId,
                startDate,
                today
        );
    }

    // =========================================================
    // 2. TOTAL ONE YEAR QUOTATIONS COUNT
    // =========================================================

    public long getOneYearQuotationCount(Long employeeId) {
        return getOneYearQuotationCount(employeeId, LocalDate.now());
    }

    public long getOneYearQuotationCount(Long employeeId, LocalDate today) {
        if (employeeId == null || today == null) {
            return 0;
        }

        LocalDate startDate = today.minusYears(1);

        return quotationRepository.countByEmployeeIdAndQuotationDateBetween(
                employeeId,
                startDate,
                today
        );
    }

    // =========================================================
    // 3. PENDING QUOTATIONS COUNT
    // =========================================================

    public long getPendingQuotationCount(Long employeeId) {
        if (employeeId == null) {
            return 0;
        }

        return quotationRepository.countByEmployeeIdAndStatus(
                employeeId,
                QuotationStatus.PENDING
        );
    }

    // =========================================================
    // 4. WORKING QUOTATIONS COUNT
    // =========================================================

    public long getWorkingQuotationCount(Long employeeId) {
        if (employeeId == null) {
            return 0;
        }

        return quotationRepository.countByEmployeeIdAndStatus(
                employeeId,
                QuotationStatus.WORKING
        );
    }

    // =========================================================
    // 5. SENT QUOTATIONS COUNT
    // =========================================================

    public long getSentQuotationCount(Long employeeId) {
        if (employeeId == null) {
            return 0;
        }

        return quotationRepository.countByEmployeeIdAndStatus(
                employeeId,
                QuotationStatus.SENT
        );
    }
}
