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
     * Normalizes the formula string to one of the 3 supported standard formats:
     * 1. L × W
     * 2. (L + W) × H
     * 3. L × H
     */
    public static String normalizeFormula(String formula) {
        if (formula == null || formula.isBlank()) {
            return FORMULA_L_X_W;
        }

        String cleaned = formula.trim().toUpperCase()
                .replace("X", "×")
                .replace("*", "×")
                .replaceAll("\\s+", " ");

        if (cleaned.contains("+") || cleaned.contains("L + W") || cleaned.contains("(L + W)")) {
            return FORMULA_L_PLUS_W_X_H;
        }

        if (cleaned.contains("H")) {
            return FORMULA_L_X_H;
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
    }

    /**
     * Calculates the area or measurement converting input inch dimensions to square feet (÷ 144).
     * Supports only:
     * 1. L × W: (L / 12) × (W / 12) = (L × W) / 144
     * 2. (L + W) × H: ((L + W) / 12) × (H / 12) = ((L + W) × H) / 144
     * 3. L × H: (L / 12) × (H / 12) = (L × H) / 144
     */
    public static BigDecimal calculateArea(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            String unit) {

        String norm = normalizeFormula(formula);
        BigDecimal l = length != null ? length : BigDecimal.ZERO;
        BigDecimal w = width != null ? width : BigDecimal.ZERO;
        BigDecimal h = height != null ? height : BigDecimal.ZERO;
        BigDecimal divisor144 = new BigDecimal("144");
        boolean isFeet = unit != null && (unit.equalsIgnoreCase("ft") || unit.equalsIgnoreCase("feet"));
        BigDecimal divisor = isFeet ? BigDecimal.ONE : divisor144;

        if (FORMULA_L_PLUS_W_X_H.equals(norm)) {
            if (l.compareTo(BigDecimal.ZERO) <= 0 || w.compareTo(BigDecimal.ZERO) <= 0 || h.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return l.add(w).multiply(h)
                    .divide(divisor, 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (FORMULA_L_X_H.equals(norm)) {
            if (l.compareTo(BigDecimal.ZERO) <= 0 || h.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return l.multiply(h)
                    .divide(divisor, 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            // FORMULA_L_X_W
            if (l.compareTo(BigDecimal.ZERO) <= 0 || w.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return l.multiply(w)
                    .divide(divisor, 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    public static BigDecimal calculateArea(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height) {
        return calculateArea(formula, length, width, height, "in");
    }

    /**
     * Flexible measurement evaluation converting inch dimensions to Sq. Ft.
     */
    public static FormulaEvaluator.EvaluationResult calculateMeasurement(
            String formula,
            String calculationType,
            String unitType,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        return FormulaEvaluator.evaluate(formula, "SQFT", unitType, length, width, height);
    }

    public static FormulaEvaluator.EvaluationResult calculateMeasurement(
            String formula,
            String calculationType,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {
        return FormulaEvaluator.evaluate(formula, "SQFT", null, length, width, height);
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
