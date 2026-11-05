package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.UserRepository;
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
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "search", required = false) String search,
            Authentication authentication,
            Model model
    ) {
        String email = authentication.getName();

        User employee = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("Employee not found")
                );

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by(Sort.Direction.ASC, "name"));

        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Customer> customerPage;
        if (employee.getBranch() != null) {
            customerPage = customerRepository.searchBranchCustomers(employee.getBranch().getId(), cleanSearch, pageable);
        } else {
            customerPage = Page.empty(pageable);
        }

        List<Customer> customers = customerPage.getContent();
        long totalRecords = customerPage.getTotalElements();
        int totalPages = customerPage.getTotalPages();
        int currentPage = customerPage.getNumber() + 1;
        long pageStart = totalRecords == 0 ? 0 : ((long) customerPage.getNumber() * 10 + 1);
        long pageEnd = Math.min((long) (customerPage.getNumber() + 1) * 10, totalRecords);

        model.addAttribute("employee", employee);
        model.addAttribute("customers", customers);
        model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("totalElements", totalRecords);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);
        model.addAttribute("hasPrevious", customerPage.hasPrevious());
        model.addAttribute("hasNext", customerPage.hasNext());
        model.addAttribute("pageNumbers", PaginationUtils.getPageNumbers(currentPage, totalPages));
        model.addAttribute("search", cleanSearch != null ? cleanSearch : "");

        return "employee/customers";
    }

    public String customers(
            Authentication authentication,
            Model model
    ) {
        return customers(1, null, authentication, model);
    }
}
