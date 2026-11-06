package com.example.pravin_quotation;

import com.example.pravin_quotation.config.DataInitializer;
import com.example.pravin_quotation.controller.AdminPasswordController;
import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.AdminPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminChangePasswordTest {

    @Autowired
    private AdminPasswordService adminPasswordService;

    @Autowired
    private AdminPasswordController adminPasswordController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private User admin;
    private Authentication adminAuth;
    private final String initialPassword = "Admin@123";
    private final String adminEmail = "abisheksr680@gmail.com";

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User u = new User();
            u.setName("Super Admin");
            u.setEmail(adminEmail);
            u.setPassword(passwordEncoder.encode(initialPassword));
            u.setRole(Role.SUPER_ADMIN);
            u.setActive(true);
            return userRepository.save(u);
        });

        // Ensure clean initial password
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setActive(true);
        admin = userRepository.save(admin);

        adminAuth = new UsernamePasswordAuthenticationToken(
                adminEmail,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
    }

    @Test
    void testSuccessfulPasswordChange() {
        long countBefore = userRepository.count();
        String newPassword = "NewAdminPassword@2026";

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = adminPasswordController.processChangePassword(
                initialPassword,
                newPassword,
                newPassword,
                adminAuth,
                redirectAttributes
        );

        assertEquals("redirect:/admin/change-password", view);
        assertNotNull(redirectAttributes.getFlashAttributes().get("successMessage"));
        assertNull(redirectAttributes.getFlashAttributes().get("errorMessage"));

        // Verify database state
        User updatedAdmin = userRepository.findByEmail(adminEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, updatedAdmin.getPassword()), "New password must match the stored hash");
        assertFalse(passwordEncoder.matches(initialPassword, updatedAdmin.getPassword()), "Old password must no longer match");

        // Verify no duplicate account was created
        assertEquals(countBefore, userRepository.count(), "No duplicate accounts must be created");
    }

    @Test
    void testIncorrectCurrentPasswordRejection() {
        String wrongPassword = "WrongPassword@999";
        String newPassword = "NewValidPassword@2026";

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = adminPasswordController.processChangePassword(
                wrongPassword,
                newPassword,
                newPassword,
                adminAuth,
                redirectAttributes
        );

        assertEquals("redirect:/admin/change-password", view);
        assertNotNull(redirectAttributes.getFlashAttributes().get("errorMessage"));
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("Current password is incorrect"));

        // Verify password in DB was not changed
        User currentAdmin = userRepository.findByEmail(adminEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(initialPassword, currentAdmin.getPassword()), "Original password must be retained");
        assertFalse(passwordEncoder.matches(newPassword, currentAdmin.getPassword()));
    }

    @Test
    void testMismatchedNewPasswordRejection() {
        String newPassword = "NewValidPassword@2026";
        String mismatchConfirm = "DifferentPassword@2026";

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = adminPasswordController.processChangePassword(
                initialPassword,
                newPassword,
                mismatchConfirm,
                adminAuth,
                redirectAttributes
        );

        assertEquals("redirect:/admin/change-password", view);
        assertNotNull(redirectAttributes.getFlashAttributes().get("errorMessage"));
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("do not match"));

        // Verify password in DB was not changed
        User currentAdmin = userRepository.findByEmail(adminEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(initialPassword, currentAdmin.getPassword()));
    }

    @Test
    void testSameAsCurrentPasswordRejection() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = adminPasswordController.processChangePassword(
                initialPassword,
                initialPassword,
                initialPassword,
                adminAuth,
                redirectAttributes
        );

        assertEquals("redirect:/admin/change-password", view);
        assertNotNull(redirectAttributes.getFlashAttributes().get("errorMessage"));
        assertTrue(redirectAttributes.getFlashAttributes().get("errorMessage").toString().contains("different from your current password"));
    }

    @Test
    void testPasswordPolicyValidation() {
        // Too short (< 8 chars)
        RedirectAttributesModelMap ra1 = new RedirectAttributesModelMap();
        adminPasswordController.processChangePassword(initialPassword, "Short1", "Short1", adminAuth, ra1);
        assertNotNull(ra1.getFlashAttributes().get("errorMessage"));
        assertTrue(ra1.getFlashAttributes().get("errorMessage").toString().contains("at least 8 characters"));

        // Letters only, no numbers
        RedirectAttributesModelMap ra2 = new RedirectAttributesModelMap();
        adminPasswordController.processChangePassword(initialPassword, "LongLettersOnlyPassword", "LongLettersOnlyPassword", adminAuth, ra2);
        assertNotNull(ra2.getFlashAttributes().get("errorMessage"));
        assertTrue(ra2.getFlashAttributes().get("errorMessage").toString().contains("contain both letters and numbers"));

        // Digits only, no letters
        RedirectAttributesModelMap ra3 = new RedirectAttributesModelMap();
        adminPasswordController.processChangePassword(initialPassword, "1234567890", "1234567890", adminAuth, ra3);
        assertNotNull(ra3.getFlashAttributes().get("errorMessage"));
        assertTrue(ra3.getFlashAttributes().get("errorMessage").toString().contains("contain both letters and numbers"));
    }

    @Test
    void testAuthenticationWithNewPasswordAndOldPasswordFailure() {
        String newPassword = "UpdatedSecurePassword@2026";

        // Change password
        adminPasswordService.changePassword(adminEmail, initialPassword, newPassword, newPassword);

        // Fetch UserDetails via Spring Security UserDetailsService
        var userDetails = userDetailsService.loadUserByUsername(adminEmail);
        assertNotNull(userDetails);

        // Verify new password authenticates
        assertTrue(passwordEncoder.matches(newPassword, userDetails.getPassword()),
                "New password must successfully authenticate against UserDetails");

        // Verify old password fails authentication
        assertFalse(passwordEncoder.matches(initialPassword, userDetails.getPassword()),
                "Old password must no longer authenticate against UserDetails");
    }

    @Test
    void testDataInitializerDoesNotResetPasswordOnRestart() throws Exception {
        String newPassword = "ChangedAfterRestart@2026";

        // 1. Admin changes password
        adminPasswordService.changePassword(adminEmail, initialPassword, newPassword, newPassword);

        // 2. Verify new password is set
        User adminBeforeRestart = userRepository.findByEmail(adminEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, adminBeforeRestart.getPassword()));

        long countBefore = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .count();

        // 3. Simulate application restart: DataInitializer CommandLineRunner executes
        CommandLineRunner runner = dataInitializer.createSuperAdmin(userRepository, passwordEncoder);
        runner.run();

        // 4. Verify password was NOT overwritten or reset back to Admin@123
        User adminAfterRestart = userRepository.findByEmail(adminEmail).orElseThrow();
        assertTrue(passwordEncoder.matches(newPassword, adminAfterRestart.getPassword()),
                "Password must remain the newly changed password after restart");
        assertFalse(passwordEncoder.matches(initialPassword, adminAfterRestart.getPassword()),
                "Old default password must not be restored after restart");

        // 5. Verify no duplicate admin was created
        long countAfter = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .count();
        assertEquals(countBefore, countAfter, "DataInitializer must not create a duplicate admin on restart");
        long emailMatchCount = userRepository.findAll().stream()
                .filter(u -> adminEmail.equalsIgnoreCase(u.getEmail()))
                .count();
        assertEquals(1, emailMatchCount, "Exactly one account must exist for the admin email");
    }

    @Test
    void testChangePasswordTemplateContents() throws IOException {
        File file = new File("src/main/resources/templates/admin/change-password.html");
        assertTrue(file.exists(), "change-password.html must exist");
        String content = Files.readString(file.toPath());

        assertTrue(content.contains("name=\"currentPassword\""), "Must contain currentPassword input");
        assertTrue(content.contains("name=\"newPassword\""), "Must contain newPassword input");
        assertTrue(content.contains("name=\"confirmPassword\""), "Must contain confirmPassword input");
        assertTrue(content.contains("th:action=\"@{/admin/change-password}\""), "Must submit to /admin/change-password");
        assertTrue(content.contains("method=\"post\""), "Must submit via POST");
        assertTrue(content.contains("_csrf"), "Must include CSRF protection");
    }

    @Test
    void testNavigationContainsChangePasswordLinks() throws IOException {
        File navFile = new File("src/main/resources/templates/fragments/admin-navigation.html");
        String content = Files.readString(navFile.toPath());

        assertTrue(content.contains("/admin/change-password"), "admin-navigation.html must link to /admin/change-password");
    }

    @Test
    void testShowChangePasswordPage() {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminPasswordController.showChangePasswordPage(adminAuth, model);
        assertEquals("admin/change-password", view);
        assertEquals(adminEmail, model.get("adminEmail"));
    }
}
