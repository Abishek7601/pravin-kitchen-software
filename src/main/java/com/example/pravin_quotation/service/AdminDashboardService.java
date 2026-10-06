package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            BranchRepository branchRepository,
            QuotationRepository quotationRepository,
            CustomerRepository customerRepository
    ) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
    }

    // ==============================
    // TOTAL EMPLOYEES
    // ==============================

    public long getTotalEmployees() {
        return userRepository.count();
    }

    // ==============================
    // TOTAL BRANCHES
    // ==============================

    public long getTotalBranches() {
        return branchRepository.count();
    }

    // ==============================
    // LAST 60 DAYS QUOTATIONS
    // ==============================

    public long getLast60DaysQuotationCount() {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(59);

        return quotationRepository.countByQuotationDateBetween(
                startDate,
                endDate
        );
    }

    // ==============================
    // LAST 60 DAYS QUOTATION LIST
    // ==============================

    public List<Quotation> getLast60DaysQuotations() {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(59);

        return quotationRepository
                .findByQuotationDateBetweenOrderByQuotationDateDesc(
                        startDate,
                        endDate
                );
    }

    // ==============================
    // LAST 60 DAYS QUOTED VALUE
    // ==============================

    public BigDecimal getLast60DaysQuotedValue() {

        List<Quotation> quotations = getLast60DaysQuotations();

        return quotations.stream()
                .map(quotation -> {

                    if (quotation.getGrandTotal() == null) {
                        return BigDecimal.ZERO;
                    }

                    return quotation.getGrandTotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==============================
    // ONE YEAR CUSTOMERS
    // ==============================

    public long getOneYearCustomerCount() {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);

        List<Quotation> quotations =
                quotationRepository
                        .findByQuotationDateBetweenOrderByQuotationDateDesc(
                                startDate,
                                endDate
                        );

        return quotations.stream()
                .map(quotation -> quotation.getCustomer())
                .filter(customer -> customer != null)
                .map(customer -> customer.getId())
                .distinct()
                .count();
    }

    // ==============================
    // ONE YEAR CUSTOMER QUOTATIONS
    // ==============================

    public List<Quotation> getOneYearCustomerQuotations() {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);

        return quotationRepository
                .findByQuotationDateBetweenOrderByQuotationDateDesc(
                        startDate,
                        endDate
                );
    }

    // ==============================
    // APPROVED CUSTOMERS
    // ==============================

    public long getApprovedCustomerCount() {

        return quotationRepository
                .findByStatusOrderByCreatedAtDesc(
                        com.example.pravin_quotation.model.QuotationStatus.APPROVED
                )
                .stream()
                .map(quotation -> quotation.getCustomer())
                .filter(customer -> customer != null)
                .map(customer -> customer.getId())
                .distinct()
                .count();
    }

    // ==============================
    // APPROVED CUSTOMER QUOTATIONS
    // ==============================

    public List<Quotation> getApprovedQuotations() {

        return quotationRepository
                .findByStatusOrderByCreatedAtDesc(
                        com.example.pravin_quotation.model.QuotationStatus.APPROVED
                );
    }

    // ==============================
    // TOTAL PORTFOLIO VALUE
    // ==============================

    public BigDecimal getTotalPortfolioValue() {

        return quotationRepository
                .findAll()
                .stream()
                .map(quotation -> {

                    if (quotation.getGrandTotal() == null) {
                        return BigDecimal.ZERO;
                    }

                    return quotation.getGrandTotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}