package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.EmployeeSessionService;
import com.example.pravin_quotation.security.OtpService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
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

        // Reset clock to normal IST working hours: today at 10:00 AM IST
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime morningTime = today.atTime(10, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(morningTime.toInstant(), ZONE_IST));

        // Create or get active Super Admin
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

        // Create or get active Employee
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

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        employeeSessionService.setClock(Clock.system(ZONE_IST));
    }

    @Test
    @DisplayName("Acceptance Test 1: Employee clicks Logout → immediate logout, session invalidated, redirected to login")
    void testManualLogoutImmediatelyInvalidatesSession() throws Exception {
        // Step 1: Employee logs in with credentials
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Session must be created on login");

        // Step 2: Employee completes Admin OTP verification
        session.setAttribute("OTP_CODE", "654321");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));
        session.setAttribute("OTP_ATTEMPTS", 0);

        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        assertTrue(Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED")), "OTP must be verified");

        // Step 3: Verified employee can access protected page
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // Step 4: Employee clicks Logout (e.g. at 11:00 AM, does not wait until 8:00 PM)
        MvcResult logoutResult = mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andReturn();

        // Step 5: Session must be invalidated and cookie cleared
        assertTrue(session.isInvalid(), "Session must be invalidated on logout");

        // Step 6: GET /logout also logs out immediately
        MockHttpSession anotherSession = new MockHttpSession();
        mockMvc.perform(get("/logout").session(anotherSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
        assertTrue(anotherSession.isInvalid());
    }

    @Test
    @DisplayName("Acceptance Test 2: Automatic Logout at exactly 8:00 PM IST on login day")
    void testAutomaticLogoutAt8PmIst() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);

        // Step 1: Log in at 9:00 AM IST
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
        assertNotNull(session);
        session.setAttribute("OTP_VERIFIED", true);

        // Step 2: Employee active at 19:59:59 IST (before 8:00 PM) -> Allowed
        ZonedDateTime time759Pm = today.atTime(19, 59, 59).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time759Pm.toInstant(), ZONE_IST));

        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());

        // Verify remaining time on session-status API
        mockMvc.perform(get("/employee/session-status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.authenticated").value(true));

        // Step 3: Clock strikes exactly 8:00:00 PM IST (20:00:00)
        ZonedDateTime time800Pm = today.atTime(20, 0, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(time800Pm.toInstant(), ZONE_IST));

        // Employee leaves application open or makes any request -> automatically rejected & redirected
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));

        // Backend must invalidate session
        assertTrue(session.isInvalid(), "Session must be invalidated when 8:00 PM IST is reached");

        // Subsequent session-status check returns expired
        mockMvc.perform(get("/employee/session-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true));
    }

    @Test
    @DisplayName("Acceptance Test 3: Access denied and redirected to login when accessing protected pages after logout")
    void testProtectedPagesDeniedAfterLogout() throws Exception {
        // Accessing protected pages without active authenticated session
        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));

        mockMvc.perform(get("/employee/quotations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));

        mockMvc.perform(get("/employee/customers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
    }

    @Test
    @DisplayName("Acceptance Test 4: Admin OTP verification is mandatory on every new employee login")
    void testAdminOtpmandatoryOnNewLogin() throws Exception {
        // Employee logs in with credentials
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(Boolean.FALSE, session.getAttribute("OTP_VERIFIED"), "New login must have OTP_VERIFIED = false");

        // Attempt to bypass OTP by navigating directly to /employee/dashboard
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));

        // Attempt to bypass OTP by navigating directly to /employee/quotations
        mockMvc.perform(get("/employee/quotations").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp"));

        // Verify with invalid OTP -> rejected
        session.setAttribute("OTP_CODE", "999888");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));
        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/otp?invalid"));

        // Verify with valid OTP -> accepted
        mockMvc.perform(post("/employee/otp/verify")
                        .session(session)
                        .with(csrf())
                        .param("otp", "999888"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));

        assertEquals(Boolean.TRUE, session.getAttribute("OTP_VERIFIED"));

        // Now dashboard is accessible
        mockMvc.perform(get("/employee/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Acceptance Test 5: Reusing old session or token is rejected")
    void testOldSessionReuseRejected() throws Exception {
        MockHttpSession oldSession = new MockHttpSession();
        oldSession.invalidate();

        mockMvc.perform(get("/employee/dashboard")
                        .session(oldSession)
                        .cookie(new Cookie("JSESSIONID", oldSession.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
    }

    @Test
    @DisplayName("Acceptance Test 6: Admin authentication continues working as before without OTP or 8:00 PM cutoff")
    void testAdminAuthenticationUnaffected() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);

        // Set time to 21:30 IST (9:30 PM, past employee cutoff)
        ZonedDateTime nightTime = today.atTime(21, 30, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(nightTime.toInstant(), ZONE_IST));

        // Admin logs in
        MvcResult adminLogin = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testAdmin.getEmail())
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard")) // Direct to dashboard, no OTP needed
                .andReturn();

        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        assertNotNull(adminSession);

        // Admin can access admin dashboard at 9:30 PM IST without restriction
        mockMvc.perform(get("/admin/dashboard").session(adminSession))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Employee login attempt after 8:00 PM IST is immediately rejected and redirected to login?expired")
    void testEmployeeLoginAfter8PmRejected() throws Exception {
        LocalDate today = LocalDate.now(ZONE_IST);
        ZonedDateTime after8Pm = today.atTime(20, 15, 0).atZone(ZONE_IST);
        employeeSessionService.setClock(Clock.fixed(after8Pm.toInstant(), ZONE_IST));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", testEmployee.getEmail())
                        .param("password", "emp123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"));
    }
}
