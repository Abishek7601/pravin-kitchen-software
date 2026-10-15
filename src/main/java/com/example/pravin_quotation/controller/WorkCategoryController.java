package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/work-categories")
public class WorkCategoryController {

    private final WorkCategoryService workCategoryService;
    private final MasterDataHierarchyController masterDataHierarchyController;

    public WorkCategoryController(
            WorkCategoryService workCategoryService,
            MasterDataHierarchyController masterDataHierarchyController) {

        this.workCategoryService = workCategoryService;
        this.masterDataHierarchyController = masterDataHierarchyController;
    }


    @GetMapping
    public String categories(
            Model model,
            @RequestParam(required = false, defaultValue = "false") boolean showArchived) {

        long archivedCount = workCategoryService.getArchivedCategoryCount();

        if (showArchived) {
            model.addAttribute(
                    "categories",
                    workCategoryService.getArchivedCategories()
            );
            model.addAttribute("isArchivedView", true);
        } else {
            model.addAttribute(
                    "categories",
                    workCategoryService.getAllCategories()
            );
            model.addAttribute("isArchivedView", false);
        }

        model.addAttribute("archivedCount", archivedCount);

        return "admin/work-categories";
    }

    @GetMapping({"/view/{id}", "/{id}/view"})
    public String viewCategory(@PathVariable Long id, Model model) {
        return masterDataHierarchyController.viewCategoryDetails(id, model);
    }


    @GetMapping("/new")
    public String newCategory(Model model) {

        model.addAttribute(
                "category",
                new WorkCategory()
        );

        return "admin/work-category-form";
    }


    @PostMapping("/save")
    public String saveCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Integer displayOrder) {

        String effectiveIcon = (icon != null && !icon.isBlank()) ? icon : emoji;
        String effectiveName = name != null ? name.trim().toUpperCase() : null;
        workCategoryService.createCategory(
                effectiveName,
                description,
                effectiveIcon,
                displayOrder
        );

        return "redirect:/admin/work-categories";
    }


    @GetMapping("/edit/{id}")
    public String editCategory(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "category",
                workCategoryService.getCategoryById(id)
        );

        return "admin/work-category-form";
    }


    @PostMapping("/update/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Integer displayOrder) {

        String effectiveIcon = (icon != null && !icon.isBlank()) ? icon : emoji;
        String effectiveName = name != null ? name.trim().toUpperCase() : null;
        workCategoryService.updateCategory(
                id,
                effectiveName,
                description,
                effectiveIcon,
                displayOrder
        );

        return "redirect:/admin/work-categories";
    }


    @PostMapping("/toggle/{id}")
    public String toggleCategory(
            @PathVariable Long id) {

        workCategoryService.toggleCategoryStatus(id);

        return "redirect:/admin/work-categories";
    }


    @PostMapping("/delete/{id}")
    public Object deleteCategory(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            WorkCategory category = workCategoryService.getCategoryById(id);
            String categoryName = category.getName();
            String message = workCategoryService.deleteCategory(id);
            String finalMsg = (message != null && !message.isBlank())
                    ? message
                    : "Work category '" + categoryName + "' deleted successfully.";

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
                        "message", "Failed to delete work category: " + e.getMessage()
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete work category: " + e.getMessage());
        }

        return "redirect:/admin/work-categories";
    }


    @PostMapping("/unarchive/{id}")
    public Object unarchiveCategory(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            String message = workCategoryService.unarchiveCategory(id);

            if (wantsJson) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "success", true,
                        "message", message
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            if (wantsJson) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", "Failed to unarchive work category: " + e.getMessage()
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to unarchive work category: " + e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/") && !redirectUrl.startsWith("//") && !redirectUrl.startsWith("/\\")) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/admin/work-categories";
    }
}