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

    private final EmployeeSessionService employeeSessionService;

    public EmployeeOtpFilter(EmployeeSessionService employeeSessionService) {
        this.employeeSessionService = employeeSessionService;
    }

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

            // Allow manual logout to proceed immediately without interference
            if (requestUri.equals("/logout")) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean staticResource =
                    requestUri.startsWith("/css/")
                            || requestUri.startsWith("/js/")
                            || requestUri.startsWith("/images/");

            boolean loginPage = requestUri.equals("/login");

            if (!staticResource && !loginPage) {

                HttpSession session = request.getSession(false);

                // =========================================================
                // 1. HARD DEADLINE: 8:00 PM IST AUTO-LOGOUT ENFORCEMENT
                // =========================================================
                if (employeeSessionService.isSessionExpired(session)) {
                    employeeSessionService.invalidateEmployeeSession(request, response);

                    String xRequestedWith = request.getHeader("X-Requested-With");
                    String accept = request.getHeader("Accept");
                    if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
                            || (accept != null && accept.contains("application/json"))) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"Session expired at 8:00 PM IST\"}");
                        return;
                    }

                    response.sendRedirect("/login?expired");
                    return;
                }

                // =========================================================
                // 2. ADMIN OTP ENFORCEMENT FOR NEW LOGINS
                // =========================================================
                boolean otpPage = requestUri.equals("/employee/otp");
                boolean otpSend = requestUri.equals("/employee/otp/send");
                boolean otpVerify = requestUri.equals("/employee/otp/verify");
                boolean sessionStatus = requestUri.equals("/employee/session-status");

                if (!otpPage && !otpSend && !otpVerify && !sessionStatus) {
                    boolean otpVerified = session != null
                            && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));

                    if (!otpVerified) {
                        response.sendRedirect("/employee/otp");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}