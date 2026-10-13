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
    public void initDefaultFormulas() {
        if (formulaConfigRepository.count() == 0) {
            seedDefaults();
        }
    }

    @Transactional
    public void seedDefaults() {
        // Standard Sqft Formulas
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

        // Standard Inches Formulas (Square Inches & Linear Inches)
        saveIfNotExists(new FormulaConfig(
                "(L + W) × H (Sq. Inches)",
                "(L + W) * H",
                "INCHES",
                "AREA",
                "SQIN",
                "₹ / SQIN",
                "Perimeter and height area in square inches (no sqft conversion).",
                4,
                true
        ));

        saveIfNotExists(new FormulaConfig(
                "L × W (Sq. Inches)",
                "L * W",
                "INCHES",
                "AREA",
                "SQIN",
                "₹ / SQIN",
                "Standard 2D surface in square inches (no sqft conversion).",
                5,
                true
        ));

        saveIfNotExists(new FormulaConfig(
                "L × H (Sq. Inches)",
                "L * H",
                "INCHES",
                "AREA",
                "SQIN",
                "₹ / SQIN",
                "Vertical panel area in square inches (no sqft conversion).",
                6,
                true
        ));

        saveIfNotExists(new FormulaConfig(
                "L (Linear Inches)",
                "L",
                "INCHES",
                "LINEAR",
                "INCH",
                "₹ / INCH",
                "Linear edge or running dimension in inches.",
                7,
                false
        ));

        saveIfNotExists(new FormulaConfig(
                "2 × (L + W) (Perimeter Inches)",
                "2 * (L + W)",
                "INCHES",
                "LINEAR",
                "INCH",
                "₹ / INCH",
                "Full perimeter dimension in linear inches.",
                8,
                false
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
        if (calculationType == null || calculationType.isBlank()) {
            return getActiveFormulas();
        }
        return formulaConfigRepository.findByCalculationTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(calculationType.toUpperCase());
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

        String cleanCalcType = (calculationType != null && !calculationType.isBlank())
                ? calculationType.trim().toUpperCase() : "SQFT";
        if (!"SQFT".equals(cleanCalcType) && !"INCHES".equals(cleanCalcType)) {
            throw new IllegalArgumentException("Calculation type must be either 'SQFT' or 'INCHES'.");
        }

        String cleanUnitType = (unitType != null && !unitType.isBlank())
                ? unitType.trim().toUpperCase() : validation.inferredUnitType();

        String cleanMeasUnit = (measurementUnit != null && !measurementUnit.isBlank())
                ? measurementUnit.trim().toUpperCase()
                : ("SQFT".equals(cleanCalcType) ? "SQFT" : ("LINEAR".equals(cleanUnitType) ? "INCH" : "SQIN"));

        String cleanPriceUnit = (pricingUnit != null && !pricingUnit.isBlank())
                ? pricingUnit.trim()
                : "₹ / " + cleanMeasUnit;

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

        String cleanCalcType = (calculationType != null && !calculationType.isBlank())
                ? calculationType.trim().toUpperCase() : "SQFT";
        String cleanUnitType = (unitType != null && !unitType.isBlank())
                ? unitType.trim().toUpperCase() : validation.inferredUnitType();
        String cleanMeasUnit = (measurementUnit != null && !measurementUnit.isBlank())
                ? measurementUnit.trim().toUpperCase()
                : ("SQFT".equals(cleanCalcType) ? "SQFT" : ("LINEAR".equals(cleanUnitType) ? "INCH" : "SQIN"));
        String cleanPriceUnit = (pricingUnit != null && !pricingUnit.isBlank())
                ? pricingUnit.trim()
                : "₹ / " + cleanMeasUnit;

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
