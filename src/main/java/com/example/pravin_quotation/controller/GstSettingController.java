package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.GstSetting;
import com.example.pravin_quotation.service.GstSettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/gst-settings")
public class GstSettingController {

    private final GstSettingService gstSettingService;

    public GstSettingController(GstSettingService gstSettingService) {
        this.gstSettingService = gstSettingService;
    }

    @GetMapping
    public String gstSettings(Model model) {

        model.addAttribute(
                "gstSettingList",
                gstSettingService.getAllGstSettings()
        );

        return "admin/gst-settings";
    }

    @GetMapping("/new")
    public String newGstSetting(Model model) {

        model.addAttribute("gstSetting", new GstSetting());

        return "admin/gst-setting-form";
    }

    @PostMapping("/save")
    public String saveGstSetting(
            @RequestParam BigDecimal gstPercentage,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {

        try {

            gstSettingService.create(
                    gstPercentage,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "GST setting added successfully."
            );

            return "redirect:/admin/gst-settings";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/gst-settings/new";
        }
    }

    @GetMapping("/edit/{id}")
    public String editGstSetting(
            @PathVariable Long id,
            Model model
    ) {

        model.addAttribute(
                "gstSetting",
                gstSettingService.getById(id)
        );

        return "admin/gst-setting-form";
    }

    @PostMapping("/update/{id}")
    public String updateGstSetting(
            @PathVariable Long id,
            @RequestParam BigDecimal gstPercentage,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {

        try {

            gstSettingService.update(
                    id,
                    gstPercentage,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "GST setting updated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/gst-settings/edit/" + id;
        }

        return "redirect:/admin/gst-settings";
    }

    @PostMapping("/activate/{id}")
    public String activateGstSetting(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            gstSettingService.activate(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "GST setting activated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/gst-settings";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateGstSetting(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            gstSettingService.deactivate(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "GST setting deactivated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/gst-settings";
    }

    @PostMapping("/delete/{id}")
    public String deleteGstSetting(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            gstSettingService.delete(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "GST setting deleted successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/gst-settings";
    }
}