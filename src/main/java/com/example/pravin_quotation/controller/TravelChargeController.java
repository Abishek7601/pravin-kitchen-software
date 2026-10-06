package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.TravelCharge;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.service.TravelChargeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/travel-charges")
public class TravelChargeController {

    private final TravelChargeService travelChargeService;
    private final BranchRepository branchRepository;

    public TravelChargeController(
            TravelChargeService travelChargeService,
            BranchRepository branchRepository
    ) {
        this.travelChargeService = travelChargeService;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public String travelCharges(Model model) {

        model.addAttribute(
                "travelChargeList",
                travelChargeService.getAllTravelCharges()
        );

        return "admin/travel-charges";
    }

    @GetMapping("/new")
    public String newTravelCharge(Model model) {

        model.addAttribute("travelCharge", new TravelCharge());
        model.addAttribute("branches", branchRepository.findAll());

        return "admin/travel-charge-form";
    }

    @PostMapping("/save")
    public String saveTravelCharge(
            @RequestParam Long branchId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        try {

            travelChargeService.create(
                    branchId,
                    amount,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Travel charge added successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/travel-charges/new";
        }

        return "redirect:/admin/travel-charges";
    }

    @GetMapping("/edit/{id}")
    public String editTravelCharge(
            @PathVariable Long id,
            Model model
    ) {

        TravelCharge travelCharge =
                travelChargeService.getById(id);

        model.addAttribute(
                "travelCharge",
                travelCharge
        );

        model.addAttribute(
                "branches",
                branchRepository.findAll()
        );

        return "admin/travel-charge-form";
    }

    @PostMapping("/update/{id}")
    public String updateTravelCharge(
            @PathVariable Long id,
            @RequestParam Long branchId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {
        try {

            travelChargeService.update(
                    id,
                    branchId,
                    amount,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Travel charge updated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/travel-charges/edit/" + id;
        }

        return "redirect:/admin/travel-charges";
    }

    @PostMapping("/toggle/{id}")
    public String toggleTravelCharge(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            TravelCharge travelCharge =
                    travelChargeService.toggleStatus(id);

            String message = Boolean.TRUE.equals(
                    travelCharge.getActive()
            )
                    ? "Travel charge activated successfully."
                    : "Travel charge deactivated successfully.";

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    message
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/travel-charges";
    }

    @PostMapping("/delete/{id}")
    public String deleteTravelCharge(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            travelChargeService.delete(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Travel charge deleted successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/travel-charges";
    }

    @GetMapping("/branch/{branchId}")
    public String travelChargesByBranch(
            @PathVariable Long branchId,
            Model model
    ) {

        model.addAttribute(
                "travelChargeList",
                travelChargeService.getAllByBranchId(branchId)
        );

        model.addAttribute(
                "selectedBranch",
                branchRepository.findById(branchId).orElse(null)
        );

        return "admin/travel-charges";
    }
}