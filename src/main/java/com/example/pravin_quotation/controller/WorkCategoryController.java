package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @GetMapping({"/view/{id}", "/{id}/view", "/{id}/work-types", "/work-types/{id}"})
    public Object viewCategory(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request,
            Model model) {

        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        String uri = request != null ? request.getRequestURI() : "";
        boolean isWorkTypesRoute = uri.contains("/work-types");
        boolean wantsJson = isWorkTypesRoute
                || (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        if (wantsJson) {
            try {
                java.util.Map<String, Object> data = workCategoryService.getCategoryWorkTypes(id);
                return org.springframework.http.ResponseEntity.ok(data);
            } catch (Exception e) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", "Failed to load category work types: " + e.getMessage(),
                        "totalCount", 0,
                        "workTypes", java.util.List.of()
                ));
            }
        }

        return masterDataHierarchyController.viewCategoryDetails(id, model);
    }

    public String viewCategory(Long id, Model model) {
        return (String) viewCategory(id, null, model);
    }

    @PostMapping({"/{categoryId}/work-types", "/{categoryId}/work-types/save", "/work-types/save"})
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> saveWorkType(
            @PathVariable(required = false) Long categoryId,
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String description) {

        Long effectiveCatId = categoryId != null ? categoryId : workCategoryId;
        String effectiveEmoji = (emoji != null && !emoji.isBlank()) ? emoji.trim() : (icon != null && !icon.isBlank() ? icon.trim() : null);

        try {
            if (effectiveCatId == null) {
                throw new IllegalArgumentException("Category ID is required");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Work type name is required");
            }

            Division division;
            boolean isUpdate = (id != null && id > 0);
            if (isUpdate) {
                division = workCategoryService.updateWorkType(effectiveCatId, id, name, effectiveEmoji, description);
            } else {
                division = workCategoryService.createWorkType(effectiveCatId, name, effectiveEmoji, description);
            }

            java.util.Map<String, Object> data = workCategoryService.getCategoryWorkTypes(effectiveCatId);
            java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>(data);
            resp.put("success", true);
            resp.put("message", isUpdate ? "Work type updated successfully." : "Work type added successfully.");
            resp.put("workTypeId", division.getId());
            return org.springframework.http.ResponseEntity.ok(resp);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to save work type."
            ));
        }
    }

    @PostMapping({"/{categoryId}/work-types/{workTypeId}", "/{categoryId}/work-types/update/{workTypeId}", "/work-types/update/{workTypeId}", "/work-types/{workTypeId}/update"})
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> updateWorkType(
            @PathVariable(required = false) Long categoryId,
            @PathVariable Long workTypeId,
            @RequestParam(required = false) Long workCategoryId,
            @RequestParam String name,
            @RequestParam(required = false) String emoji,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String description) {

        Long effectiveCatId = categoryId != null ? categoryId : workCategoryId;
        String effectiveEmoji = (emoji != null && !emoji.isBlank()) ? emoji.trim() : (icon != null && !icon.isBlank() ? icon.trim() : null);

        try {
            Division division = workCategoryService.updateWorkType(effectiveCatId, workTypeId, name, effectiveEmoji, description);
            Long catId = division.getWorkCategory() != null ? division.getWorkCategory().getId() : effectiveCatId;
            java.util.Map<String, Object> data = workCategoryService.getCategoryWorkTypes(catId);
            java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>(data);
            resp.put("success", true);
            resp.put("message", "Work type updated successfully.");
            resp.put("workTypeId", division.getId());
            return org.springframework.http.ResponseEntity.ok(resp);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update work type."
            ));
        }
    }

    @PostMapping({"/{categoryId}/work-types/{workTypeId}/delete", "/{categoryId}/work-types/delete/{workTypeId}", "/work-types/{workTypeId}/delete", "/work-types/delete/{workTypeId}"})
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteWorkType(
            @PathVariable(required = false) Long categoryId,
            @PathVariable Long workTypeId,
            @RequestParam(required = false) Long workCategoryId) {

        Long effectiveCatId = categoryId != null ? categoryId : workCategoryId;

        try {
            String msg = workCategoryService.deleteWorkType(effectiveCatId, workTypeId);
            java.util.Map<String, Object> resp = (effectiveCatId != null)
                    ? new java.util.LinkedHashMap<>(workCategoryService.getCategoryWorkTypes(effectiveCatId))
                    : new java.util.LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", msg);
            return org.springframework.http.ResponseEntity.ok(resp);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to delete work type."
            ));
        }
    }

    // =========================================================
    // WORK TYPE MATERIALS MANAGEMENT
    // =========================================================

    @GetMapping("/work-types/{workTypeId}/materials")
    @ResponseBody
    public ResponseEntity<?> getWorkTypeMaterials(@PathVariable Long workTypeId) {
        try {
            Map<String, Object> data = workCategoryService.getWorkTypeMaterials(workTypeId);
            Map<String, Object> resp = new LinkedHashMap<>(data);
            resp.put("success", true);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to load materials for work type."
            ));
        }
    }

    @PostMapping("/work-types/{workTypeId}/materials")
    @ResponseBody
    public ResponseEntity<?> createWorkTypeMaterial(
            @PathVariable Long workTypeId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String pricingGrade,
            @RequestParam String name,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false, defaultValue = "Sq.ft") String unit,
            @RequestParam BigDecimal rate) {
        try {
            String gradeStr = (grade != null && !grade.isBlank()) ? grade : pricingGrade;
            PricingMode mode = parsePricingGrade(gradeStr);
            Material material = workCategoryService.createWorkTypeMaterial(workTypeId, mode, name, specification, unit, rate);

            Map<String, Object> resp = new LinkedHashMap<>(workCategoryService.getWorkTypeMaterials(workTypeId));
            resp.put("success", true);
            resp.put("message", "Material '" + material.getName() + "' added successfully under " + mode.name() + ".");
            resp.put("materialId", material.getId());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to add material."
            ));
        }
    }

    @PostMapping({"/work-types/materials/{materialId}", "/work-types/materials/{materialId}/update", "/work-types/{workTypeId}/materials/{materialId}"})
    @ResponseBody
    public ResponseEntity<?> updateWorkTypeMaterial(
            @PathVariable Long materialId,
            @PathVariable(required = false) Long workTypeId,
            @RequestParam(required = false) Long targetWorkTypeId,
            @RequestParam String name,
            @RequestParam(required = false) String specification,
            @RequestParam(required = false, defaultValue = "Sq.ft") String unit,
            @RequestParam BigDecimal rate) {
        try {
            Material material = workCategoryService.updateWorkTypeMaterial(materialId, name, specification, unit, rate);
            Long effectiveWorkTypeId = workTypeId != null ? workTypeId : (targetWorkTypeId != null ? targetWorkTypeId : (material.getDivision() != null ? material.getDivision().getId() : null));

            Map<String, Object> resp = (effectiveWorkTypeId != null)
                    ? new LinkedHashMap<>(workCategoryService.getWorkTypeMaterials(effectiveWorkTypeId))
                    : new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Material '" + material.getName() + "' updated successfully.");
            resp.put("materialId", material.getId());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to update material."
            ));
        }
    }

    @PostMapping({"/work-types/materials/{materialId}/delete", "/work-types/{workTypeId}/materials/{materialId}/delete"})
    @ResponseBody
    public ResponseEntity<?> deleteWorkTypeMaterial(
            @PathVariable Long materialId,
            @PathVariable(required = false) Long workTypeId,
            @RequestParam(required = false) Long targetWorkTypeId) {
        try {
            Long effectiveWorkTypeId = workTypeId != null ? workTypeId : targetWorkTypeId;
            String msg = workCategoryService.deleteWorkTypeMaterial(materialId);

            Map<String, Object> resp = (effectiveWorkTypeId != null)
                    ? new LinkedHashMap<>(workCategoryService.getWorkTypeMaterials(effectiveWorkTypeId))
                    : new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", msg);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to delete material."
            ));
        }
    }

    public ResponseEntity<?> createWorkTypeMaterial(Long workTypeId, String grade, String name, String specification, String unit, BigDecimal rate) {
        return createWorkTypeMaterial(workTypeId, grade, null, name, specification, unit, rate);
    }

    public ResponseEntity<?> updateWorkTypeMaterial(Long materialId, String name, String specification, String unit, BigDecimal rate) {
        return updateWorkTypeMaterial(materialId, null, null, name, specification, unit, rate);
    }

    public ResponseEntity<?> deleteWorkTypeMaterial(Long materialId) {
        return deleteWorkTypeMaterial(materialId, null, null);
    }

    private PricingMode parsePricingGrade(String grade) {
        if (grade == null || grade.isBlank()) return PricingMode.MIDDLE;
        String g = grade.trim().toUpperCase();
        if (g.contains("PREMIUM")) return PricingMode.PREMIUM;
        if (g.contains("ECONOMY")) return PricingMode.ECONOMY;
        return PricingMode.MIDDLE;
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