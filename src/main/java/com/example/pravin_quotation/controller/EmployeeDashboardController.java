package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {

    private final UserRepository userRepository;
    private final QuotationRepository quotationRepository;

    public EmployeeDashboardController(
            UserRepository userRepository,
            QuotationRepository quotationRepository
    ) {
        this.userRepository = userRepository;
        this.quotationRepository = quotationRepository;
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
        // QUOTATION COUNTS
        // =====================================================

        long totalQuotations =
                quotations.size();

        long pendingQuotations =
                quotations.stream()
                        .filter(q ->
                                q.getStatus()
                                        == QuotationStatus.PENDING
                        )
                        .count();

        long sentQuotations =
                quotations.stream()
                        .filter(q ->
                                q.getStatus()
                                        == QuotationStatus.SENT
                        )
                        .count();

        long approvedQuotations =
                quotations.stream()
                        .filter(q ->
                                q.getStatus()
                                        == QuotationStatus.APPROVED
                        )
                        .count();

        // =====================================================
        // TOTAL QUOTED VALUE
        // =====================================================

        BigDecimal totalQuotedValue =
                quotations.stream()
                        .map(Quotation::getGrandTotal)
                        .filter(value ->
                                value != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

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
                "pendingQuotations",
                pendingQuotations
        );

        model.addAttribute(
                "sentQuotations",
                sentQuotations
        );

        model.addAttribute(
                "approvedQuotations",
                approvedQuotations
        );

        model.addAttribute(
                "totalQuotedValue",
                totalQuotedValue
        );

        return "employee/dashboard";
    }
}