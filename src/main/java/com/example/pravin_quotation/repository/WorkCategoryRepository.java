package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.WorkCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkCategoryRepository
        extends JpaRepository<WorkCategory, Long> {

    Optional<WorkCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );

    List<WorkCategory> findAllByOrderByDisplayOrderAsc();

    List<WorkCategory> findByActiveTrueOrderByDisplayOrderAsc();
}