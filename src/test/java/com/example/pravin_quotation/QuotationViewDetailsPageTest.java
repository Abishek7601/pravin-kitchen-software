package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.QuotationController;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.repository.QuotationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationViewDetailsPageTest {

    @Autowired
    private QuotationController quotationController;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    private String loadResource(String path) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        assertTrue(resource.exists(), "Resource must exist: " + path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Top Menubar logo is properly styled, centered, and aspect ratio preserved in quotation-view.css")
    public void testTopMenubarLogoStylesInQuotationViewCss() throws Exception {
        String css = loadResource("static/css/quotation-view.css");

        // Body padding must be 0 to not distort fixed navbar/sidebar layout
        assertFalse(css.contains("padding: 28px 20px 48px;"), "body in quotation-view.css must not have padding pushing down layout");

        // Top navbar brand icon and logo image styles
        assertTrue(css.contains(".top-navbar .brand-icon"), "quotation-view.css must specifically style .top-navbar .brand-icon");
        assertTrue(css.contains(".top-navbar .brand-icon .brand-logo-img"), "quotation-view.css must specifically style .top-navbar .brand-icon .brand-logo-img");
        assertTrue(css.contains("object-fit: contain"), "Logo must maintain aspect ratio with object-fit: contain");
        assertTrue(css.contains("width: 100%"), "Logo must fill the brand icon box width");
        assertTrue(css.contains("height: 100%"), "Logo must fill the brand icon box height");

        // Eyebrow / page header logo scoped
        assertTrue(css.contains(".page-header .brand-logo-img"), "Page header logo must be scoped");
    }

    @Test
    @DisplayName("Internal Notes and Status History sections are removed from admin/quotation-view.html")
    public void testInternalNotesAndStatusHistoryRemovedFromTemplate() throws Exception {
        String template = loadResource("templates/admin/quotation-view.html");

        // Internal Notes removed
        assertFalse(template.contains("Internal Notes"), "Template must not contain Internal Notes heading");
        assertFalse(template.contains("Internal notes"), "Template must not contain Internal notes placeholder text");
        assertFalse(template.contains("No notes added."), "Template must not contain No notes added message");

        // Status History removed
        assertFalse(template.contains("Status History"), "Template must not contain Status History heading");
        assertFalse(template.contains("Track every status change made to this quotation."), "Template must not contain Status History subtitle");
        assertFalse(template.contains("No status history yet"), "Template must not contain No status history message");

        // Customer Requirements removed
        assertFalse(template.contains("Customer Requirements"), "Template must not contain Customer Requirements heading");
        assertFalse(template.contains("quotation.customerRequirements"), "Template must not contain quotation.customerRequirements expression");

        // Other sections preserved
        assertTrue(template.contains("Communication History"), "Communication History section must be preserved");

        // Quotation status remains visible outside removed sections
        assertTrue(template.contains("quotation.status"), "Quotation status must be visible in badge and panels");
    }

    @Test
    @DisplayName("Quotation view controller endpoint loads view successfully with status and details preserved")
    public void testQuotationViewControllerLoad() {
        Quotation sampleQuotation = quotationRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(sampleQuotation, "At least one quotation should exist in test database");

        Model model = new ConcurrentModel();
        String view = quotationController.viewQuotation(sampleQuotation.getId(), model);

        assertEquals("admin/quotation-view", view, "Endpoint must return admin/quotation-view");
        assertTrue(model.containsAttribute("quotation"), "Model must contain quotation");

        Quotation modelQuotation = (Quotation) model.getAttribute("quotation");
        assertNotNull(modelQuotation.getStatus(), "Quotation status must be present");
        assertEquals(sampleQuotation.getStatus(), modelQuotation.getStatus(), "Quotation status must match database record");
    }
}
