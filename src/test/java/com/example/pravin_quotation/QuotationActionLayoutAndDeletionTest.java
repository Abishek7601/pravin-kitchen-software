package com.example.pravin_quotation;

import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationActionLayoutAndDeletionTest {

    @Test
    @DisplayName("Verify Pricing column displays all used grades and never shows generic 'Multi-Grade'")
    void testPricingColumnDisplayFormatting() {
        QuotationCalculationService service = new QuotationCalculationService(
                null, null, null, null, null, null, null
        );

        // 1. Single grade quotes
        assertEquals("PREMIUM", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM), null));
        assertEquals("MIDDLE", service.formatPricingColumnDisplay(Set.of(PricingMode.MIDDLE), null));
        assertEquals("ECONOMY", service.formatPricingColumnDisplay(Set.of(PricingMode.ECONOMY), null));

        // 2. Confirmed single grade quotes with checkmark
        assertEquals("✓ PREMIUM", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM), PricingMode.PREMIUM));
        assertEquals("✓ MIDDLE", service.formatPricingColumnDisplay(Set.of(PricingMode.MIDDLE), PricingMode.MIDDLE));
        assertEquals("✓ ECONOMY", service.formatPricingColumnDisplay(Set.of(PricingMode.ECONOMY), PricingMode.ECONOMY));

        // 3. Multi-grade combinations
        assertEquals("PREMIUM + MIDDLE", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), null));
        assertEquals("PREMIUM + ECONOMY", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.ECONOMY), null));
        assertEquals("MIDDLE + ECONOMY", service.formatPricingColumnDisplay(Set.of(PricingMode.MIDDLE, PricingMode.ECONOMY), null));
        assertEquals("PREMIUM + MIDDLE + ECONOMY", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE, PricingMode.ECONOMY), null));

        // 4. Confirmed multi-grade combinations
        assertEquals("✓ PREMIUM + MIDDLE", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), PricingMode.PREMIUM));
        assertEquals("PREMIUM + ✓ MIDDLE", service.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), PricingMode.MIDDLE));

        // 5. Empty / null
        assertEquals("Not Prepared", service.formatPricingColumnDisplay(null, null));
        assertEquals("Not Prepared", service.formatPricingColumnDisplay(Collections.emptySet(), null));
    }

    @Test
    @DisplayName("Verify quotations list templates do not contain hardcoded or unformatted 'Multi-Grade' badges")
    void testQuotationListTemplatesNoMultiGradeBadges() throws IOException {
        String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/quotations.html"));
        String employeeHtml = Files.readString(Path.of("src/main/resources/templates/employee/quotations.html"));

        assertFalse(adminHtml.contains(">Multi-Grade<"), "Admin quotations list must not display hardcoded 'Multi-Grade' text");
        assertFalse(employeeHtml.contains(">Multi-Grade<"), "Employee quotations list must not display hardcoded 'Multi-Grade' text");
        assertFalse(employeeHtml.contains("(Multi-Grade)"), "Employee quotations list must not display '(Multi-Grade)'");
    }

    @Test
    @DisplayName("Verify bottom action buttons in quotation view are arranged in a single horizontal row on desktop")
    void testBottomActionButtonsLayout() throws IOException {
        String adminView = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));
        String employeeView = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/quotation-view.css"));

        // Admin details page must contain all 4 action buttons
        assertTrue(adminView.contains("btn-grade-edit"), "Admin details must have Edit button");
        assertTrue(adminView.contains("btn-grade-pdf"), "Admin details must have PDF button");
        assertTrue(adminView.contains("btn-grade-whatsapp"), "Admin details must have WhatsApp button");
        assertTrue(adminView.contains("btn-grade-email"), "Admin details must have Email button");

        // Employee details page must contain all 4 action buttons
        assertTrue(employeeView.contains("btn-grade-edit"), "Employee workspace must have Edit button");
        assertTrue(employeeView.contains("btn-grade-pdf"), "Employee workspace must have PDF button");
        assertTrue(employeeView.contains("btn-grade-whatsapp"), "Employee workspace must have WhatsApp button");
        assertTrue(employeeView.contains("btn-grade-email"), "Employee workspace must have Email button");

        // CSS must enforce single horizontal row on desktop and prevent 100% width stretching
        assertTrue(css.contains(".quotation-bottom-actions-bar"), "CSS must define .quotation-bottom-actions-bar");
        assertTrue(css.contains("flex-direction: row !important;"), "Must align buttons horizontally");
        assertTrue(css.contains("flex-wrap: nowrap !important;"), "Desktop media query must enforce flex-wrap: nowrap");
        assertTrue(css.contains(".quotation-bottom-actions-bar form"), "CSS must style form inside bottom actions bar");
        assertTrue(css.contains("width: auto !important;"), "Form and email button must have width: auto");
    }
}
