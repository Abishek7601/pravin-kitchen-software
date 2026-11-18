package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.ActiveEmployeeSessionRegistry;
import com.example.pravin_quotation.security.EmployeeDailyOtpVerificationService;
import com.example.pravin_quotation.security.EmployeeSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class AuthController {

    private final EmployeeSessionService employeeSessionService;
    private final UserRepository userRepository;
    private final EmployeeDailyOtpVerificationService employeeDailyOtpVerificationService;
    private final ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry;

    public AuthController(EmployeeSessionService employeeSessionService) {
        this(employeeSessionService, null, null, null);
    }

    @Autowired
    public AuthController(
            EmployeeSessionService employeeSessionService,
            UserRepository userRepository,
            EmployeeDailyOtpVerificationService employeeDailyOtpVerificationService,
            ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry
    ) {
        this.employeeSessionService = employeeSessionService;
        this.userRepository = userRepository;
        this.employeeDailyOtpVerificationService = employeeDailyOtpVerificationService;
        this.activeEmployeeSessionRegistry = activeEmployeeSessionRegistry;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
            HttpSession session = request.getSession(false);

            if (roles.contains("ROLE_SUPER_ADMIN")) {
                if (session != null && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"))) {
                    return "redirect:/admin/dashboard";
                }
                return "redirect:/admin/otp";
            } else if (roles.contains("ROLE_EMPLOYEE")) {
                if (employeeSessionService != null && employeeSessionService.isSessionExpired(session)) {
                    return "auth/login";
                }
                if (session != null && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"))) {
                    return "redirect:/employee/dashboard";
                }
                return "redirect:/employee/otp";
            }
        }
        return "auth/login";
    }

    @GetMapping("/public/auth/check-role")
    @ResponseBody
    public Map<String, Object> checkRole(@RequestParam(name = "email", required = false) String email) {
        if (email == null || email.trim().isEmpty() || userRepository == null) {
            return Map.of("exists", false, "requiresOtp", false);
        }
        String cleanEmail = email.trim();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(cleanEmail))
                    .findFirst();
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            boolean active = Boolean.TRUE.equals(user.getActive());
            boolean requiresOtp;
            if (!active) {
                requiresOtp = false;
            } else if (user.getRole() == Role.SUPER_ADMIN) {
                requiresOtp = true;
            } else if (user.getRole() == Role.EMPLOYEE) {
                boolean verifiedToday = employeeDailyOtpVerificationService != null
                        && employeeDailyOtpVerificationService.isDailyOtpVerified(user.getId());
                requiresOtp = !verifiedToday;
            } else {
                requiresOtp = false;
            }

            return Map.of(
                    "exists", true,
                    "role", user.getRole().name(),
                    "requiresOtp", requiresOtp
            );
        }
        return Map.of("exists", false, "requiresOtp", false);
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        if (activeEmployeeSessionRegistry != null && request.getSession(false) != null) {
            activeEmployeeSessionRegistry.unregisterEmployeeSession(request.getSession(false));
        }
        employeeSessionService.invalidateEmployeeSession(request, response);
        return "redirect:/login?logout";
    }
}
