package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.service.EmployeeForgotPasswordService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employee/forgot-password")
public class EmployeeForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeForgotPasswordController.class);

    private final EmployeeForgotPasswordService forgotPasswordService;

    public EmployeeForgotPasswordController(EmployeeForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    // =========================================================
    // 1. STEP 1: ENTER EMPLOYEE ID / IDENTIFIER
    // =========================================================

    @GetMapping
    public String showForgotPasswordPage() {
        return "auth/employee-forgot-password";
    }

    @PostMapping
    public String handleInitiateReset(
            @RequestParam(name = "employeeIdentifier", required = false) String employeeIdentifier,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            forgotPasswordService.initiateReset(session, employeeIdentifier);
            return "redirect:/employee/forgot-password/verify?sent";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/forgot-password";
        } catch (MailException e) {
            log.error("Failed to send password reset OTP email: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to send OTP right now. Please check network connection and try again.");
            return "redirect:/employee/forgot-password";
        } catch (Exception e) {
            log.error("Unexpected error during forgot password initiation: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An unexpected error occurred. Please try again.");
            return "redirect:/employee/forgot-password";
        }
    }

    // =========================================================
    // 2. STEP 2: VERIFY OTP
    // =========================================================

    @GetMapping("/verify")
    public String showVerifyOtpPage(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!forgotPasswordService.isResetInitiated(session)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please enter your Employee ID first.");
            return "redirect:/employee/forgot-password";
        }

        model.addAttribute("employeeName", forgotPasswordService.getResetEmployeeName(session));

        String adminEmail = "Administrator";
        try {
            adminEmail = forgotPasswordService.getAdminEmail();
        } catch (Exception ignored) {
        }
        model.addAttribute("adminEmail", adminEmail);

        return "auth/employee-reset-otp";
    }

    @PostMapping("/verify")
    public String handleVerifyOtp(
            @RequestParam(name = "otp", required = false) String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            forgotPasswordService.verifyOtp(session, otp);
            return "redirect:/employee/forgot-password/reset";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/forgot-password/verify";
        } catch (Exception e) {
            log.error("Unexpected error during OTP verification: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An unexpected error occurred during OTP verification.");
            return "redirect:/employee/forgot-password/verify";
        }
    }

    @PostMapping("/resend")
    public String handleResendOtp(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            forgotPasswordService.resendOtp(session);
            redirectAttributes.addFlashAttribute("successMessage",
                    "A new verification OTP has been sent to the Super Admin.");
            return "redirect:/employee/forgot-password/verify?sent";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/forgot-password/verify";
        } catch (MailException e) {
            log.error("Failed to resend password reset OTP email: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to resend OTP right now. Please try again.");
            return "redirect:/employee/forgot-password/verify";
        } catch (Exception e) {
            log.error("Unexpected error during OTP resend: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An unexpected error occurred while resending the OTP.");
            return "redirect:/employee/forgot-password/verify";
        }
    }

    // =========================================================
    // 3. STEP 3: RESET PASSWORD
    // =========================================================

    @GetMapping("/reset")
    public String showResetPasswordPage(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!forgotPasswordService.isResetVerified(session)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please verify your OTP first before setting a new password.");
            return "redirect:/employee/forgot-password";
        }

        return "auth/employee-reset-password";
    }

    @PostMapping("/reset")
    public String handleResetPassword(
            @RequestParam(name = "newPassword", required = false) String newPassword,
            @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            forgotPasswordService.resetPassword(session, newPassword, confirmPassword);
            return "redirect:/login?resetSuccess";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/forgot-password/reset";
        } catch (Exception e) {
            log.error("Unexpected error while resetting password: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An unexpected error occurred while resetting your password.");
            return "redirect:/employee/forgot-password/reset";
        }
    }
}
