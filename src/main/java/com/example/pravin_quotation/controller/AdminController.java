
package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final QuotationRepository quotationRepository;

    public AdminController(
            AdminDashboardService adminDashboardService,
            QuotationRepository quotationRepository) {
        this.adminDashboardService = adminDashboardService;
        this.quotationRepository = quotationRepository;
    }

    // ADMIN DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("totalEmployees",
                adminDashboardService.getTotalEmployees());

        model.addAttribute("totalBranches",
                adminDashboardService.getTotalBranches());

        model.addAttribute("last60DaysQuotationCount",
                adminDashboardService.getLast60DaysQuotationCount());

        model.addAttribute("last60DaysQuotedValue",
                adminDashboardService.getLast60DaysQuotedValue());

        model.addAttribute("oneYearCustomerCount",
                adminDashboardService.getOneYearCustomerCount());

        model.addAttribute("approvedCustomerCount",
                adminDashboardService.getApprovedCustomerCount());

        model.addAttribute("totalPortfolioValue",
                adminDashboardService.getTotalPortfolioValue());

        return "admin/dashboard";
    }

    // LAST 60 DAYS QUOTATION VALUE
    @GetMapping("/quotations/value")
    public String quotationValue(Model model) {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(59);

        List<Quotation> quotations =
                quotationRepository
                        .findByQuotationDateBetweenOrderByQuotationDateDesc(
                                startDate,
                                endDate
                        );

        BigDecimal totalQuotedValue = quotations.stream()
                .map(Quotation::getGrandTotal)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("quotations", quotations);
        model.addAttribute("totalQuotedValue", totalQuotedValue);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/quotation-value";
    }

    // PORTFOLIO PAGE

    @GetMapping("/portfolio")
    public String portfolio(Model model) {

        List<Quotation> quotations = quotationRepository.findAll();

        BigDecimal totalPortfolioValue = quotations.stream()
                .map(Quotation::getGrandTotal)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("quotations", quotations);
        model.addAttribute("totalPortfolioValue", totalPortfolioValue);

        return "admin/portfolio";
    }
}