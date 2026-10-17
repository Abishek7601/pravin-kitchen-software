package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.MaterialSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialSpecificationRepository extends JpaRepository<MaterialSpecification, Long> {

    @Query("SELECT s FROM MaterialSpecification s WHERE s.materialOption.id = :materialOptionId AND s.active = true ORDER BY s.name ASC")
    List<MaterialSpecification> findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(@Param("materialOptionId") Long materialOptionId);

    @Query("SELECT s FROM MaterialSpecification s WHERE s.materialOption.id = :materialOptionId ORDER BY s.name ASC")
    List<MaterialSpecification> findByMaterialOptionIdOrderByDisplayOrderAsc(@Param("materialOptionId") Long materialOptionId);

    @Query("SELECT s FROM MaterialSpecification s WHERE s.material.id = :materialId AND s.active = true ORDER BY s.name ASC")
    List<MaterialSpecification> findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(@Param("materialId") Long materialId);

    @Query("SELECT s FROM MaterialSpecification s WHERE s.material.id = :materialId ORDER BY s.name ASC")
    List<MaterialSpecification> findByMaterialIdOrderByDisplayOrderAsc(@Param("materialId") Long materialId);

    @Query("SELECT s FROM MaterialSpecification s WHERE s.active = true ORDER BY s.name ASC")
    List<MaterialSpecification> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByMaterialOptionIdAndNameIgnoreCase(Long materialOptionId, String name);

    boolean existsByMaterialIdAndNameIgnoreCase(Long materialId, String name);

    Optional<MaterialSpecification> findByMaterialOptionIdAndNameIgnoreCase(Long materialOptionId, String name);

    Optional<MaterialSpecification> findByMaterialIdAndNameIgnoreCase(Long materialId, String name);
}
