package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBranchId(Long branchId);

    List<Customer> findByBranchIdAndActiveTrueOrderByNameAsc(Long branchId);

    List<Customer> findByActiveTrue();

    List<Customer> findByActiveTrueOrderByNameAsc();

    List<Customer> findByNameContainingIgnoreCase(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);

    long countByActiveTrue();

    long countByBranchId(Long branchId);

    @Query("SELECT c.branch.id, c.branch.name, COUNT(DISTINCT c.id) FROM Customer c WHERE c.branch IS NOT NULL GROUP BY c.branch.id, c.branch.name")
    List<Object[]> countDistinctCustomersGroupedByBranch();

    @Query("SELECT COUNT(DISTINCT c.branch.id) FROM Customer c WHERE c.branch IS NOT NULL")
    long countDistinctBranchesWithCustomers();
}