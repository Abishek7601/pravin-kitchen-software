package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminController;
import com.example.pravin_quotation.controller.AdminFormulaController;
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
    private AdminFormulaController adminFormulaController;

    @Test
    public void testFormulaCalculationEndpoint() {
        Model model = new ConcurrentModel();
        String viewName = adminFormulaController.formulaCalculation(model);
        assertEquals("admin/formula-calculation", viewName);
    }

    @Test
    public void testDashboardManageQuotationDataOrderAndCards() throws IOException {
        Path dashboardPath = Paths.get("src/main/resources/templates/admin/dashboard.html");
        assertTrue(Files.exists(dashboardPath), "dashboard.html must exist");

        String content = Files.readString(dashboardPath);

        // Verify master-data-grid container is completely removed from dashboard
        assertFalse(content.contains("<div class=\"master-data-grid\">"),
                "master-data-grid container must be completely removed from dashboard");

        // Verify Category and Pricing shortcut cards are NOT present in dashboard
        assertFalse(content.contains("th:href=\"@{/admin/work-categories}\""),
                "Category shortcut card must be removed from dashboard");
        assertFalse(content.contains("th:href=\"@{/admin/pricing}\""),
                "Pricing shortcut card must be removed from dashboard");
        assertFalse(content.contains("Manage categories"),
                "Category card description must be removed from dashboard");
        assertFalse(content.contains("Manage quotation pricing"),
                "Pricing card description must be removed from dashboard");

        // Verify footer is still present
        assertTrue(content.contains("<footer class=\"dashboard-footer\">"),
                "Footer must remain present on dashboard");
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
