package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MaterialUnitDropdownTest {

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private BranchRepository branchRepository;

    private Division division;
    private Branch branch;

    @BeforeEach
    void setUp() {
        WorkCategory cat = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory c = new WorkCategory();
            c.setName("Test Modular Kitchen");
            c.setActive(true);
            return workCategoryRepository.save(c);
        });

        division = divisionRepository.findAll().stream().findFirst().orElseGet(() -> {
            Division d = new Division();
            d.setName("Test Base Units");
            d.setWorkCategory(cat);
            d.setActive(true);
            return divisionRepository.save(d);
        });

        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Test Chennai Branch");
            b.setBranchAddress("Anna Salai");
            b.setActive(true);
            return branchRepository.save(b);
        });
    }

    @Test
    @DisplayName("1. Unit dropdown in work-categories.html contains ONLY Sq.ft and removes all other options")
    void testUnitDropdownContainsOnlySqFt() throws IOException {
        File file = new File("src/main/resources/templates/admin/work-categories.html");
        assertTrue(file.exists(), "work-categories.html must exist");
        String content = Files.readString(file.toPath());

        // Find the unit select block
        int selectIndex = content.indexOf("id=\"materialUnitSelect\"");
        assertTrue(selectIndex > 0, "id='materialUnitSelect' select element must exist");
        int endIndex = content.indexOf("</select>", selectIndex);
        String selectSnippet = content.substring(selectIndex, endIndex);

        // Ensure Sq.ft is present and selected
        assertTrue(selectSnippet.contains("Sq.ft"), "Dropdown must contain Sq.ft");

        // Ensure removed options are NOT in the dropdown source
        assertFalse(selectSnippet.contains("Running ft"), "Running ft must not be in dropdown");
        assertFalse(selectSnippet.contains("Piece"), "Piece must not be in dropdown");
        assertFalse(selectSnippet.contains("Set"), "Set must not be in dropdown");
        assertFalse(selectSnippet.contains("Meter"), "Meter must not be in dropdown");
        assertFalse(selectSnippet.contains("Item"), "Item must not be in dropdown");
    }

    @Test
    @DisplayName("2. Creating material uses Sq.ft and validates branch rates correctly")
    void testCreateMaterialWithSqFt() {
        String branchRatesJson = "{\"" + branch.getId() + "\":{\"MIDDLE\":1250.00}}";

        ResponseEntity<?> response = workCategoryController.createWorkTypeMaterial(
                division.getId(),
                "MIDDLE",
                null,
                "Marine Plywood 710",
                "L × W",
                null,
                "Grade A Century Ply",
                "Sq.ft",
                null,
                branchRatesJson,
                Map.of()
        );

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Creation should succeed");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        Long materialId = (Long) body.get("materialId");
        assertNotNull(materialId);

        Material mat = materialRepository.findById(materialId).orElseThrow();
        assertEquals("Sq.ft", mat.getUnit(), "Material unit must be Sq.ft");
    }

    @Test
    @DisplayName("3. Updating existing material displays and saves Sq.ft")
    void testUpdateMaterialWithSqFt() {
        String branchRatesJson = "{\"" + branch.getId() + "\":{\"MIDDLE\":1400.00}}";

        // Create initial material
        Material mat = workCategoryService.createWorkTypeMaterial(
                division.getId(),
                PricingMode.MIDDLE,
                "Acrylic Shutters",
                "High gloss",
                "Sq.ft",
                new BigDecimal("1400.00"),
                "(L + W) × H",
                Map.of(branch.getId(), Map.of(PricingMode.MIDDLE, new BigDecimal("1400.00")))
        );

        // Update material through controller
        ResponseEntity<?> updateResp = workCategoryController.updateWorkTypeMaterial(
                mat.getId(),
                division.getId(),
                null,
                "Acrylic Shutters Premium",
                "(L + W) × H",
                null,
                "Scratch resistant high gloss",
                "Sq.ft",
                new BigDecimal("1500.00"),
                branchRatesJson,
                Map.of()
        );

        assertTrue(updateResp.getStatusCode().is2xxSuccessful(), "Update should succeed");
        Material updated = materialRepository.findById(mat.getId()).orElseThrow();
        assertEquals("Sq.ft", updated.getUnit(), "Updated material unit must be Sq.ft");
        assertEquals("Acrylic Shutters Premium", updated.getName());
    }

    @Test
    @DisplayName("4. Controller rejects non-Sq.ft unit")
    void testControllerRejectsNonSqFtUnit() {
        String branchRatesJson = "{\"" + branch.getId() + "\":{\"MIDDLE\":1000.00}}";

        ResponseEntity<?> response = workCategoryController.createWorkTypeMaterial(
                division.getId(),
                "MIDDLE",
                null,
                "Invalid Unit Test Item",
                "L × W",
                null,
                "",
                "Running ft", // Attempting an invalid unit
                null,
                branchRatesJson,
                Map.of()
        );

        assertTrue(response.getStatusCode().is4xxClientError(), "Non-Sq.ft unit must be rejected");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("Only 'Sq.ft' is supported"));
    }
}
