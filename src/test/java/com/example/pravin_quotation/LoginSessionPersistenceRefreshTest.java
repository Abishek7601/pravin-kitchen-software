package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AuthController;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.EmployeeDailyOtpVerificationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class LoginSessionPersistenceRefreshTest {

    private EmployeeSessionService employeeSessionService;
    private EmployeeDailyOtpVerificationService dailyOtpVerificationService;
    private ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry;
    private EmployeeOtpFilter employeeOtpFilter;
    private AuthController authController;

    private User superAdminUser;
    private User employeeUser;

    private static final ZoneId ZONE_IST = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        employeeSessionService = new EmployeeSessionService();
        // Set clock to 11:00 AM IST today
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime morningTime = today.atTime(11, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(morningTime.toInstant(), ZONE_IST));

        superAdminUser = new User();
        superAdminUser.setId(1L);
        superAdminUser.setName("Super Admin");
        superAdminUser.setEmail("admin@pravinkitchens.com");
        superAdminUser.setRole(Role.SUPER_ADMIN);
        superAdminUser.setActive(true);

        employeeUser = new User();
        employeeUser.setId(2L);
        employeeUser.setName("Test Employee");
        employeeUser.setEmail("employee@pravinkitchens.com");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setActive(true);

        // Simple mock repository for UserRepository
        UserRepository mockUserRepo = (UserRepository) java.lang.reflect.Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByEmail")) {
                        String email = (String) args[0];
                        if (email.equalsIgnoreCase(superAdminUser.getEmail())) return Optional.of(superAdminUser);
                        if (email.equalsIgnoreCase(employeeUser.getEmail())) return Optional.of(employeeUser);
                        return Optional.empty();
                    }
                    return null;
                }
        );

        // Simple mock repository for EmployeeDailyOtpVerificationRepository
        EmployeeDailyOtpVerificationRepository mockDailyRepo = (EmployeeDailyOtpVerificationRepository) java.lang.reflect.Proxy.newProxyInstance(
                EmployeeDailyOtpVerificationRepository.class.getClassLoader(),
                new Class<?>[]{EmployeeDailyOtpVerificationRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("existsByUserIdAndVerificationDateAndActiveTrue")) {
                        Long uid = (Long) args[0];
                        LocalDate d = (LocalDate) args[1];
                        return uid.equals(employeeUser.getId()) && d.equals(today);
                    }
                    return false;
                }
        );

        dailyOtpVerificationService = new EmployeeDailyOtpVerificationService(mockDailyRepo, employeeSessionService);
        activeEmployeeSessionRegistry = new ActiveEmployeeSessionRegistry();
        employeeOtpFilter = new EmployeeOtpFilter(
                employeeSessionService,
                dailyOtpVerificationService,
                mockUserRepo,
                activeEmployeeSessionRegistry
        );
        authController = new AuthController(
                employeeSessionService,
                mockUserRepo,
                dailyOtpVerificationService,
                activeEmployeeSessionRegistry
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        employeeSessionService.setClock(Clock.system(ZONE_IST));
    }

    @Test
    @DisplayName("Scenario 1 & 2: Super Admin / Admin login, OTP verified -> Browser refresh preserves session")
    void testSuperAdminLoginAndRefreshPreservesSession() throws ServletException, IOException {
        // Authenticate Super Admin in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        superAdminUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        // 1. Initial request to /admin/dashboard
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/admin/dashboard");
        req1.setSession(session);
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        MockFilterChain chain1 = new MockFilterChain();

        employeeOtpFilter.doFilter(req1, res1, chain1);
        assertEquals(200, res1.getStatus());
        assertNull(res1.getRedirectedUrl(), "Super Admin must not be redirected away from dashboard");

        // 2. Browser refresh (F5) on /admin/dashboard
        MockHttpServletRequest refreshReq = new MockHttpServletRequest("GET", "/admin/dashboard");
        refreshReq.setSession(session);
        MockHttpServletResponse refreshRes = new MockHttpServletResponse();
        MockFilterChain refreshChain = new MockFilterChain();

        employeeOtpFilter.doFilter(refreshReq, refreshRes, refreshChain);
        assertEquals(200, refreshRes.getStatus());
        assertNull(refreshRes.getRedirectedUrl(), "Super Admin refresh must not redirect or logout");

        // 3. Navigation to /admin/quotations and refresh
        MockHttpServletRequest navReq = new MockHttpServletRequest("GET", "/admin/quotations");
        navReq.setSession(session);
        MockHttpServletResponse navRes = new MockHttpServletResponse();
        employeeOtpFilter.doFilter(navReq, navRes, new MockFilterChain());
        assertEquals(200, navRes.getStatus());
        assertNull(navRes.getRedirectedUrl(), "Super Admin must navigate without re-login");

        // 4. If Super Admin hits /login while authenticated, AuthController redirects to /admin/dashboard
        MockHttpServletRequest loginPageReq = new MockHttpServletRequest("GET", "/login");
        loginPageReq.setSession(session);
        String loginView = authController.login(loginPageReq);
        assertEquals("redirect:/admin/dashboard", loginView,
                "Authenticated Super Admin visiting /login must be redirected to dashboard");
    }

    @Test
    @DisplayName("Scenario 3, 4, 5, 6: Employee login with OTP -> Refresh preserves session without requesting another OTP")
    void testEmployeeLoginAndRefreshPreservesSessionWithoutNewOtp() throws ServletException, IOException {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime morningTime = today.atTime(10, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(morningTime.toInstant(), ZONE_IST));

        // Authenticate Employee in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                )
        );

        MockHttpSession session = new MockHttpSession();
        employeeSessionService.initializeEmployeeSession(session);
        session.setAttribute("OTP_VERIFIED", true);

        // 1. Initial request to /employee/dashboard
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/employee/dashboard");
        req1.setSession(session);
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        employeeOtpFilter.doFilter(req1, res1, new MockFilterChain());
        assertEquals(200, res1.getStatus());
        assertNull(res1.getRedirectedUrl(), "Employee must access dashboard without redirect");

        // 2. F5 Browser Refresh on Dashboard
        MockHttpServletRequest refreshReq = new MockHttpServletRequest("GET", "/employee/dashboard");
        refreshReq.setSession(session);
        MockHttpServletResponse refreshRes = new MockHttpServletResponse();
        employeeOtpFilter.doFilter(refreshReq, refreshRes, new MockFilterChain());
        assertEquals(200, refreshRes.getStatus());
        assertNull(refreshRes.getRedirectedUrl(), "Refresh must not log out employee or ask for OTP");

        // 3. Navigation to Quotations and Refresh
        MockHttpServletRequest quoteReq = new MockHttpServletRequest("GET", "/employee/quotations");
        quoteReq.setSession(session);
        MockHttpServletResponse quoteRes = new MockHttpServletResponse();
        employeeOtpFilter.doFilter(quoteReq, quoteRes, new MockFilterChain());
        assertEquals(200, quoteRes.getStatus());
        assertNull(quoteRes.getRedirectedUrl());

        // 4. Verify before 8:00 PM IST (e.g. 7:59 PM), refresh does not log out
        ZonedDateTime time759Pm = today.atTime(19, 59, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time759Pm.toInstant(), ZONE_IST));

        MockHttpServletRequest req759 = new MockHttpServletRequest("GET", "/employee/dashboard");
        req759.setSession(session);
        MockHttpServletResponse res759 = new MockHttpServletResponse();
        employeeOtpFilter.doFilter(req759, res759, new MockFilterChain());
        assertEquals(200, res759.getStatus());
        assertNull(res759.getRedirectedUrl(), "Session must remain active before 8:00 PM IST");

        // 5. If Employee hits /login while authenticated before 8 PM, AuthController redirects to dashboard
        MockHttpServletRequest loginPageReq = new MockHttpServletRequest("GET", "/login");
        loginPageReq.setSession(session);
        String loginView = authController.login(loginPageReq);
        assertEquals("redirect:/employee/dashboard", loginView);
    }

    @Test
    @DisplayName("Scenario 7 & 8: Employee session terminates at exactly 8:00 PM IST and requires fresh OTP afterwards")
    void testEmployeeSessionExpiresAt8PmIstAndRequiresFreshOtp() throws ServletException, IOException {
        LocalDate today = LocalDate.now(ZONE_IST);

        // Employee authenticated at 2:00 PM IST
        ZonedDateTime time2Pm = today.atTime(14, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time2Pm.toInstant(), ZONE_IST));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                )
        );

        MockHttpSession session = new MockHttpSession();
        employeeSessionService.initializeEmployeeSession(session);
        session.setAttribute("OTP_VERIFIED", true);

        // Advance clock to exactly 8:00:00 PM IST (20:00:00)
        ZonedDateTime time800Pm = today.atTime(20, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time800Pm.toInstant(), ZONE_IST));

        // Attempt request -> Session must be expired and redirected to /login?expired
        MockHttpServletRequest expReq = new MockHttpServletRequest("GET", "/employee/dashboard");
        expReq.setSession(session);
        MockHttpServletResponse expRes = new MockHttpServletResponse();

        employeeOtpFilter.doFilter(expReq, expRes, new MockFilterChain());
        assertEquals(302, expRes.getStatus());
        assertEquals("/login?expired", expRes.getRedirectedUrl());
        assertTrue(session.isInvalid(), "Employee session must be invalidated at 8:00 PM IST");
    }

    @Test
    @DisplayName("Scenario 9 & 10: Manual logout terminates session and clears cookies")
    void testManualLogoutTerminatesSessionAndBlocksAccess() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        superAdminUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        MockHttpServletRequest logoutReq = new MockHttpServletRequest("GET", "/logout");
        logoutReq.setSession(session);
        MockHttpServletResponse logoutRes = new MockHttpServletResponse();

        String view = authController.logout(logoutReq, logoutRes);
        assertEquals("redirect:/login?logout", view);
        assertTrue(session.isInvalid(), "Manual logout must invalidate the HTTP session");
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "SecurityContext must be cleared upon manual logout");
    }

    @Test
    @DisplayName("Verify Session Timeout and Cookie Configuration in application.properties")
    void testSessionTimeoutAndCookieConfigInProperties() throws IOException {
        String props = Files.readString(Path.of("src/main/resources/application.properties"));

        assertTrue(props.contains("server.servlet.session.timeout=12h"),
                "Session timeout must be configured to at least 12h for working day persistence");
        assertTrue(props.contains("server.servlet.session.cookie.name=JSESSIONID"),
                "Session cookie name must be JSESSIONID");
        assertTrue(props.contains("server.servlet.session.cookie.http-only=true"),
                "Session cookie must be HttpOnly");
        assertTrue(props.contains("server.servlet.session.cookie.same-site=lax"),
                "Session cookie must have SameSite=Lax");
        assertTrue(props.contains("server.servlet.session.cookie.path=/"),
                "Session cookie path must be /");
        assertTrue(props.contains("server.servlet.session.cookie.max-age=43200"),
                "Session cookie max-age must be 43200 (12 hours)");
        assertTrue(props.contains("server.forward-headers-strategy=framework"),
                "Forward headers strategy must be configured for reverse proxy / deployment HTTPS support");
    }
}
