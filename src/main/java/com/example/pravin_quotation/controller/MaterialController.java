package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.service.MaterialService;
import com.example.pravin_quotation.util.CalculationFormula;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final ItemRepository itemRepository;
    private final DivisionRepository divisionRepository;

    public MaterialController(
            MaterialService materialService,
            ItemRepository itemRepository,
            DivisionRepository divisionRepository
    ) {
        this.materialService = materialService;
        this.itemRepository = itemRepository;
        this.divisionRepository = divisionRepository;
    }

    private String resolveRedirectUrl(String redirectUrl, HttpServletRequest request, String defaultFallback) {
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            String trimmed = redirectUrl.trim();
            if (trimmed.startsWith("/") && !trimmed.startsWith("//") && !trimmed.startsWith("/\\")) {
                return trimmed;
            }
        }
        if (request != null) {
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("/admin/work-categories/view/")) {
                try {
                    java.net.URI uri = java.net.URI.create(referer);
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/\\")) {
                        return path;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return defaultFallback;
    }

    @GetMapping
    public String materials(Model model) {
        return "redirect:/admin/work-categories";
    }

    @GetMapping("/new")
    public String newMaterial(
            @RequestParam(required = false) String redirectUrl,
            Model model
    ) {
        model.addAttribute(
                "material",
                new Material()
        );
        model.addAttribute(
                "items",
                itemRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );
        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );
        model.addAttribute(
                "formulas",
                CalculationFormula.ALL_FORMULAS
        );
        model.addAttribute(
                "redirectUrl",
                resolveRedirectUrl(redirectUrl, null, null)
        );
        return "admin/material-form";
    }

    @PostMapping("/save")
    public String saveMaterial(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) List<Long> divisionIds,
            @RequestParam(required = false) String formula,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) String redirectUrl
    ) {
        String effectiveName = name != null ? name.trim().toUpperCase() : null;
        materialService.createMaterial(
                effectiveName,
                description,
                itemId,
                divisionIds,
                formula,
                displayOrder
        );
        return "redirect:" + resolveRedirectUrl(redirectUrl, null, "/admin/materials");
    }

    @GetMapping("/edit/{id}")
    public String editMaterial(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute(
                "material",
                materialService.getMaterialById(id)
        );
        model.addAttribute(
                "items",
                itemRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );
        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );
        model.addAttribute(
                "formulas",
                CalculationFormula.ALL_FORMULAS
        );
        model.addAttribute(
                "redirectUrl",
                resolveRedirectUrl(redirectUrl, request, null)
        );
        return "admin/material-form";
    }

    @PostMapping("/update/{id}")
    public String updateMaterial(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) List<Long> divisionIds,
            @RequestParam(required = false) String formula,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) String redirectUrl
    ) {
        String effectiveName = name != null ? name.trim().toUpperCase() : null;
        materialService.updateMaterial(
                id,
                effectiveName,
                description,
                itemId,
                divisionIds,
                formula,
                displayOrder
        );
        return "redirect:" + resolveRedirectUrl(redirectUrl, null, "/admin/materials");
    }

    @PostMapping("/toggle/{id}")
    public String toggleMaterial(
            @PathVariable Long id
    ) {
        materialService.toggleMaterialStatus(id);
        return "redirect:/admin/materials";
    }

    @PostMapping("/delete/{id}")
    public Object deleteMaterial(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            Material material = materialService.getMaterialById(id);
            String materialName = material.getName();
            String message = materialService.deleteMaterial(id);
            String finalMsg = (message != null && !message.isBlank())
                    ? message
                    : "Material '" + materialName + "' deleted successfully.";

            if (wantsJson) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", finalMsg
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", finalMsg);
        } catch (Exception e) {
            if (wantsJson) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Failed to delete material: " + e.getMessage()
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete material: " + e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/admin/materials";
    }

    @GetMapping("/by-item/{itemId}")
    @ResponseBody
    public Object getMaterialsByItem(
            @PathVariable Long itemId
    ) {
        return materialService.getActiveMaterialsByItem(
                itemId
        );
    }

    @GetMapping("/by-division/{divisionId}")
    @ResponseBody
    public Object getMaterialsByDivision(
            @PathVariable Long divisionId
    ) {
        return materialService.getActiveMaterialsByDivision(
                divisionId
        );
    }

    @PostMapping("/map-division")
    public String mapDivision(
            @RequestParam Long materialId,
            @RequestParam Long divisionId
    ) {
        materialService.mapDivision(materialId, divisionId);
        return "redirect:/admin/materials";
    }

    @PostMapping("/unmap-division")
    public String unmapDivision(
            @RequestParam Long materialId,
            @RequestParam Long divisionId
    ) {
        materialService.unmapDivision(materialId, divisionId);
        return "redirect:/admin/materials";
    }
}