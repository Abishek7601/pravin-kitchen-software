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

    public WorkCategoryController(
            WorkCategoryService workCategoryService) {

        this.workCategoryService = workCategoryService;
    }


    @GetMapping
    public String categories(Model model) {

        model.addAttribute(
                "categories",
                workCategoryService.getAllCategories()
        );

        return "admin/work-categories";
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
            @RequestParam(required = false) Integer displayOrder) {

        workCategoryService.createCategory(
                name,
                description,
                icon,
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
            @RequestParam(required = false) Integer displayOrder) {

        workCategoryService.updateCategory(
                id,
                name,
                description,
                icon,
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
    public String deleteCategory(
            @PathVariable Long id) {

        workCategoryService.deleteCategory(id);

        return "redirect:/admin/work-categories";
    }
}