package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.EmployeeSessionService;
import com.example.pravin_quotation.security.OtpService;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class EmployeeLogoutAndOtpAuthenticationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmployeeSessionService employeeSessionService;

    @Autowired
    private OtpService otpService;

    private MockMvc mockMvc;
    private User testEmployee;
    private User testAdmin;

    private static final ZoneId ZONE_IST = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Default clock to 10:00 AM IST today
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime morningTime = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(morningTime.toInstant(), ZONE_IST));

        // Create or retrieve active Super Admin
        testAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName("Super Admin");
                    admin.setEmail("admin-test@pravinkitchens.com");
                    admin.setRole(Role.SUPER_ADMIN);
                    return admin;
                });
        testAdmin.setPassword(passwordEncoder.encode("admin123"));
        testAdmin.setActive(true);
        testAdmin = userRepository.save(testAdmin);

        // Create or retrieve active Employee
        testEmployee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User emp = new User();
                    emp.setName("Employee One");
                    emp.setEmail("employee-test@pravinkitchens.com");
                    emp.setRole(Role.EMPLOYEE);
                    return emp;
                });
        testEmployee.setPassword(passwordEncoder.encode("emp123"));
        testEmployee.setActive(true);
        testEmployee = userRepository.save(testEmployee);
    }

    @AfterEach
    void tearDown() {
        employeeSessionService.setClock(Clock.system(ZONE_IST));
    }

    @Test
    @DisplayName("Verification 1: Employee login at 8:00 AM requires Super Admin OTP")
    void test1_EmployeeLoginAt8AmRequiresSuperAdminOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time8Am = today.atTime(8, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time8Am.toInstant(), ZONE_IST));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Session must be created");
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"),
                "Employee login at 8:00 AM must require OTP verification (OTP_VERIFIED == false)");

        // Direct access to dashboard before OTP verification must be redirected to /employee/otp
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));
    }

    @Test
    @DisplayName("Verification 2: Employee login during working hours requires Super Admin OTP")
    void test2_EmployeeLoginDuringWorkingHoursRequiresSuperAdminOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time1130Am = today.atTime(11, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time1130Am.toInstant(), ZONE_IST));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"),
                "Employee login during working hours must require OTP verification");

        // Cannot bypass OTP by requesting dashboard directly
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));
    }

    @Test
    @DisplayName("Verification 3: Employee can use software normally after successful OTP verification")
    void test3_EmployeeCanUseSoftwareNormallyAfterOtpVerification() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time10Am = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time10Am.toInstant(), ZONE_IST));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        // Simulate OTP stored in session
        session.setAttribute("OTP_CODE", "654321");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        // Attempt verify with incorrect OTP -> fails
        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp?invalid"));

        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"));

        // Verify with correct OTP -> succeeds and redirects to dashboard
        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        assertEquals(Boolean.TRUE, session.getAttribute("OTP_VERIFIED"));

        // Can access dashboard normally without repeated OTP prompts
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // Can access quotations normally without repeated OTP prompts
        mockMvc.perform(get("/employee/quotations").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verification 4: Employee manual logout works immediately")
    void test4_EmployeeManualLogoutWorksImmediately() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time12Pm = today.atTime(12, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time12Pm.toInstant(), ZONE_IST));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        // POST /logout terminates session immediately
        mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session.isInvalid(), "Session must be immediately invalidated upon manual logout");
    }

    @Test
    @DisplayName("Verification 5: Employee login after manual logout on same day does not require OTP")
    void test5_EmployeeLoginAfterManualLogoutSkipsOtpOnSameDay() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time13Pm = today.atTime(13, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time13Pm.toInstant(), ZONE_IST));

        // 1. First login + OTP verification
        MvcResult login1 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session1 = (MockHttpSession) login1.getRequest().getSession(false);
        session1.setAttribute("OTP_CODE", "111222");
        session1.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session1)
                        .with(csrf())
                        .param("otp", "111222"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        assertEquals(Boolean.TRUE, session1.getAttribute("OTP_VERIFIED"));

        // 2. Manual logout
        mockMvc.perform(post("/logout")
                        .session(session1)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session1.isInvalid());

        // 3. Subsequent login on the same day skips OTP and goes directly to dashboard
        MvcResult login2 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andReturn();

        MockHttpSession session2 = (MockHttpSession) login2.getRequest().getSession(false);
        assertNotNull(session2);
        assertNotEquals(session1.getId(), session2.getId(), "Must be a new session");
        assertEquals(Boolean.TRUE, session2.getAttribute("OTP_VERIFIED"),
                "Re-login on the same day must reuse daily verification and skip OTP");
    }

    @Test
    @DisplayName("Verification 6: All active Employee sessions automatically terminate at exactly 8:00 PM IST")
    void test6_AllActiveEmployeeSessionsTerminateAtExactly8PmIst() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);

        // Log in at 9:00 AM IST
        ZonedDateTime time9Am = today.atTime(9, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time9Am.toInstant(), ZONE_IST));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        session.setAttribute("OTP_CODE", "999888");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "999888"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        // Active at 19:59:59 IST (before 8:00 PM) -> Allowed
        ZonedDateTime time759Pm = today.atTime(19, 59, 59).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time759Pm.toInstant(), ZONE_IST));

        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // Clock reaches exactly 8:00:00 PM IST (20:00:00)
        ZonedDateTime time800Pm = today.atTime(20, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time800Pm.toInstant(), ZONE_IST));

        // Any request is rejected, session is invalidated, and redirected to /login?expired
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        assertTrue(session.isInvalid(), "Active session must be terminated at 8:00 PM IST");
    }

    @Test
    @DisplayName("Verification 7: Employee login after 8:00 PM requires a fresh OTP")
    void test7_EmployeeLoginAfter8PmRequiresFreshOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime after8Pm = today.atTime(20, 15, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(after8Pm.toInstant(), ZONE_IST));

        // Login at 8:15 PM -> requires OTP
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

        // Cannot bypass OTP
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));

        // Verify valid OTP
        session.setAttribute("OTP_CODE", "555444");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "555444"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        assertEquals(Boolean.TRUE, session.getAttribute("OTP_VERIFIED"));

        // Allowed to use dashboard after verification
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Verification 8: Employee login the next morning also requires OTP")
    void test8_EmployeeLoginNextMorningAlsoRequiresOtp() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        LocalDate tomorrow = today.plusDays(1);
        ZonedDateTime nextMorning = tomorrow.atTime(8, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(nextMorning.toInstant(), ZONE_IST));

        // Login next morning -> requires Super Admin OTP
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"),
                "Employee login the next morning must require OTP verification");

        // Cannot access dashboard without completing OTP verification
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));
    }

    @Test
    @DisplayName("Verification 9: Old sessions cannot access protected pages after logout")
    void test9_OldSessionsCannotAccessProtectedPagesAfterLogout() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime time2Pm = today.atTime(14, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time2Pm.toInstant(), ZONE_IST));

        // Normal login + OTP
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        session.setAttribute("OTP_CODE", "333222");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "333222"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        // Logout
        mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertTrue(session.isInvalid());

        // Subsequent access with invalidated session cannot access protected page
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Verification 10: Super Admin login requires OTP verification and is not restricted by 8:00 PM auto-logout")
    void test10_SuperAdminRequiresOtpAndNotRestrictedBy8Pm() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);

        // 10A: Morning 10:00 AM IST
        ZonedDateTime morningTime = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(morningTime.toInstant(), ZONE_IST));

        MvcResult adminLogin1 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testAdmin.getEmail())
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp"))
                .andReturn();

        MockHttpSession session1 = (MockHttpSession) adminLogin1.getRequest().getSession(false);
        assertNotNull(session1);
        assertEquals(false, session1.getAttribute("OTP_VERIFIED"));

        // Before OTP verification, access to /admin/dashboard redirects to /admin/otp
        mockMvc.perform(get("/admin/dashboard").session(session1))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp"));

        // Verify OTP
        session1.setAttribute("OTP_VERIFIED", true);
        mockMvc.perform(get("/admin/dashboard").session(session1))
                .andExpect(status().isOk());

        // 10B: Night 21:30 IST (9:30 PM, after 8:00 PM) - Super Admin can log in with OTP (no 8 PM auto-logout)
        ZonedDateTime nightTime = today.atTime(21, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(nightTime.toInstant(), ZONE_IST));

        MvcResult adminLogin2 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testAdmin.getEmail())
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp"))
                .andReturn();

        MockHttpSession session2 = (MockHttpSession) adminLogin2.getRequest().getSession(false);
        assertNotNull(session2);

        // Before OTP verification, direct URL access is blocked
        mockMvc.perform(get("/admin/dashboard").session(session2))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp"));

        // Verify OTP - access allowed even at night (no 8:00 PM cutoff for Super Admin)
        session2.setAttribute("OTP_VERIFIED", true);
        mockMvc.perform(get("/admin/dashboard").session(session2))
                .andExpect(status().isOk());
    }
}
