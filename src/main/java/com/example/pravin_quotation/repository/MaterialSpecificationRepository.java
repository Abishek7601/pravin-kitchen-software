package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.MaterialSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialSpecificationRepository extends JpaRepository<MaterialSpecification, Long> {

    List<MaterialSpecification> findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(Long materialOptionId);

    List<MaterialSpecification> findByMaterialOptionIdOrderByDisplayOrderAsc(Long materialOptionId);

    List<MaterialSpecification> findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(Long materialId);

    List<MaterialSpecification> findByMaterialIdOrderByDisplayOrderAsc(Long materialId);

    List<MaterialSpecification> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByMaterialOptionIdAndNameIgnoreCase(Long materialOptionId, String name);

    boolean existsByMaterialIdAndNameIgnoreCase(Long materialId, String name);

    Optional<MaterialSpecification> findByMaterialOptionIdAndNameIgnoreCase(Long materialOptionId, String name);

    Optional<MaterialSpecification> findByMaterialIdAndNameIgnoreCase(Long materialId, String name);
}
