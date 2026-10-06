package com.example.pravin_quotation.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        HttpSession session = request.getSession();

        // Every new login must start without OTP verification.
        session.removeAttribute("OTP_CODE");
        session.removeAttribute("OTP_EXPIRY");
        session.removeAttribute("OTP_ATTEMPTS");
        session.removeAttribute("OTP_LAST_SENT");
        session.setAttribute("OTP_VERIFIED", false);

        Set<String> roles =
                AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_SUPER_ADMIN")) {

            // Admin does NOT need OTP
            session.setAttribute("OTP_VERIFIED", true);

            response.sendRedirect("/admin/dashboard");

        } else if (roles.contains("ROLE_EMPLOYEE")) {

            // Employee MUST verify OTP
            session.setAttribute("OTP_VERIFIED", false);

            response.sendRedirect("/employee/otp");

        } else {

            response.sendRedirect("/login?error");
        }
    }
}