package com.example.pravin_quotation.security;

import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class EmployeeOtpFilter extends OncePerRequestFilter {

    private final EmployeeSessionService employeeSessionService;
    private final EmployeeDailyOtpVerificationService employeeDailyOtpVerificationService;
    private final UserRepository userRepository;
    private final ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry;

    public EmployeeOtpFilter(EmployeeSessionService employeeSessionService) {
        this(employeeSessionService, null, null, null);
    }

    @Autowired
    public EmployeeOtpFilter(
            EmployeeSessionService employeeSessionService,
            EmployeeDailyOtpVerificationService employeeDailyOtpVerificationService,
            UserRepository userRepository,
            ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry
    ) {
        this.employeeSessionService = employeeSessionService;
        this.employeeDailyOtpVerificationService = employeeDailyOtpVerificationService;
        this.userRepository = userRepository;
        this.activeEmployeeSessionRegistry = activeEmployeeSessionRegistry;
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
                && authentication.isAuthenticated()) {

            boolean isEmployee = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_EMPLOYEE"));

            boolean isSuperAdmin = authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_SUPER_ADMIN"));

            String requestUri = request.getRequestURI();

            // Allow manual logout to proceed immediately without interference
            if (requestUri.equals("/logout")) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean staticResource =
                    requestUri.startsWith("/css/")
                            || requestUri.startsWith("/js/")
                            || requestUri.startsWith("/images/")
                            || requestUri.startsWith("/uploads/");

            boolean loginPage = requestUri.equals("/login");

            if (isEmployee) {
                boolean forgotPasswordPage = requestUri.startsWith("/employee/forgot-password");

                if (!staticResource && !loginPage && !forgotPasswordPage) {

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
                            response.getWriter().write("{\"error\":\"SESSION_EXPIRED\",\"message\":\"Your session has expired at 8:00 PM IST. Please log in again.\"}");
                            return;
                        }

                        response.sendRedirect("/login?expired");
                        return;
                    }

                    // =========================================================
                    // 2. EMPLOYEE OTP ENFORCEMENT
                    // =========================================================
                    boolean otpPage = requestUri.equals("/employee/otp");
                    boolean otpSend = requestUri.equals("/employee/otp/send");
                    boolean otpVerify = requestUri.equals("/employee/otp/verify");
                    boolean sessionStatus = requestUri.equals("/employee/session-status");

                    boolean otpVerified = session != null
                            && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));

                    if (!otpVerified && employeeDailyOtpVerificationService != null && userRepository != null) {
                        String email = authentication.getName();
                        User employee = userRepository.findByEmail(email).orElse(null);
                        if (employee != null && employeeDailyOtpVerificationService.isDailyOtpVerified(employee.getId())) {
                            if (session != null) {
                                session.setAttribute("OTP_VERIFIED", true);
                                if (activeEmployeeSessionRegistry != null) {
                                    activeEmployeeSessionRegistry.registerEmployeeSession(session, employee.getId());
                                }
                            }
                            otpVerified = true;
                        }
                    }

                    if (!otpVerified) {
                        if (!otpPage && !otpSend && !otpVerify && !sessionStatus) {
                            response.sendRedirect("/employee/otp");
                            return;
                        }
                    } else {
                        if (otpPage) {
                            response.sendRedirect("/employee/dashboard");
                            return;
                        }
                    }
                }
            } else if (isSuperAdmin) {
                // =========================================================
                // 3. SUPER ADMIN OTP ENFORCEMENT (NO 8:00 PM RESTRICTION)
                // =========================================================
                if (!staticResource && !loginPage) {

                    HttpSession session = request.getSession(false);

                    boolean adminOtpPage = requestUri.equals("/admin/otp");
                    boolean adminOtpSend = requestUri.equals("/admin/otp/send");
                    boolean adminOtpVerify = requestUri.equals("/admin/otp/verify");

                    boolean otpVerified = session != null
                            && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));

                    if (!otpVerified) {
                        if (!adminOtpPage && !adminOtpSend && !adminOtpVerify) {
                            String xRequestedWith = request.getHeader("X-Requested-With");
                            String accept = request.getHeader("Accept");
                            if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
                                    || (accept != null && accept.contains("application/json"))) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"OTP_REQUIRED\",\"message\":\"Super Admin OTP verification is required.\"}");
                                return;
                            }

                            response.sendRedirect("/admin/otp");
                            return;
                        }
                    } else {
                        if (adminOtpPage) {
                            response.sendRedirect("/admin/dashboard");
                            return;
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
