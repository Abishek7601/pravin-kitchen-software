package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.FormulaConfig;
import com.example.pravin_quotation.service.FormulaConfigService;
import com.example.pravin_quotation.util.FormulaEvaluator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminFormulaController {

    private final FormulaConfigService formulaConfigService;

    public AdminFormulaController(FormulaConfigService formulaConfigService) {
        this.formulaConfigService = formulaConfigService;
    }

    @GetMapping("/formula-calculation")
    public String formulaCalculation(Model model) {
        List<FormulaConfig> allFormulas = formulaConfigService.getAllFormulas();
        List<FormulaConfig> sqftFormulas = formulaConfigService.getFormulasByType("SQFT");
        List<FormulaConfig> inchesFormulas = formulaConfigService.getFormulasByType("INCHES");

        model.addAttribute("formulas", allFormulas);
        model.addAttribute("sqftFormulas", sqftFormulas);
        model.addAttribute("inchesFormulas", inchesFormulas);
        return "admin/formula-calculation";
    }

    @PostMapping("/formula-calculation/save")
    public String saveFormula(
            @RequestParam("name") String name,
            @RequestParam("expression") String expression,
            @RequestParam(value = "calculationType", defaultValue = "SQFT") String calculationType,
            @RequestParam(value = "unitType", required = false) String unitType,
            @RequestParam(value = "measurementUnit", required = false) String measurementUnit,
            @RequestParam(value = "pricingUnit", required = false) String pricingUnit,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "displayOrder", required = false, defaultValue = "0") Integer displayOrder,
            RedirectAttributes redirectAttributes
    ) {
        try {
            formulaConfigService.createFormula(
                    name,
                    expression,
                    calculationType,
                    unitType,
                    measurementUnit,
                    pricingUnit,
                    description,
                    displayOrder
            );
            redirectAttributes.addFlashAttribute("success", "Calculation formula '" + name.trim() + "' added successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to add formula: " + ex.getMessage());
        }
        return "redirect:/admin/formula-calculation";
    }

    @PostMapping("/formula-calculation/update/{id}")
    public String updateFormula(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("expression") String expression,
            @RequestParam(value = "calculationType", defaultValue = "SQFT") String calculationType,
            @RequestParam(value = "unitType", required = false) String unitType,
            @RequestParam(value = "measurementUnit", required = false) String measurementUnit,
            @RequestParam(value = "pricingUnit", required = false) String pricingUnit,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "displayOrder", required = false, defaultValue = "0") Integer displayOrder,
            RedirectAttributes redirectAttributes
    ) {
        try {
            formulaConfigService.updateFormula(
                    id,
                    name,
                    expression,
                    calculationType,
                    unitType,
                    measurementUnit,
                    pricingUnit,
                    description,
                    displayOrder
            );
            redirectAttributes.addFlashAttribute("success", "Calculation formula '" + name.trim() + "' updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to update formula: " + ex.getMessage());
        }
        return "redirect:/admin/formula-calculation";
    }

    @PostMapping("/formula-calculation/toggle/{id}")
    public String toggleFormula(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            formulaConfigService.toggleFormulaStatus(id);
            FormulaConfig config = formulaConfigService.getFormulaById(id);
            redirectAttributes.addFlashAttribute("success",
                    "Formula '" + config.getName() + "' is now " + (Boolean.TRUE.equals(config.getActive()) ? "ACTIVE" : "INACTIVE") + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to toggle status: " + ex.getMessage());
        }
        return "redirect:/admin/formula-calculation";
    }

    @PostMapping("/formula-calculation/delete/{id}")
    public String deleteFormula(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            formulaConfigService.deleteFormula(id);
            redirectAttributes.addFlashAttribute("success", "Formula deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete formula: " + ex.getMessage());
        }
        return "redirect:/admin/formula-calculation";
    }

    @GetMapping("/formula-calculation/api/test")
    @ResponseBody
    public Map<String, Object> testFormulaApi(
            @RequestParam("expression") String expression,
            @RequestParam(value = "calculationType", defaultValue = "SQFT") String calculationType,
            @RequestParam(value = "unitType", required = false) String unitType,
            @RequestParam(value = "length", defaultValue = "0") BigDecimal length,
            @RequestParam(value = "width", defaultValue = "0") BigDecimal width,
            @RequestParam(value = "height", defaultValue = "0") BigDecimal height,
            @RequestParam(value = "rate", required = false) BigDecimal rate
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            FormulaEvaluator.EvaluationResult result = FormulaEvaluator.evaluate(
                    expression,
                    calculationType,
                    unitType,
                    length,
                    width,
                    height
            );

            BigDecimal safeRate = (rate != null) ? rate : BigDecimal.ZERO;
            BigDecimal amount = result.finalMeasurement().multiply(safeRate).setScale(2, RoundingMode.HALF_UP);

            response.put("valid", true);
            response.put("expression", expression);
            response.put("calculationType", calculationType);
            response.put("unitType", result.unitType());
            response.put("measurementUnit", result.measurementUnit());
            response.put("convertedMeasurement", result.finalMeasurement());
            response.put("rawInchesResult", result.rawValue());
            response.put("rate", safeRate);
            response.put("amount", amount);
        } catch (Exception ex) {
            response.put("valid", false);
            response.put("error", ex.getMessage());
        }
        return response;
    }

    @GetMapping("/formula-calculation/api/validate")
    @ResponseBody
    public Map<String, Object> validateFormulaApi(@RequestParam("expression") String expression) {
        Map<String, Object> response = new HashMap<>();
        FormulaEvaluator.ValidationResult result = FormulaEvaluator.validateExpression(expression);
        response.put("valid", result.valid());
        response.put("errorMessage", result.errorMessage());
        response.put("normalizedExpression", expression);
        response.put("inferredUnitType", result.inferredUnitType());
        response.put("usedVariables", result.variablesUsed());
        return response;
    }
}
