package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.FormulaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaConfigRepository extends JpaRepository<FormulaConfig, Long> {

    List<FormulaConfig> findAllByOrderByDisplayOrderAscIdAsc();

    List<FormulaConfig> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<FormulaConfig> findByCalculationTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(String calculationType);

    Optional<FormulaConfig> findByNameIgnoreCase(String name);

    Optional<FormulaConfig> findFirstByExpressionIgnoreCaseAndCalculationType(String expression, String calculationType);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
