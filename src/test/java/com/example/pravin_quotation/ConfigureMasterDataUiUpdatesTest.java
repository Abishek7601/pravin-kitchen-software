package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.PricingController;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.repository.WorkCategoryRepository;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ConfigureMasterDataUiUpdatesTest {

    @Autowired
    private PricingController pricingController;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    private String loadTemplateContent(String templatePath) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:templates/" + templatePath);
        assertTrue(resource.exists(), "Template must exist: " + templatePath);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Req 1: Complete Hierarchy UI elements and links removed everywhere")
    public void testCompleteHierarchyRemovedEverywhere() throws Exception {
        String[] templatesToCheck = {
                "admin/dashboard.html",
                "admin/pricing.html",
                "admin/work-categories.html",
                "admin/pricing-form.html",
                "admin/work-category-view.html"
        };

        for (String tmpl : templatesToCheck) {
            String content = loadTemplateContent(tmpl);
            assertFalse(content.contains("Complete Hierarchy"),
                    "Template " + tmpl + " must NOT contain 'Complete Hierarchy' text or UI button");
            assertFalse(content.contains("/admin/master-data/hierarchy"),
                    "Template " + tmpl + " must NOT contain link to '/admin/master-data/hierarchy'");
        }
    }

    @Test
    @DisplayName("Req 2: Remove Add Category from Work Category page, retain single creation flow in Configure Master Data")
    public void testRemoveAddCategoryFromWorkCategory() throws Exception {
        String workCategoriesHtml = loadTemplateContent("admin/work-categories.html");
        assertFalse(workCategoriesHtml.contains("Add Category"),
                "Work categories page must NOT contain 'Add Category' button");
        assertFalse(workCategoriesHtml.contains("add-category-btn"),
                "Work categories page must NOT contain add-category-btn class");

        String pricingFormHtml = loadTemplateContent("admin/pricing-form.html");
        // Verify Work Category input exists as the single creation entry point
        assertTrue(pricingFormHtml.contains("id=\"workCategoryName\""),
                "Configure Master Data must retain Work Category direct text input");
        assertTrue(pricingFormHtml.contains("id=\"categoryEmoji\""),
                "Configure Master Data must retain Category Emoji input");
        assertFalse(pricingFormHtml.contains("Add Category"),
                "Configure Master Data must not have a separate 'Add Category' button");
    }

    @Test
    @DisplayName("Req 3: Cancel Button in Configure Master Data returns to previous page / appropriate master data, never blindly to Pricing List")
    public void testCancelButtonNavigationFlow() throws Exception {
        // 1. Verify template has cancel button bound to cancelUrl and history.back()
        String pricingFormHtml = loadTemplateContent("admin/pricing-form.html");
        assertTrue(pricingFormHtml.contains("id=\"cancelBtn\""),
                "Configure Master Data must have cancelBtn");
        assertTrue(pricingFormHtml.contains("cancelUrl"),
                "Configure Master Data Cancel button must bind to cancelUrl");

        // 2. Test when opened from Work Category View with workCategoryId
        WorkCategory cat = new WorkCategory();
        cat.setName("FOYER_" + System.currentTimeMillis());
        cat.setIcon("🏠");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Model model1 = new ConcurrentModel();
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        String view1 = pricingController.newPricing(cat.getId(), null, request1, model1);
        assertEquals("admin/pricing-form", view1);
        assertEquals("/admin/work-categories/view/" + cat.getId(), model1.getAttribute("cancelUrl"),
                "When accessed with workCategoryId, Cancel button must return to that category's view page");
        assertEquals(cat.getName(), model1.getAttribute("categoryName"),
                "Category name must be pre-filled from workCategoryId");
        assertEquals("🏠", model1.getAttribute("categoryEmoji"),
                "Category emoji must be pre-filled from workCategoryId");

        // 3. Test when opened with referrer from Work Categories list
        Model model2 = new ConcurrentModel();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader("Referer", "http://localhost:8080/admin/work-categories");
        pricingController.newPricing(null, null, request2, model2);
        assertEquals("http://localhost:8080/admin/work-categories", model2.getAttribute("cancelUrl"),
                "When referred from Work Categories, Cancel button must return to Work Categories");

        // 4. Test when opened with referrer from Dashboard
        Model model3 = new ConcurrentModel();
        MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.addHeader("Referer", "http://localhost:8080/admin/dashboard");
        pricingController.newPricing(null, null, request3, model3);
        assertEquals("/admin/dashboard", model3.getAttribute("cancelUrl"),
                "When referred from Dashboard, Cancel button must return to Dashboard");

        // 5. Test default when no referrer: defaults to /admin/work-categories, NOT /admin/pricing
        Model model4 = new ConcurrentModel();
        MockHttpServletRequest request4 = new MockHttpServletRequest();
        pricingController.newPricing(null, null, request4, model4);
        assertEquals("/admin/work-categories", model4.getAttribute("cancelUrl"),
                "Default Cancel destination must be /admin/work-categories, NEVER blindly /admin/pricing");

        // 6. Test only when referrer actually is /admin/pricing, cancelUrl is /admin/pricing
        Model model5 = new ConcurrentModel();
        MockHttpServletRequest request5 = new MockHttpServletRequest();
        request5.addHeader("Referer", "http://localhost:8080/admin/pricing");
        pricingController.newPricing(null, null, request5, model5);
        assertEquals("/admin/pricing", model5.getAttribute("cancelUrl"),
                "Only when coming from pricing list should Cancel return to /admin/pricing");
    }

    @Test
    @DisplayName("Req 4: Expanded Emoji Palette contains all 11 requested categories and saves correctly")
    public void testExpandedEmojiPaletteCategoriesAndSaving() throws Exception {
        String pricingFormHtml = loadTemplateContent("admin/pricing-form.html");
        String categoryFormHtml = loadTemplateContent("admin/work-category-form.html");

        String[] expectedCategories = {
                "kitchen",   // Kitchen and cabinets
                "foyer",     // Foyer and shoe racks
                "living",    // Living rooms and TV units
                "bedroom",   // Bedrooms and wardrobes
                "storage",   // Storage and shelves
                "doors",     // Doors and windows
                "lighting",  // Lighting and electrical work
                "furniture", // Furniture and decorative items
                "bathroom",  // Bathroom interiors
                "office",    // Office interiors
                "other"      // Other interior categories
        };

        for (String cat : expectedCategories) {
            assertTrue(pricingFormHtml.contains("data-tab=\"" + cat + "\""),
                    "pricing-form.html must have tab for " + cat);
            assertTrue(categoryFormHtml.contains("data-tab=\"" + cat + "\""),
                    "work-category-form.html must have tab for " + cat);
        }

        // Verify specific interior emojis are present
        String[] specificEmojis = {"🍳", "👞", "👟", "📺", "🛏️", "🚪", "📦", "🪟", "💡", "🔌", "🪑", "🚿", "💼", "🧘"};
        for (String emoji : specificEmojis) {
            assertTrue(pricingFormHtml.contains(emoji), "pricing-form.html must contain emoji " + emoji);
            assertTrue(categoryFormHtml.contains(emoji), "work-category-form.html must contain emoji " + emoji);
        }

        // Test saving with a new emoji (e.g. 👞 for Shoe Rack)
        String suffix = String.valueOf(System.currentTimeMillis());
        Pricing savedPricing = pricingService.saveUnifiedMasterData(
                null,
                null, "FOYER SHOE STORAGE " + suffix, "👞",
                null, "SHOE CABINETRY",
                null, "MARINE PLYWOOD",
                null, "CUSHION TOP",
                "SOFT CLOSE HINGES",
                PricingMode.PREMIUM,
                new BigDecimal("650.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Foyer shoe cabinet test"
        );

        assertNotNull(savedPricing);
        WorkCategory savedCat = workCategoryRepository.findByNameIgnoreCase("FOYER SHOE STORAGE " + suffix).orElseThrow();
        assertEquals("👞", savedCat.getIcon(), "Saved category emoji must match selected emoji 👞");

        // Verify existing emoji is preserved when editing without emoji change
        Pricing updatedPricing = pricingService.saveUnifiedMasterData(
                savedPricing.getId(),
                savedCat.getId(), "FOYER SHOE STORAGE " + suffix, null, // null emoji passed
                null, "SHOE CABINETRY",
                null, "MARINE PLYWOOD",
                null, "CUSHION TOP",
                "SOFT CLOSE HINGES",
                PricingMode.PREMIUM,
                new BigDecimal("680.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Updated rate"
        );

        assertNotNull(updatedPricing);
        WorkCategory preservedCat = workCategoryRepository.findById(savedCat.getId()).orElseThrow();
        assertEquals("👞", preservedCat.getIcon(), "Existing emoji must be preserved when not changed");
    }

    @Autowired(required = false)
    private com.example.pravin_quotation.controller.WorkCategoryController workCategoryController;

    @Test
    @DisplayName("Req 5: Admin Dashboard removes Configure Master Data button/card while preserving Category and Pricing cards")
    public void testAdminDashboardRemovesConfigureMasterData() throws Exception {
        String dashboardHtml = loadTemplateContent("admin/dashboard.html");
        assertFalse(dashboardHtml.contains("Configure Master Data"),
                "Dashboard must NOT contain 'Configure Master Data' button, card, or link");
        assertTrue(dashboardHtml.contains("@{/admin/work-categories}"),
                "Dashboard must retain Category link");
        assertTrue(dashboardHtml.contains("@{/admin/pricing}"),
                "Dashboard must retain Pricing link");
    }

    @Test
    @DisplayName("Req 6: Category Page contains Configure Master Data modal with emoji selection, preview, and no page redirect")
    public void testCategoryPageContainsConfigureMasterDataModal() throws Exception {
        String workCategoriesHtml = loadTemplateContent("admin/work-categories.html");

        // 1. Configure Master Data button is present above category table
        assertTrue(workCategoriesHtml.contains("id=\"openConfigureMasterDataBtn\""),
                "Category page must have Configure Master Data button with id 'openConfigureMasterDataBtn'");
        assertTrue(workCategoriesHtml.contains("Configure Master Data"),
                "Category page must display Configure Master Data button label");

        // 2. Modal markup is present on the same page
        assertTrue(workCategoriesHtml.contains("id=\"masterDataModal\""),
                "Category page must contain modal container 'masterDataModal'");
        assertTrue(workCategoriesHtml.contains("id=\"categoryNameInput\""),
                "Modal must contain Category Name input field");
        assertTrue(workCategoriesHtml.contains("id=\"categoryEmojiInput\""),
                "Modal must contain Category Emoji input field");
        assertTrue(workCategoriesHtml.contains("id=\"modalSaveCategoryBtn\""),
                "Modal must contain Save Category button");
        assertTrue(workCategoriesHtml.contains("id=\"modalCancelBtn\""),
                "Modal must contain Cancel button");

        // 3. Common interior-design and kitchen emojis
        String[] requiredEmojis = {"🏠", "🍳", "🛏️", "🚪", "🪟", "🛋️", "💡", "🪵", "🎨", "🧱"};
        for (String emoji : requiredEmojis) {
            assertTrue(workCategoriesHtml.contains(emoji),
                    "Category modal emoji picker must contain interior emoji: " + emoji);
        }

        // 4. Live preview element
        assertTrue(workCategoriesHtml.contains("id=\"previewEmoji\""),
                "Modal must display live emoji preview");
        assertTrue(workCategoriesHtml.contains("id=\"previewName\""),
                "Modal must display live name preview");

        // 5. Table displays emoji inline beside category name
        assertTrue(workCategoriesHtml.contains("category-inline-emoji"),
                "Category table must render inline emoji beside the category name");

        // 6. View action remains removed
        assertFalse(workCategoriesHtml.contains("title=\"View\""),
                "Category table must NOT contain View action button");
        assertFalse(workCategoriesHtml.contains("/admin/work-categories/view/"),
                "Category table must NOT link to category view page");

        // 7. Verify pagination method correctness
        assertFalse(workCategoriesHtml.contains("pagination.init()"),
                "Category page must NOT call nonexistent 'pagination.init()'");
        assertTrue(workCategoriesHtml.contains("pagination.applyFilterAndRender"),
                "Category page must use valid 'pagination.applyFilterAndRender()'");
    }

    @Test
    @DisplayName("Req 7: Saving category with emoji via AJAX persists to database and returns JSON response")
    public void testSaveCategoryAjaxWithEmoji() {
        assertNotNull(workCategoryController, "WorkCategoryController must be autowired");

        String uniqueName = "LIBRARY_STUDY_" + System.currentTimeMillis();
        String selectedEmoji = "🪵";

        WorkCategory formCat = new WorkCategory();
        formCat.setName(uniqueName);
        formCat.setIcon(selectedEmoji);

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Object response = workCategoryController.saveCategory(
                uniqueName,
                "Library room",
                selectedEmoji,
                null,
                1,
                request,
                redirectAttributes
        );
        assertNotNull(response);
        assertTrue(response instanceof org.springframework.http.ResponseEntity,
                "AJAX request must return ResponseEntity JSON");

        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(org.springframework.http.HttpStatus.OK, entity.getStatusCode());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) entity.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("success")), "Response should indicate success");

        // Verify entity persisted in MySQL with emoji
        WorkCategory persisted = workCategoryRepository.findByNameIgnoreCase(uniqueName).orElse(null);
        assertNotNull(persisted, "Category must be persisted in database");
        assertEquals("🪵", persisted.getIcon(), "Category emoji must match selected emoji 🪵");
        assertEquals(uniqueName.toUpperCase(), persisted.getName(), "Category name should be uppercase");
    }
}
