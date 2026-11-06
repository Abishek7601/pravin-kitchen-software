package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.OtpEmailService;
import com.example.pravin_quotation.security.OtpService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminOtpController {

    private static final Logger log = LoggerFactory.getLogger(AdminOtpController.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final OtpEmailService otpEmailService;

    public AdminOtpController(
            UserRepository userRepository,
            OtpService otpService,
            OtpEmailService otpEmailService
    ) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.otpEmailService = otpEmailService;
    }

    // =========================================================
    // SUPER ADMIN OTP PAGE
    // =========================================================

    @GetMapping("/otp")
    public String otpPage(
            @RequestParam(name = "mailError", required = false) String mailError,
            HttpSession session,
            Model model
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User admin = userRepository.findByEmail(email).orElse(null);

        if (admin == null || admin.getRole() != Role.SUPER_ADMIN) {
            return "redirect:/login?error";
        }

        if (!Boolean.TRUE.equals(admin.getActive())) {
            return "redirect:/login?error";
        }

        // Already verified -> redirect to admin dashboard
        if (Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"))) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("adminName", admin.getName() != null ? admin.getName() : "Super Admin");
        model.addAttribute("adminEmail", admin.getEmail());
        model.addAttribute("maxAttempts", otpService.getMaxAttempts());

        /*
         * AUTOMATIC FIRST OTP:
         * If this login session has never requested an OTP or it has expired,
         * automatically attempt to send one when the OTP page opens.
         * If delivery previously failed (mailError parameter present),
         * do not trigger an immediate retry loop.
         */
        String existingOtp = (String) session.getAttribute("OTP_CODE");
        LocalDateTime existingExpiry = (LocalDateTime) session.getAttribute("OTP_EXPIRY");

        if (mailError == null && (existingOtp == null
                || existingExpiry == null
                || otpService.isExpired(existingExpiry))) {

            boolean sent = sendOtpToAdmin(admin, session);
            if (sent) {
                return "redirect:/admin/otp?sent";
            } else {
                return "redirect:/admin/otp?mailError";
            }
        }

        return "auth/admin-otp";
    }

    // =========================================================
    // RESEND OTP
    // =========================================================

    @PostMapping("/otp/send")
    public String sendOtp(HttpSession session) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User admin = userRepository.findByEmail(email).orElse(null);

        if (admin == null || admin.getRole() != Role.SUPER_ADMIN) {
            return "redirect:/login?error";
        }

        if (!Boolean.TRUE.equals(admin.getActive())) {
            return "redirect:/login?error";
        }

        LocalDateTime lastSent = (LocalDateTime) session.getAttribute("OTP_LAST_SENT");

        /*
         * Prevent OTP spam.
         * User must wait 60 seconds before resend, only if OTP was previously successfully sent.
         */
        if (session.getAttribute("OTP_CODE") != null && !otpService.isResendAllowed(lastSent)) {
            return "redirect:/admin/otp?cooldown";
        }

        boolean sent = sendOtpToAdmin(admin, session);
        if (sent) {
            return "redirect:/admin/otp?sent";
        } else {
            return "redirect:/admin/otp?mailError";
        }
    }

    // =========================================================
    // VERIFY OTP
    // =========================================================

    @PostMapping("/otp/verify")
    public String verifyOtp(
            @RequestParam(name = "otp", required = false) String otp,
            HttpSession session
    ) {
        String storedOtp = (String) session.getAttribute("OTP_CODE");
        LocalDateTime expiry = (LocalDateTime) session.getAttribute("OTP_EXPIRY");
        Integer attempts = (Integer) session.getAttribute("OTP_ATTEMPTS");

        // No OTP generated
        if (storedOtp == null || expiry == null) {
            return "redirect:/admin/otp?notSent";
        }

        // OTP expired
        if (otpService.isExpired(expiry)) {
            session.removeAttribute("OTP_CODE");
            session.removeAttribute("OTP_EXPIRY");
            return "redirect:/admin/otp?expired";
        }

        // Maximum attempts
        if (otpService.isAttemptsExceeded(attempts)) {
            return "redirect:/admin/otp?attempts";
        }

        // Invalid OTP
        if (otp == null || !otp.trim().equals(storedOtp)) {
            int currentAttempts = attempts == null ? 0 : attempts;
            session.setAttribute("OTP_ATTEMPTS", currentAttempts + 1);
            return "redirect:/admin/otp?invalid";
        }

        // =====================================================
        // OTP VERIFIED
        // =====================================================
        session.setAttribute("OTP_VERIFIED", true);
        session.removeAttribute("OTP_CODE");
        session.removeAttribute("OTP_EXPIRY");
        session.removeAttribute("OTP_ATTEMPTS");

        return "redirect:/admin/dashboard";
    }

    // =========================================================
    // AUTOMATIC OTP SENDING WITH SAFE EXCEPTION HANDLING
    // =========================================================

    private boolean sendOtpToAdmin(User admin, HttpSession session) {
        String otp = otpService.generateOtp();
        LocalDateTime expiry = otpService.getExpiryTime();

        try {
            otpEmailService.sendSuperAdminOtp(
                    admin.getEmail(),
                    admin.getName(),
                    otp
            );

            // Persist OTP in session ONLY after successful delivery
            session.setAttribute("OTP_CODE", otp);
            session.setAttribute("OTP_EXPIRY", expiry);
            session.setAttribute("OTP_ATTEMPTS", 0);
            session.setAttribute("OTP_LAST_SENT", LocalDateTime.now());
            session.setAttribute("OTP_VERIFIED", false);

            return true;
        } catch (Exception e) {
            log.error("Failed to send Super Admin login OTP email to [{}]: {}",
                    admin.getEmail(), e.getMessage(), e);

            session.removeAttribute("OTP_CODE");
            session.removeAttribute("OTP_EXPIRY");
            session.removeAttribute("OTP_ATTEMPTS");

            return false;
        }
    }
}

