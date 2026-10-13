package com.example.pravin_quotation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin/district-pricing")
public class DistrictPricingController {

    @GetMapping({"", "/**"})
    public String redirectToPricing() {
        return "redirect:/admin/pricing";
    }
}