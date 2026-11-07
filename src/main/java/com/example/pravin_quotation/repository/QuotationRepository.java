package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByQuotationNumber(String quotationNumber);

    boolean existsByQuotationNumber(String quotationNumber);

    // Multi-grade child queries
    List<Quotation> findByParentIdOrderByIdAsc(Long parentId);

    Optional<Quotation> findByParentIdAndPricingMode(Long parentId, PricingMode pricingMode);

    boolean existsByParentIdAndPricingMode(Long parentId, PricingMode pricingMode);

    // Queries scoped to parent quotations (and legacy single-grade quotations)
    List<Quotation> findByParentIsNullOrderByCreatedAtDesc();

    List<Quotation> findByParentIsNullAndStatusOrderByCreatedAtDesc(
            QuotationStatus status
    );

    List<Quotation> findByParentIsNullAndCustomerIdOrderByCreatedAtDesc(
            Long customerId
    );

    List<Quotation> findByParentIsNullAndEmployeeIdOrderByCreatedAtDesc(
            Long employeeId
    );

    List<Quotation> findByParentIsNullAndBranchIdOrderByCreatedAtDesc(
            Long branchId
    );

    List<Quotation> findByParentIsNullAndQuotationDateBetweenOrderByQuotationDateDesc(
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByParentIsNullAndBranchIdAndQuotationDateBetweenOrderByQuotationDateDesc(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByParentIsNullAndStatusAndQuotationDateBetweenOrderByQuotationDateDesc(
            QuotationStatus status,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Quotation> findByParentIsNullAndBranchIdAndStatusOrderByCreatedAtDesc(
            Long branchId,
            QuotationStatus status
    );

    List<Quotation> findByParentIsNullAndEmployeeIdAndStatusOrderByCreatedAtDesc(
            Long employeeId,
            QuotationStatus status
    );

    List<Quotation> findByParentIsNullAndEmployeeIdAndQuotationDateBetweenOrderByQuotationDateDesc(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByParentIsNull();

    long countByParentIsNullAndStatus(QuotationStatus status);

    long countByParentIsNullAndBranchId(Long branchId);

    long countByParentIsNullAndEmployeeId(Long employeeId);

    long countByParentIsNullAndEmployeeIdAndStatus(
            Long employeeId,
            QuotationStatus status
    );

    long countByParentIsNullAndBranchIdAndStatus(
            Long branchId,
            QuotationStatus status
    );

    long countByParentIsNullAndQuotationDateBetween(
            LocalDate startDate,
            LocalDate endDate
    );

    long countByParentIsNullAndBranchIdAndQuotationDateBetween(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByParentIsNullAndEmployeeIdAndQuotationDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Legacy queries kept for backwards compatibility
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

    @Query("SELECT q FROM Quotation q WHERE (q.quotationDate IS NOT NULL AND q.quotationDate <= :cutoffDate) OR (q.quotationDate IS NULL AND q.createdAt <= :cutoffDateTime)")
    List<Quotation> findQuotationsEligibleForDeletion(
            @Param("cutoffDate") LocalDate cutoffDate,
            @Param("cutoffDateTime") java.time.LocalDateTime cutoffDateTime
    );

    Page<Quotation> findByEmployeeId(Long employeeId, Pageable pageable);

    @Query("SELECT q FROM Quotation q LEFT JOIN q.customer c WHERE q.employee.id = :employeeId " +
           "AND q.parent IS NULL " +
           "AND (:status IS NULL OR q.status = :status) " +
           "AND (:startDate IS NULL OR q.quotationDate >= :startDate) " +
           "AND (:endDate IS NULL OR q.quotationDate <= :endDate) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(q.quotationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Quotation> searchEmployeeQuotations(
            @Param("employeeId") Long employeeId,
            @Param("status") QuotationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable
    );
}