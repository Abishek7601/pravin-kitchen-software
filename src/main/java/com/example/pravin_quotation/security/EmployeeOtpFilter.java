package com.example.pravin_quotation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class EmployeeOtpFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_EMPLOYEE"))) {

            String requestUri = request.getRequestURI();

            boolean otpPage =
                    requestUri.equals("/employee/otp");

            boolean otpSend =
                    requestUri.equals("/employee/otp/send");

            boolean otpVerify =
                    requestUri.equals("/employee/otp/verify");

            boolean staticResource =
                    requestUri.startsWith("/css/")
                            || requestUri.startsWith("/js/")
                            || requestUri.startsWith("/images/");

            boolean loginPage =
                    requestUri.equals("/login");

            if (!otpPage
                    && !otpSend
                    && !otpVerify
                    && !staticResource
                    && !loginPage) {

                HttpSession session =
                        request.getSession(false);

                boolean otpVerified =
                        session != null
                                && Boolean.TRUE.equals(
                                session.getAttribute("OTP_VERIFIED")
                        );

                if (!otpVerified) {

                    response.sendRedirect("/employee/otp");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}