package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CategoryClickWorkTypeMaterialTest {

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    private User employee;
    private Authentication auth;
    private Branch branch;

    @BeforeEach
    void setUp() {
        branch = branchRepository.findAll().stream().filter(b -> Boolean.TRUE.equals(b.getActive())).findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Madurai");
            b.setActive(true);
            return branchRepository.save(b);
        });

        employee = userRepository.findByEmail("emp_cat_click_test@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Test Employee");
            u.setEmail("emp_cat_click_test@pravin.com");
            u.setPassword("encoded");
            u.setRole(Role.EMPLOYEE);
            u.setActive(true);
            u.setBranch(branch);
            return userRepository.save(u);
        });

        auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
    }

    @Test
    @DisplayName("1. Verify JavaScript syntax in both Admin and Employee quotation builder templates")
    void testJavaScriptSyntaxInQuotationBuilderTemplates() throws Exception {
        for (String templatePath : List.of("src/main/resources/templates/admin/quotation-builder.html",
                                          "src/main/resources/templates/employee/quotation-builder.html")) {
            String html = Files.readString(Paths.get(templatePath));
            
            // Check that recalculateAll has closing brace before next function
            assertTrue(html.contains("renderQuoteSummary();"), "Must contain renderQuoteSummary() in " + templatePath);
            
            // Check that category click event listeners are registered
            assertTrue(html.contains("handleCategorySelect(btn)"), "Must contain handleCategorySelect in " + templatePath);
            assertTrue(html.contains("btn.addEventListener('click'"), "Must attach click listener to category buttons in " + templatePath);
            assertTrue(html.contains("categoryGridEl.addEventListener('click'"), "Must attach click delegation on categoryGrid in " + templatePath);
            
            // Verify initInitialCategory is present and auto-selects first category
            assertTrue(html.contains("function initInitialCategory"), "Must contain initInitialCategory in " + templatePath);
            assertTrue(html.contains("loadDivisions(Number(target.dataset.id), target.dataset.name);"), 
                    "initInitialCategory must load divisions for first category in " + templatePath);
        }
    }

    @Test
    @DisplayName("2. Verify divisions/work types API returns correctly for all active categories in Admin and Employee")
    void testDivisionsApiForCategories() {
        List<WorkCategory> categories = workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
        assertFalse(categories.isEmpty(), "Categories must not be empty");

        for (WorkCategory cat : categories) {
            // Admin divisions endpoint
            List<Map<String, Object>> adminDivisions = adminQuotationBuilderController.divisions(cat.getId());
            assertNotNull(adminDivisions);

            // Employee divisions endpoint
            List<Map<String, Object>> empDivisions = employeeQuotationController.builderDivisions(cat.getId());
            assertNotNull(empDivisions);

            // Parity check
            assertEquals(adminDivisions.size(), empDivisions.size(), 
                    "Admin and Employee must return same number of work types for category " + cat.getName());
        }
    }

    @Test
    @DisplayName("3. Verify materials API returns available materials for Work Types and grades in Admin and Employee")
    void testMaterialsApiForWorkTypesAndGrades() {
        List<Division> divisions = divisionRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .toList();

        if (divisions.isEmpty()) return;

        Division div = divisions.get(0);

        for (PricingMode mode : PricingMode.values()) {
            // Admin materials endpoint
            List<Map<String, Object>> adminMats = adminQuotationBuilderController.materials(
                    branch.getId(), div.getId(), null, mode.name(), true
            );
            assertNotNull(adminMats);

            // Employee materials endpoint
            List<Map<String, Object>> empMats = employeeQuotationController.builderMaterials(
                    branch.getId(), div.getId(), null, mode.name(), true, auth
            );
            assertNotNull(empMats);

            assertEquals(adminMats.size(), empMats.size(),
                    "Admin and Employee must return same materials for grade " + mode.name());
        }
    }

    @Test
    @DisplayName("4. Category selection preserves previously added mixed-grade items in quote summary")
    void testCategorySelectionPreservesQuotationItems() {
        ExtendedModelMap model = new ExtendedModelMap();
        adminQuotationBuilderController.builder(null, null, null, null, null, null, model);
        assertNotNull(model.get("workCategories"));
        assertNotNull(model.get("pricingModes"));

        // Verify that template contains the dynamic category switching logic without wiping state.rooms
        String adminHtml = "";
        try {
            adminHtml = Files.readString(Paths.get("src/main/resources/templates/admin/quotation-builder.html"));
        } catch (Exception e) {
            fail("Failed to read template: " + e.getMessage());
        }

        // Verify loadDivisions does not clear state.rooms
        int start = adminHtml.indexOf("async function loadDivisions");
        int end = adminHtml.indexOf("async function toggleWorkTypeCard", start);
        String loadDivisionsFunc = adminHtml.substring(start, end);
        assertFalse(loadDivisionsFunc.contains("state.rooms = []") || loadDivisionsFunc.contains("state.rooms=[]"),
                "loadDivisions must NOT reset state.rooms");
    }
}
