package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.AdminPasswordService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Admin Change Password feature under Admin Settings / Security.
 */
@Controller
@RequestMapping("/admin")
public class AdminPasswordController {

    private final AdminPasswordService adminPasswordService;
    private final UserRepository userRepository;

    public AdminPasswordController(AdminPasswordService adminPasswordService, UserRepository userRepository) {
        this.adminPasswordService = adminPasswordService;
        this.userRepository = userRepository;
    }

    /**
     * Renders the Change Password page.
     */
    @GetMapping({"/change-password", "/settings/change-password", "/security/change-password"})
    public String showChangePasswordPage(Authentication authentication, Model model) {
        if (authentication != null) {
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(user -> model.addAttribute("adminUser", user));
            model.addAttribute("adminEmail", email);
        }
        return "admin/change-password";
    }

    /**
     * Processes the Change Password form submission.
     */
    @PostMapping({"/change-password", "/settings/change-password", "/security/change-password"})
    public String processChangePassword(
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            adminPasswordService.changePassword(
                    authentication.getName(),
                    currentPassword,
                    newPassword,
                    confirmPassword
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Password changed successfully! Your new password is now active."
            );
            return "redirect:/admin/change-password";

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while updating the password.");
            return "redirect:/admin/change-password";
        }
    }
}
