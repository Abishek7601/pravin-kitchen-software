package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/employee/customers")
public class EmployeeCustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public EmployeeCustomerController(
            CustomerRepository customerRepository,
            UserRepository userRepository
    ) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String customers(
            Authentication authentication,
            Model model
    ) {

        String email = authentication.getName();

        User employee = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Employee not found")
                );

        List<Customer> customers;

        if (employee.getBranch() != null) {

            customers = customerRepository
                    .findByBranchId(employee.getBranch().getId());

        } else {

            customers = customerRepository.findByActiveTrue();

        }

        model.addAttribute("employee", employee);
        model.addAttribute("customers", customers);

        return "employee/customers";
    }
}