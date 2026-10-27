package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.repository.MaterialRepository;
import com.example.pravin_quotation.service.MaterialOptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/material-options")
public class MaterialOptionController {

    private final MaterialOptionService materialOptionService;
    private final MaterialRepository materialRepository;

    public MaterialOptionController(
            MaterialOptionService materialOptionService,
            MaterialRepository materialRepository
    ) {
        this.materialOptionService = materialOptionService;
        this.materialRepository = materialRepository;
    }

    @GetMapping
    public String options(Model model) {
        return "redirect:/admin/work-categories";
    }

    @GetMapping("/new")
    public String newOption(Model model) {

        model.addAttribute(
                "option",
                new MaterialOption()
        );

        model.addAttribute(
                "materials",
                materialRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        return "admin/material-option-form";
    }

    @PostMapping("/save")
    public String saveOption(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Integer displayOrder,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Material option name is required");
            }
            if (materialId == null) {
                throw new IllegalArgumentException("Material is required");
            }
            String effectiveName = name.trim().toUpperCase();
            materialOptionService.createOption(
                    effectiveName,
                    description,
                    materialId,
                    displayOrder
            );
            redirectAttributes.addFlashAttribute("successMessage", "Material option created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/material-options";
    }

    @GetMapping("/edit/{id}")
    public String editOption(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            Model model
    ) {

        model.addAttribute(
                "option",
                materialOptionService.getOptionById(id)
        );

        model.addAttribute(
                "materials",
                materialRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        model.addAttribute(
                "redirectUrl",
                redirectUrl
        );

        return "admin/material-option-form";
    }

    @PostMapping("/update/{id}")
    public String updateOption(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) String redirectUrl,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Material option name is required");
            }
            String effectiveName = name.trim().toUpperCase();
            materialOptionService.updateOption(
                    id,
                    effectiveName,
                    description,
                    materialId,
                    displayOrder
            );
            redirectAttributes.addFlashAttribute("successMessage", "Material option updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/admin/material-options";
    }

    @PostMapping(value = "/api/update/{id}", produces = "application/json")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> apiUpdateOption(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Integer displayOrder
    ) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Material option name is required");
            }
            String effectiveName = name.trim().toUpperCase();
            MaterialOption updated = materialOptionService.updateOption(
                    id,
                    effectiveName,
                    description,
                    materialId,
                    displayOrder
            );
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "id", updated.getId(),
                    "name", updated.getName(),
                    "description", updated.getDescription() != null ? updated.getDescription() : "",
                    "displayOrder", updated.getDisplayOrder() != null ? updated.getDisplayOrder() : 0
            ));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/toggle/{id}")
    public String toggleOption(
            @PathVariable Long id
    ) {

        materialOptionService.toggleOptionStatus(id);

        return "redirect:/admin/material-options";
    }

    @PostMapping("/delete/{id}")
    public Object deleteOption(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            MaterialOption option = materialOptionService.getOptionById(id);
            String optionName = option.getName();
            String message = materialOptionService.deleteOption(id);
            String finalMsg = (message != null && !message.isBlank())
                    ? message
                    : "Material option '" + optionName + "' deleted successfully.";

            if (wantsJson) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "success", true,
                        "message", finalMsg
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", finalMsg);
        } catch (Exception e) {
            if (wantsJson) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", "Failed to delete material option: " + e.getMessage()
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete material option: " + e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/admin/material-options";
    }

    @GetMapping("/by-material/{materialId}")
    @ResponseBody
    public Object getOptionsByMaterial(
            @PathVariable Long materialId
    ) {

        return materialOptionService.getActiveOptionsByMaterial(
                materialId
        );
    }
}