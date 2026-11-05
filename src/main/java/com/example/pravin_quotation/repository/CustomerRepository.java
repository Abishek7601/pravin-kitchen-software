package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBranchId(Long branchId);

    Page<Customer> findByBranchId(Long branchId, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.branch.id = :branchId AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    Page<Customer> searchBranchCustomers(
            @Param("branchId") Long branchId,
            @Param("search") String search,
            Pageable pageable
    );

    List<Customer> findByBranchIdOrderByNameAsc(Long branchId);

    List<Customer> findByBranchIdAndNameContainingIgnoreCaseOrderByNameAsc(Long branchId, String name);

    List<Customer> findByBranchIdAndActiveTrueOrderByNameAsc(Long branchId);

    List<Customer> findByActiveTrue();

    List<Customer> findByActiveTrueOrderByNameAsc();

    List<Customer> findByNameContainingIgnoreCase(String name);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);

    long countByActiveTrue();

    long countByBranchId(Long branchId);

    @Query("SELECT c.branch.id, c.branch.name, COUNT(DISTINCT c.id) FROM Customer c WHERE c.branch IS NOT NULL GROUP BY c.branch.id, c.branch.name")
    List<Object[]> countDistinctCustomersGroupedByBranch();

    @Query("SELECT COUNT(DISTINCT c.branch.id) FROM Customer c WHERE c.branch IS NOT NULL")
    long countDistinctBranchesWithCustomers();
}
