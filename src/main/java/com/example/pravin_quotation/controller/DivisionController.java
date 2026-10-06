package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import com.example.pravin_quotation.service.DivisionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/divisions")
public class DivisionController {

    private final DivisionService divisionService;
    private final WorkCategoryRepository workCategoryRepository;

    public DivisionController(
            DivisionService divisionService,
            WorkCategoryRepository workCategoryRepository
    ) {
        this.divisionService = divisionService;
        this.workCategoryRepository = workCategoryRepository;
    }

    @GetMapping
    public String divisions(Model model) {

        model.addAttribute(
                "divisions",
                divisionService.getAllDivisions()
        );

        return "admin/divisions";
    }

    @GetMapping("/new")
    public String newDivision(Model model) {

        model.addAttribute(
                "division",
                new Division()
        );

        model.addAttribute(
                "categories",
                workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        return "admin/division-form";
    }

    @PostMapping("/save")
    public String saveDivision(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long workCategoryId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        divisionService.createDivision(
                name,
                description,
                workCategoryId,
                displayOrder
        );

        return "redirect:/admin/divisions";
    }

    @GetMapping("/edit/{id}")
    public String editDivision(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "division",
                divisionService.getDivisionById(id)
        );

        model.addAttribute(
                "categories",
                workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        return "admin/division-form";
    }

    @PostMapping("/update/{id}")
    public String updateDivision(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long workCategoryId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        divisionService.updateDivision(
                id,
                name,
                description,
                workCategoryId,
                displayOrder
        );

        return "redirect:/admin/divisions";
    }

    @PostMapping("/toggle/{id}")
    public String toggleDivision(
            @PathVariable Long id
    ) {

        divisionService.toggleDivisionStatus(id);

        return "redirect:/admin/divisions";
    }

    @PostMapping("/delete/{id}")
    public String deleteDivision(
            @PathVariable Long id
    ) {

        divisionService.deleteDivision(id);

        return "redirect:/admin/divisions";
    }

    @GetMapping("/by-category/{categoryId}")
    @ResponseBody
    public Object getDivisionsByCategory(
            @PathVariable Long categoryId
    ) {

        return divisionService.getActiveDivisionsByCategory(
                categoryId
        );
    }
}