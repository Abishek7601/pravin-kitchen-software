package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminManageQuotationDataTest {

    @Autowired
    private AdminController adminController;

    @Test
    public void testFormulaCalculationEndpoint() {
        Model model = new ConcurrentModel();
        String viewName = adminController.formulaCalculation(model);
        assertEquals("admin/formula-calculation", viewName);
    }

    @Test
    public void testDashboardManageQuotationDataOrderAndCards() throws IOException {
        Path dashboardPath = Paths.get("src/main/resources/templates/admin/dashboard.html");
        assertTrue(Files.exists(dashboardPath), "dashboard.html must exist");

        String content = Files.readString(dashboardPath);

        // Find the master-data-grid container
        int gridStart = content.indexOf("<div class=\"master-data-grid\">");
        int gridEnd = content.indexOf("<footer class=\"dashboard-footer\">", gridStart);
        assertTrue(gridStart != -1 && gridEnd != -1, "master-data-grid and footer must be present");

        String masterDataGrid = content.substring(gridStart, gridEnd);

        // Verify removed cards are NOT present in master-data-grid
        assertFalse(masterDataGrid.contains("th:href=\"@{/admin/items}\""),
                "Items card must be removed from Manage Quotation Data grid");
        assertFalse(masterDataGrid.contains("th:href=\"@{/admin/district-pricing}\""),
                "District Pricing card must be removed from Manage Quotation Data grid");
        assertFalse(masterDataGrid.contains("th:href=\"@{/admin/travel-charges}\""),
                "Travel Charges card must be removed from Manage Quotation Data grid");

        // Verify the 5 required cards exist in exact order
        int posWorkCategories = masterDataGrid.indexOf("th:href=\"@{/admin/work-categories}\"");
        int posDivisions = masterDataGrid.indexOf("th:href=\"@{/admin/divisions}\"");
        int posMaterials = masterDataGrid.indexOf("th:href=\"@{/admin/materials}\"");
        int posMaterialOptions = masterDataGrid.indexOf("th:href=\"@{/admin/material-options}\"");
        int posFifth = masterDataGrid.indexOf("th:href=\"@{/admin/pricing}\"");
        if (posFifth == -1) {
            posFifth = masterDataGrid.indexOf("th:href=\"@{/admin/formula-calculation}\"");
        }

        assertTrue(posWorkCategories != -1, "Work Categories must be present");
        assertTrue(posDivisions != -1, "Divisions must be present");
        assertTrue(posMaterials != -1, "Materials must be present");
        assertTrue(posMaterialOptions != -1, "Material Options must be present");
        assertTrue(posFifth != -1, "Fifth card (Pricing / Formula Calculation) must be present");

        assertTrue(posWorkCategories < posDivisions,
                "Work Categories must appear before Divisions");
        assertTrue(posDivisions < posMaterials,
                "Divisions must appear before Materials");
        assertTrue(posMaterials < posMaterialOptions,
                "Materials must appear before Material Options");
        assertTrue(posMaterialOptions < posFifth,
                "Material Options must appear before Pricing");
    }

    @Test
    public void testFormulaCalculationTemplateExistsAndContainsFormulas() throws IOException {
        Path templatePath = Paths.get("src/main/resources/templates/admin/formula-calculation.html");
        assertTrue(Files.exists(templatePath), "formula-calculation.html must exist");

        String content = Files.readString(templatePath);
        assertTrue(content.contains("(Length + Width) × Height / 144") || content.contains("(Length + Width)"),
                "Must document the measurement calculation formula");
        assertTrue(content.contains("SQ.FT") || content.contains("sq.ft"),
                "Must mention SQ.FT calculation");
    }
}
