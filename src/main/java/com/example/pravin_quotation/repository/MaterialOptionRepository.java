package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.MaterialOption;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<MaterialOption> findAllByOrderByDisplayOrderAsc();

    List<MaterialOption> findByActiveTrueOrderByDisplayOrderAsc();

    List<MaterialOption> findByMaterialIdOrderByDisplayOrderAsc(
            Long materialId
    );

    List<MaterialOption> findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(
            Long materialId
    );
}