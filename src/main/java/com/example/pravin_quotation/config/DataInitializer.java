package com.example.pravin_quotation.config;

import com.example.pravin_quotation.model.Role;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner createSuperAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String adminEmail = "admin@gmail.com";
            String adminPassword = "Admin@123";

            if (!userRepository.existsByEmail(adminEmail) && !userRepository.existsByRole(Role.SUPER_ADMIN)) {

                User admin = new User();

                admin.setName("Super Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(
                        passwordEncoder.encode(adminPassword)
                );
                admin.setRole(Role.SUPER_ADMIN);
                admin.setActive(true);

                userRepository.save(admin);

                System.out.println("==========================================");
                System.out.println("SUPER ADMIN CREATED");
                System.out.println("Email: " + adminEmail);
                System.out.println("Password: " + adminPassword);
                System.out.println("==========================================");
            }
        };
    }

    @Bean
    CommandLineRunner ensureSchemaCompatibility(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE materials MODIFY COLUMN item_id BIGINT NULL");
            } catch (Exception e) {
                // Table might not exist yet or already altered
            }
        };
    }
}