package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.FormulaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaConfigRepository extends JpaRepository<FormulaConfig, Long> {

    @Query("SELECT f FROM FormulaConfig f ORDER BY f.name ASC, f.id ASC")
    List<FormulaConfig> findAllByOrderByDisplayOrderAscIdAsc();

    @Query("SELECT f FROM FormulaConfig f WHERE f.active = true ORDER BY f.name ASC, f.id ASC")
    List<FormulaConfig> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    @Query("SELECT f FROM FormulaConfig f WHERE f.calculationType = :calculationType AND f.active = true ORDER BY f.name ASC, f.id ASC")
    List<FormulaConfig> findByCalculationTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(@Param("calculationType") String calculationType);

    Optional<FormulaConfig> findByNameIgnoreCase(String name);

    Optional<FormulaConfig> findFirstByExpressionIgnoreCaseAndCalculationType(String expression, String calculationType);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
