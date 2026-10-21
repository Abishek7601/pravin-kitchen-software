package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for securely updating the Admin user's password.
 */
@Service
public class AdminPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminPasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Changes the password for the given admin user after verifying current password,
     * validating confirmation, and enforcing the password policy.
     *
     * @param adminEmail the email of the authenticated admin
     * @param currentPassword the plain-text current password to verify
     * @param newPassword the plain-text new password
     * @param confirmPassword the confirmation of the new password
     */
    @Transactional
    public void changePassword(String adminEmail, String currentPassword, String newPassword, String confirmPassword) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalArgumentException("Admin account is not identified.");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user account not found."));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            throw new IllegalStateException("Only administrator accounts can change admin password here.");
        }

        // Validate Current Password
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }

        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        // Validate New Password & Confirmation
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("Please confirm your new password.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        if (passwordEncoder.matches(newPassword, admin.getPassword())) {
            throw new IllegalArgumentException("New password must be different from your current password.");
        }

        // Policy: at least 8 characters, containing both letters and numbers
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }

        boolean hasLetter = newPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("New password must contain both letters and numbers.");
        }

        // Securely hash and update the existing account without creating duplicate accounts
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }
}
