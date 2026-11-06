package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminOtpController;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.EmployeeOtpFilter;
import com.example.pravin_quotation.security.EmployeeSessionService;
import com.example.pravin_quotation.security.OtpEmailService;
import com.example.pravin_quotation.security.OtpService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SuperAdminOtpFlowTest {

    private UserRepository userRepository;
    private OtpService otpService;
    private FakeOtpEmailService otpEmailService;
    private EmployeeSessionService employeeSessionService;
    private AdminOtpController adminOtpController;
    private EmployeeOtpFilter employeeOtpFilter;
    private MockMvc mockMvc;

    private User adminUser;
    private final Map<String, User> userDb = new HashMap<>();

    static class FakeOtpEmailService extends OtpEmailService {
        String lastEmail;
        String lastName;
        String lastOtp;
        int sendCount = 0;

        public FakeOtpEmailService() {
            super(null);
        }

        @Override
        public void sendSuperAdminOtp(String adminEmail, String adminName, String otp) {
            this.lastEmail = adminEmail;
            this.lastName = adminName;
            this.lastOtp = otp;
            this.sendCount++;
        }
    }

    @BeforeEach
    void setUp() {
        userDb.clear();

        userRepository = (UserRepository) java.lang.reflect.Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName())) {
                        String email = (String) args[0];
                        return Optional.ofNullable(userDb.get(email));
                    }
                    if ("findAll".equals(method.getName())) {
                        return new ArrayList<>(userDb.values());
                    }
                    return null;
                }
        );

        otpService = new OtpService();
        otpEmailService = new FakeOtpEmailService();
        employeeSessionService = new EmployeeSessionService();

        adminOtpController = new AdminOtpController(userRepository, otpService, otpEmailService);
        employeeOtpFilter = new EmployeeOtpFilter(employeeSessionService);

        mockMvc = MockMvcBuilders.standaloneSetup(adminOtpController).build();

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("Pravin Super Admin");
        adminUser.setEmail("admin@pravinkitchens.com");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);

        userDb.put(adminUser.getEmail(), adminUser);

        // Set security context to authenticated SUPER_ADMIN
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminUser.getEmail(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. GET /admin/otp automatically generates and sends OTP to Super Admin email on first visit")
    void testGetOtp_SendsOtpAndRedirectsToSent() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/admin/otp").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp?sent"));

        // Verify email service was called with the admin's email and a 6-digit OTP
        assertEquals(1, otpEmailService.sendCount);
        assertEquals("admin@pravinkitchens.com", otpEmailService.lastEmail);
        assertEquals("Pravin Super Admin", otpEmailService.lastName);

        String generatedOtp = otpEmailService.lastOtp;
        assertNotNull(generatedOtp);
        assertEquals(6, generatedOtp.length());

        // Verify session attributes
        assertEquals(generatedOtp, session.getAttribute("OTP_CODE"));
        assertNotNull(session.getAttribute("OTP_EXPIRY"));
        assertEquals(0, session.getAttribute("OTP_ATTEMPTS"));
        assertEquals(false, session.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("2. GET /admin/otp?sent displays the OTP verification template with admin details")
    void testGetOtpSent_RendersAdminOtpView() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "123456");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));
        session.setAttribute("OTP_ATTEMPTS", 0);
        session.setAttribute("OTP_VERIFIED", false);

        mockMvc.perform(get("/admin/otp").param("sent", "true").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/admin-otp"))
                .andExpect(model().attribute("adminName", "Pravin Super Admin"))
                .andExpect(model().attribute("adminEmail", "admin@pravinkitchens.com"))
                .andExpect(model().attribute("maxAttempts", 5));
    }

    @Test
    @DisplayName("3. GET /admin/otp redirects to /admin/dashboard when already verified")
    void testGetOtp_AlreadyVerifiedRedirectsToDashboard() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);

        mockMvc.perform(get("/admin/otp").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("4. POST /admin/otp/verify with correct OTP sets OTP_VERIFIED=true and redirects to dashboard")
    void testVerifyOtp_CorrectOtpSucceeds() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "852963");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));
        session.setAttribute("OTP_ATTEMPTS", 0);
        session.setAttribute("OTP_VERIFIED", false);

        mockMvc.perform(post("/admin/otp/verify").param("otp", "852963").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        assertTrue(Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED")));
        assertNull(session.getAttribute("OTP_CODE"));
        assertNull(session.getAttribute("OTP_EXPIRY"));
        assertNull(session.getAttribute("OTP_ATTEMPTS"));
    }

    @Test
    @DisplayName("5. POST /admin/otp/verify with incorrect OTP increments attempts and redirects to ?invalid")
    void testVerifyOtp_IncorrectOtpIncrementsAttempts() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "852963");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));
        session.setAttribute("OTP_ATTEMPTS", 1);
        session.setAttribute("OTP_VERIFIED", false);

        mockMvc.perform(post("/admin/otp/verify").param("otp", "111111").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp?invalid"));

        assertEquals(2, session.getAttribute("OTP_ATTEMPTS"));
        assertEquals(false, session.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("6. POST /admin/otp/verify when attempts exceeded redirects to ?attempts")
    void testVerifyOtp_MaxAttemptsExceeded() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "852963");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().plusMinutes(5));
        session.setAttribute("OTP_ATTEMPTS", 5);
        session.setAttribute("OTP_VERIFIED", false);

        mockMvc.perform(post("/admin/otp/verify").param("otp", "852963").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp?attempts"));

        assertEquals(false, session.getAttribute("OTP_VERIFIED"));
    }

    @Test
    @DisplayName("7. POST /admin/otp/verify when expired redirects to ?expired")
    void testVerifyOtp_ExpiredOtp() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "852963");
        session.setAttribute("OTP_EXPIRY", LocalDateTime.now().minusMinutes(1)); // Expired
        session.setAttribute("OTP_ATTEMPTS", 0);
        session.setAttribute("OTP_VERIFIED", false);

        mockMvc.perform(post("/admin/otp/verify").param("otp", "852963").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp?expired"));

        assertNull(session.getAttribute("OTP_CODE"));
        assertNull(session.getAttribute("OTP_EXPIRY"));
    }

    @Test
    @DisplayName("8. POST /admin/otp/send resends new OTP and invalidates previous OTP")
    void testResendOtp_GeneratesNewOtp() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "111111");
        session.setAttribute("OTP_LAST_SENT", LocalDateTime.now().minusSeconds(65)); // 65s ago (past 60s cooldown)
        session.setAttribute("OTP_ATTEMPTS", 3);

        mockMvc.perform(post("/admin/otp/send").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp?sent"));

        assertEquals(1, otpEmailService.sendCount);
        String newOtp = otpEmailService.lastOtp;
        assertNotEquals("111111", newOtp);
        assertEquals(newOtp, session.getAttribute("OTP_CODE"));
        assertEquals(0, session.getAttribute("OTP_ATTEMPTS"));
    }

    @Test
    @DisplayName("9. POST /admin/otp/send within 60-second cooldown is blocked with ?cooldown")
    void testResendOtp_CooldownEnforced() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_CODE", "111111");
        session.setAttribute("OTP_LAST_SENT", LocalDateTime.now().minusSeconds(20)); // Only 20s ago

        mockMvc.perform(post("/admin/otp/send").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/otp?cooldown"));

        assertEquals(0, otpEmailService.sendCount);
    }

    @Test
    @DisplayName("10. EmployeeOtpFilter protects /admin/dashboard until OTP is verified")
    void testFilter_BlocksDirectAdminAccessWithoutOtp() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", false);
        request.setSession(session);

        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> filterChainCalled.set(true);

        employeeOtpFilter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
        assertEquals("/admin/otp", response.getRedirectedUrl());
        assertFalse(filterChainCalled.get());
    }

    @Test
    @DisplayName("11. EmployeeOtpFilter allows access to /admin/dashboard when OTP is verified")
    void testFilter_AllowsAdminAccessWithOtpVerified() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);
        request.setSession(session);

        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> filterChainCalled.set(true);

        employeeOtpFilter.doFilter(request, response, chain);

        assertTrue(filterChainCalled.get());
    }

    @Test
    @DisplayName("12. EmployeeOtpFilter returns 401 JSON for AJAX requests when unverified")
    void testFilter_AjaxRequestReturns401WhenUnverified() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/materials");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", false);
        request.setSession(session);

        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> filterChainCalled.set(true);

        employeeOtpFilter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("OTP_REQUIRED"));
        assertFalse(filterChainCalled.get());
    }

    @Test
    @DisplayName("13. EmployeeOtpFilter redirects /admin/otp to /admin/dashboard when already verified")
    void testFilter_RedirectsOtpPageIfAlreadyVerified() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/otp");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);
        request.setSession(session);

        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> filterChainCalled.set(true);

        employeeOtpFilter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
        assertEquals("/admin/dashboard", response.getRedirectedUrl());
        assertFalse(filterChainCalled.get());
    }

    @Test
    @DisplayName("14. Super Admin is NOT subject to 8:00 PM auto-logout")
    void testFilter_SuperAdminNotSubjectTo8PmAutoLogout() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);
        request.setSession(session);

        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> filterChainCalled.set(true);

        employeeOtpFilter.doFilter(request, response, chain);

        // Chain continues because 8:00 PM rule only applies to ROLE_EMPLOYEE
        assertTrue(filterChainCalled.get());
        assertNotEquals("/login?expired", response.getRedirectedUrl());
    }
}
