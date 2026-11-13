package com.example.pravin_quotation.security;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private final EmployeeSessionService employeeSessionService;
    private final EmployeeDailyOtpVerificationService employeeDailyOtpVerificationService;
    private final UserRepository userRepository;
    private final ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry;

    @Autowired
    public CustomAuthenticationSuccessHandler(
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
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        HttpSession session = request.getSession();

        // Clear any old OTP session state
        session.removeAttribute("OTP_CODE");
        session.removeAttribute("OTP_EXPIRY");
        session.removeAttribute("OTP_ATTEMPTS");
        session.removeAttribute("OTP_LAST_SENT");

        Set<String> roles =
                AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_SUPER_ADMIN")) {

            // Super Admin requires OTP verification on every new login.
            // Super Admin has NO 8:00 PM IST auto-logout restriction.
            session.setAttribute("OTP_VERIFIED", false);

            response.sendRedirect("/admin/otp");

        } else if (roles.contains("ROLE_EMPLOYEE")) {

            // Initialize employee session (tracks login date and 8:00 PM IST cutoff)
            employeeSessionService.initializeEmployeeSession(session);

            String email = authentication.getName();
            User employee = userRepository != null ? userRepository.findByEmail(email).orElse(null) : null;

            // Check if Employee has already successfully verified OTP for today's 9:00 AM - 8:00 PM IST window
            boolean alreadyVerifiedToday = false;
            if (employee != null && employeeDailyOtpVerificationService != null) {
                alreadyVerifiedToday = employeeDailyOtpVerificationService.isDailyOtpVerified(employee.getId());
            }

            if (alreadyVerifiedToday) {
                // Subsequent login on the same day within 9:00 AM - 8:00 PM IST:
                // Skip OTP completely (no OTP email, no OTP screen, direct entry to dashboard)
                log.info("Employee [{}] already verified OTP for today. Bypassing OTP screen directly to dashboard.", email);
                session.setAttribute("OTP_VERIFIED", true);

                if (activeEmployeeSessionRegistry != null && employee != null) {
                    activeEmployeeSessionRegistry.registerEmployeeSession(session, employee.getId());
                }

                response.sendRedirect("/employee/dashboard");

            } else {
                // First login of the day (or after-hours): require fresh OTP verification
                log.info("Employee [{}] requires OTP verification for today.", email);
                session.setAttribute("OTP_VERIFIED", false);

                if (activeEmployeeSessionRegistry != null && employee != null) {
                    activeEmployeeSessionRegistry.registerEmployeeSession(session, employee.getId());
                }

                response.sendRedirect("/employee/otp");
            }

        } else {

            response.sendRedirect("/login?error");
        }
    }
}
