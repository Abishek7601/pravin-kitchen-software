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
    public Object saveCategory(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Integer displayOrder,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name is required");
            }

            String effectiveIcon = (icon != null && !icon.isBlank()) ? icon.trim() : (emoji != null && !emoji.isBlank() ? emoji.trim() : null);
            String effectiveName = name.trim().toUpperCase();

            WorkCategory category;
            boolean isUpdate = (id != null && id > 0);
            if (isUpdate) {
                category = workCategoryService.updateCategory(
                        id,
                        effectiveName,
                        description,
                        effectiveIcon,
                        displayOrder
                );
            } else {
                category = workCategoryService.createCategory(
                        effectiveName,
                        description,
                        effectiveIcon,
                        displayOrder
                );
            }

            String actionText = isUpdate ? "updated" : "created";

            if (wantsJson) {
                java.util.Map<String, Object> catMap = new java.util.HashMap<>();
                catMap.put("id", category.getId());
                catMap.put("name", category.getName());
                catMap.put("icon", category.getIcon() != null ? category.getIcon() : "");
                catMap.put("description", category.getDescription() != null ? category.getDescription() : "");
                catMap.put("active", category.getActive() != null ? category.getActive() : true);
                catMap.put("archived", category.getArchived());

                return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "success", true,
                        "message", "Category '" + category.getName() + "' " + actionText + " successfully.",
                        "category", catMap
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", "Category '" + category.getName() + "' " + actionText + " successfully.");
            return "redirect:/admin/work-categories";
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error saving category";
            if (errorMsg.contains("Work category already exists")
                    || errorMsg.contains("Another work category with this name already exists")) {
                errorMsg = "Category with name '" + (name != null ? name.trim().toUpperCase() : "") + "' already exists.";
            } else if (errorMsg.contains("Work category name is required")
                    || errorMsg.contains("Category name is required")) {
                errorMsg = "Category name is required.";
            }

            if (wantsJson) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", errorMsg
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/work-categories";
        }
    }

    public Object saveCategory(
            String name,
            String description,
            String icon,
            String emoji,
            Integer displayOrder,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        return saveCategory(null, name, description, icon, emoji, displayOrder, request, redirectAttributes);
    }


    @GetMapping("/edit/{id}")
    public Object editCategory(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request,
            Model model) {

        WorkCategory category = workCategoryService.getCategoryById(id);
        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        if ((accept != null && accept.contains("application/json")) || "XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return org.springframework.http.ResponseEntity.ok(category);
        }

        model.addAttribute(
                "category",
                category
        );

        return "admin/work-category-form";
    }


    @PostMapping("/update/{id}")
    public Object updateCategory(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) Integer displayOrder,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name is required");
            }

            String effectiveIcon = (icon != null && !icon.isBlank()) ? icon.trim() : (emoji != null && !emoji.isBlank() ? emoji.trim() : null);
            String effectiveName = name.trim().toUpperCase();

            WorkCategory category = workCategoryService.updateCategory(
                    id,
                    effectiveName,
                    description,
                    effectiveIcon,
                    displayOrder
            );

            if (wantsJson) {
                java.util.Map<String, Object> catMap = new java.util.HashMap<>();
                catMap.put("id", category.getId());
                catMap.put("name", category.getName());
                catMap.put("icon", category.getIcon() != null ? category.getIcon() : "");
                catMap.put("description", category.getDescription() != null ? category.getDescription() : "");
                catMap.put("active", category.getActive() != null ? category.getActive() : true);
                catMap.put("archived", category.getArchived());

                return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "success", true,
                        "message", "Category '" + category.getName() + "' updated successfully.",
                        "category", catMap
                ));
            }

            if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("successMessage", "Category '" + category.getName() + "' updated successfully.");
            }
            return "redirect:/admin/work-categories";
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error updating category";
            if (errorMsg.contains("Work category already exists")
                    || errorMsg.contains("Another work category with this name already exists")) {
                errorMsg = "Category with name '" + (name != null ? name.trim().toUpperCase() : "") + "' already exists.";
            } else if (errorMsg.contains("Work category name is required")
                    || errorMsg.contains("Category name is required")) {
                errorMsg = "Category name is required.";
            }

            if (wantsJson) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", errorMsg
                ));
            }
            if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            }
            return "redirect:/admin/work-categories";
        }
    }

    public String updateCategory(
            Long id,
            String name,
            String description,
            String icon,
            String emoji,
            Integer displayOrder) {
        updateCategory(id, name, description, icon, emoji, displayOrder, null, null);
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