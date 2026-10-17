package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Division;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DivisionRepository extends JpaRepository<Division, Long> {

    Optional<Division> findByNameIgnoreCaseAndWorkCategoryId(
            String name,
            Long workCategoryId
    );

    boolean existsByNameIgnoreCaseAndWorkCategoryId(
            String name,
            Long workCategoryId
    );

    boolean existsByNameIgnoreCaseAndWorkCategoryIdAndIdNot(
            String name,
            Long workCategoryId,
            Long id
    );

    @Query("SELECT d FROM Division d ORDER BY d.name ASC")
    List<Division> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT d FROM Division d WHERE d.active = true ORDER BY d.name ASC")
    List<Division> findByActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT d FROM Division d WHERE d.workCategory.id = :workCategoryId ORDER BY d.name ASC")
    List<Division> findByWorkCategoryIdOrderByDisplayOrderAsc(
            @Param("workCategoryId") Long workCategoryId
    );

    @Query("SELECT d FROM Division d WHERE d.workCategory.id = :workCategoryId AND d.active = true ORDER BY d.name ASC")
    List<Division> findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("workCategoryId") Long workCategoryId
    );
}