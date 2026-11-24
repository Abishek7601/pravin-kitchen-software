package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.MeasurementType;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.util.CalculationFormula;
import com.example.pravin_quotation.util.DimensionFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DimensionFormatterTest {

    private final DimensionFormatter formatter = new DimensionFormatter();

    @Test
    @DisplayName("Formula L × W formats as L : 34 × W : 34 in. and omits unused Height")
    public void testFormulaLxW() {
        String result = DimensionFormatter.formatViewDimension(
                CalculationFormula.FORMULA_L_X_W,
                new BigDecimal("34"),
                new BigDecimal("34"),
                new BigDecimal("0.00")
        );
        assertEquals("L : 34 × W : 34 in.", result);
        assertFalse(result.contains("0.00"), "Must not display unused dimensions with 0.00");
        assertFalse(result.contains("H"), "Must not display Height label for L × W");
    }

    @Test
    @DisplayName("Formula (L + W) × H formats as (L : 23 + W : 23) × H : 12 in.")
    public void testFormulaLplusWxH() {
        String result = DimensionFormatter.formatViewDimension(
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("23"),
                new BigDecimal("23"),
                new BigDecimal("12")
        );
        assertEquals("(L : 23 + W : 23) × H : 12 in.", result);
    }

    @Test
    @DisplayName("Formula L × H formats as L : 33 × H : 23 in. and omits unused Width")
    public void testFormulaLxH() {
        String result = DimensionFormatter.formatViewDimension(
                CalculationFormula.FORMULA_L_X_H,
                new BigDecimal("33"),
                new BigDecimal("0.00"),
                new BigDecimal("23")
        );
        assertEquals("L : 33 × H : 23 in.", result);
        assertFalse(result.contains("0.00"), "Must not display unused dimensions with 0.00");
        assertFalse(result.contains("W"), "Must not display Width label for L × H");
    }

    @Test
    @DisplayName("Preserves user-entered decimal values without trailing zeroes")
    public void testPreservesDecimals() {
        String result = DimensionFormatter.formatViewDimension(
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("23.50"),
                new BigDecimal("23.75"),
                new BigDecimal("12.20")
        );
        assertEquals("(L : 23.5 + W : 23.75) × H : 12.2 in.", result);
    }

    @Test
    @DisplayName("Resolves formula from QuotationItem and Material correctly")
    public void testResolveItemFormula() {
        QuotationItem itemWithFormula = new QuotationItem();
        itemWithFormula.setFormula("L × W");
        itemWithFormula.setLengthValue(new BigDecimal("34"));
        itemWithFormula.setWidthValue(new BigDecimal("34"));
        assertEquals("L : 34 × W : 34 in.", formatter.formatItem(itemWithFormula));

        QuotationItem itemWithMaterialFormula = new QuotationItem();
        Material mat = new Material();
        mat.setFormula("L × H");
        itemWithMaterialFormula.setMaterial(mat);
        itemWithMaterialFormula.setLengthValue(new BigDecimal("33"));
        itemWithMaterialFormula.setHeightValue(new BigDecimal("23"));
        assertEquals("L : 33 × H : 23 in.", formatter.formatItem(itemWithMaterialFormula));
    }

    @Test
    @DisplayName("Quantity based non-sqft items display Qty")
    public void testNonSqftItems() {
        QuotationItem qtyItem = new QuotationItem();
        qtyItem.setMeasurementType(MeasurementType.QUANTITY);
        qtyItem.setQuantity(new BigDecimal("3"));
        assertEquals("Qty: 3", formatter.formatItem(qtyItem));
    }

    @Test
    @DisplayName("Templates contain dimFormatter.formatDimension in admin and employee quotation views")
    public void testTemplatesContainDimFormatter() throws Exception {
        String adminView = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));
        String empWorkspace = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        String fragTable = Files.readString(Path.of("src/main/resources/templates/fragments/quotation-breakdown-table.html"));

        assertTrue(adminView.contains("@dimFormatter.formatDimension"), "Admin view must use @dimFormatter.formatDimension");
        assertTrue(empWorkspace.contains("@dimFormatter.formatDimension"), "Employee workspace must use @dimFormatter.formatDimension");
        assertTrue(fragTable.contains("@dimFormatter.formatDimension"), "Fragment table must use @dimFormatter.formatDimension");
    }
}
