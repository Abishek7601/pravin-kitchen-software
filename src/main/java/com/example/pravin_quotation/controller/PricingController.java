package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.service.PricingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/pricing")
public class PricingController {

    private final PricingService pricingService;
    private final MaterialOptionRepository materialOptionRepository;

    public PricingController(
            PricingService pricingService,
            MaterialOptionRepository materialOptionRepository
    ) {
        this.pricingService = pricingService;
        this.materialOptionRepository = materialOptionRepository;
    }

    @GetMapping
    public String pricing(
            @RequestParam(required = false) String error,
            Model model
    ) {

        model.addAttribute(
                "pricingList",
                pricingService.getAllPricing()
        );

        if (error != null && !error.isBlank()) {
            model.addAttribute("deleteError", error);
        }

        return "admin/pricing";
    }

    @GetMapping("/new")
    public String newPricing(Model model) {

        model.addAttribute(
                "pricing",
                new Pricing()
        );

        model.addAttribute(
                "materialOptions",
                materialOptionRepository
                        .findByActiveTrueOrderByDisplayOrderAsc()
        );

        model.addAttribute(
                "pricingModes",
                PricingMode.values()
        );

        return "admin/pricing-form";
    }

    @PostMapping("/save")
    public String savePricing(
            @RequestParam Long materialOptionId,
            @RequestParam PricingMode pricingMode,
            @RequestParam java.math.BigDecimal rate,
            @RequestParam String unit,
            @RequestParam(required = false) String description
    ) {

        pricingService.createPricing(
                materialOptionId,
                pricingMode,
                rate,
                unit,
                description
        );

        return "redirect:/admin/pricing";
    }

    @GetMapping("/edit/{id}")
    public String editPricing(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "pricing",
                pricingService.getPricingById(id)
        );

        model.addAttribute(
                "materialOptions",
                materialOptionRepository
                        .findByActiveTrueOrderByDisplayOrderAsc()
        );

        model.addAttribute(
                "pricingModes",
                PricingMode.values()
        );

        return "admin/pricing-form";
    }

    @PostMapping("/update/{id}")
    public String updatePricing(
            @PathVariable Long id,
            @RequestParam Long materialOptionId,
            @RequestParam PricingMode pricingMode,
            @RequestParam java.math.BigDecimal rate,
            @RequestParam String unit,
            @RequestParam(required = false) String description
    ) {

        pricingService.updatePricing(
                id,
                materialOptionId,
                pricingMode,
                rate,
                unit,
                description
        );

        return "redirect:/admin/pricing";
    }

    @PostMapping("/toggle/{id}")
    public String togglePricing(
            @PathVariable Long id
    ) {

        pricingService.togglePricingStatus(id);

        return "redirect:/admin/pricing";
    }

    @PostMapping("/delete/{id}")
    public String deletePricing(
            @PathVariable Long id
    ) {

        try {

            pricingService.deletePricing(id);

            return "redirect:/admin/pricing";

        } catch (IllegalStateException e) {

            return "redirect:/admin/pricing?error="
                    + java.net.URLEncoder.encode(
                    e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8
            );
        }
    }
}