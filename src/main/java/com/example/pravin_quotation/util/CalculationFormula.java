package com.example.pravin_quotation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Calculation formulas for quotation item area/sizing:
 * 1. "L × W"
 * 2. "L + (W × H)"  -- Exact mathematical order: Length + (Width × Height)
 * 3. "L × H"
 */
public final class CalculationFormula {

    public static final String FORMULA_L_X_W = "L × W";
    public static final String FORMULA_L_PLUS_W_X_H = "(L + W) × H";
    public static final String FORMULA_L_X_H = "L × H";

    public static final List<String> ALL_FORMULAS = List.of(
            FORMULA_L_X_W,
            FORMULA_L_PLUS_W_X_H,
            FORMULA_L_X_H
    );

    private CalculationFormula() {
    }

    /**
     * Normalizes the formula string to one of the 3 supported standard formats.
     */
    public static String normalizeFormula(String formula) {
        if (formula == null || formula.isBlank()) {
            return FORMULA_L_X_W;
        }

        String cleaned = formula.trim().toUpperCase()
                .replace("X", "×")
                .replace("*", "×")
                .replaceAll("\\s+", " ");

        if (cleaned.contains("+") || cleaned.contains("W × H") || cleaned.contains("L + W") || cleaned.contains("(L + W)") || cleaned.contains("144")) {
            return FORMULA_L_PLUS_W_X_H;
        }

        if (cleaned.contains("H") && !cleaned.contains("W")) {
            return FORMULA_L_X_H;
        }

        if (cleaned.contains("W") && !cleaned.contains("H")) {
            return FORMULA_L_X_W;
        }

        return FORMULA_L_X_W;
    }

    /**
     * Validates that all required dimensions for the formula are present and > 0.
     */
    public static void validateDimensions(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {

        String norm = normalizeFormula(formula);

        if (FORMULA_L_X_W.equals(norm)) {
            if (length == null || length.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Length must be greater than zero.");
            }
            if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Width must be greater than zero for formula 'L × W'.");
            }
            return;
        } else if (FORMULA_L_X_H.equals(norm)) {
            if (length == null || length.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Length must be greater than zero.");
            }
            if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Height must be greater than zero for formula 'L × H'.");
            }
            return;
        } else if (FORMULA_L_PLUS_W_X_H.equals(norm)) {
            if (length == null || length.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Length must be greater than zero.");
            }
            if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Width must be greater than zero for formula '(L + W) × H'.");
            }
            if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Height must be greater than zero for formula '(L + W) × H'.");
            }
            return;
        }

        // For any custom configurable formula, validate variables present in expression
        Set<String> vars = FormulaEvaluator.detectVariables(formula != null ? formula : "");
        if (vars.contains("L") && (length == null || length.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Length must be greater than zero for formula '" + formula + "'.");
        }
        if (vars.contains("W") && (width == null || width.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Width must be greater than zero for formula '" + formula + "'.");
        }
        if (vars.contains("H") && (height == null || height.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Height must be greater than zero for formula '" + formula + "'.");
        }
        if (vars.isEmpty() && (length == null || length.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Length must be greater than zero.");
        }
    }

    /**
     * Calculates the area or measurement using either canonical formula rules or custom expression.
     */
    public static BigDecimal calculateArea(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            String unit) {

        String norm = normalizeFormula(formula);

        // If it matches one of the canonical 3 formulas, use precise existing logic
        if (isCanonical(formula) || isCanonical(norm)) {
            BigDecimal l = length != null ? length : BigDecimal.ZERO;
            BigDecimal w = width != null ? width : BigDecimal.ZERO;
            BigDecimal h = height != null ? height : BigDecimal.ZERO;

            boolean isInches = unit == null
                    || unit.isBlank()
                    || "in".equalsIgnoreCase(unit)
                    || "inch".equalsIgnoreCase(unit)
                    || "inches".equalsIgnoreCase(unit);

            if (FORMULA_L_PLUS_W_X_H.equals(norm)) {
                // Formula 2: (Length + Width) × Height
                // When dimensions are in inches: ((Length + Width) * Height) / 144
                if (isInches) {
                    return (length != null ? length : BigDecimal.ZERO)
                            .add(width != null ? width : BigDecimal.ZERO)
                            .multiply(height != null ? height : BigDecimal.ZERO)
                            .divide(new BigDecimal("144"), 6, RoundingMode.HALF_UP)
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    return l.add(w).multiply(h).setScale(2, RoundingMode.HALF_UP);
                }
            } else if (FORMULA_L_X_H.equals(norm)) {
                if (isInches) {
                    BigDecimal twelve = new BigDecimal("12");
                    BigDecimal l_ft = l.divide(twelve, 6, RoundingMode.HALF_UP);
                    BigDecimal h_ft = h.divide(twelve, 6, RoundingMode.HALF_UP);
                    return l_ft.multiply(h_ft).setScale(2, RoundingMode.HALF_UP);
                } else {
                    return l.multiply(h).setScale(2, RoundingMode.HALF_UP);
                }
            } else {
                // Default "L × W"
                if (isInches) {
                    BigDecimal twelve = new BigDecimal("12");
                    BigDecimal l_ft = l.divide(twelve, 6, RoundingMode.HALF_UP);
                    BigDecimal w_ft = w.divide(twelve, 6, RoundingMode.HALF_UP);
                    return l_ft.multiply(w_ft).setScale(2, RoundingMode.HALF_UP);
                } else {
                    return l.multiply(w).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        // Custom configurable formula
        String calcType = isInchUnit(unit) ? "INCHES" : "SQFT";
        FormulaEvaluator.EvaluationResult res = FormulaEvaluator.evaluate(
                formula,
                calcType,
                null,
                length,
                width,
                height
        );
        return res.finalMeasurement();
    }

    public static BigDecimal calculateArea(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {
        return calculateArea(formula, length, width, height, "in");
    }

    /**
     * Flexible measurement evaluation supporting Sqft and Inches calculation types.
     */
    public static FormulaEvaluator.EvaluationResult calculateMeasurement(
            String formula,
            String calculationType,
            String unitType,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        return FormulaEvaluator.evaluate(formula, calculationType, unitType, length, width, height);
    }

    public static FormulaEvaluator.EvaluationResult calculateMeasurement(
            String formula,
            String calculationType,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        return FormulaEvaluator.evaluate(formula, calculationType, null, length, width, height);
    }

    private static boolean isCanonical(String formula) {
        if (formula == null) return false;
        String f = formula.trim();
        return FORMULA_L_X_W.equals(f)
                || FORMULA_L_PLUS_W_X_H.equals(f)
                || FORMULA_L_X_H.equals(f)
                || "L x W".equalsIgnoreCase(f)
                || "(L + W) x H".equalsIgnoreCase(f)
                || "L x H".equalsIgnoreCase(f);
    }

    private static boolean isInchUnit(String unit) {
        if (unit == null) return false;
        String u = unit.trim().toUpperCase();
        return "INCH".equals(u) || "INCHES".equals(u) || "SQIN".equals(u);
    }

    /**
     * Calculates item amount: Area / Measurement × Rate.
     */
    public static BigDecimal calculateItemAmount(BigDecimal area, BigDecimal rate) {
        if (area == null) area = BigDecimal.ZERO;
        if (rate == null) rate = BigDecimal.ZERO;

        if (area.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Area cannot be negative.");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rate cannot be negative.");
        }

        return area.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
