package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.service.MaterialSpecificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/material-specifications")
public class MaterialSpecificationController {

    private final MaterialSpecificationService materialSpecificationService;

    public MaterialSpecificationController(MaterialSpecificationService materialSpecificationService) {
        this.materialSpecificationService = materialSpecificationService;
    }

    @PostMapping("/save")
    public String saveSpecification(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long materialOptionId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false, defaultValue = "/admin/master-data/hierarchy") String redirectUrl,
            RedirectAttributes redirectAttributes
    ) {
        try {
            materialSpecificationService.saveSpecification(materialId, materialOptionId, name, description, displayOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Material specification saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/toggle/{id}")
    public String toggleSpecification(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "/admin/master-data/hierarchy") String redirectUrl
    ) {
        materialSpecificationService.toggleStatus(id);
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/delete/{id}")
    public String deleteSpecification(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "/admin/master-data/hierarchy") String redirectUrl,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String msg = materialSpecificationService.deleteSpecification(id);
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + redirectUrl;
    }
}
