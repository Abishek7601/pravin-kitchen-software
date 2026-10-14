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

import jakarta.servlet.http.HttpServletRequest;
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
    private final com.example.pravin_quotation.service.FormulaConfigService formulaConfigService;
    private final MaterialSpecificationRepository materialSpecificationRepository;

    public PricingController(
            PricingService pricingService,
            MaterialOptionRepository materialOptionRepository,
            MaterialRepository materialRepository,
            ItemRepository itemRepository,
            DivisionRepository divisionRepository,
            WorkCategoryRepository workCategoryRepository,
            com.example.pravin_quotation.service.FormulaConfigService formulaConfigService,
            MaterialSpecificationRepository materialSpecificationRepository
    ) {
        this.pricingService = pricingService;
        this.materialOptionRepository = materialOptionRepository;
        this.materialRepository = materialRepository;
        this.itemRepository = itemRepository;
        this.divisionRepository = divisionRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.formulaConfigService = formulaConfigService;
        this.materialSpecificationRepository = materialSpecificationRepository;
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
    public String newPricing(
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) String returnUrl,
            HttpServletRequest request,
            Model model
    ) {
        String catName = "";
        String catEmoji = "";
        WorkCategory selectedCategory = null;
        if (workCategoryId != null) {
            Optional<WorkCategory> wcOpt = workCategoryRepository.findById(workCategoryId);
            if (wcOpt.isPresent()) {
                selectedCategory = wcOpt.get();
                catName = selectedCategory.getName();
                catEmoji = selectedCategory.getIcon() != null ? selectedCategory.getIcon() : "";
            }
        }

        model.addAttribute("pricing", new Pricing());
        model.addAttribute("isEdit", false);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("workCategoryId", workCategoryId != null ? workCategoryId : "");
        model.addAttribute("categoryName", catName);
        model.addAttribute("categoryEmoji", catEmoji);
        model.addAttribute("divisionName", "");
        model.addAttribute("materialName", "");
        model.addAttribute("materialOptionName", "");
        model.addAttribute("specificationName", "");

        // Determine context-aware Cancel URL
        String referer = request != null ? request.getHeader("Referer") : null;
        String effectiveCancelUrl = "/admin/work-categories";
        if (returnUrl != null && !returnUrl.isBlank()) {
            effectiveCancelUrl = returnUrl;
        } else if (workCategoryId != null) {
            effectiveCancelUrl = "/admin/work-categories/view/" + workCategoryId;
        } else if (referer != null && !referer.isBlank()) {
            if (referer.contains("/admin/pricing") && !referer.contains("/admin/pricing/new") && !referer.contains("/admin/pricing/configure")) {
                effectiveCancelUrl = "/admin/pricing";
            } else if (referer.contains("/admin/work-categories")) {
                effectiveCancelUrl = referer;
            } else if (referer.contains("/admin/dashboard")) {
                effectiveCancelUrl = "/admin/dashboard";
            }
        }
        model.addAttribute("cancelUrl", effectiveCancelUrl);

        populateUnifiedFormModel(model, null);

        return "admin/pricing-form";
    }

    @PostMapping("/save")
    public String savePricing(
            @RequestParam(required = false) Long pricingId,
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) String workCategoryName,
            @RequestParam(required = false) String newWorkCategoryName,
            @RequestParam(required = false) String categoryEmoji,
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
            @RequestParam(required = false) String specificationName,
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
            String optName = firstNonBlank(materialOptionName, newMaterialOptionName);
            String specName = firstNonBlank(specificationName, specification);

            if (catName != null) catName = catName.trim().toUpperCase();
            if (divName != null) divName = divName.trim().toUpperCase();
            if (matName != null) matName = matName.trim().toUpperCase();
            if (optName != null) optName = optName.trim().toUpperCase();
            if (specName != null) specName = specName.trim().toUpperCase();

            pricingService.saveUnifiedMasterData(
                    pricingId,
                    workCategoryId,
                    catName,
                    categoryEmoji,
                    divisionId,
                    divName,
                    materialId,
                    matName,
                    materialOptionId,
                    optName,
                    specName,
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
            @RequestParam(required = false) String returnUrl,
            HttpServletRequest request,
            Model model
    ) {
        Pricing pricing = pricingService.getPricingById(id);
        model.addAttribute("pricing", pricing);
        model.addAttribute("isEdit", true);

        String catName = "";
        String catEmoji = "";
        String divName = "";
        String matName = "";
        String optName = "";
        String specName = "";
        Long catId = null;

        if (pricing.getMaterialOption() != null) {
            MaterialOption option = pricing.getMaterialOption();
            if (option.getName() != null && !option.getName().equalsIgnoreCase("Standard")) {
                optName = option.getName();
            }
            List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(option.getId());
            if (!specs.isEmpty()) {
                specName = specs.get(0).getName();
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
                            catEmoji = wc.getIcon() != null ? wc.getIcon() : "";
                            catId = wc.getId();
                        }
                    }
                }
                if (divName.isEmpty() && mat.getDivisions() != null && !mat.getDivisions().isEmpty()) {
                    Division div = mat.getDivisions().iterator().next();
                    divName = div.getName();
                    if (div.getWorkCategory() != null) {
                        catName = div.getWorkCategory().getName();
                        catEmoji = div.getWorkCategory().getIcon() != null ? div.getWorkCategory().getIcon() : "";
                        catId = div.getWorkCategory().getId();
                    }
                }
            }
        }

        model.addAttribute("categoryName", catName);
        model.addAttribute("categoryEmoji", catEmoji);
        model.addAttribute("divisionName", divName);
        model.addAttribute("materialName", matName);
        model.addAttribute("materialOptionName", optName);
        model.addAttribute("specificationName", !specName.isEmpty() ? specName : (pricing.getDescription() != null ? pricing.getDescription() : ""));

        // Determine context-aware Cancel URL
        String referer = request != null ? request.getHeader("Referer") : null;
        String effectiveCancelUrl = "/admin/pricing";
        if (returnUrl != null && !returnUrl.isBlank()) {
            effectiveCancelUrl = returnUrl;
        } else if (referer != null && !referer.isBlank()) {
            if (referer.contains("/admin/work-categories")) {
                effectiveCancelUrl = referer;
            } else if (referer.contains("/admin/pricing") && !referer.contains("/admin/pricing/edit")) {
                effectiveCancelUrl = "/admin/pricing";
            } else if (referer.contains("/admin/dashboard")) {
                effectiveCancelUrl = "/admin/dashboard";
            }
        } else if (catId != null) {
            effectiveCancelUrl = "/admin/work-categories/view/" + catId;
        }
        model.addAttribute("cancelUrl", effectiveCancelUrl);

        populateUnifiedFormModel(model, pricing);

        return "admin/pricing-form";
    }

    public String newPricing(Model model) {
        return newPricing(null, null, null, model);
    }

    public String editPricing(Long id, Model model) {
        return editPricing(id, null, null, model);
    }

    @PostMapping("/update/{id}")
    public String updatePricing(
            @PathVariable Long id,
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) String workCategoryName,
            @RequestParam(required = false) String newWorkCategoryName,
            @RequestParam(required = false) String categoryEmoji,
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
            @RequestParam(required = false) String specificationName,
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
            String optName = firstNonBlank(materialOptionName, newMaterialOptionName);
            String specName = firstNonBlank(specificationName, specification);

            pricingService.saveUnifiedMasterData(
                    id,
                    workCategoryId,
                    catName,
                    categoryEmoji,
                    divisionId,
                    divName,
                    materialId,
                    matName,
                    materialOptionId,
                    optName,
                    specName,
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
            String workCategoryName,
            String newWorkCategoryName,
            Long divisionId,
            String divisionName,
            String newDivisionName,
            Long materialId,
            String materialName,
            String newMaterialName,
            Long materialOptionId,
            String materialOptionName,
            String newMaterialOptionName,
            String specification,
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
                workCategoryName,
                newWorkCategoryName,
                null,
                divisionId,
                divisionName,
                newDivisionName,
                materialId,
                materialName,
                newMaterialName,
                materialOptionId,
                materialOptionName,
                newMaterialOptionName,
                specification,
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
            String workCategoryName,
            String newWorkCategoryName,
            Long divisionId,
            String divisionName,
            String newDivisionName,
            Long materialId,
            String materialName,
            String newMaterialName,
            Long materialOptionId,
            String materialOptionName,
            String newMaterialOptionName,
            String specification,
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
                workCategoryName,
                newWorkCategoryName,
                null,
                divisionId,
                divisionName,
                newDivisionName,
                materialId,
                materialName,
                newMaterialName,
                materialOptionId,
                materialOptionName,
                newMaterialOptionName,
                specification,
                null,
                pricingMode,
                rate,
                unit,
                formula,
                description,
                redirectAttributes
        );
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
                null,
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
                null,
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
        List<Material> materials = materialRepository.findActiveByDivisionId(divisionId);
        return materials.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("name", m.getName());
                    map.put("formula", m.getFormula() != null ? m.getFormula() : "(L + W) × H");
                    return map;
                })
                .toList();
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

    @GetMapping("/api/material-specifications")
    @ResponseBody
    public List<Map<String, Object>> getMaterialSpecifications(
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam(required = false) Long materialId) {
        if (materialOptionId != null) {
            return materialSpecificationRepository.findByMaterialOptionIdAndActiveTrueOrderByDisplayOrderAsc(materialOptionId)
                    .stream()
                    .map(s -> Map.<String, Object>of(
                            "id", s.getId(),
                            "name", s.getName(),
                            "description", s.getDescription() != null ? s.getDescription() : ""
                    ))
                    .toList();
        } else if (materialId != null) {
            return materialSpecificationRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                    .stream()
                    .map(s -> Map.<String, Object>of(
                            "id", s.getId(),
                            "name", s.getName(),
                            "description", s.getDescription() != null ? s.getDescription() : ""
                    ))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void populateUnifiedFormModel(Model model, Pricing pricing) {
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("formulaOptions", CalculationFormula.ALL_FORMULAS);
        model.addAttribute("formulas", CalculationFormula.ALL_FORMULAS);
        model.addAttribute("configuredFormulas", formulaConfigService.getActiveFormulas());
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