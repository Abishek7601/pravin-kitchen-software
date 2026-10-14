package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.security.EmployeeSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final EmployeeSessionService employeeSessionService;

    public AuthController(EmployeeSessionService employeeSessionService) {
        this.employeeSessionService = employeeSessionService;
    }

    @GetMapping("/login")
    public String login(){
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        employeeSessionService.invalidateEmployeeSession(request, response);
        return "redirect:/login?logout";
    }
}
