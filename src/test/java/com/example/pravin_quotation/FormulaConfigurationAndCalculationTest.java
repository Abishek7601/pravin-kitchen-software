package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminFormulaController;
import com.example.pravin_quotation.model.FormulaConfig;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.repository.FormulaConfigRepository;
import com.example.pravin_quotation.service.FormulaConfigService;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.util.CalculationFormula;
import com.example.pravin_quotation.util.FormulaEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class FormulaConfigurationAndCalculationTest {

    @Autowired
    private AdminFormulaController adminFormulaController;

    @Autowired
    private FormulaConfigService formulaConfigService;

    @Autowired
    private FormulaConfigRepository formulaConfigRepository;

    @Autowired
    private PricingService pricingService;

    @Test
    @DisplayName("Default formulas are seeded into MySQL and accessible")
    void testDefaultFormulasSeeded() {
        List<FormulaConfig> allFormulas = formulaConfigService.getAllFormulas();
        assertThat(allFormulas).isNotEmpty();

        List<FormulaConfig> sqftFormulas = formulaConfigService.getFormulasByType("SQFT");
        assertThat(sqftFormulas).isNotEmpty();
        assertThat(sqftFormulas).anyMatch(f -> f.getExpression().contains("(L + W) * H"));
        assertThat(sqftFormulas).anyMatch(f -> f.getExpression().contains("L * W"));
        assertThat(sqftFormulas).anyMatch(f -> f.getExpression().contains("L * H"));
        assertThat(sqftFormulas).noneMatch(f -> f.getExpression().contains("W * H"));

        List<FormulaConfig> inchesFormulas = formulaConfigService.getFormulasByType("INCHES");
        assertThat(inchesFormulas).isEmpty();
    }

    @Test
    @DisplayName("Admin can create, update, toggle, and delete custom formulas with SQFT; rejects inches and non-canonical formulas")
    void testFormulaCrudOperations() {
        FormulaConfig created = formulaConfigService.createFormula(
                "Custom L × W Profile",
                "L * W",
                "SQFT",
                "AREA",
                "SQFT",
                "₹ / SQFT",
                "Custom surface formula",
                99
        );
        assertThat(created.getId()).isNotNull();
        assertThat(created.getIsSystem()).isFalse();
        assertThat(created.getActive()).isTrue();
        assertThat(created.getCalculationType()).isEqualTo("SQFT");

        // Update
        FormulaConfig updated = formulaConfigService.updateFormula(
                created.getId(),
                "Custom L × W Profile Updated",
                "L * W",
                "SQFT",
                "AREA",
                "SQFT",
                "₹ / SQFT",
                "Updated description",
                100
        );
        assertThat(updated.getName()).isEqualTo("Custom L × W Profile Updated");
        assertThat(updated.getDescription()).isEqualTo("Updated description");

        // Toggle Status
        formulaConfigService.toggleFormulaStatus(created.getId());
        FormulaConfig toggled = formulaConfigService.getFormulaById(created.getId());
        assertThat(toggled.getActive()).isFalse();

        // Delete
        formulaConfigService.deleteFormula(created.getId());
        assertThat(formulaConfigRepository.findById(created.getId())).isEmpty();

        // Reject Inches as calculation type
        assertThatThrownBy(() -> formulaConfigService.createFormula(
                "Invalid Inches Formula",
                "L * W",
                "INCHES",
                "AREA",
                "INCH",
                "₹ / INCH",
                "Should fail",
                101
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Inches is no longer supported");

        // Reject non-3 formulas
        assertThatThrownBy(() -> formulaConfigService.createFormula(
                "Invalid Formula",
                "2 * (L + W)",
                "SQFT",
                "AREA",
                "SQFT",
                "₹ / SQFT",
                "Should fail",
                102
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Only the three standard formulas are supported");
    }

    @Test
    @DisplayName("System default formulas cannot be deleted")
    void testSystemDefaultCannotBeDeleted() {
        FormulaConfig systemFormula = formulaConfigService.getAllFormulas().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsSystem()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> formulaConfigService.deleteFormula(systemFormula.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("System default formula cannot be deleted");
    }

    @Test
    @DisplayName("Formula validation rejects invalid expressions and unsupported characters")
    void testFormulaValidationRejectsInvalid() {
        FormulaEvaluator.ValidationResult invalidChars = FormulaEvaluator.validateExpression("L * Z");
        assertThat(invalidChars.valid()).isFalse();
        assertThat(invalidChars.errorMessage()).contains("Invalid character in formula");

        FormulaEvaluator.ValidationResult unmatchedParen = FormulaEvaluator.validateExpression("(L + W");
        assertThat(unmatchedParen.valid()).isFalse();
        assertThat(unmatchedParen.errorMessage()).contains("Missing closing parenthesis");

        FormulaEvaluator.ValidationResult divideZero = FormulaEvaluator.validateExpression("L / 0");
        assertThat(divideZero.valid()).isFalse();
        assertThat(divideZero.errorMessage()).contains("Division by zero");

        FormulaEvaluator.ValidationResult valid = FormulaEvaluator.validateExpression("(L + W) * H");
        assertThat(valid.valid()).isTrue();
    }

    @Test
    @DisplayName("Formula Evaluator correctly computes Sqft area with /144 divisor")
    void testSqftCalculation() {
        // Cabinetry formula: (L + W) * H with L=72, W=24, H=36 -> (96 * 36) / 144 = 24.00 sq.ft
        FormulaEvaluator.EvaluationResult res1 = FormulaEvaluator.evaluate(
                "(L + W) * H",
                "SQFT",
                null,
                new BigDecimal("72"),
                new BigDecimal("24"),
                new BigDecimal("36")
        );
        assertThat(res1.finalMeasurement()).isEqualByComparingTo(new BigDecimal("24.00"));
        assertThat(res1.measurementUnit()).isEqualTo("SQFT");

        // Formula 2 benchmark values: L=23, W=12, H=15 -> ((23+12)*15)/144 = 525/144 = 3.6458 -> 3.65 sq.ft
        FormulaEvaluator.EvaluationResult res2 = FormulaEvaluator.evaluate(
                "(L + W) * H",
                "SQFT",
                null,
                new BigDecimal("23"),
                new BigDecimal("12"),
                new BigDecimal("15")
        );
        assertThat(res2.finalMeasurement()).isEqualByComparingTo(new BigDecimal("3.65"));

        // Surface area formula: L * W with L=36, W=48 -> 1728 / 144 = 12.00 sq.ft
        FormulaEvaluator.EvaluationResult res3 = FormulaEvaluator.evaluate(
                "L * W",
                "SQFT",
                null,
                new BigDecimal("36"),
                new BigDecimal("48"),
                BigDecimal.ZERO
        );
        assertThat(res3.finalMeasurement()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("Formula Evaluator converts inch measurements to Sq. Ft. by dividing by 144")
    void testInchesToSqftConversionRule() {
        // Example from requirements: Length = 120 inches, Height = 96 inches -> Area = (120 × 96) / 144 = 80 Sq. Ft.
        FormulaEvaluator.EvaluationResult resExample = FormulaEvaluator.evaluate(
                "L * H",
                "SQFT",
                null,
                new BigDecimal("120"),
                BigDecimal.ZERO,
                new BigDecimal("96")
        );
        assertThat(resExample.finalMeasurement()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(resExample.measurementUnit()).isEqualTo("SQFT");
        assertThat(resExample.pricingUnit()).isEqualTo("₹ / SQFT");

        // Pricing check: 80 Sq. Ft. * ₹1,200.00 = ₹96,000.00
        BigDecimal rate = new BigDecimal("1200.00");
        BigDecimal itemAmount = CalculationFormula.calculateItemAmount(resExample.finalMeasurement(), rate);
        assertThat(itemAmount).isEqualByComparingTo(new BigDecimal("96000.00"));

        // Formula L × W: Length = 72, Width = 96 -> (72 * 96) / 144 = 48.00 Sq. Ft.
        FormulaEvaluator.EvaluationResult resLW = FormulaEvaluator.evaluate(
                "L * W",
                "SQFT",
                null,
                new BigDecimal("72"),
                new BigDecimal("96"),
                BigDecimal.ZERO
        );
        assertThat(resLW.finalMeasurement()).isEqualByComparingTo(new BigDecimal("48.00"));
        assertThat(resLW.measurementUnit()).isEqualTo("SQFT");

        // Formula (L + W) × H: Length = 72, Width = 24, Height = 36 -> (96 * 36) / 144 = 24.00 Sq. Ft.
        FormulaEvaluator.EvaluationResult resLWH = FormulaEvaluator.evaluate(
                "(L + W) * H",
                "SQFT",
                null,
                new BigDecimal("72"),
                new BigDecimal("24"),
                new BigDecimal("36")
        );
        assertThat(resLWH.finalMeasurement()).isEqualByComparingTo(new BigDecimal("24.00"));
        assertThat(resLWH.measurementUnit()).isEqualTo("SQFT");
    }

    @Test
    @DisplayName("CalculationFormula canonical helper preserves backward compatibility")
    void testCalculationFormulaCompatibility() {
        BigDecimal sqftCanonical = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("23"),
                new BigDecimal("12"),
                new BigDecimal("15")
        );
        assertThat(sqftCanonical).isEqualByComparingTo(new BigDecimal("3.65"));

        BigDecimal sqftDynamic = CalculationFormula.calculateMeasurement(
                "(L + W) * H",
                "SQFT",
                new BigDecimal("23"),
                new BigDecimal("12"),
                new BigDecimal("15")
        ).finalMeasurement();
        assertThat(sqftDynamic).isEqualByComparingTo(new BigDecimal("3.65"));
    }

    @Test
    @DisplayName("Admin Formula Calculation endpoints return correct HTML and JSON")
    void testAdminFormulaControllerEndpoints() {
        // GET /admin/formula-calculation page
        Model model = new ConcurrentModel();
        String view = adminFormulaController.formulaCalculation(model);
        assertThat(view).isEqualTo("admin/formula-calculation");
        assertThat(model.containsAttribute("formulas")).isTrue();
        assertThat(model.containsAttribute("sqftFormulas")).isTrue();
        assertThat(model.containsAttribute("inchesFormulas")).isTrue();

        // GET /admin/formula-calculation/api/test simulator
        Map<String, Object> testRes = adminFormulaController.testFormulaApi(
                "(L + W) * H",
                "SQFT",
                null,
                new BigDecimal("72"),
                new BigDecimal("24"),
                new BigDecimal("36"),
                new BigDecimal("1250")
        );
        assertThat(testRes.get("valid")).isEqualTo(true);
        assertThat(testRes.get("convertedMeasurement")).isEqualTo(new BigDecimal("24.00"));
        assertThat(testRes.get("amount")).isEqualTo(new BigDecimal("30000.00"));

        // GET /admin/formula-calculation/api/validate
        Map<String, Object> valRes = adminFormulaController.validateFormulaApi("L * W");
        assertThat(valRes.get("valid")).isEqualTo(true);
        assertThat(valRes.get("inferredUnitType")).isEqualTo("AREA");
    }

    @Test
    @DisplayName("Configure Master Data saves with configured formula and SQFT unit, rejects INCHES")
    void testSaveMasterDataWithConfiguredFormula() {
        var pricing = pricingService.saveUnifiedMasterData(
                null,
                null,
                "Living Room Custom",
                null,
                "TV Unit Base",
                null,
                "HDF Board",
                null,
                "High Gloss",
                PricingMode.PREMIUM,
                new BigDecimal("450.00"),
                "SQFT",
                "L * W",
                "Custom TV unit surface area pricing"
        );

        assertThat(pricing.getId()).isNotNull();
        assertThat(pricing.getUnit()).isEqualTo("SQFT");
        assertThat(pricing.getFormula()).isEqualTo(CalculationFormula.FORMULA_L_X_W);
        assertThat(pricing.getRate()).isEqualByComparingTo(new BigDecimal("450.00"));

        // Inches unit must be rejected
        assertThatThrownBy(() -> pricingService.saveUnifiedMasterData(
                null,
                null,
                "Living Room Inches",
                null,
                "TV Unit Inches",
                null,
                "HDF Board Inches",
                null,
                "High Gloss Inches",
                PricingMode.PREMIUM,
                new BigDecimal("450.00"),
                "INCHES",
                "L * W",
                "Should fail because inches unit is not permitted"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Inches is not a permitted calculation unit");
    }
}
