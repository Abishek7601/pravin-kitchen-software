package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WallpaperMaterialDuplicationTest {

    @Autowired
    private AdminQuotationBuilderController adminBuilderController;

    @Autowired
    private EmployeeQuotationController employeeBuilderController;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    @DisplayName("Admin Quotation Creation: Under Wallpapers (division 7300) in Middle grade, ONLY Fixed Wallpaper - Roll (ID 8215) appears")
    public void testAdminBuilderWallpaperMiddleGradeNoDuplicates() {
        // Kanniyakumari branch = 4, Wallpapers division = 7300, grade = MIDDLE, onlyAvailable = true
        List<Map<String, Object>> materials = adminBuilderController.materials(4L, 7300L, null, "MIDDLE", true);

        assertNotNull(materials);
        assertEquals(1, materials.size(), "Should return exactly ONE material for Middle grade under Wallpapers, not duplicates");

        Map<String, Object> mat = materials.get(0);
        assertEquals(8215L, ((Number) mat.get("id")).longValue(), "Should be material ID 8215 configured for Middle grade");
        assertEquals("FIXED WALLPAPER", mat.get("name"));
        assertEquals("ROLL", mat.get("measurementType"));
        assertEquals("Roll", mat.get("unit"));
        assertEquals(new BigDecimal("40.00"), mat.get("coveragePerRoll"));
        assertEquals(0, new BigDecimal("8000.00").compareTo((BigDecimal) mat.get("rate")));
        assertEquals(true, mat.get("available"));
    }

    @Test
    @Transactional
    @DisplayName("Employee Quotation Creation: Under Wallpapers (division 7300) in Middle grade, ONLY Fixed Wallpaper - Roll appears")
    public void testEmployeeBuilderWallpaperMiddleGradeNoDuplicates() {
        User employee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Test Employee");
                    u.setEmail("test_emp_wp@example.com");
                    u.setPassword("password");
                    u.setRole(Role.EMPLOYEE);
                    u.setActive(true);
                    return userRepository.save(u);
                });

        Authentication auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(), "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        List<Map<String, Object>> materials = employeeBuilderController.builderMaterials(
                4L, 7300L, null, "MIDDLE", true, auth
        );

        assertNotNull(materials);
        assertEquals(1, materials.size(), "Employee builder must also return exactly ONE material for Middle grade, no duplicates");

        Map<String, Object> mat = materials.get(0);
        assertEquals(8215L, ((Number) mat.get("id")).longValue());
        assertEquals("FIXED WALLPAPER", mat.get("name"));
        assertEquals("ROLL", mat.get("measurementType"));
        assertEquals("Roll", mat.get("unit"));
        assertEquals(0, new BigDecimal("8000.00").compareTo((BigDecimal) mat.get("rate")));
    }

    @Test
    @Transactional
    @DisplayName("Grade separation: Premium returns ID 6429, Economy returns ID 8216")
    public void testGradeSeparation() {
        // Premium
        List<Map<String, Object>> premMats = adminBuilderController.materials(4L, 7300L, null, "PREMIUM", true);
        assertEquals(1, premMats.size());
        assertEquals(6429L, ((Number) premMats.get(0).get("id")).longValue());

        // Economy
        List<Map<String, Object>> econMats = adminBuilderController.materials(4L, 7300L, null, "ECONOMY", true);
        assertEquals(1, econMats.size());
        assertEquals(8216L, ((Number) econMats.get(0).get("id")).longValue());
    }
}

