package com.example.pravin_quotation.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

        if (length == null || length.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero.");
        }

        if (FORMULA_L_X_W.equals(norm)) {
            if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Width must be greater than zero for formula 'L × W'.");
            }
        } else if (FORMULA_L_X_H.equals(norm)) {
            if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Height must be greater than zero for formula 'L × H'.");
            }
        } else if (FORMULA_L_PLUS_W_X_H.equals(norm)) {
            if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Width must be greater than zero for formula '(L + W) × H'.");
            }
            if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Height must be greater than zero for formula '(L + W) × H'.");
            }
        }
    }

    /**
     * Calculates the area in square feet.
     * If dimensions are in inches ("in" or default), converts them to feet first before calculating,
     * or for Formula 2 calculates ((Length + Width) * Height) / 144.
     * If dimensions are in feet ("ft"), calculates directly.
     */
    public static BigDecimal calculateArea(
            String formula,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            String unit) {

        BigDecimal l = length != null ? length : BigDecimal.ZERO;
        BigDecimal w = width != null ? width : BigDecimal.ZERO;
        BigDecimal h = height != null ? height : BigDecimal.ZERO;

        boolean isInches = unit == null
                || unit.isBlank()
                || "in".equalsIgnoreCase(unit)
                || "inch".equalsIgnoreCase(unit)
                || "inches".equalsIgnoreCase(unit);

        if (isInches) {
            BigDecimal twelve = new BigDecimal("12");
            l = l.divide(twelve, 6, RoundingMode.HALF_UP);
            w = w.divide(twelve, 6, RoundingMode.HALF_UP);
            h = h.divide(twelve, 6, RoundingMode.HALF_UP);
        }

        String norm = normalizeFormula(formula);
        BigDecimal area;

        if (FORMULA_L_PLUS_W_X_H.equals(norm)) {
            // Formula 2: (Length + Width) × Height
            // When dimensions are in inches: ((Length + Width) * Height) / 144
            if (isInches) {
                area = (length != null ? length : BigDecimal.ZERO)
                        .add(width != null ? width : BigDecimal.ZERO)
                        .multiply(height != null ? height : BigDecimal.ZERO)
                        .divide(new BigDecimal("144"), 6, RoundingMode.HALF_UP);
            } else {
                area = l.add(w).multiply(h);
            }
        } else if (FORMULA_L_X_H.equals(norm)) {
            area = l.multiply(h);
        } else {
            // Default "L × W"
            area = l.multiply(w);
        }

        return area.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates item amount: Area × Rate.
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
