package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByQuotationNumber(String quotationNumber);

    boolean existsByQuotationNumber(String quotationNumber);

    List<Quotation> findAllByOrderByCreatedAtDesc();

    List<Quotation> findByStatusOrderByCreatedAtDesc(
            QuotationStatus status
    );

    List<Quotation> findByCustomerIdOrderByCreatedAtDesc(
            Long customerId
    );

    List<Quotation> findByEmployeeIdOrderByCreatedAtDesc(
            Long employeeId
    );

    List<Quotation> findByBranchIdOrderByCreatedAtDesc(
            Long branchId
    );

    List<Quotation> findByQuotationDateBetweenOrderByQuotationDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByBranchIdAndQuotationDateBetweenOrderByQuotationDateDesc(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByStatusAndQuotationDateBetweenOrderByQuotationDateDesc(
            QuotationStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByBranchIdAndStatusOrderByCreatedAtDesc(
            Long branchId,
            QuotationStatus status
    );

    long countByStatus(QuotationStatus status);

    long countByBranchId(Long branchId);

    long countByEmployeeId(Long employeeId);

    long countByCustomerId(Long customerId);

    long countByBranchIdAndStatus(
            Long branchId,
            QuotationStatus status
    );

    long countByQuotationDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    long countByBranchIdAndQuotationDateBetween(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );
}