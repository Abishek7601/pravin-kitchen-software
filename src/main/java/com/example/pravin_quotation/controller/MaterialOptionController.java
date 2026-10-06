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

        model.addAttribute(
                "options",
                materialOptionService.getAllOptions()
        );

        return "admin/material-options";
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
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long materialId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        materialOptionService.createOption(
                name,
                description,
                materialId,
                displayOrder
        );

        return "redirect:/admin/material-options";
    }

    @GetMapping("/edit/{id}")
    public String editOption(
            @PathVariable Long id,
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

        return "admin/material-option-form";
    }

    @PostMapping("/update/{id}")
    public String updateOption(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long materialId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        materialOptionService.updateOption(
                id,
                name,
                description,
                materialId,
                displayOrder
        );

        return "redirect:/admin/material-options";
    }

    @PostMapping("/toggle/{id}")
    public String toggleOption(
            @PathVariable Long id
    ) {

        materialOptionService.toggleOptionStatus(id);

        return "redirect:/admin/material-options";
    }

    @PostMapping("/delete/{id}")
    public String deleteOption(
            @PathVariable Long id
    ) {

        materialOptionService.deleteOption(id);

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