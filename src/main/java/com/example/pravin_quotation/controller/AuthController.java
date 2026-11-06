package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.EmployeeSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final EmployeeSessionService employeeSessionService;
    private final UserRepository userRepository;

    public AuthController(EmployeeSessionService employeeSessionService) {
        this(employeeSessionService, null);
    }

    @Autowired
    public AuthController(
            EmployeeSessionService employeeSessionService,
            UserRepository userRepository
    ) {
        this.employeeSessionService = employeeSessionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String login(){
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
            boolean requiresOtp = active && (user.getRole() == Role.EMPLOYEE || user.getRole() == Role.SUPER_ADMIN);
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
        employeeSessionService.invalidateEmployeeSession(request, response);
        return "redirect:/login?logout";
    }
}
