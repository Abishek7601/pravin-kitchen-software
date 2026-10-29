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

    long countByDivisionId(Long divisionId);

    long countByStatusIn(java.util.Collection<QuotationStatus> statuses);

    long countByBranchId(Long branchId);

    long countByEmployeeId(Long employeeId);

    long countByEmployeeIdAndStatus(
            Long employeeId,
            QuotationStatus status
    );

    List<Quotation> findByEmployeeIdAndStatusOrderByCreatedAtDesc(
            Long employeeId,
            QuotationStatus status
    );

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

    long countByEmployeeIdAndQuotationDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByEmployeeIdAndQuotationDateBetweenOrderByQuotationDateDesc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT q FROM Quotation q WHERE (q.quotationDate IS NOT NULL AND q.quotationDate <= :cutoffDate) OR (q.quotationDate IS NULL AND q.createdAt <= :cutoffDateTime)")
    List<Quotation> findQuotationsEligibleForDeletion(
            @org.springframework.data.repository.query.Param("cutoffDate") LocalDate cutoffDate,
            @org.springframework.data.repository.query.Param("cutoffDateTime") java.time.LocalDateTime cutoffDateTime
    );
}