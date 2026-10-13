package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.FormulaConfig;
import com.example.pravin_quotation.repository.FormulaConfigRepository;
import com.example.pravin_quotation.util.FormulaEvaluator;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FormulaConfigService {

    private final FormulaConfigRepository formulaConfigRepository;

    public FormulaConfigService(FormulaConfigRepository formulaConfigRepository) {
        this.formulaConfigRepository = formulaConfigRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaultFormulas() {
        seedDefaults();
        // Deactivate any formula that is not one of the 3 canonical formulas
        List<FormulaConfig> existing = formulaConfigRepository.findAll();
        for (FormulaConfig fc : existing) {
            String expr = FormulaEvaluator.cleanExpression(fc.getExpression());
            if (!"(L+W)*H".equalsIgnoreCase(expr) && !"L*W".equalsIgnoreCase(expr) && !"L*H".equalsIgnoreCase(expr)) {
                fc.setActive(false);
                formulaConfigRepository.save(fc);
            } else {
                fc.setCalculationType("SQFT");
                fc.setMeasurementUnit("SQFT");
                fc.setPricingUnit("₹ / SQFT");
                formulaConfigRepository.save(fc);
            }
        }
    }

    @Transactional
    public void seedDefaults() {
        // Standard Sqft Formulas: only the 3 canonical formulas
        saveIfNotExists(new FormulaConfig(
                "(L + W) × H (Sqft)",
                "(L + W) * H",
                "SQFT",
                "AREA",
                "SQFT",
                "₹ / SQFT",
                "Standard cabinetry / 3D perimeter formula. Evaluates as ((L + W) * H) / 144 to convert inches to sq.ft.",
                1,
                true
        ));

        saveIfNotExists(new FormulaConfig(
                "L × W (Sqft)",
                "L * W",
                "SQFT",
                "AREA",
                "SQFT",
                "₹ / SQFT",
                "Standard 2D surface area (e.g. countertop, shutter). Evaluates as (L * W) / 144 for sq.ft.",
                2,
                true
        ));

        saveIfNotExists(new FormulaConfig(
                "L × H (Sqft)",
                "L * H",
                "SQFT",
                "AREA",
                "SQFT",
                "₹ / SQFT",
                "Vertical elevation area (e.g. back panel, partition). Evaluates as (L * H) / 144 for sq.ft.",
                3,
                true
        ));
    }

    private void saveIfNotExists(FormulaConfig config) {
        if (!formulaConfigRepository.existsByNameIgnoreCase(config.getName())) {
            formulaConfigRepository.save(config);
        }
    }

    public List<FormulaConfig> getAllFormulas() {
        return formulaConfigRepository.findAllByOrderByDisplayOrderAscIdAsc();
    }

    public List<FormulaConfig> getActiveFormulas() {
        return formulaConfigRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    public List<FormulaConfig> getFormulasByType(String calculationType) {
        if ("INCHES".equalsIgnoreCase(calculationType)) {
            return List.of();
        }
        return formulaConfigRepository.findByCalculationTypeAndActiveTrueOrderByDisplayOrderAscIdAsc("SQFT");
    }

    public FormulaConfig getFormulaById(Long id) {
        return formulaConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formula configuration not found with ID: " + id));
    }

    @Transactional
    public FormulaConfig createFormula(
            String name,
            String expression,
            String calculationType,
            String unitType,
            String measurementUnit,
            String pricingUnit,
            String description,
            Integer displayOrder
    ) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula name is required.");
        }
        String cleanName = name.trim();
        if (formulaConfigRepository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("A formula with the name '" + cleanName + "' already exists.");
        }

        FormulaEvaluator.ValidationResult validation = FormulaEvaluator.validateExpression(expression);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.errorMessage());
        }

        if ("INCHES".equalsIgnoreCase(calculationType) || "INCH".equalsIgnoreCase(calculationType)) {
            throw new IllegalArgumentException("Inches is no longer supported as a calculation type. Only 'SQFT' is supported.");
        }

        String cleanedExpr = FormulaEvaluator.cleanExpression(expression);
        if (!"(L+W)*H".equalsIgnoreCase(cleanedExpr) && !"L*W".equalsIgnoreCase(cleanedExpr) && !"L*H".equalsIgnoreCase(cleanedExpr)) {
            throw new IllegalArgumentException("Only the three standard formulas are supported: L × W, (L + W) × H, and L × H.");
        }

        String cleanCalcType = "SQFT";
        String cleanUnitType = "AREA";
        String cleanMeasUnit = "SQFT";
        String cleanPriceUnit = "₹ / SQFT";

        FormulaConfig config = new FormulaConfig();
        config.setName(cleanName);
        config.setExpression(expression.trim());
        config.setCalculationType(cleanCalcType);
        config.setUnitType(cleanUnitType);
        config.setMeasurementUnit(cleanMeasUnit);
        config.setPricingUnit(cleanPriceUnit);
        config.setDescription(description != null ? description.trim() : null);
        config.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        config.setIsSystem(false);
        config.setActive(true);

        return formulaConfigRepository.save(config);
    }

    @Transactional
    public FormulaConfig updateFormula(
            Long id,
            String name,
            String expression,
            String calculationType,
            String unitType,
            String measurementUnit,
            String pricingUnit,
            String description,
            Integer displayOrder
    ) {
        FormulaConfig config = getFormulaById(id);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula name is required.");
        }
        String cleanName = name.trim();
        if (formulaConfigRepository.existsByNameIgnoreCaseAndIdNot(cleanName, id)) {
            throw new IllegalArgumentException("Another formula with the name '" + cleanName + "' already exists.");
        }

        FormulaEvaluator.ValidationResult validation = FormulaEvaluator.validateExpression(expression);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.errorMessage());
        }

        if ("INCHES".equalsIgnoreCase(calculationType) || "INCH".equalsIgnoreCase(calculationType)) {
            throw new IllegalArgumentException("Inches is no longer supported as a calculation type. Only 'SQFT' is supported.");
        }

        String cleanedExpr = FormulaEvaluator.cleanExpression(expression);
        if (!"(L+W)*H".equalsIgnoreCase(cleanedExpr) && !"L*W".equalsIgnoreCase(cleanedExpr) && !"L*H".equalsIgnoreCase(cleanedExpr)) {
            throw new IllegalArgumentException("Only the three standard formulas are supported: L × W, (L + W) × H, and L × H.");
        }

        String cleanCalcType = "SQFT";
        String cleanUnitType = "AREA";
        String cleanMeasUnit = "SQFT";
        String cleanPriceUnit = "₹ / SQFT";

        config.setName(cleanName);
        config.setExpression(expression.trim());
        config.setCalculationType(cleanCalcType);
        config.setUnitType(cleanUnitType);
        config.setMeasurementUnit(cleanMeasUnit);
        config.setPricingUnit(cleanPriceUnit);
        config.setDescription(description != null ? description.trim() : null);
        if (displayOrder != null) {
            config.setDisplayOrder(displayOrder);
        }

        return formulaConfigRepository.save(config);
    }

    @Transactional
    public void toggleFormulaStatus(Long id) {
        FormulaConfig config = getFormulaById(id);
        config.setActive(!Boolean.TRUE.equals(config.getActive()));
        formulaConfigRepository.save(config);
    }

    @Transactional
    public void deleteFormula(Long id) {
        FormulaConfig config = getFormulaById(id);
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new IllegalStateException("System default formula cannot be deleted. You can disable it instead.");
        }
        formulaConfigRepository.delete(config);
    }

    /**
     * Evaluates a formula and returns the calculated measurement, unit, and details.
     */
    public FormulaEvaluator.EvaluationResult calculate(
            String formulaExpression,
            String calculationType,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        return FormulaEvaluator.evaluate(
                formulaExpression,
                calculationType,
                null,
                length,
                width,
                height
        );
    }
}
