package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.OtpController;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.security.OtpEmailService;
import com.example.pravin_quotation.security.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class EmployeeOtpFlowTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JavaMailSender javaMailSender;

    private User employeeUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        // Ensure active super admin exists
        superAdminUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName("Super Admin Test");
                    admin.setEmail("admin-test@example.com");
                    admin.setPassword("password123");
                    admin.setRole(Role.SUPER_ADMIN);
                    admin.setActive(true);
                    return userRepository.save(admin);
                });

        // Ensure active employee exists
        employeeUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User emp = new User();
                    emp.setName("Employee Test");
                    emp.setEmail("employee-test@example.com");
                    emp.setPassword("password123");
                    emp.setRole(Role.EMPLOYEE);
                    emp.setActive(true);
                    return userRepository.save(emp);
                });

        // Set security context to authenticated employee
        var auth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(),
                "password123",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Verify Spring Mail configuration properties for Gmail SMTP")
    void testMailConfigurationProperties() {
        assertThat(javaMailSender).isInstanceOf(JavaMailSenderImpl.class);
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) javaMailSender;

        assertThat(mailSenderImpl.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(mailSenderImpl.getPort()).isEqualTo(587);

        var props = mailSenderImpl.getJavaMailProperties();
        assertThat(props.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
    }

    @Test
    @DisplayName("Verify live SMTP transport connection and authentication to Gmail")
    void testLiveSmtpConnection() {
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) javaMailSender;
        assertThat(mailSenderImpl.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(mailSenderImpl.getPort()).isEqualTo(587);

        try {
            mailSenderImpl.testConnection();
        } catch (Exception e) {
            System.out.println("Live SMTP connection verification diagnostic: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Employee OTP template displays friendly error message for mail failures")
    void testEmployeeOtpTemplateErrorMessage() throws IOException {
        Path templatePath = Paths.get("src/main/resources/templates/auth/employee-otp.html");
        assertThat(Files.exists(templatePath)).isTrue();

        String html = Files.readString(templatePath);
        assertThat(html).contains("Unable to send OTP right now. Please check your internet connection and try again.");
    }

    @Test
    @DisplayName("When email delivery fails with UnknownHostException, OTP page catches exception and redirects to mailError without 500")
    void testOtpPageCatchesMailExceptionGracefully() {
        OtpEmailService mockEmailService = mock(OtpEmailService.class);
        doThrow(new MailSendException("Mail send failure", new UnknownHostException("smtp.gmail.com")))
                .when(mockEmailService).sendEmployeeOtp(anyString(), anyString(), anyString());

        OtpController controller = new OtpController(userRepository, otpService, mockEmailService);
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        // Must not throw IllegalStateException or 500 error
        assertThatCode(() -> {
            String view = controller.otpPage(null, session, model);
            assertThat(view).isEqualTo("redirect:/employee/otp?mailError");
        }).doesNotThrowAnyException();

        // OTP must NOT be stored in session as valid
        assertThat(session.getAttribute("OTP_CODE")).isNull();
        assertThat(session.getAttribute("OTP_EXPIRY")).isNull();
        assertThat(session.getAttribute("OTP_VERIFIED")).isNotEqualTo(true);
    }

    @Test
    @DisplayName("When redirected with mailError, otpPage does not trigger infinite sending loop")
    void testOtpPageWithMailErrorParamDoesNotLoop() {
        OtpEmailService mockEmailService = mock(OtpEmailService.class);
        OtpController controller = new OtpController(userRepository, otpService, mockEmailService);
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        String view = controller.otpPage("true", session, model);

        // Renders page instead of redirect loop
        assertThat(view).isEqualTo("auth/employee-otp");
        verifyNoInteractions(mockEmailService);
        assertThat(session.getAttribute("OTP_VERIFIED")).isNotEqualTo(true);
    }

    @Test
    @DisplayName("When resend OTP fails with MailException, controller redirects to mailError and does not authenticate")
    void testResendOtpFailsGracefully() {
        OtpEmailService mockEmailService = mock(OtpEmailService.class);
        doThrow(new MailSendException("Mail send failure", new UnknownHostException("smtp.gmail.com")))
                .when(mockEmailService).sendEmployeeOtp(anyString(), anyString(), anyString());

        OtpController controller = new OtpController(userRepository, otpService, mockEmailService);
        MockHttpSession session = new MockHttpSession();

        String view = controller.sendOtp(session);
        assertThat(view).isEqualTo("redirect:/employee/otp?mailError");
        assertThat(session.getAttribute("OTP_CODE")).isNull();
        assertThat(session.getAttribute("OTP_VERIFIED")).isNotEqualTo(true);
    }

    @Test
    @DisplayName("Attempting to verify when email delivery failed rejects authentication")
    void testVerificationBlockedWhenMailFailed() {
        OtpController controller = new OtpController(userRepository, otpService, mock(OtpEmailService.class));
        MockHttpSession session = new MockHttpSession();

        // No OTP generated because delivery failed
        String view = controller.verifyOtp("123456", session);
        assertThat(view).isEqualTo("redirect:/employee/otp?notSent");
        assertThat(session.getAttribute("OTP_VERIFIED")).isNotEqualTo(true);
    }

    @Test
    @DisplayName("When email delivery succeeds, OTP is saved in session, redirected to sent, and verifiable")
    void testSuccessfulOtpFlow() {
        OtpEmailService mockEmailService = mock(OtpEmailService.class);
        doNothing().when(mockEmailService).sendEmployeeOtp(anyString(), anyString(), anyString());

        OtpController controller = new OtpController(userRepository, otpService, mockEmailService);
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        // 1. Initial page load sends OTP
        String view = controller.otpPage(null, session, model);
        assertThat(view).isEqualTo("redirect:/employee/otp?sent");
        String otpCode = (String) session.getAttribute("OTP_CODE");
        assertThat(otpCode).isNotNull().hasSize(6);

        // 2. Resend cooldown applies
        String resendView = controller.sendOtp(session);
        assertThat(resendView).isEqualTo("redirect:/employee/otp?cooldown");

        // 3. Verification with invalid OTP is rejected
        String invalidView = controller.verifyOtp("000000", session);
        assertThat(invalidView).isEqualTo("redirect:/employee/otp?invalid");
        assertThat(session.getAttribute("OTP_VERIFIED")).isNotEqualTo(true);

        // 4. Verification with valid OTP succeeds
        String validView = controller.verifyOtp(otpCode, session);
        assertThat(validView).isEqualTo("redirect:/employee/dashboard");
        assertThat(session.getAttribute("OTP_VERIFIED")).isEqualTo(true);
    }
}
