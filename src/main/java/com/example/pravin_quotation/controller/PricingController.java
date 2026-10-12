package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/admin/pricing")
public class PricingController {

    private final PricingService pricingService;
    private final MaterialOptionRepository materialOptionRepository;
    private final MaterialRepository materialRepository;
    private final ItemRepository itemRepository;
    private final DivisionRepository divisionRepository;
    private final WorkCategoryRepository workCategoryRepository;

    public PricingController(
            PricingService pricingService,
            MaterialOptionRepository materialOptionRepository,
            MaterialRepository materialRepository,
            ItemRepository itemRepository,
            DivisionRepository divisionRepository,
            WorkCategoryRepository workCategoryRepository
    ) {
        this.pricingService = pricingService;
        this.materialOptionRepository = materialOptionRepository;
        this.materialRepository = materialRepository;
        this.itemRepository = itemRepository;
        this.divisionRepository = divisionRepository;
        this.workCategoryRepository = workCategoryRepository;
    }

    @GetMapping
    public String pricing(
            @RequestParam(required = false) String error,
            Model model
    ) {

        model.addAttribute(
                "pricingList",
                pricingService.getAllPricing()
        );

        if (error != null && !error.isBlank()) {
            model.addAttribute("deleteError", error);
        }

        return "admin/pricing";
    }

    @GetMapping({"/new", "/configure"})
    public String newPricing(Model model) {

        model.addAttribute("pricing", new Pricing());
        model.addAttribute("isEdit", false);
        model.addAttribute("categoryName", "");
        model.addAttribute("divisionName", "");
        model.addAttribute("materialName", "");
        model.addAttribute("specificationName", "");
        populateUnifiedFormModel(model, null);

        return "admin/pricing-form";
    }

