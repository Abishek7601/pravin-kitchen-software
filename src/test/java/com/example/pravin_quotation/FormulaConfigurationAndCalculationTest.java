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

        List<FormulaConfig> inchesFormulas = formulaConfigService.getFormulasByType("INCHES");
        assertThat(inchesFormulas).isNotEmpty();
        assertThat(inchesFormulas).anyMatch(f -> f.getExpression().equals("L"));
    }

    @Test
    @DisplayName("Admin can create, update, toggle, and delete custom formulas")
    void testFormulaCrudOperations() {
        FormulaConfig created = formulaConfigService.createFormula(
                "Test Custom Perimeter",
                "2 * (L + W)",
                "INCHES",
                "LINEAR",
                "INCH",
                "₹ / INCH",
                "Linear perimeter in inches",
                99
        );
        assertThat(created.getId()).isNotNull();
        assertThat(created.getIsSystem()).isFalse();
        assertThat(created.getActive()).isTrue();

        // Update
        FormulaConfig updated = formulaConfigService.updateFormula(
                created.getId(),
                "Test Custom Perimeter Updated",
                "2 * (L + W)",
                "INCHES",
                "LINEAR",
                "INCH",
                "₹ / INCH",
                "Updated description",
                100
        );
        assertThat(updated.getName()).isEqualTo("Test Custom Perimeter Updated");
        assertThat(updated.getDescription()).isEqualTo("Updated description");

        // Toggle Status
        formulaConfigService.toggleFormulaStatus(created.getId());
        FormulaConfig toggled = formulaConfigService.getFormulaById(created.getId());
        assertThat(toggled.getActive()).isFalse();

        // Delete
        formulaConfigService.deleteFormula(created.getId());
        assertThat(formulaConfigRepository.findById(created.getId())).isEmpty();
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
    @DisplayName("Formula Evaluator computes Inches calculations without dividing by 144")
    void testInchesCalculation() {
        // Area in sq.in: (L + W) * H with L=72, W=24, H=36 -> 96 * 36 = 3456.00 sq.in
        FormulaEvaluator.EvaluationResult res1 = FormulaEvaluator.evaluate(
                "(L + W) * H",
                "INCHES",
                null,
                new BigDecimal("72"),
                new BigDecimal("24"),
                new BigDecimal("36")
        );
        assertThat(res1.finalMeasurement()).isEqualByComparingTo(new BigDecimal("3456.00"));
        assertThat(res1.measurementUnit()).isEqualTo("SQIN");

        // Linear inches: L with L=48 -> 48.00 in
        FormulaEvaluator.EvaluationResult res2 = FormulaEvaluator.evaluate(
                "L",
                "INCHES",
                null,
                new BigDecimal("48"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        assertThat(res2.finalMeasurement()).isEqualByComparingTo(new BigDecimal("48.00"));
        assertThat(res2.measurementUnit()).isEqualTo("INCH");

        // Perimeter in linear inches: 2 * (L + W) with L=30, W=18 -> 2 * 48 = 96.00 in
        FormulaEvaluator.EvaluationResult res3 = FormulaEvaluator.evaluate(
                "2 * (L + W)",
                "INCHES",
                null,
                new BigDecimal("30"),
                new BigDecimal("18"),
                BigDecimal.ZERO
        );
        assertThat(res3.finalMeasurement()).isEqualByComparingTo(new BigDecimal("96.00"));
        assertThat(res3.measurementUnit()).isEqualTo("INCH");
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
    @DisplayName("Configure Master Data saves with configured formula and unit (Sqft / Inches)")
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
        assertThat(pricing.getFormula()).isEqualTo("L * W");
        assertThat(pricing.getRate()).isEqualByComparingTo(new BigDecimal("450.00"));
    }
}
