package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.DistrictPricing;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import com.example.pravin_quotation.service.DistrictPricingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/district-pricing")
public class DistrictPricingController {

    private final DistrictPricingService districtPricingService;
    private final BranchRepository branchRepository;
    private final PricingRepository pricingRepository;

    public DistrictPricingController(
            DistrictPricingService districtPricingService,
            BranchRepository branchRepository,
            PricingRepository pricingRepository
    ) {
        this.districtPricingService = districtPricingService;
        this.branchRepository = branchRepository;
        this.pricingRepository = pricingRepository;
    }

    // =========================
    // LIST
    // =========================

    @GetMapping
    public String districtPricing(Model model) {

        model.addAttribute(
                "districtPricingList",
                districtPricingService.getAllDistrictPricing()
        );
        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        return "admin/district-pricing";
    }


    // =========================
    // ADD FORM
    // =========================

    @GetMapping("/new")
    public String newDistrictPricing(Model model) {

        model.addAttribute(
                "districtPricing",
                new DistrictPricing()
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        model.addAttribute(
                "pricingList",
                pricingRepository.findByActiveTrueOrderByCreatedAtDesc()
        );

        return "admin/district-pricing-form";
    }


    // =========================
    // SAVE
    // =========================

    @PostMapping("/save")
    public String saveDistrictPricing(
            @RequestParam Long branchId,
            @RequestParam Long pricingId,
            @RequestParam BigDecimal rate,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {

        try {

            districtPricingService.createDistrictPricing(
                    branchId,
                    pricingId,
                    rate,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "District pricing added successfully."
            );

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/district-pricing/new";
        }

        return "redirect:/admin/district-pricing";
    }


    // =========================
    // EDIT FORM
    // =========================

    @GetMapping("/edit/{id}")
    public String editDistrictPricing(
            @PathVariable Long id,
            Model model
    ) {

        DistrictPricing districtPricing =
                districtPricingService.getDistrictPricingById(id);

        model.addAttribute(
                "districtPricing",
                districtPricing
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        model.addAttribute(
                "pricingList",
                pricingRepository.findByActiveTrueOrderByCreatedAtDesc()
        );

        return "admin/district-pricing-form";
    }


    // =========================
    // UPDATE
    // =========================

    @PostMapping("/update/{id}")
    public String updateDistrictPricing(
            @PathVariable Long id,
            @RequestParam Long branchId,
            @RequestParam Long pricingId,
            @RequestParam BigDecimal rate,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {

        try {

            districtPricingService.updateDistrictPricing(
                    id,
                    branchId,
                    pricingId,
                    rate,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "District pricing updated successfully."
            );

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/district-pricing/edit/" + id;
        }

        return "redirect:/admin/district-pricing";
    }


    // =========================
    // TOGGLE
    // =========================

    @PostMapping("/toggle/{id}")
    public String toggleDistrictPricing(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            districtPricingService.toggleDistrictPricingStatus(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "District pricing status updated successfully."
            );

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/district-pricing";
    }


    // =========================
    // DELETE
    // =========================

    @PostMapping("/delete/{id}")
    public String deleteDistrictPricing(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            districtPricingService.deleteDistrictPricing(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "District pricing deleted successfully."
            );

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/district-pricing";
    }


    // =========================
    // FILTER BY DISTRICT
    // =========================

    @GetMapping("/branch/{branchId}")
    public String districtPricingByBranch(
            @PathVariable Long branchId,
            Model model
    ) {

        model.addAttribute(
                "districtPricingList",
                districtPricingService.getDistrictPricingByBranch(branchId)
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        model.addAttribute(
                "selectedBranch",
                branchRepository.findById(branchId).orElse(null)
        );

        return "admin/district-pricing";
    }
}