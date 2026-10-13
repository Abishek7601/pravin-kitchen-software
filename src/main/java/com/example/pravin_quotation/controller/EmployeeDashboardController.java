package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.EmployeeDashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {

    private final UserRepository userRepository;
    private final QuotationRepository quotationRepository;
    private final EmployeeDashboardService employeeDashboardService;

    public EmployeeDashboardController(
            UserRepository userRepository,
            QuotationRepository quotationRepository,
            EmployeeDashboardService employeeDashboardService
    ) {
        this.userRepository = userRepository;
        this.quotationRepository = quotationRepository;
        this.employeeDashboardService = employeeDashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model
    ) {

        // =====================================================
        // GET LOGGED-IN EMPLOYEE
        // =====================================================

        String email = authentication.getName();

        User employee = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Employee not found"
                        )
                );

        // =====================================================
        // GET EMPLOYEE QUOTATIONS
        // =====================================================

        List<Quotation> quotations =
                quotationRepository
                        .findByEmployeeIdOrderByCreatedAtDesc(
                                employee.getId()
                        );

        // =====================================================
        // QUOTATION SUMMARY COUNTS (SCOPED TO LOGGED-IN EMPLOYEE)
        // =====================================================

        long last60DaysQuotations =
                employeeDashboardService.getLast60DaysQuotationCount(employee.getId());

        long oneYearQuotations =
                employeeDashboardService.getOneYearQuotationCount(employee.getId());

        long pendingQuotations =
                employeeDashboardService.getPendingQuotationCount(employee.getId());

        long workingQuotations =
                employeeDashboardService.getWorkingQuotationCount(employee.getId());

        long sentQuotations =
                employeeDashboardService.getSentQuotationCount(employee.getId());

        long totalQuotations =
                quotations.size();

        // =====================================================
        // RECENT QUOTATIONS
        // =====================================================

        List<Quotation> recentQuotations =
                quotations.stream()
                        .limit(5)
                        .toList();

        // =====================================================
        // SEND DATA TO THYMELEAF
        // =====================================================

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "quotations",
                quotations
        );

        model.addAttribute(
                "recentQuotations",
                recentQuotations
        );

        model.addAttribute(
                "totalQuotations",
                totalQuotations
        );

        model.addAttribute(
                "last60DaysQuotations",
                last60DaysQuotations
        );

        model.addAttribute(
                "oneYearQuotations",
                oneYearQuotations
        );

        model.addAttribute(
                "totalOneYearQuotations",
                oneYearQuotations
        );

        model.addAttribute(
                "pendingQuotations",
                pendingQuotations
        );

        model.addAttribute(
                "workingQuotations",
                workingQuotations
        );

        model.addAttribute(
                "sentQuotations",
                sentQuotations
        );

        model.addAttribute(
                "approvedQuotations",
                workingQuotations
        );

        return "employee/dashboard";
    }
}