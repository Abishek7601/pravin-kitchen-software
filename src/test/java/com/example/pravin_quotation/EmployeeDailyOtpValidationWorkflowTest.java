package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.EmployeeDailyOtpVerificationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.ActiveEmployeeSessionRegistry;
import com.example.pravin_quotation.security.EmployeeDailyOtpVerificationService;
import com.example.pravin_quotation.security.EmployeeSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class EmployeeDailyOtpValidationWorkflowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeDailyOtpVerificationRepository dailyOtpVerificationRepository;

    @Autowired
    private EmployeeDailyOtpVerificationService employeeDailyOtpVerificationService;

    @Autowired
    private EmployeeSessionService employeeSessionService;

    @Autowired
    private ActiveEmployeeSessionRegistry activeEmployeeSessionRegistry;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User testEmployee;
    private User testSuperAdmin;

    private static final ZoneId ZONE_IST = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime morningTime = today.atTime(9, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(morningTime.toInstant(), ZONE_IST));

        testSuperAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName("Super Admin Tester");
                    admin.setEmail("admin-daily-test@pravinkitchens.com");
                    admin.setRole(Role.SUPER_ADMIN);
                    return admin;
                });
        testSuperAdmin.setPassword(passwordEncoder.encode("admin123"));
        testSuperAdmin.setActive(true);
        testSuperAdmin = userRepository.save(testSuperAdmin);

        testEmployee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User emp = new User();
                    emp.setName("Employee Tester");
                    emp.setEmail("employee-daily-test@pravinkitchens.com");
                    emp.setRole(Role.EMPLOYEE);
                    return emp;
                });
        testEmployee.setPassword(passwordEncoder.encode("emp123"));
        testEmployee.setActive(true);
        testEmployee = userRepository.save(testEmployee);

        // Ensure clean daily verification state for test
        dailyOtpVerificationRepository.deleteByUserIdAndVerificationDate(testEmployee.getId(), today);
    }

    @AfterEach
    void tearDown() {
        employeeSessionService.setClock(Clock.system(ZONE_IST));
    }

    @Test
    @DisplayName("Requirement 1 & 2: First Employee login at 9:30 AM requires OTP, correct verification succeeds")
    void test1And2_FirstLoginAt930AmRequiresOtpAndSucceeds() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time930Am = today.atTime(9, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time930Am.toInstant(), ZONE_IST));

        // 1. Password login at 9:30 AM
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"));

        // Direct access before OTP is blocked
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));

        // 2. Submit correct OTP
        session.setAttribute("OTP_CODE", "123456");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        assertEquals(Boolean.TRUE, session.getAttribute("OTP_VERIFIED"));
        assertTrue(employeeDailyOtpVerificationService.isDailyOtpVerified(testEmployee.getId()),
                "Database record must be created for today");

        // Can access dashboard
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requirement 3 & 4: Employee logout and subsequent logins on same day do not request OTP")
    void test3And4_SubsequentLoginsOnSameDaySkipOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time1000Am = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1000Am.toInstant(), ZONE_IST));

        // Perform initial verification
        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);
        assertTrue(employeeDailyOtpVerificationService.isDailyOtpVerified(testEmployee.getId()));

        // Subsequent login at 11:00 AM
        ZonedDateTime time1100Am = today.atTime(11, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1100Am.toInstant(), ZONE_IST));

        MvcResult loginResult1 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andReturn();

        MockHttpSession session1 = (MockHttpSession) loginResult1.getRequest().getSession(false);
        assertNotNull(session1);
        assertEquals(Boolean.TRUE, session1.getAttribute("OTP_VERIFIED"));

        // Manual logout at 12:00 PM
        mockMvc.perform(post("/logout")
                        .session(session1)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session1.isInvalid());

        // Login again at 2:00 PM (14:00)
        ZonedDateTime time200Pm = today.atTime(14, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time200Pm.toInstant(), ZONE_IST));

        MvcResult loginResult2 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andReturn();

        MockHttpSession session2 = (MockHttpSession) loginResult2.getRequest().getSession(false);
        assertNotNull(session2);
        assertEquals(Boolean.TRUE, session2.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("Requirement 5 & 6: Browser refresh and browser restart retain daily verification")
    void test5And6_BrowserRefreshAndRestartRetainVerification() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time1200Pm = today.atTime(12, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1200Pm.toInstant(), ZONE_IST));

        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);

        // 5. Browser refresh: user already logged in with verified OTP
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // 6. Browser restart (new session, same user re-authenticates with password)
        MvcResult restartLogin = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andReturn();

        MockHttpSession newSession = (MockHttpSession) restartLogin.getRequest().getSession(false);
        assertNotNull(newSession);
        assertEquals(Boolean.TRUE, newSession.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("Requirement 7: Employee login at 7:59 PM after already verifying skips OTP")
    void test7_LoginAt759PmSkipsOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time759Pm = today.atTime(19, 59, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time759Pm.toInstant(), ZONE_IST));

        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.TRUE, session.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("Requirement 8: At exactly 8:00 PM, all active Employee sessions expire")
    void test8_AtExactly8PmAllActiveEmployeeSessionsExpire() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);

        // Active daytime session before 8:00 PM
        ZonedDateTime time730Pm = today.atTime(19, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time730Pm.toInstant(), ZONE_IST));

        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        activeEmployeeSessionRegistry.registerEmployeeSession(session, testEmployee.getId());

        // Access allowed at 7:59 PM
        ZonedDateTime time759Pm = today.atTime(19, 59, 59).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time759Pm.toInstant(), ZONE_IST));
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // Clock hits 8:00:00 PM IST
        ZonedDateTime time800Pm = today.atTime(20, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time800Pm.toInstant(), ZONE_IST));

        // Any request by active session at or after 8:00 PM is intercepted by filter and redirected to /login?expired
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        assertTrue(session.isInvalid(), "Filter invalidates session upon detecting 8:00 PM expiration");

        // Verify scheduled invalidation of tracked sessions
        MockHttpSession anotherSession = new MockHttpSession();
        activeEmployeeSessionRegistry.registerEmployeeSession(anotherSession, testEmployee.getId());
        assertTrue(activeEmployeeSessionRegistry.getActiveEmployeeSessionCount() >= 1,
                "Registry must contain at least one registered employee session");
        activeEmployeeSessionRegistry.invalidateAllEmployeeSessionsAt8Pm();
        assertTrue(anotherSession.isInvalid(), "Scheduler invalidates all registered employee sessions");
        assertEquals(0, activeEmployeeSessionRegistry.getActiveEmployeeSessionCount(),
                "Registry must be empty after 8:00 PM invalidation");
    }

    @Test
    @DisplayName("Requirement 9: Employee login at 8:15 PM requires a fresh OTP")
    void test9_EmployeeLoginAt815PmRequiresFreshOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time815Pm = today.atTime(20, 15, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time815Pm.toInstant(), ZONE_IST));

        // Even if employee had verified earlier in the day, after 8:00 PM fresh OTP is required
        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("Requirement 10: Next day at 9:30 AM requires a fresh OTP")
    void test10_NextDayAt930AmRequiresFreshOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        LocalDate tomorrow = today.plusDays(1);

        // Verified today
        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);
        assertTrue(employeeDailyOtpVerificationService.isDailyOtpVerified(testEmployee.getId()));

        // Advance to tomorrow 9:30 AM
        ZonedDateTime tomorrow930Am = tomorrow.atTime(9, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(tomorrow930Am.toInstant(), ZONE_IST));

        assertFalse(employeeDailyOtpVerificationService.isDailyOtpVerified(testEmployee.getId()),
                "Daily verification must expire when the date advances to tomorrow");

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("Requirement 11 & 12: Super Admin login behavior remains unchanged (requires OTP on each login, no 8 PM restriction)")
    void test11And12_SuperAdminBehaviorUnchanged() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time1000Am = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1000Am.toInstant(), ZONE_IST));

        // 1. Super Admin login requires OTP
        MvcResult adminLogin1 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testSuperAdmin.getEmail())
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp"))
                .andReturn();

        MockHttpSession adminSession1 = (MockHttpSession) adminLogin1.getRequest().getSession(false);
        assertNotNull(adminSession1);
        assertEquals(Boolean.FALSE, adminSession1.getAttribute("OTP_VERIFIED"));

        // Verify OTP
        adminSession1.setAttribute("OTP_VERIFIED", true);
        mockMvc.perform(get("/admin/dashboard").session(adminSession1))
                .andExpect(status().isOk());

        // 2. Night at 9:00 PM (21:00) - Super Admin session is NOT invalidated by 8 PM scheduler
        ZonedDateTime time900Pm = today.atTime(21, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time900Pm.toInstant(), ZONE_IST));

        activeEmployeeSessionRegistry.invalidateAllEmployeeSessionsAt8Pm();
        assertFalse(adminSession1.isInvalid(), "Super Admin session must NOT be invalidated by 8:00 PM auto-logout");

        mockMvc.perform(get("/admin/dashboard").session(adminSession1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requirement 13: Invalid Employee password rejected even if daily OTP was already verified")
    void test13_InvalidPasswordRejectedEvenIfDailyOtpVerified() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time1100Am = today.atTime(11, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1100Am.toInstant(), ZONE_IST));

        // Employee has valid OTP record in DB
        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);
        assertTrue(employeeDailyOtpVerificationService.isDailyOtpVerified(testEmployee.getId()));

        // Login with WRONG password
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "WRONG_PASSWORD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("Requirement 14: Expired or invalid OTP rejected during verification")
    void test14_ExpiredOrInvalidOtpRejected() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time930Am = today.atTime(9, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time930Am.toInstant(), ZONE_IST));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        session.setAttribute("OTP_CODE", "888999");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        // Submit invalid OTP
        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp?invalid"));

        // Expire the OTP
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().minusMinutes(1));

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "888999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp?expired"));
    }

    @Test
    @DisplayName("Requirement 15: Direct URL/API access after 8:00 PM is blocked")
    void test15_DirectAccessAfter8PmBlocked() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time1000Am = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1000Am.toInstant(), ZONE_IST));

        employeeDailyOtpVerificationService.recordSuccessfulVerification(testEmployee);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        // Allowed during the day
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // Advance clock past 8:00 PM IST (e.g. 20:05 IST)
        ZonedDateTime time805Pm = today.atTime(20, 5, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time805Pm.toInstant(), ZONE_IST));

        // Direct request to dashboard after 8 PM must be blocked and redirected to /login?expired
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        assertTrue(session.isInvalid(), "Session must be invalidated after 8:00 PM access attempt");
    }
}
