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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/employee")
public class OtpController {

    private static final Logger log = LoggerFactory.getLogger(OtpController.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final OtpEmailService otpEmailService;

    public OtpController(
            UserRepository userRepository,
            OtpService otpService,
            OtpEmailService otpEmailService
    ) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.otpEmailService = otpEmailService;
    }

    // =========================================================
    // EMPLOYEE OTP PAGE
    // =========================================================

    @GetMapping("/otp")
    public String otpPage(
            @RequestParam(name = "mailError", required = false) String mailError,
            HttpSession session,
            Model model
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return "redirect:/login";
        }

        String email = authentication.getName();

        User employee =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {
            return "redirect:/login?error";
        }

        // Super Admin does not need OTP
        if (employee.getRole() == Role.SUPER_ADMIN) {
            return "redirect:/admin/dashboard";
        }

        // Inactive employee cannot login
        if (!Boolean.TRUE.equals(employee.getActive())) {
            return "redirect:/login?error";
        }

        // Already verified
        if (Boolean.TRUE.equals(
                session.getAttribute("OTP_VERIFIED")
        )) {

            return "redirect:/employee/dashboard";
        }

        model.addAttribute(
                "employeeName",
                employee.getName()
        );

        String adminEmail = null;
        try {
            adminEmail = getAdminEmail();
        } catch (Exception e) {
            log.warn("Super Admin email could not be determined: {}", e.getMessage());
        }

        model.addAttribute(
                "adminEmail",
                adminEmail != null ? adminEmail : "Administrator"
        );

        model.addAttribute(
                "maxAttempts",
                otpService.getMaxAttempts()
        );

        /*
         * -----------------------------------------------------
         * AUTOMATIC FIRST OTP
         * -----------------------------------------------------
         *
         * If this login session has never requested an OTP,
         * automatically attempt to send one when the OTP page opens.
         * If delivery previously failed (mailError parameter present),
         * do not trigger an immediate retry loop.
         */

        String existingOtp =
                (String) session.getAttribute("OTP_CODE");

        LocalDateTime existingExpiry =
                (LocalDateTime) session.getAttribute(
                        "OTP_EXPIRY"
                );

        if (mailError == null && (existingOtp == null
                || existingExpiry == null
                || otpService.isExpired(existingExpiry))) {

            boolean sent = sendOtpToEmployee(
                    employee,
                    session
            );

            if (sent) {
                return "redirect:/employee/otp?sent";
            } else {
                return "redirect:/employee/otp?mailError";
            }
        }

        return "auth/employee-otp";
    }


    // =========================================================
    // AUTOMATIC OTP SENDING WITH SAFE EXCEPTION HANDLING
    // =========================================================

    private boolean sendOtpToEmployee(
            User employee,
            HttpSession session
    ) {

        String otp =
                otpService.generateOtp();

        LocalDateTime expiry =
                otpService.getExpiryTime();

        try {

            String adminEmail =
                    getAdminEmail();

            otpEmailService.sendEmployeeOtp(
                    adminEmail,
                    employee.getName(),
                    otp
            );

            // Persist OTP in session ONLY after successful delivery
            session.setAttribute(
                    "OTP_CODE",
                    otp
            );

            session.setAttribute(
                    "OTP_EXPIRY",
                    expiry
            );

            session.setAttribute(
                    "OTP_ATTEMPTS",
                    0
            );

            session.setAttribute(
                    "OTP_LAST_SENT",
                    LocalDateTime.now()
            );

            session.setAttribute(
                    "OTP_VERIFIED",
                    false
            );

            return true;

        } catch (Exception e) {

            log.error("Failed to send employee login OTP to Super Admin for employee [{}]: {}",
                    employee.getName(), e.getMessage(), e);

            session.removeAttribute(
                    "OTP_CODE"
            );

            session.removeAttribute(
                    "OTP_EXPIRY"
            );

            session.removeAttribute(
                    "OTP_ATTEMPTS"
            );

            return false;
        }
    }


    // =========================================================
    // RESEND OTP
    // =========================================================

    @PostMapping("/otp/send")
    public String sendOtp(
            HttpSession session
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return "redirect:/login";
        }

        String email =
                authentication.getName();

        User employee =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null
                || employee.getRole() != Role.EMPLOYEE) {

            return "redirect:/login?error";
        }

        if (!Boolean.TRUE.equals(
                employee.getActive()
        )) {

            return "redirect:/login?error";
        }

        LocalDateTime lastSent =
                (LocalDateTime)
                        session.getAttribute(
                                "OTP_LAST_SENT"
                        );

        /*
         * Prevent OTP spam.
         * User must wait 60 seconds before resend, only if OTP was previously successfully sent.
         */

        if (session.getAttribute("OTP_CODE") != null && !otpService.isResendAllowed(lastSent)) {

            return "redirect:/employee/otp?cooldown";
        }

        boolean sent = sendOtpToEmployee(
                employee,
                session
        );

        if (sent) {
            return "redirect:/employee/otp?sent";
        } else {
            return "redirect:/employee/otp?mailError";
        }
    }


    // =========================================================
    // VERIFY OTP
    // =========================================================

    @PostMapping("/otp/verify")
    public String verifyOtp(
            @RequestParam String otp,
            HttpSession session
    ) {

        String storedOtp =
                (String)
                        session.getAttribute(
                                "OTP_CODE"
                        );

        LocalDateTime expiry =
                (LocalDateTime)
                        session.getAttribute(
                                "OTP_EXPIRY"
                        );

        Integer attempts =
                (Integer)
                        session.getAttribute(
                                "OTP_ATTEMPTS"
                        );

        // No OTP generated
        if (storedOtp == null
                || expiry == null) {

            return "redirect:/employee/otp?notSent";
        }

        // OTP expired
        if (otpService.isExpired(expiry)) {

            session.removeAttribute(
                    "OTP_CODE"
            );

            session.removeAttribute(
                    "OTP_EXPIRY"
            );

            return "redirect:/employee/otp?expired";
        }

        // Maximum attempts
        if (otpService.isAttemptsExceeded(
                attempts
        )) {

            return "redirect:/employee/otp?attempts";
        }

        // Invalid OTP
        if (otp == null
                || !otp.trim().equals(storedOtp)) {

            int currentAttempts =
                    attempts == null
                            ? 0
                            : attempts;

            session.setAttribute(
                    "OTP_ATTEMPTS",
                    currentAttempts + 1
            );

            return "redirect:/employee/otp?invalid";
        }

        // =====================================================
        // OTP VERIFIED
        // =====================================================

        session.setAttribute(
                "OTP_VERIFIED",
                true
        );

        session.removeAttribute(
                "OTP_CODE"
        );

        session.removeAttribute(
                "OTP_EXPIRY"
        );

        session.removeAttribute(
                "OTP_ATTEMPTS"
        );

        return "redirect:/employee/dashboard";
    }


    // =========================================================
    // FIND ACTIVE SUPER ADMIN EMAIL
    // =========================================================

    private String getAdminEmail() {

        return userRepository
                .findAll()
                .stream()

                .filter(user ->
                        user.getRole()
                                == Role.SUPER_ADMIN
                )

                .filter(user ->
                        Boolean.TRUE.equals(
                                user.getActive()
                        )
                )

                .map(User::getEmail)

                .filter(email ->
                        email != null
                                && !email.trim().isEmpty()
                )

                .findFirst()

                .orElseThrow(
                        () -> new IllegalStateException(
                                "No active Super Admin email found."
                        )
                );
    }
}