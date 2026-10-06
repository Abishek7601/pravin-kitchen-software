package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Division;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<Division> findAllByOrderByDisplayOrderAsc();

    List<Division> findByActiveTrueOrderByDisplayOrderAsc();

    List<Division> findByWorkCategoryIdOrderByDisplayOrderAsc(
            Long workCategoryId
    );

    List<Division> findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(
            Long workCategoryId
    );
}