package com.example.pravin_quotation;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class EmployeeLoginOtpLoadingTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private User testEmployee;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testAdmin = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setName("Super Admin Test");
                    admin.setEmail("admin-otp-test@pravin.com");
                    admin.setRole(Role.SUPER_ADMIN);
                    admin.setActive(true);
                    return admin;
                });
        testAdmin.setPassword(passwordEncoder.encode("Admin@123"));
        testAdmin.setActive(true);
        testAdmin = userRepository.save(testAdmin);

        testEmployee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && Boolean.TRUE.equals(u.getActive()))
                .findFirst()
                .orElseGet(() -> {
                    User emp = new User();
                    emp.setName("Anish Employee");
                    emp.setEmail("anish-test@gmail.com");
                    emp.setRole(Role.EMPLOYEE);
                    emp.setActive(true);
                    return emp;
                });
        testEmployee.setPassword(passwordEncoder.encode("Emp@123"));
        testEmployee.setActive(true);
        testEmployee = userRepository.save(testEmployee);
    }

    @Test
    @DisplayName("Verify /public/auth/check-role returns requiresOtp=true for employee")
    void testCheckRole_EmployeeRequiresOtp() throws Exception {
        mockMvc.perform(get("/public/auth/check-role")
                        .param("email", testEmployee.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.requiresOtp").value(true));
    }

    @Test
    @DisplayName("Verify /public/auth/check-role returns requiresOtp=true for Super Admin")
    void testCheckRole_SuperAdminRequiresOtp() throws Exception {
        mockMvc.perform(get("/public/auth/check-role")
                        .param("email", testAdmin.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.requiresOtp").value(true));
    }

    @Test
    @DisplayName("Verify /public/auth/check-role returns requiresOtp=false for unknown or empty email")
    void testCheckRole_UnknownEmail() throws Exception {
        mockMvc.perform(get("/public/auth/check-role")
                        .param("email", "unknown-user@doesnotexist.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.requiresOtp").value(false));

        mockMvc.perform(get("/public/auth/check-role")
                        .param("email", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.requiresOtp").value(false));
    }

    @Test
    @DisplayName("Verify login page contains spinner element, button structure, and dynamicAlert")
    void testLoginPageContainsLoadingIndicatorElements() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("loginBtnSpinner")))
                .andExpect(content().string(containsString("btn-spinner")))
                .andExpect(content().string(containsString("loginBtnText")))
                .andExpect(content().string(containsString("dynamicAlert")))
                .andExpect(content().string(containsString("Sending OTP...")))
                .andExpect(content().string(containsString("Unable to send OTP. Please try again.")));
    }
}

