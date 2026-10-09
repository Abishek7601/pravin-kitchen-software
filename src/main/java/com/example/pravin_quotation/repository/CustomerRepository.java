package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBranchId(Long branchId);

    List<Customer> findByActiveTrue();

    List<Customer> findByNameContainingIgnoreCase(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<Customer> findByPhone(String phone);
}