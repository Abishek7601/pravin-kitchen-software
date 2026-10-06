package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Item;
import com.example.pravin_quotation.repository.DivisionRepository;
import com.example.pravin_quotation.service.ItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/items")
public class ItemController {

    private final ItemService itemService;
    private final DivisionRepository divisionRepository;

    public ItemController(
            ItemService itemService,
            DivisionRepository divisionRepository
    ) {
        this.itemService = itemService;
        this.divisionRepository = divisionRepository;
    }

    @GetMapping
    public String items(Model model) {

        model.addAttribute(
                "items",
                itemService.getAllItems()
        );

        return "admin/items";
    }

    @GetMapping("/new")
    public String newItem(Model model) {

        model.addAttribute(
                "item",
                new Item()
        );

        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        return "admin/item-form";
    }

    @PostMapping("/save")
    public String saveItem(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long divisionId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        itemService.createItem(
                name,
                description,
                divisionId,
                displayOrder
        );

        return "redirect:/admin/items";
    }

    @GetMapping("/edit/{id}")
    public String editItem(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "item",
                itemService.getItemById(id)
        );

        model.addAttribute(
                "divisions",
                divisionRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        return "admin/item-form";
    }

    @PostMapping("/update/{id}")
    public String updateItem(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long divisionId,
            @RequestParam(required = false) Integer displayOrder
    ) {

        itemService.updateItem(
                id,
                name,
                description,
                divisionId,
                displayOrder
        );

        return "redirect:/admin/items";
    }

    @PostMapping("/toggle/{id}")
    public String toggleItem(
            @PathVariable Long id
    ) {

        itemService.toggleItemStatus(id);

        return "redirect:/admin/items";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(
            @PathVariable Long id
    ) {

        itemService.deleteItem(id);

        return "redirect:/admin/items";
    }

    @GetMapping("/by-division/{divisionId}")
    @ResponseBody
    public Object getItemsByDivision(
            @PathVariable Long divisionId
    ) {

        return itemService.getActiveItemsByDivision(
                divisionId
        );
    }
}