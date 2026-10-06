package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.service.MaterialService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final ItemRepository itemRepository;

    public MaterialController(
            MaterialService materialService,
            ItemRepository itemRepository
    ) {
        this.materialService = materialService;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String materials(Model model) {

        model.addAttribute(
                "materials",
                materialService.getAllMaterials()
        );

        return "admin/materials";
    }

    @GetMapping("/new")
    public String newMaterial(Model model) {

        model.addAttribute(
                "material",
                new Material()
        );

        model.addAttribute(
                "items",
                itemRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        return "admin/material-form";
    }

    @PostMapping("/save")
    public String saveMaterial(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long itemId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        materialService.createMaterial(
                name,
                description,
                itemId,
                displayOrder
        );

        return "redirect:/admin/materials";
    }

    @GetMapping("/edit/{id}")
    public String editMaterial(
            @PathVariable Long id,
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

        return "admin/material-form";
    }

    @PostMapping("/update/{id}")
    public String updateMaterial(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long itemId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        materialService.updateMaterial(
                id,
                name,
                description,
                itemId,
                displayOrder
        );

        return "redirect:/admin/materials";
    }

    @PostMapping("/toggle/{id}")
    public String toggleMaterial(
            @PathVariable Long id
    ) {

        materialService.toggleMaterialStatus(id);

        return "redirect:/admin/materials";
    }

    @PostMapping("/delete/{id}")
    public String deleteMaterial(
            @PathVariable Long id
    ) {

        materialService.deleteMaterial(id);

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
}