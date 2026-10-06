package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final BranchRepository branchRepository;
    private final QuotationRepository quotationRepository;

    public EmployeeController(
            EmployeeService employeeService,
            BranchRepository branchRepository,
            QuotationRepository quotationRepository) {

        this.employeeService = employeeService;
        this.branchRepository = branchRepository;
        this.quotationRepository = quotationRepository;
    }

    // =========================
    // EMPLOYEE LIST
    // =========================

    @GetMapping
    public String employees(Model model) {

        model.addAttribute(
                "employees",
                employeeService.getAllEmployees()
        );

        return "admin/employees";
    }

    // =========================
    // NEW EMPLOYEE
    // =========================

    @GetMapping("/new")
    public String newEmployee(
            @RequestParam(required = false) Long branchId,
            Model model) {

        model.addAttribute("employee", new User());

        model.addAttribute(
                "branches",
                branchRepository.findByActiveTrueOrderByNameAsc()
        );

        model.addAttribute("selectedBranchId", branchId);

        return "admin/employee-form";
    }

    // =========================
    // SAVE EMPLOYEE
    // =========================

    @PostMapping("/save")
    public String saveEmployee(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Long branchId) {

        employeeService.createEmployee(
                name,
                email,
                password,
                branchId
        );

        return "redirect:/admin/employees";
    }

    // =========================
    // EDIT EMPLOYEE PAGE
    // =========================

    @GetMapping("/edit/{id}")
    public String editEmployee(
            @PathVariable Long id,
            Model model) {

        User employee = employeeService.getEmployeeById(id);

        List<Branch> branches = new ArrayList<>(
                branchRepository.findByActiveTrueOrderByNameAsc()
        );

        Branch currentBranch = employee.getBranch();

        if (currentBranch != null
                && branches.stream().noneMatch(
                branch -> branch.getId().equals(currentBranch.getId()))) {

            branches.add(currentBranch);
        }

        model.addAttribute("employee", employee);
        model.addAttribute("branches", branches);

        return "admin/employee-form";
    }

    // =========================
    // UPDATE EMPLOYEE
    // =========================

    @PostMapping("/update/{id}")
    public String updateEmployee(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam Long branchId) {

        employeeService.updateEmployee(
                id,
                name,
                email,
                branchId
        );

        return "redirect:/admin/employees";
    }

    // =========================
    // ACTIVATE / DEACTIVATE
    // =========================

    @PostMapping("/toggle/{id}")
    public String toggleEmployee(
            @PathVariable Long id) {

        employeeService.toggleEmployeeStatus(id);

        return "redirect:/admin/employees";
    }

    // =========================
    // VIEW EMPLOYEE
    // =========================

    @GetMapping("/view/{id}")
    public String viewEmployee(
            @PathVariable Long id,
            Model model) {

        User employee =
                employeeService.getEmployeeById(id);

        /*
         * Get all quotations created/managed
         * by this employee.
         */
        List<Quotation> quotations =
                quotationRepository
                        .findByEmployeeIdOrderByCreatedAtDesc(id);

        /*
         * Total quotations
         */
        long totalQuotations =
                quotations.size();

        /*
         * Last 60 days
         */
        LocalDate today =
                LocalDate.now();

        LocalDate sixtyDaysAgo =
                today.minusDays(59);

        long last60Days =
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
                        .count();

        /*
         * Total quoted value
         */
        BigDecimal quotedValue =
                quotations.stream()
                        .map(Quotation::getGrandTotal)
                        .filter(value -> value != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        /*
         * Approved quotations
         */
        long approvedCount =
                quotations.stream()
                        .filter(q ->
                                q.getStatus() ==
                                        QuotationStatus.APPROVED
                        )
                        .count();

        /*
         * Send employee details
         */
        model.addAttribute(
                "employee",
                employee
        );

        /*
         * Send quotations
         */
        model.addAttribute(
                "quotations",
                quotations
        );

        /*
         * Send dashboard numbers
         */
        model.addAttribute(
                "totalQuotations",
                totalQuotations
        );

        model.addAttribute(
                "last60Days",
                last60Days
        );

        model.addAttribute(
                "quotedValue",
                quotedValue
        );

        model.addAttribute(
                "approvedCount",
                approvedCount
        );

        return "admin/employee-details";
    }

    // =========================
    // DELETE EMPLOYEE
    // =========================

    @PostMapping("/delete/{id}")
    public String deleteEmployee(
            @PathVariable Long id) {

        employeeService.deleteEmployee(id);

        return "redirect:/admin/employees";
    }
}