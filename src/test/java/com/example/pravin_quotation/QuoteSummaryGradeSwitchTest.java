package com.example.pravin_quotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class QuoteSummaryGradeSwitchTest {

    @Test
    @DisplayName("Verify Admin Quotation Builder has both independent grade controls")
    void testAdminBuilderHasTwoIndependentGradeControls() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/templates/admin/quotation-builder.html"));

        // Control A: Top Pricing Mode for new material selection
        assertTrue(html.contains("name=\"pricingMode\""), "Admin builder must retain top pricingMode control");
        assertTrue(html.contains("id=\"pricingModes\""), "Admin builder must have #pricingModes container");
        assertTrue(html.contains("handleTopPricingModeChange"), "Admin builder must handle top pricing mode separately");

        // Control B: Quote Summary Grade Switch for recalculating existing items
        assertTrue(html.contains("name=\"summaryPricingMode\""), "Admin builder must have separate summaryPricingMode control");
        assertTrue(html.contains("id=\"summaryPricingModes\""), "Admin builder must have #summaryPricingModes container");
        assertTrue(html.contains("id=\"summaryPricingMessage\""), "Admin builder must have #summaryPricingMessage notice banner");
        assertTrue(html.contains("handleSummaryGradeSwitch"), "Admin builder must handle Quote Summary grade switch separately");
        assertTrue(html.contains("updateSummaryGradeDisplay"), "Admin builder must have updateSummaryGradeDisplay function");

        // Verify independent event binding
        assertTrue(html.contains("document.querySelectorAll('input[name=\"pricingMode\"]').forEach"),
                "Admin builder must bind input[name='pricingMode'] to top mode handler");
        assertTrue(html.contains("document.querySelectorAll('input[name=\"summaryPricingMode\"]').forEach"),
                "Admin builder must bind input[name='summaryPricingMode'] to summary grade switch handler");
    }

    @Test
    @DisplayName("Verify Employee Quotation Builder has both independent grade controls")
    void testEmployeeBuilderHasTwoIndependentGradeControls() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/templates/employee/quotation-builder.html"));

        // Control A: Top Pricing Mode for new material selection
        assertTrue(html.contains("name=\"pricingMode\""), "Employee builder must retain top pricingMode control");
        assertTrue(html.contains("id=\"pricingModes\""), "Employee builder must have #pricingModes container");
        assertTrue(html.contains("handleTopPricingModeChange"), "Employee builder must handle top pricing mode separately");

        // Control B: Quote Summary Grade Switch for recalculating existing items
        assertTrue(html.contains("name=\"summaryPricingMode\""), "Employee builder must have separate summaryPricingMode control");
        assertTrue(html.contains("id=\"summaryPricingModes\""), "Employee builder must have #summaryPricingModes container");
        assertTrue(html.contains("id=\"summaryPricingMessage\""), "Employee builder must have #summaryPricingMessage notice banner");
        assertTrue(html.contains("handleSummaryGradeSwitch"), "Employee builder must handle Quote Summary grade switch separately");
        assertTrue(html.contains("updateSummaryGradeDisplay"), "Employee builder must have updateSummaryGradeDisplay function");

        // Verify independent event binding
        assertTrue(html.contains("document.querySelectorAll('input[name=\"pricingMode\"]').forEach"),
                "Employee builder must bind input[name='pricingMode'] to top mode handler");
        assertTrue(html.contains("document.querySelectorAll('input[name=\"summaryPricingMode\"]').forEach"),
                "Employee builder must bind input[name='summaryPricingMode'] to summary grade switch handler");
    }

    @Test
    @DisplayName("Verify CSS defines styles for Quote Summary grade switch")
    void testCssDefinesSummaryGradeSwitchStyles() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/static/css/admin-quotation-builder.css"));

        assertTrue(css.contains(".quote-summary-title-wrapper"), "CSS must define .quote-summary-title-wrapper");
        assertTrue(css.contains(".summary-grade-switch-container"), "CSS must define .summary-grade-switch-container");
        assertTrue(css.contains(".summary-segmented-control"), "CSS must define .summary-segmented-control");
        assertTrue(css.contains(".summary-grade-label"), "CSS must define .summary-grade-label");
    }
}
