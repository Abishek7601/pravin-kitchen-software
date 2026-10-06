package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository
        extends JpaRepository<Material, Long> {

    Optional<Material> findByNameIgnoreCaseAndItemId(
            String name,
            Long itemId
    );

    boolean existsByNameIgnoreCaseAndItemId(
            String name,
            Long itemId
    );

    boolean existsByNameIgnoreCaseAndItemIdAndIdNot(
            String name,
            Long itemId,
            Long id
    );

    List<Material> findAllByOrderByDisplayOrderAsc();

    List<Material> findByActiveTrueOrderByDisplayOrderAsc();

    List<Material> findByItemIdOrderByDisplayOrderAsc(
            Long itemId
    );

    List<Material> findByItemIdAndActiveTrueOrderByDisplayOrderAsc(
            Long itemId
    );
}