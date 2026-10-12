package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.service.BranchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/branches")
public class BranchController {

    private final BranchService branchService;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;

    public BranchController(
            BranchService branchService,
            QuotationRepository quotationRepository,
            CustomerRepository customerRepository) {

        this.branchService = branchService;
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
    }

    // =========================
    // BRANCH LIST
    // =========================

    @GetMapping
    public String branches(Model model) {

        model.addAttribute(
                "branches",
                branchService.getAllBranches()
        );

        return "admin/branches";
    }

    // =========================
    // NEW BRANCH
    // =========================

    @GetMapping("/new")
    public String newBranch(Model model) {

        model.addAttribute(
                "branch",
                new Branch()
        );

        return "admin/branch-form";
    }

    // =========================
    // SAVE BRANCH
    // =========================

    @PostMapping("/save")
    public String saveBranch(
            @ModelAttribute("branch") Branch branch) {

        branchService.saveBranch(branch);

        return "redirect:/admin/branches";
    }

    // =========================
    // EDIT BRANCH
    // =========================

    @GetMapping("/edit/{id}")
    public String editBranch(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "branch",
                branchService.getBranchById(id)
        );

        return "admin/branch-form";
    }

    // =========================
    // ACTIVATE / DEACTIVATE
    // =========================

    @PostMapping("/toggle/{id}")
    public String toggleBranch(
            @PathVariable Long id) {

        branchService.toggleBranchStatus(id);

        return "redirect:/admin/branches";
    }

    // =========================
    // VIEW BRANCH / DISTRICT
    // =========================

    @GetMapping("/view/{id}")
    public String viewBranch(
            @PathVariable Long id,
            @RequestParam(required = false) QuotationStatus status,
            Model model) {

        // ---------------------------------
        // BRANCH
        // ---------------------------------

        Branch branch =
                branchService.getBranchById(id);

        // ---------------------------------
        // EMPLOYEES
        // ---------------------------------

        model.addAttribute(
                "branch",
                branch
        );

        model.addAttribute(
                "employees",
                branchService.getEmployeesByBranch(id)
        );

        // ---------------------------------
        // DISTRICT-SPECIFIC STATUS COUNTS
        // ---------------------------------

        long pendingCount =
                quotationRepository.countByBranchIdAndStatus(id, QuotationStatus.PENDING);

        long workingCount =
                quotationRepository.countByBranchIdAndStatus(id, QuotationStatus.WORKING);

        long sentCount =
                quotationRepository.countByBranchIdAndStatus(id, QuotationStatus.SENT);

        model.addAttribute(
                "pendingCount",
                pendingCount
        );

        model.addAttribute(
                "workingCount",
                workingCount
        );

        model.addAttribute(
                "sentCount",
                sentCount
        );

        model.addAttribute(
                "selectedStatus",
                status
        );

        // ---------------------------------
        // QUOTATIONS
        // ---------------------------------

        List<Quotation> allQuotations =
                quotationRepository.findByBranchIdOrderByCreatedAtDesc(id);

        List<Quotation> quotations = (status != null)
                ? quotationRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(id, status)
                : allQuotations;

        model.addAttribute(
                "quotations",
                quotations
        );

        // ---------------------------------
        // CUSTOMERS
        // ---------------------------------

        List<Customer> customers =
                customerRepository.findByBranchId(id);

        model.addAttribute(
                "customers",
                customers
        );

        // ---------------------------------
        // DATE RANGE - LAST 60 DAYS
        // ---------------------------------

        LocalDate today = LocalDate.now();

        LocalDate sixtyDaysAgo =
                today.minusDays(59);

        List<Quotation> last60DayQuotations =
                quotations.stream()
                        .filter(q ->
                                q.getQuotationDate() != null
                                        &&
                                        !q.getQuotationDate()
                                                .isBefore(sixtyDaysAgo)
                                        &&
                                        !q.getQuotationDate()
                                                .isAfter(today)
                        )
                        .toList();

        // ---------------------------------
        // 60 DAY QUOTATION COUNT
        // ---------------------------------

        long last60DaysCount =
                last60DayQuotations.size();

        model.addAttribute(
                "last60DaysCount",
                last60DaysCount
        );

        // ---------------------------------
        // 60 DAY QUOTED VALUE
        // ---------------------------------

        BigDecimal last60DaysValue =
                last60DayQuotations.stream()
                        .map(Quotation::getGrandTotal)
                        .filter(value -> value != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute(
                "last60DaysValue",
                last60DaysValue
        );

        // ---------------------------------
        // APPROVED QUOTATIONS
        // ---------------------------------

        List<Quotation> approvedQuotations =
                quotations.stream()
                        .filter(q ->
                                q.getStatus() ==
                                        QuotationStatus.SENT
                        )
                        .toList();

        model.addAttribute(
                "approvedQuotations",
                approvedQuotations
        );

        // ---------------------------------
        // APPROVED QUOTATION COUNT
        // ---------------------------------

        long approvedCount =
                approvedQuotations.size();

        model.addAttribute(
                "approvedCount",
                approvedCount
        );

        // ---------------------------------
        // APPROVED QUOTED VALUE
        // ---------------------------------

        BigDecimal approvedValue =
                approvedQuotations.stream()
                        .map(Quotation::getGrandTotal)
                        .filter(value -> value != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute(
                "approvedValue",
                approvedValue
        );

        // ---------------------------------
        // TOTAL QUOTED VALUE
        // ---------------------------------

        BigDecimal totalQuotedValue =
                quotations.stream()
                        .map(Quotation::getGrandTotal)
                        .filter(value -> value != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute(
                "totalQuotedValue",
                totalQuotedValue
        );

        // ---------------------------------
        // TOTAL QUOTATION COUNT
        // ---------------------------------

        long totalQuotationCount =
                quotations.size();

        model.addAttribute(
                "totalQuotationCount",
                totalQuotationCount
        );

        // ---------------------------------
        // RETURN PAGE
        // ---------------------------------

        return "admin/branch-details";
    }

    // =========================================================
    // DISTRICT-WISE QUOTATIONS BY STATUS
    // =========================================================

    @GetMapping("/{id}/quotations/status/{status}")
    public String viewBranchQuotationsByStatus(
            @PathVariable Long id,
            @PathVariable QuotationStatus status,
            Model model) {

        Branch branch =
                branchService.getBranchById(id);

        List<Quotation> quotations =
                quotationRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(id, status);

        long pendingCount =
                quotationRepository.countByBranchIdAndStatus(id, QuotationStatus.PENDING);

        long workingCount =
                quotationRepository.countByBranchIdAndStatus(id, QuotationStatus.WORKING);

        long sentCount =
                quotationRepository.countByBranchIdAndStatus(id, QuotationStatus.SENT);

        model.addAttribute("branch", branch);
        model.addAttribute("quotations", quotations);
        model.addAttribute("quotationList", quotations);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("workingCount", workingCount);
        model.addAttribute("sentCount", sentCount);

        return "admin/branch-quotations";
    }

    // =========================
    // DELETE BRANCH
    // =========================

    @PostMapping("/delete/{id}")
    public String deleteBranch(
            @PathVariable Long id) {

        branchService.deleteBranch(id);

        return "redirect:/admin/branches";
    }
}