package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.service.CustomerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final BranchRepository branchRepository;

    public CustomerController(
            CustomerService customerService,
            BranchRepository branchRepository) {

        this.customerService = customerService;
        this.branchRepository = branchRepository;
    }


    // ==========================================
    // CUSTOMER LIST
    // ==========================================

    @GetMapping
    public String customers(
            @RequestParam(required = false) String search,
            Model model) {

        if (search != null && !search.trim().isEmpty()) {

            model.addAttribute(
                    "customers",
                    customerService.searchCustomers(search)
            );

        } else {

            model.addAttribute(
                    "customers",
                    customerService.getAllCustomers()
            );
        }

        model.addAttribute(
                "search",
                search == null ? "" : search
        );

        return "admin/customers";
    }


    // ==========================================
    // NEW CUSTOMER
    // ==========================================

    @GetMapping("/new")
    public String newCustomer(Model model) {

        model.addAttribute(
                "customer",
                new Customer()
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        return "admin/customer-form";
    }


    // ==========================================
    // SAVE CUSTOMER
    // ==========================================

    @PostMapping("/save")
    public String saveCustomer(
            @RequestParam String name,
            @RequestParam(required = false) String email,
            @RequestParam String phone,
            @RequestParam(required = false) String address,
            @RequestParam Long branchId) {

        customerService.createCustomer(
                name,
                email,
                phone,
                address,
                branchId
        );

        return "redirect:/admin/customers";
    }


    // ==========================================
    // CUSTOMER DETAILS
    // ==========================================

    @GetMapping("/view/{id}")
    public String viewCustomer(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "customer",
                customerService.getCustomerById(id)
        );

        return "admin/customer-details";
    }


    // ==========================================
    // EDIT CUSTOMER
    // ==========================================

    @GetMapping("/edit/{id}")
    public String editCustomer(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "customer",
                customerService.getCustomerById(id)
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        return "admin/customer-form";
    }


    // ==========================================
    // UPDATE CUSTOMER
    // ==========================================

    @PostMapping("/update/{id}")
    public String updateCustomer(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String email,
            @RequestParam String phone,
            @RequestParam(required = false) String address,
            @RequestParam Long branchId) {

        customerService.updateCustomer(
                id,
                name,
                email,
                phone,
                address,
                branchId
        );

        return "redirect:/admin/customers";
    }


    // ==========================================
    // ACTIVATE / DEACTIVATE
    // ==========================================

    @PostMapping("/toggle/{id}")
    public String toggleCustomer(
            @PathVariable Long id) {

        customerService.toggleCustomerStatus(id);

        return "redirect:/admin/customers";
    }


    @PostMapping("/delete/{id}")
    public String deleteCustomer(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            customerService.deleteCustomer(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Customer deleted successfully."
            );

        } catch (IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/customers";
    }
}