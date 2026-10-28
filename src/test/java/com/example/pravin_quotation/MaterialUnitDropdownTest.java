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
    @DisplayName("1. Material modal contains 3 radio options (Sq.ft, Quantity, Files) and removed old unit options")
    void testMaterialModalHasThreeRadioOptions() throws IOException {
        File file = new File("src/main/resources/templates/admin/work-categories.html");
        assertTrue(file.exists(), "work-categories.html must exist");
        String content = Files.readString(file.toPath());

        // Ensure 3 radio button cards exist
        assertTrue(content.contains("id=\"mTypeRadioSqft\""), "mTypeRadioSqft radio must exist");
        assertTrue(content.contains("id=\"mTypeRadioQuantity\""), "mTypeRadioQuantity radio must exist");
        assertTrue(content.contains("id=\"mTypeRadioFiles\""), "mTypeRadioFiles radio must exist");

        // Ensure dynamic containers exist
        assertTrue(content.contains("id=\"formulaSectionContainer\""), "formulaSectionContainer must exist");
        assertTrue(content.contains("id=\"quantitySectionContainer\""), "quantitySectionContainer must exist");
        assertTrue(content.contains("id=\"filesSectionContainer\""), "filesSectionContainer must exist");

        // Ensure removed options from old unit dropdown are NOT in work-categories.html
        assertFalse(content.contains("value=\"Running ft\""), "Running ft must not be in modal");
        assertFalse(content.contains("value=\"Piece\""), "Piece must not be in modal");
        assertFalse(content.contains("value=\"Set\""), "Set must not be in modal");
        assertFalse(content.contains("value=\"Meter\""), "Meter must not be in modal");
        assertFalse(content.contains("value=\"Item\""), "Item must not be in modal");
    }

    @Test
    @DisplayName("2. Creating material uses Sq.ft (SQFT) and validates branch rates correctly")
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
                "SQFT",
                null,
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
        assertEquals(MeasurementType.SQFT, mat.getMeasurementType(), "Material measurementType must be SQFT");
        assertEquals("Sq.ft", mat.getUnit(), "Material unit must be Sq.ft");
    }

    @Test
    @DisplayName("3. Creating material with Quantity (QUANTITY) sets unit to Quantity and clears formula")
    void testCreateMaterialWithQuantity() {
        String branchRatesJson = "{\"" + branch.getId() + "\":{\"MIDDLE\":450.00}}";

        ResponseEntity<?> response = workCategoryController.createWorkTypeMaterial(
                division.getId(),
                "MIDDLE",
                null,
                "Cabinet Handle Brass",
                null,
                null,
                "Brass Finish 6 inch",
                null,
                "QUANTITY",
                null,
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
        assertEquals(MeasurementType.QUANTITY, mat.getMeasurementType(), "Material measurementType must be QUANTITY");
        assertEquals("Quantity", mat.getUnit(), "Material unit must be Quantity");
        assertNull(mat.getFormula(), "Formula should be null for Quantity items");
    }

    @Test
    @DisplayName("4. Creating material with Files (FILES) sets unit to Files and clears formula")
    void testCreateMaterialWithFiles() {
        String branchRatesJson = "{\"" + branch.getId() + "\":{\"MIDDLE\":15000.00}}";

        ResponseEntity<?> response = workCategoryController.createWorkTypeMaterial(
                division.getId(),
                "MIDDLE",
                null,
                "Custom 3D CNC Panel",
                null,
                null,
                "CNC Cut wave pattern",
                null,
                "FILES",
                null,
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
        assertEquals(MeasurementType.FILES, mat.getMeasurementType(), "Material measurementType must be FILES");
        assertEquals("Files", mat.getUnit(), "Material unit must be Files");
        assertNull(mat.getFormula(), "Formula should be null for Files items");
    }

    @Test
    @DisplayName("5. Updating existing material displays and saves Sq.ft")
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
                "SQFT",
                null,
                false,
                new BigDecimal("1500.00"),
                branchRatesJson,
                Map.of()
        );

        assertTrue(updateResp.getStatusCode().is2xxSuccessful(), "Update should succeed");
        Material updated = materialRepository.findById(mat.getId()).orElseThrow();
        assertEquals("Sq.ft", updated.getUnit(), "Updated material unit must be Sq.ft");
        assertEquals(MeasurementType.SQFT, updated.getMeasurementType());
        assertEquals("Acrylic Shutters Premium", updated.getName());
    }
}
