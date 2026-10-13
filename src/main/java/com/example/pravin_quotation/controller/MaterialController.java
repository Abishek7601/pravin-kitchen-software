package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Material;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.repository.ItemRepository;
import com.example.pravin_quotation.service.MaterialService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public String materials(Model model) {
        model.addAttribute(
                "materials",
                materialService.getAllMaterials()
        );
        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
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
        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );
        model.addAttribute(
                "formulas",
                CalculationFormula.ALL_FORMULAS
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
            @RequestParam(required = false) Integer displayOrder
    ) {
        materialService.createMaterial(
                name,
                description,
                itemId,
                divisionIds,
                formula,
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
        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );
        model.addAttribute(
                "formulas",
                CalculationFormula.ALL_FORMULAS
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
            @RequestParam(required = false) Integer displayOrder
    ) {
        materialService.updateMaterial(
                id,
                name,
                description,
                itemId,
                divisionIds,
                formula,
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