package com.example.pravin_quotation;

import com.example.pravin_quotation.config.DataInitializer;
import com.example.pravin_quotation.controller.EmployeeForgotPasswordController;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.OtpEmailService;
import com.example.pravin_quotation.security.OtpService;
import com.example.pravin_quotation.service.EmployeeForgotPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class EmployeeForgotPasswordTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserDetailsService userDetailsService;

    private OtpEmailService mockOtpEmailService;
    private EmployeeForgotPasswordService forgotPasswordService;
    private EmployeeForgotPasswordController forgotPasswordController;

    private User employeeUser;
    private User superAdminUser;
    private final String initialEmployeePassword = "EmpOldPassword@123";
    private final String employeeEmail = "emp.test.forgot@pravin.com";
    private final String adminEmail = "admin.dynamic.test@pravin.com";

    @BeforeEach
    void setUp() {
        mockOtpEmailService = mock(OtpEmailService.class);

        // Ensure active Super Admin exists with dynamic adminEmail
        superAdminUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName("Super Admin Test");
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode("AdminPass@123"));
                    admin.setRole(Role.SUPER_ADMIN);
                    admin.setActive(true);
                    return userRepository.save(admin);
                });

        // Ensure active employee exists
        employeeUser = userRepository.findByEmail(employeeEmail).orElseGet(() -> {
            User emp = new User();
            emp.setName("Ravi Kumar");
            emp.setEmail(employeeEmail);
            emp.setPassword(passwordEncoder.encode(initialEmployeePassword));
            emp.setRole(Role.EMPLOYEE);
            emp.setActive(true);
            return userRepository.save(emp);
        });
        employeeUser.setPassword(passwordEncoder.encode(initialEmployeePassword));
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        forgotPasswordService = new EmployeeForgotPasswordService(
                userRepository,
                otpService,
                mockOtpEmailService,
                passwordEncoder
        );

        forgotPasswordController = new EmployeeForgotPasswordController(forgotPasswordService);
    }

    @Test
    @DisplayName("1. Initiate reset with valid numeric employee ID sends OTP to dynamic Admin email (never employee)")
    void testInitiateResetWithValidEmployeeId() {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, String.valueOf(employeeUser.getId()));

        ArgumentCaptor<String> adminEmailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockOtpEmailService, times(1)).sendPasswordResetOtp(
                adminEmailCaptor.capture(),
                eq(employeeUser.getName()),
                eq(String.valueOf(employeeUser.getId())),
                otpCaptor.capture()
        );

        // Admin email verification: must be sent to active Admin, not employee
        assertEquals(superAdminUser.getEmail(), adminEmailCaptor.getValue());
        assertNotEquals(employeeUser.getEmail(), adminEmailCaptor.getValue());

        // OTP must be 6 digits
        String capturedOtp = otpCaptor.getValue();
        assertNotNull(capturedOtp);
        assertEquals(6, capturedOtp.length());
        assertTrue(capturedOtp.matches("^\\d{6}$"));

        // Verify session attributes: OTP must NOT be stored in plaintext!
        assertEquals(employeeUser.getId(), session.getAttribute(EmployeeForgotPasswordService.SESSION_EMPLOYEE_ID));
        String storedHash = (String) session.getAttribute(EmployeeForgotPasswordService.SESSION_OTP_HASH);
        assertNotNull(storedHash);
        assertNotEquals(capturedOtp, storedHash, "OTP must never be stored in plaintext");
        assertTrue(passwordEncoder.matches(capturedOtp, storedHash), "Hashed OTP must match captured OTP");
        assertNotNull(session.getAttribute(EmployeeForgotPasswordService.SESSION_OTP_EXPIRY));
    }

    @Test
    @DisplayName("2. Initiate reset with valid employee email")
    void testInitiateResetWithValidEmail() {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        verify(mockOtpEmailService, times(1)).sendPasswordResetOtp(
                eq(superAdminUser.getEmail()),
                eq(employeeUser.getName()),
                eq(employeeUser.getEmail()),
                anyString()
        );
    }

    @Test
    @DisplayName("3. Reject invalid or non-existent Employee ID / email")
    void testInitiateResetWithInvalidEmployeeId() {
        MockHttpSession session = new MockHttpSession();

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.initiateReset(session, "999999999"));
        assertTrue(ex1.getMessage().contains("No employee account found"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.initiateReset(session, "unknown.user@pravin.com"));
        assertTrue(ex2.getMessage().contains("No employee account found"));

        verifyNoInteractions(mockOtpEmailService);
    }

    @Test
    @DisplayName("4. Reject reset attempt for Super Admin accounts through employee forgot password")
    void testInitiateResetWithSuperAdminAccountRejection() {
        MockHttpSession session = new MockHttpSession();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.initiateReset(session, superAdminUser.getEmail()));
        assertTrue(ex.getMessage().contains("only available for Employee accounts"));

        verifyNoInteractions(mockOtpEmailService);
    }

    @Test
    @DisplayName("5. Reject reset attempt for inactive employee account")
    void testInitiateResetWithInactiveEmployeeRejection() {
        employeeUser.setActive(false);
        userRepository.save(employeeUser);

        MockHttpSession session = new MockHttpSession();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.initiateReset(session, employeeUser.getEmail()));
        assertTrue(ex.getMessage().contains("inactive"));

        verifyNoInteractions(mockOtpEmailService);
    }

    @Test
    @DisplayName("6. Correct OTP verification successfully validates and invalidates the OTP immediately")
    void testCorrectOtpVerificationSuccess() {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockOtpEmailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), otpCaptor.capture());
        String generatedOtp = otpCaptor.getValue();

        // Verify with correct OTP
        assertDoesNotThrow(() -> forgotPasswordService.verifyOtp(session, generatedOtp));

        // OTP must be marked verified
        assertTrue(Boolean.TRUE.equals(session.getAttribute(EmployeeForgotPasswordService.SESSION_VERIFIED)));
        assertEquals(employeeUser.getId(), session.getAttribute(EmployeeForgotPasswordService.SESSION_VERIFIED_EMPLOYEE_ID));

        // OTP hash must be removed immediately to prevent replay
        assertNull(session.getAttribute(EmployeeForgotPasswordService.SESSION_OTP_HASH),
                "OTP hash must be wiped immediately upon verification");

        // Replay attempt must fail
        assertThrows(IllegalStateException.class, () -> forgotPasswordService.verifyOtp(session, generatedOtp));
    }

    @Test
    @DisplayName("7. Incorrect OTP rejection and attempt limit enforcement (max 5)")
    void testIncorrectOtpRejectionAndAttemptCounter() {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        // 4 failed attempts
        for (int i = 1; i <= 4; i++) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    forgotPasswordService.verifyOtp(session, "000000"));
            assertTrue(ex.getMessage().contains("attempt(s) remaining"));
        }

        // 5th failed attempt: reaches max attempts limit
        IllegalStateException limitEx = assertThrows(IllegalStateException.class, () ->
                forgotPasswordService.verifyOtp(session, "000000"));
        assertTrue(limitEx.getMessage().contains("Maximum verification attempts reached"));

        // Subsequent attempts blocked
        assertThrows(IllegalStateException.class, () ->
                forgotPasswordService.verifyOtp(session, "000000"));
    }

    @Test
    @DisplayName("8. Expired OTP rejection")
    void testExpiredOtpRejection() {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        // Simulate expired OTP (past 5 minutes)
        session.setAttribute(EmployeeForgotPasswordService.SESSION_OTP_EXPIRY, LocalDateTime.now().minusMinutes(1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                forgotPasswordService.verifyOtp(session, "123456"));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("9. Resend cooldown enforcement (60 seconds)")
    void testResendCooldownEnforcement() {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        // Immediate resend should be blocked by cooldown
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                forgotPasswordService.resendOtp(session));
        assertTrue(ex.getMessage().contains("wait"));

        // Simulate 61 seconds later
        session.setAttribute(EmployeeForgotPasswordService.SESSION_LAST_SENT, LocalDateTime.now().minusSeconds(65));
        assertDoesNotThrow(() -> forgotPasswordService.resendOtp(session));
        verify(mockOtpEmailService, times(2)).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("10. Reject setting new password without prior OTP verification (direct access prevention)")
    void testDirectAccessToResetPasswordWithoutVerificationRejection() {
        MockHttpSession session = new MockHttpSession();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                forgotPasswordService.resetPassword(session, "NewPass@123", "NewPass@123"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("11. Password confirmation mismatch rejection")
    void testPasswordMismatchRejection() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(EmployeeForgotPasswordService.SESSION_VERIFIED, true);
        session.setAttribute(EmployeeForgotPasswordService.SESSION_VERIFIED_EMPLOYEE_ID, employeeUser.getId());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.resetPassword(session, "Password@123", "Mismatch@456"));
        assertTrue(ex.getMessage().contains("do not match"));
    }

    @Test
    @DisplayName("12. Password policy rejection (length < 8, missing letter or digit)")
    void testPasswordPolicyRejection() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(EmployeeForgotPasswordService.SESSION_VERIFIED, true);
        session.setAttribute(EmployeeForgotPasswordService.SESSION_VERIFIED_EMPLOYEE_ID, employeeUser.getId());

        // Too short
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.resetPassword(session, "Pass@1", "Pass@1"));
        assertTrue(ex1.getMessage().contains("at least 8 characters"));

        // Missing digit
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.resetPassword(session, "PasswordOnly", "PasswordOnly"));
        assertTrue(ex2.getMessage().contains("one letter and one number"));

        // Missing letter
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                forgotPasswordService.resetPassword(session, "123456789", "123456789"));
        assertTrue(ex3.getMessage().contains("one letter and one number"));
    }

    @Test
    @DisplayName("13. Full successful password reset and login authentication")
    void testSuccessfulPasswordResetAndLoginVerification() {
        MockHttpSession session = new MockHttpSession();

        // 1. Initiate reset
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockOtpEmailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), otpCaptor.capture());
        String otp = otpCaptor.getValue();

        // 2. Verify OTP
        forgotPasswordService.verifyOtp(session, otp);

        // 3. Reset password
        String newPassword = "BrandNewEmployeePassword@2026";
        forgotPasswordService.resetPassword(session, newPassword, newPassword);

        // 4. Verify database state
        User updatedEmployee = userRepository.findByEmail(employeeEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, updatedEmployee.getPassword()),
                "New password must match stored hash");
        assertFalse(passwordEncoder.matches(initialEmployeePassword, updatedEmployee.getPassword()),
                "Old password must no longer match");

        // 5. Verify Spring Security UserDetails authentication with new password
        UserDetails userDetails = userDetailsService.loadUserByUsername(employeeEmail);
        assertTrue(passwordEncoder.matches(newPassword, userDetails.getPassword()),
                "New password must authenticate via Spring Security UserDetailsService");
        assertFalse(passwordEncoder.matches(initialEmployeePassword, userDetails.getPassword()),
                "Old password must fail authentication");

        // 6. Verify session reset state cleared
        assertFalse(forgotPasswordService.isResetVerified(session));
        assertFalse(forgotPasswordService.isResetInitiated(session));
    }

    @Test
    @DisplayName("14. Password remains valid after application restart (DataInitializer simulation)")
    void testPasswordPersistenceAcrossSimulatedRestart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        forgotPasswordService.initiateReset(session, employeeUser.getEmail());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockOtpEmailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), otpCaptor.capture());
        String otp = otpCaptor.getValue();

        forgotPasswordService.verifyOtp(session, otp);

        String newPassword = "PersistedEmpPassword@2026";
        forgotPasswordService.resetPassword(session, newPassword, newPassword);

        // Simulate application restart
        CommandLineRunner runner = dataInitializer.createSuperAdmin(userRepository, passwordEncoder);
        runner.run();

        // Check employee password after restart
        User employeeAfterRestart = userRepository.findByEmail(employeeEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, employeeAfterRestart.getPassword()),
                "Employee password must persist across application restart");
    }

    @Test
    @DisplayName("15. Controller navigation and flash message handling")
    void testControllerFlow() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Step 1: Initiate via controller with valid ID
        String view1 = forgotPasswordController.handleInitiateReset(
                String.valueOf(employeeUser.getId()), session, redirectAttributes);
        assertEquals("redirect:/employee/forgot-password/verify?sent", view1);

        // Step 2: GET verify page
        String viewVerify = forgotPasswordController.showVerifyOtpPage(session, new org.springframework.ui.ConcurrentModel(), redirectAttributes);
        assertEquals("auth/employee-reset-otp", viewVerify);

        // Step 3: Verify OTP via controller with invalid OTP
        RedirectAttributesModelMap redirectVerify = new RedirectAttributesModelMap();
        String viewInvalidOtp = forgotPasswordController.handleVerifyOtp("999999", session, redirectVerify);
        assertEquals("redirect:/employee/forgot-password/verify", viewInvalidOtp);
        assertNotNull(redirectVerify.getFlashAttributes().get("errorMessage"));

        // Step 4: GET reset page without verified OTP should redirect to forgot-password
        RedirectAttributesModelMap redirectReset = new RedirectAttributesModelMap();
        String viewResetUnauthorized = forgotPasswordController.showResetPasswordPage(session, redirectReset);
        assertEquals("redirect:/employee/forgot-password", viewResetUnauthorized);
    }

    @Test
    @DisplayName("16. Verify template files and login page Forgot Password link")
    void testTemplateFilesExist() throws IOException {
        File forgotPwdFile = new File("src/main/resources/templates/auth/employee-forgot-password.html");
        File otpVerifyFile = new File("src/main/resources/templates/auth/employee-reset-otp.html");
        File resetPwdFile = new File("src/main/resources/templates/auth/employee-reset-password.html");
        File loginFile = new File("src/main/resources/templates/auth/login.html");

        assertTrue(forgotPwdFile.exists(), "employee-forgot-password.html must exist");
        assertTrue(otpVerifyFile.exists(), "employee-reset-otp.html must exist");
        assertTrue(resetPwdFile.exists(), "employee-reset-password.html must exist");
        assertTrue(loginFile.exists(), "login.html must exist");

        String loginContent = Files.readString(loginFile.toPath());
        assertTrue(loginContent.contains("/employee/forgot-password"), "login.html must contain link to forgot password");
        assertTrue(loginContent.contains("resetSuccess"), "login.html must handle resetSuccess param");
    }
}
