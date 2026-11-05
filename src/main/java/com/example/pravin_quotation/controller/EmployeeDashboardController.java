package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.EmployeeDashboardService;
import com.example.pravin_quotation.util.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
            @RequestParam(name = "page", defaultValue = "1") int page,
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
        // RECENT QUOTATIONS (PAGINATED AT 10 PER PAGE)
        // =====================================================

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Quotation> recentQuotationsPage = quotationRepository.findByEmployeeId(employee.getId(), pageable);
        List<Quotation> recentQuotations = recentQuotationsPage.getContent();
        long totalRecords = recentQuotationsPage.getTotalElements();
        int totalPages = recentQuotationsPage.getTotalPages();
        int currentPage = recentQuotationsPage.getNumber() + 1;
        long pageStart = totalRecords == 0 ? 0 : ((long) recentQuotationsPage.getNumber() * 10 + 1);
        long pageEnd = Math.min((long) (recentQuotationsPage.getNumber() + 1) * 10, totalRecords);

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

        long totalQuotations = totalRecords;

        // =====================================================
        // SEND DATA TO THYMELEAF
        // =====================================================

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "quotations",
                recentQuotations
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
                "totalRecords",
                totalRecords
        );

        model.addAttribute(
                "totalElements",
                totalRecords
        );

        model.addAttribute(
                "totalPages",
                totalPages
        );

        model.addAttribute(
                "currentPage",
                currentPage
        );

        model.addAttribute(
                "pageStart",
                pageStart
        );

        model.addAttribute(
                "pageEnd",
                pageEnd
        );

        model.addAttribute(
                "hasPrevious",
                recentQuotationsPage.hasPrevious()
        );

        model.addAttribute(
                "hasNext",
                recentQuotationsPage.hasNext()
        );

        model.addAttribute(
                "pageNumbers",
                PaginationUtils.getPageNumbers(currentPage, totalPages)
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

    public String dashboard(
            Authentication authentication,
            Model model
    ) {
        return dashboard(1, authentication, model);
    }
}
