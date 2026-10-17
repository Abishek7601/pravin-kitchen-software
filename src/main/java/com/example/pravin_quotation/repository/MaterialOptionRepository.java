package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.MaterialOption;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialOptionRepository
        extends JpaRepository<MaterialOption, Long> {

    Optional<MaterialOption> findByNameIgnoreCaseAndMaterialId(
            String name,
            Long materialId
    );

    boolean existsByNameIgnoreCaseAndMaterialId(
            String name,
            Long materialId
    );

    boolean existsByNameIgnoreCaseAndMaterialIdAndIdNot(
            String name,
            Long materialId,
            Long id
    );

    @Query("SELECT o FROM MaterialOption o ORDER BY o.name ASC")
    List<MaterialOption> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT o FROM MaterialOption o WHERE o.active = true ORDER BY o.name ASC")
    List<MaterialOption> findByActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT o FROM MaterialOption o WHERE o.material.id = :materialId ORDER BY o.name ASC")
    List<MaterialOption> findByMaterialIdOrderByDisplayOrderAsc(
            @Param("materialId") Long materialId
    );

    @Query("SELECT o FROM MaterialOption o WHERE o.material.id = :materialId AND o.active = true ORDER BY o.name ASC")
    List<MaterialOption> findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("materialId") Long materialId
    );
}