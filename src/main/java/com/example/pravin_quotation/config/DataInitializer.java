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

            String adminEmail = "pravinkitchensquotation@gmail.com";
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
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations MODIFY COLUMN customer_id BIGINT NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN last_active_step VARCHAR(50) NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN last_active_floor VARCHAR(100) NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN last_active_room VARCHAR(100) NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN last_active_category_id BIGINT NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN last_active_division_id BIGINT NULL");
            } catch (Exception ignored) {
            }
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD COLUMN builder_state LONGTEXT NULL");
            } catch (Exception ignored) {
            }

            // Reconcile any existing duplicate child quotations for (parent_id, pricing_mode) safely
            try {
                java.util.List<java.util.Map<String, Object>> duplicates = jdbcTemplate.queryForList(
                        "SELECT parent_id, pricing_mode, COUNT(*) as cnt FROM quotations " +
                        "WHERE parent_id IS NOT NULL AND pricing_mode IS NOT NULL " +
                        "GROUP BY parent_id, pricing_mode HAVING COUNT(*) > 1"
                );
                for (java.util.Map<String, Object> dup : duplicates) {
                    Long pId = ((Number) dup.get("parent_id")).longValue();
                    String mode = (String) dup.get("pricing_mode");
                    java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SELECT id, grand_total, quotation_number FROM quotations " +
                            "WHERE parent_id = ? AND pricing_mode = ? ORDER BY grand_total DESC, id DESC",
                            pId, mode
                    );
                    if (rows.size() > 1) {
                        Long keepId = ((Number) rows.get(0).get("id")).longValue();
                        for (int i = 1; i < rows.size(); i++) {
                            Long discardId = ((Number) rows.get(i).get("id")).longValue();
                            // Reassign any child rooms from discardId to keepId
                            jdbcTemplate.update("UPDATE quotation_rooms SET quotation_id = ? WHERE quotation_id = ?", keepId, discardId);
                            // Delete the empty/duplicate quotation record
                            jdbcTemplate.update("DELETE FROM quotations WHERE id = ?", discardId);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // Enforce database-level uniqueness for (parent_id, pricing_mode)
            try {
                jdbcTemplate.execute("ALTER TABLE quotations ADD CONSTRAINT uk_quotation_parent_grade UNIQUE (parent_id, pricing_mode)");
            } catch (Exception ignored) {
            }

            // Ensure child tables referencing quotations have ON DELETE CASCADE for foreign keys
            ensureCascadeForeignKey(jdbcTemplate, "quotation_communications", "quotation_id", "quotations", "id", "fk_quotation_comm_cascade");
            ensureCascadeForeignKey(jdbcTemplate, "quotation_status_history", "quotation_id", "quotations", "id", "fk_quotation_status_hist_cascade");
            ensureCascadeForeignKey(jdbcTemplate, "quotation_workflow", "quotation_id", "quotations", "id", "fk_quotation_workflow_cascade");
            ensureCascadeForeignKey(jdbcTemplate, "quotation_rooms", "quotation_id", "quotations", "id", "fk_quotation_rooms_cascade");
            ensureCascadeForeignKey(jdbcTemplate, "quotation_items", "quotation_room_id", "quotation_rooms", "id", "fk_quotation_items_cascade");
            ensureCascadeForeignKey(jdbcTemplate, "quotation_item_sizes", "quotation_item_id", "quotation_items", "id", "fk_quotation_item_sizes_cascade");
            ensureCascadeForeignKey(jdbcTemplate, "quotations", "parent_id", "quotations", "id", "fk_quotation_parent_cascade");
        };
    }

    private void ensureCascadeForeignKey(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            String childTable,
            String childColumn,
            String parentTable,
            String parentColumn,
            String preferredConstraintName
    ) {
        try {
            java.util.List<String> existingFks = jdbcTemplate.query(
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? " +
                    "AND REFERENCED_TABLE_NAME = ? AND REFERENCED_COLUMN_NAME = ?",
                    (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"),
                    childTable, childColumn, parentTable, parentColumn
            );
            if (existingFks != null) {
                for (String fk : existingFks) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE " + childTable + " DROP FOREIGN KEY " + fk);
                    } catch (Exception ignored) {}
                }
            }
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE " + childTable + " ADD CONSTRAINT " + preferredConstraintName +
                        " FOREIGN KEY (" + childColumn + ") REFERENCES " + parentTable + "(" + parentColumn + ") ON DELETE CASCADE"
                );
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }
}