package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.PricingMode;
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

    @Query("SELECT m FROM Material m ORDER BY m.name ASC")
    List<Material> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT m FROM Material m WHERE m.active = true ORDER BY m.name ASC")
    List<Material> findByActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT m FROM Material m WHERE m.item.id = :itemId ORDER BY m.name ASC")
    List<Material> findByItemIdOrderByDisplayOrderAsc(
            @Param("itemId") Long itemId
    );

    @Query("SELECT m FROM Material m WHERE m.item.id = :itemId AND m.active = true ORDER BY m.name ASC")
    List<Material> findByItemIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("itemId") Long itemId
    );

    @Query("SELECT DISTINCT m FROM Material m JOIN m.divisions d WHERE d.id = :divisionId AND m.active = true ORDER BY m.name ASC")
    List<Material> findByDivisionsIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("divisionId") Long divisionId
    );

    @Query("SELECT DISTINCT m FROM Material m JOIN m.divisions d WHERE d.id = :divisionId ORDER BY m.name ASC")
    List<Material> findByDivisionsIdOrderByDisplayOrderAsc(
            @Param("divisionId") Long divisionId
    );

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) AND m.active = true ORDER BY m.name ASC")
    List<Material> findActiveByDivisionId(@Param("divisionId") Long divisionId);

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) ORDER BY m.name ASC")
    List<Material> findAllByDivisionId(@Param("divisionId") Long divisionId);

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) AND m.pricingGrade = :pricingGrade AND m.active = true ORDER BY m.displayOrder ASC, m.name ASC")
    List<Material> findActiveByDivisionIdAndPricingGrade(@Param("divisionId") Long divisionId, @Param("pricingGrade") PricingMode pricingGrade);

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) AND m.pricingGrade = :pricingGrade ORDER BY m.displayOrder ASC, m.name ASC")
    List<Material> findAllByDivisionIdAndPricingGrade(@Param("divisionId") Long divisionId, @Param("pricingGrade") PricingMode pricingGrade);

    @Query("SELECT DISTINCT m FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) ORDER BY m.displayOrder ASC, m.name ASC")
    List<Material> findAllByDivisionIdUnified(@Param("divisionId") Long divisionId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) AND m.pricingGrade = :pricingGrade AND LOWER(m.name) = LOWER(:name)")
    boolean existsByDivisionIdAndPricingGradeAndNameIgnoreCase(@Param("divisionId") Long divisionId, @Param("pricingGrade") PricingMode pricingGrade, @Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Material m LEFT JOIN m.divisions d LEFT JOIN m.item i WHERE (m.division.id = :divisionId OR d.id = :divisionId OR i.division.id = :divisionId) AND m.pricingGrade = :pricingGrade AND LOWER(m.name) = LOWER(:name) AND m.id <> :id")
    boolean existsByDivisionIdAndPricingGradeAndNameIgnoreCaseAndIdNot(@Param("divisionId") Long divisionId, @Param("pricingGrade") PricingMode pricingGrade, @Param("name") String name, @Param("id") Long id);
}