package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/quotations")
public class AdminQuotationBuilderController {

    private final BranchRepository branchRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final DivisionRepository divisionRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final MaterialOptionRepository materialOptionRepository;
    private final AdminQuotationBuilderService builderService;

    public AdminQuotationBuilderController(
            BranchRepository branchRepository,
            WorkCategoryRepository workCategoryRepository,
            DivisionRepository divisionRepository,
            ItemRepository itemRepository,
            MaterialRepository materialRepository,
            MaterialOptionRepository materialOptionRepository,
            AdminQuotationBuilderService builderService) {
        this.branchRepository = branchRepository;
        this.workCategoryRepository = workCategoryRepository;
        this.divisionRepository = divisionRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.materialOptionRepository = materialOptionRepository;
        this.builderService = builderService;
    }

    @GetMapping("/builder")
    public String builder(Model model) {
        model.addAttribute("branches", branchRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        model.addAttribute("pricingModes", PricingMode.values());
        model.addAttribute("today", LocalDate.now());
        return "admin/quotation-builder";
    }

    @GetMapping("/builder/divisions")
    @ResponseBody
    public List<Map<String, Object>> divisions(@RequestParam Long workCategoryId) {
        return divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategoryId)
                .stream().map(d -> map("id", d.getId(), "name", d.getName(), "description", d.getDescription()))
                .toList();
    }

    @GetMapping("/builder/items")
    @ResponseBody
    public List<Map<String, Object>> items(@RequestParam Long divisionId) {
        return itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(divisionId)
                .stream().map(i -> map("id", i.getId(), "name", i.getName(), "description", i.getDescription()))
                .toList();
    }

    @GetMapping("/builder/materials")
    @ResponseBody
    public List<Map<String, Object>> materials(@RequestParam Long itemId) {
        return materialRepository.findByItemIdAndActiveTrueOrderByDisplayOrderAsc(itemId)
                .stream().map(m -> map("id", m.getId(), "name", m.getName(), "description", m.getDescription()))
                .toList();
    }

    @GetMapping("/builder/material-options")
    @ResponseBody
    public List<Map<String, Object>> materialOptions(@RequestParam Long materialId) {
        return materialOptionRepository.findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc(materialId)
                .stream().map(o -> map("id", o.getId(), "name", o.getName(), "description", o.getDescription()))
                .toList();
    }

    @GetMapping("/builder/rate")
    @ResponseBody
    public ResponseEntity<?> rate(
            @RequestParam Long branchId,
            @RequestParam Long materialOptionId,
            @RequestParam PricingMode pricingMode) {
        try {
            BigDecimal rate = builderService.resolveRate(branchId, materialOptionId, pricingMode);
            return ResponseEntity.ok(map("rate", rate, "pricingMode", pricingMode.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(map("message", e.getMessage()));
        }
    }

    @PostMapping("/builder/save")
    public String save(
            @RequestBody AdminQuotationBuilderService.BuilderRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Quotation quotation = builderService.save(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Quotation " + quotation.getQuotationNumber() + " created successfully.");
            return "redirect:/admin/quotations/view/" + quotation.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/quotations/builder";
        }
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}