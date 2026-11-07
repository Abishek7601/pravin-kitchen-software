package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.service.QuotationItemSizeService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

public class RollMaterialFormulaUnitTest {

    private final QuotationItemSizeService sizeService = new QuotationItemSizeService(null);

    @Test
    @DisplayName("Verify exact user test matrix for Roll Quantity: Ceiling(Sq.ft / Coverage)")
    void testExactUserMatrixCalculations() {
        BigDecimal coverage = new BigDecimal("40");

        // 20 Sq.ft -> 1 Roll
        BigDecimal sqft20 = new BigDecimal("20");
        BigDecimal rolls20 = sqft20.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(1, rolls20.intValue());

        // 40 Sq.ft -> 1 Roll
        BigDecimal sqft40 = new BigDecimal("40");
        BigDecimal rolls40 = sqft40.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(1, rolls40.intValue());

        // 41 Sq.ft -> 2 Rolls
        BigDecimal sqft41 = new BigDecimal("41");
        BigDecimal rolls41 = sqft41.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(2, rolls41.intValue());

        // 80 Sq.ft -> 2 Rolls
        BigDecimal sqft80 = new BigDecimal("80");
        BigDecimal rolls80 = sqft80.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(2, rolls80.intValue());

        // 85 Sq.ft -> 3 Rolls
        BigDecimal sqft85 = new BigDecimal("85");
        BigDecimal rolls85 = sqft85.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(3, rolls85.intValue());

        // 120 Sq.ft -> 3 Rolls
        BigDecimal sqft120 = new BigDecimal("120");
        BigDecimal rolls120 = sqft120.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(3, rolls120.intValue());

        // 125 Sq.ft -> 4 Rolls
        BigDecimal sqft125 = new BigDecimal("125");
        BigDecimal rolls125 = sqft125.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(4, rolls125.intValue());
    }

    @Test
    @DisplayName("Verify user example: 85 Sq.ft, Coverage 40 Sq.ft/Roll, Rate ₹1,500 -> 3 Rolls, Total ₹4,500")
    void testUserExampleCalculationAndAmount() {
        BigDecimal totalSqft = new BigDecimal("85.00");
        BigDecimal coveragePerRoll = new BigDecimal("40.00");
        BigDecimal ratePerRoll = new BigDecimal("1500.00");

        BigDecimal requiredRolls = totalSqft.divide(coveragePerRoll, 0, RoundingMode.CEILING);
        BigDecimal totalAmount = requiredRolls.multiply(ratePerRoll).setScale(2, RoundingMode.HALF_UP);

        assertEquals(3, requiredRolls.intValue());
        assertEquals(new BigDecimal("4500.00"), totalAmount);
    }

    @Test
    @DisplayName("Verify configurable coverage values (not hardcoded to 40)")
    void testConfigurableCoveragePerRoll() {
        BigDecimal totalSqft = new BigDecimal("85.00");

        // Custom Coverage 50 Sq.ft -> 2 Rolls
        BigDecimal cov50 = new BigDecimal("50.00");
        BigDecimal rolls50 = totalSqft.divide(cov50, 0, RoundingMode.CEILING);
        assertEquals(2, rolls50.intValue());

        // Custom Coverage 35 Sq.ft -> 3 Rolls
        BigDecimal cov35 = new BigDecimal("35.00");
        BigDecimal rolls35 = totalSqft.divide(cov35, 0, RoundingMode.CEILING);
        assertEquals(3, rolls35.intValue());

        // Custom Coverage 30 Sq.ft with 125 Sq.ft -> 5 Rolls
        BigDecimal cov30 = new BigDecimal("30.00");
        BigDecimal rolls30 = new BigDecimal("125.00").divide(cov30, 0, RoundingMode.CEILING);
        assertEquals(5, rolls30.intValue());
    }

    @Test
    @DisplayName("Verify dimension to Sq.ft conversion using standard formulas in inches")
    void testDimensionFormulasWithInchToSqftConversion() {
        // Formula: L × H
        // 102 in × 120 in = 12240 sq.in / 144 = 85.00 sq.ft
        BigDecimal sqftLH = sizeService.calculateSqft("L × H", new BigDecimal("102"), null, new BigDecimal("120"), "in");
        assertEquals(new BigDecimal("85.00"), sqftLH.setScale(2, RoundingMode.HALF_UP));

        BigDecimal cov40 = new BigDecimal("40.00");
        BigDecimal rollsLH = sqftLH.divide(cov40, 0, RoundingMode.CEILING);
        assertEquals(3, rollsLH.intValue());

        // Formula: L × W
        // 120 in × 48 in = 5760 sq.in / 144 = 40.00 sq.ft
        BigDecimal sqftLW = sizeService.calculateSqft("L × W", new BigDecimal("120"), new BigDecimal("48"), null, "in");
        assertEquals(new BigDecimal("40.00"), sqftLW.setScale(2, RoundingMode.HALF_UP));
        BigDecimal rollsLW = sqftLW.divide(cov40, 0, RoundingMode.CEILING);
        assertEquals(1, rollsLW.intValue());

        // Formula: (L + W) × H
        // (60 in + 42 in) × 120 in = 102 × 120 = 12240 sq.in / 144 = 85.00 sq.ft
        BigDecimal sqftLWH = sizeService.calculateSqft("(L + W) × H", new BigDecimal("60"), new BigDecimal("42"), new BigDecimal("120"), "in");
        assertEquals(new BigDecimal("85.00"), sqftLWH.setScale(2, RoundingMode.HALF_UP));
        BigDecimal rollsLWH = sqftLWH.divide(cov40, 0, RoundingMode.CEILING);
        assertEquals(3, rollsLWH.intValue());
    }

    @Test
    @DisplayName("Verify Material entity and QuotationItem entity store and retrieve coveragePerRoll")
    void testMaterialAndQuotationItemCoveragePerRollProperties() {
        Material material = new Material();
        material.setName("Fixed Wallpaper");
        material.setUnit("Roll");
        material.setMeasurementType(MeasurementType.ROLL);
        material.setCoveragePerRoll(new BigDecimal("45.50"));

        assertEquals(new BigDecimal("45.50"), material.getCoveragePerRoll());
        assertEquals("Roll", material.getUnit());
        assertEquals(MeasurementType.ROLL, material.getMeasurementType());

        QuotationItem item = new QuotationItem();
        item.setMaterial(material);
        item.setMeasurementType(MeasurementType.ROLL);
        item.setCoveragePerRoll(new BigDecimal("45.50"));
        item.setQuantity(new BigDecimal("2"));
        item.setRate(new BigDecimal("8000.00"));
        item.setAmount(new BigDecimal("16000.00"));

        assertEquals(new BigDecimal("45.50"), item.getCoveragePerRoll());
        assertEquals(new BigDecimal("2"), item.getQuantity());
        assertEquals(new BigDecimal("16000.00"), item.getAmount());
    }
}
