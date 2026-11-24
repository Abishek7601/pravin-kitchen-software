package com.example.pravin_quotation.util;

import com.example.pravin_quotation.model.QuotationItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("dimFormatter")
public class DimensionFormatter {

    /**
     * Formats dimension for quotation details / view page:
     * - Formula L × W: L : 34 × W : 34 in.
     * - Formula (L + W) × H: (L : 23 + W : 23) × H : 12 in.
     * - Formula L × H: L : 33 × H : 23 in.
     */
    public static String formatViewDimension(String formula, BigDecimal length, BigDecimal width, BigDecimal height) {
        String norm = CalculationFormula.normalizeFormula(formula);
        String l = formatValue(length);
        String w = formatValue(width);
        String h = formatValue(height);

        if (CalculationFormula.FORMULA_L_X_H.equals(norm)) {
            return "L : " + l + " × H : " + h + " in.";
        } else if (CalculationFormula.FORMULA_L_PLUS_W_X_H.equals(norm)) {
            return "(L : " + l + " + W : " + w + ") × H : " + h + " in.";
        } else {
            return "L : " + l + " × W : " + w + " in.";
        }
    }

    public static String formatValue(BigDecimal val) {
        if (val == null) return "0";
        BigDecimal scaled = val.setScale(2, RoundingMode.HALF_UP);
        if (scaled.stripTrailingZeros().scale() <= 0) {
            return scaled.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return scaled.stripTrailingZeros().toPlainString();
    }

    public String formatItem(QuotationItem item) {
        if (item == null) return "-";
        if (item.getMeasurementType() != null && !item.getMeasurementType().isSqft() && !item.getMeasurementType().isRoll()) {
            BigDecimal qty = (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getQuantity() : BigDecimal.ONE;
            return "Qty: " + qty.stripTrailingZeros().toPlainString();
        }
        String formula = resolveItemFormula(item);
        return formatViewDimension(formula, item.getLengthValue(), item.getWidthValue(), item.getHeightValue());
    }

    public String formatDimension(QuotationItem item, BigDecimal length, BigDecimal width, BigDecimal height) {
        if (item == null) return "-";
        if (item.getMeasurementType() != null && !item.getMeasurementType().isSqft() && !item.getMeasurementType().isRoll()) {
            BigDecimal qty = (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                    ? item.getQuantity() : BigDecimal.ONE;
            return "Qty: " + qty.stripTrailingZeros().toPlainString();
        }
        String formula = resolveItemFormula(item);
        return formatViewDimension(formula, length, width, height);
    }

    public static String resolveItemFormula(QuotationItem item) {
        if (item == null) return CalculationFormula.FORMULA_L_X_W;
        if (item.getFormula() != null && !item.getFormula().isBlank()) {
            return item.getFormula();
        }
        if (item.getMaterial() != null && item.getMaterial().getFormula() != null && !item.getMaterial().getFormula().isBlank()) {
            return item.getMaterial().getFormula();
        }
        if (item.getMaterialOption() != null && item.getMaterialOption().getFormula() != null && !item.getMaterialOption().getFormula().isBlank()) {
            return item.getMaterialOption().getFormula();
        }
        return CalculationFormula.FORMULA_L_PLUS_W_X_H;
    }
}