    @PostMapping("/save")
    public String savePricing(
            @RequestParam(required = false) Long pricingId,
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) String workCategoryName,
            @RequestParam(required = false) String newWorkCategoryName,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) String divisionName,
            @RequestParam(required = false) String newDivisionName,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String newMaterialName,
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) String materialOptionName,
            @RequestParam(required = false) String newMaterialOptionName,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false) PricingMode pricingMode,
            @RequestParam BigDecimal rate,
            @RequestParam(defaultValue = "SQFT") String unit,
            @RequestParam(required = false) String formula,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String catName = firstNonBlank(workCategoryName, newWorkCategoryName);
            String divName = firstNonBlank(divisionName, newDivisionName);
            String matName = firstNonBlank(materialName, newMaterialName);
            String optName = firstNonBlank(materialOptionName, newMaterialOptionName, specification);

            pricingService.saveUnifiedMasterData(
                    pricingId,
                    workCategoryId,
                    catName,
                    divisionId,
                    divName,
                    materialId,
                    matName,
                    materialOptionId,
                    optName,
                    pricingMode,
                    rate,
                    unit,
                    formula,
                    description
            );
            redirectAttributes.addFlashAttribute("successMessage", "Master data and pricing configuration saved successfully.");
            return "redirect:/admin/pricing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/pricing/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String editPricing(
            @PathVariable Long id,
            Model model
    ) {
        Pricing pricing = pricingService.getPricingById(id);
        model.addAttribute("pricing", pricing);
        model.addAttribute("isEdit", true);

        String catName = "";
        String divName = "";
        String matName = "";
        String optName = "";

        if (pricing.getMaterialOption() != null) {
            MaterialOption option = pricing.getMaterialOption();
            if (option.getName() != null && !option.getName().equalsIgnoreCase("Standard")) {
                optName = option.getName();
            }
            Material mat = option.getMaterial();
            if (mat != null) {
                matName = mat.getName();
                Item item = mat.getItem();
                if (item != null) {
                    Division div = item.getDivision();
                    if (div != null) {
                        divName = div.getName();
                        WorkCategory wc = div.getWorkCategory();
                        if (wc != null) {
                            catName = wc.getName();
                        }
                    }
                }
            }
        }

        model.addAttribute("categoryName", catName);
        model.addAttribute("divisionName", divName);
        model.addAttribute("materialName", matName);
        model.addAttribute("specificationName", optName);

        populateUnifiedFormModel(model, pricing);

        return "admin/pricing-form";
    }

    @PostMapping("/update/{id}")
    public String updatePricing(
            @PathVariable Long id,
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) String workCategoryName,
            @RequestParam(required = false) String newWorkCategoryName,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) String divisionName,
            @RequestParam(required = false) String newDivisionName,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String newMaterialName,
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) String materialOptionName,
            @RequestParam(required = false) String newMaterialOptionName,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false) PricingMode pricingMode,
            @RequestParam BigDecimal rate,
            @RequestParam(defaultValue = "SQFT") String unit,
            @RequestParam(required = false) String formula,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String catName = firstNonBlank(workCategoryName, newWorkCategoryName);
            String divName = firstNonBlank(divisionName, newDivisionName);
            String matName = firstNonBlank(materialName, newMaterialName);
            String optName = firstNonBlank(materialOptionName, newMaterialOptionName, specification);

            pricingService.saveUnifiedMasterData(
                    id,
                    workCategoryId,
                    catName,
                    divisionId,
                    divName,
                    materialId,
                    matName,
                    materialOptionId,
                    optName,
                    pricingMode,
                    rate,
                    unit,
                    formula,
                    description
            );
            redirectAttributes.addFlashAttribute("successMessage", "Pricing configuration updated successfully.");
            return "redirect:/admin/pricing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/pricing/edit/" + id;
        }
    }

    public String savePricing(
            Long pricingId,
            Long workCategoryId,
            String newWorkCategoryName,
            Long divisionId,
            String newDivisionName,
            Long materialId,
            String newMaterialName,
            Long materialOptionId,
            String newMaterialOptionName,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String formula,
            String description,
            RedirectAttributes redirectAttributes
    ) {
        return savePricing(
                pricingId,
                workCategoryId,
                null,
                newWorkCategoryName,
                divisionId,
                null,
                newDivisionName,
                materialId,
                null,
                newMaterialName,
                materialOptionId,
                null,
                newMaterialOptionName,
                null,
                pricingMode,
                rate,
                unit,
                formula,
                description,
                redirectAttributes
        );
    }

    public String updatePricing(
            Long id,
            Long workCategoryId,
            String newWorkCategoryName,
            Long divisionId,
            String newDivisionName,
            Long materialId,
            String newMaterialName,
            Long materialOptionId,
            String newMaterialOptionName,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String formula,
            String description,
            RedirectAttributes redirectAttributes
    ) {
        return updatePricing(
                id,
                workCategoryId,
                null,
                newWorkCategoryName,
                divisionId,
                null,
                newDivisionName,
                materialId,
                null,
                newMaterialName,
                materialOptionId,
                null,
                newMaterialOptionName,
                null,
                pricingMode,
                rate,
                unit,
                formula,
                description,
                redirectAttributes
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    // =========================================================
    // AJAX DEPENDENT DROPDOWNS & RESOLUTION API
    // =========================================================

    @GetMapping("/api/divisions")
    @ResponseBody
    public List<Map<String, Object>> getDivisions(@RequestParam Long workCategoryId) {
        return divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategoryId)
                .stream()
                .map(d -> Map.<String, Object>of("id", d.getId(), "name", d.getName()))
                .toList();
    }

    @GetMapping("/api/materials")
    @ResponseBody
    public List<Map<String, Object>> getMaterials(@RequestParam Long divisionId) {
        // Collect materials under all items of this division
        List<Item> items = itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(divisionId);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Item item : items) {
            List<Material> materials = materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(item.getId());
            for (Material m : materials) {
                if (seen.add(m.getId())) {
                    result.add(Map.of("id", m.getId(), "name", m.getName()));
                }
            }
        }
        return result;
    }

    @GetMapping("/api/material-options")
    @ResponseBody
    public List<Map<String, Object>> getMaterialOptions(@RequestParam Long materialId) {
        return materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                .stream()
                .map(o -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", o.getId());
                    map.put("name", o.getName());
                    map.put("formula", o.getFormula() != null ? o.getFormula() : CalculationFormula.FORMULA_L_X_W);
                    return map;
                })
                .toList();
    }

    private void populateUnifiedFormModel(Model model, Pricing pricing) {
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("formulaOptions", CalculationFormula.ALL_FORMULAS);
        model.addAttribute("formulas", CalculationFormula.ALL_FORMULAS);
        model.addAttribute("divisions", Collections.emptyList());
        model.addAttribute("materials", Collections.emptyList());
        model.addAttribute("materialOptions", Collections.emptyList());

        if (pricing != null && pricing.getMaterialOption() != null) {
            MaterialOption option = pricing.getMaterialOption();
            Material material = option.getMaterial();
            Item item = material != null ? material.getItem() : null;
            Division division = item != null ? item.getDivision() : null;
            WorkCategory category = division != null ? division.getWorkCategory() : null;

            model.addAttribute("selectedOption", option);
            model.addAttribute("selectedMaterial", material);
            model.addAttribute("selectedDivision", division);
            model.addAttribute("selectedCategory", category);

            if (category != null) {
                model.addAttribute("divisions", divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(category.getId()));
            }
            if (division != null) {
                List<Item> items = itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(division.getId());
                List<Material> mats = new ArrayList<>();
                for (Item it : items) {
                    mats.addAll(materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(it.getId()));
                }
                model.addAttribute("materials", mats);
            }
            if (material != null) {
                model.addAttribute("materialOptions", materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(material.getId()));
            }
        }
    }

    @PostMapping("/toggle/{id}")
    public String togglePricing(
            @PathVariable Long id
    ) {

        pricingService.togglePricingStatus(id);

        return "redirect:/admin/pricing";
    }

    @PostMapping("/delete/{id}")
    public String deletePricing(
            @PathVariable Long id
    ) {

        try {

            pricingService.deletePricing(id);

            return "redirect:/admin/pricing";

        } catch (IllegalStateException e) {

            return "redirect:/admin/pricing?error="
                    + java.net.URLEncoder.encode(
                    e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
        }
    }
}