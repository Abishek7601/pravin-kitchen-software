package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.WorkCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT w FROM WorkCategory w WHERE (w.archived IS NULL OR w.archived = false) ORDER BY w.displayOrder ASC")
    List<WorkCategory> findNormalCategories();

    @Query("SELECT w FROM WorkCategory w WHERE (w.archived IS NULL OR w.archived = false) AND w.active = true ORDER BY w.displayOrder ASC")
    List<WorkCategory> findByActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT w FROM WorkCategory w WHERE w.archived = true ORDER BY w.displayOrder ASC")
    List<WorkCategory> findArchivedCategories();

    @Query("SELECT COUNT(w) FROM WorkCategory w WHERE w.archived = true")
    long countArchivedCategories();
}