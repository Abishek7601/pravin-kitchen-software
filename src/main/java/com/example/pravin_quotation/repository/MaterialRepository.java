package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository
        extends JpaRepository<Material, Long> {

    Optional<Material> findByNameIgnoreCaseAndItemId(
            String name,
            Long itemId
    );

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

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

    List<Material> findByDivisionsIdAndActiveTrueOrderByDisplayOrderAsc(
            Long divisionId
    );

    List<Material> findByDivisionsIdOrderByDisplayOrderAsc(
            Long divisionId
    );

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (d.id = :divisionId OR i.division.id = :divisionId) AND m.active = true ORDER BY m.displayOrder ASC")
    List<Material> findActiveByDivisionId(@Param("divisionId") Long divisionId);

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (d.id = :divisionId OR i.division.id = :divisionId) ORDER BY m.displayOrder ASC")
    List<Material> findAllByDivisionId(@Param("divisionId") Long divisionId);
}