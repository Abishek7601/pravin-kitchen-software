package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Division;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import com.example.pravin_quotation.service.DivisionService;
import jakarta.servlet.http.HttpServletRequest;
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

    private String resolveRedirectUrl(String redirectUrl, HttpServletRequest request, String defaultFallback) {
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            String trimmed = redirectUrl.trim();
            if (trimmed.startsWith("/") && !trimmed.startsWith("//") && !trimmed.startsWith("/\\")) {
                return trimmed;
            }
        }
        if (request != null) {
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("/admin/work-categories/view/")) {
                try {
                    java.net.URI uri = java.net.URI.create(referer);
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/") && !path.startsWith("//") && !path.startsWith("/\\")) {
                        return path;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return defaultFallback;
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
    public String newDivision(
            @RequestParam(required = false) String redirectUrl,
            Model model
    ) {

        model.addAttribute(
                "division",
                new Division()
        );

        model.addAttribute(
                "categories",
                workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
        );

        model.addAttribute(
                "redirectUrl",
                resolveRedirectUrl(redirectUrl, null, null)
        );

        return "admin/division-form";
    }

    @PostMapping("/save")
    public String saveDivision(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long workCategoryId,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) String redirectUrl
    ) {

        divisionService.createDivision(
                name,
                description,
                workCategoryId,
                displayOrder
        );

        return "redirect:" + resolveRedirectUrl(redirectUrl, null, "/admin/divisions");
    }

    @GetMapping("/edit/{id}")
    public String editDivision(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
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

        model.addAttribute(
                "redirectUrl",
                resolveRedirectUrl(redirectUrl, request, null)
        );

        return "admin/division-form";
    }

    @PostMapping("/update/{id}")
    public String updateDivision(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long workCategoryId,
            @RequestParam(required = false) Integer displayOrder,
            @RequestParam(required = false) String redirectUrl
    ) {

        divisionService.updateDivision(
                id,
                name,
                description,
                workCategoryId,
                displayOrder
        );

        return "redirect:" + resolveRedirectUrl(redirectUrl, null, "/admin/divisions");
    }

    @PostMapping("/toggle/{id}")
    public String toggleDivision(
            @PathVariable Long id
    ) {

        divisionService.toggleDivisionStatus(id);

        return "redirect:/admin/divisions";
    }

    @PostMapping("/delete/{id}")
    public Object deleteDivision(
            @PathVariable Long id,
            @RequestParam(required = false) String redirectUrl,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        String accept = request != null ? request.getHeader("Accept") : null;
        String requestedWith = request != null ? request.getHeader("X-Requested-With") : null;
        boolean wantsJson = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        try {
            Division division = divisionService.getDivisionById(id);
            String divisionName = division.getName();
            String message = divisionService.deleteDivision(id);
            String finalMsg = (message != null && !message.isBlank())
                    ? message
                    : "Division '" + divisionName + "' deleted successfully.";

            if (wantsJson) {
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                        "success", true,
                        "message", finalMsg
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", finalMsg);
        } catch (Exception e) {
            if (wantsJson) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", "Failed to delete division: " + e.getMessage()
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete division: " + e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
            return "redirect:" + redirectUrl;
        }

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