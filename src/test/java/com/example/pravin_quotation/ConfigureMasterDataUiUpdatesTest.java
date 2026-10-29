package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.PricingController;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.WorkCategory;
import com.example.pravin_quotation.model.Division;
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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ConfigureMasterDataUiUpdatesTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Autowired
    private PricingController pricingController;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private com.example.pravin_quotation.service.WorkCategoryService workCategoryService;

    @Autowired
    private ResourceLoader resourceLoader;

    private String loadTemplateContent(String templatePath) throws Exception {
        java.nio.file.Path sourcePath = java.nio.file.Paths.get("src/main/resources/templates/" + templatePath);
        if (java.nio.file.Files.exists(sourcePath)) {
            return java.nio.file.Files.readString(sourcePath, StandardCharsets.UTF_8);
        }
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
        }
        assertFalse(categoryFormHtml.contains("data-tab="),
                "work-category-form.html must have separate emoji tabs removed per requirement");

        // Verify specific interior emojis are present in pricing-form.html
        String[] specificEmojis = {"🍳", "👞", "👟", "📺", "🛏️", "🚪", "📦", "🪟", "💡", "🔌", "🪑", "🚿", "💼", "🧘"};
        for (String emoji : specificEmojis) {
            assertTrue(pricingFormHtml.contains(emoji), "pricing-form.html must contain emoji " + emoji);
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
    @WithMockUser(username = "superadmin@pravin.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Req 5: Admin Dashboard removes Configure Master Data button/card and Category and Pricing shortcut cards")
    public void testAdminDashboardRemovesConfigureMasterData() throws Exception {
        String dashboardHtml = loadTemplateContent("admin/dashboard.html");
        assertFalse(dashboardHtml.contains("Configure Master Data"),
                "Dashboard template must NOT contain 'Configure Master Data' button, card, or link");
        assertFalse(dashboardHtml.contains("@{/admin/work-categories}"),
                "Dashboard must remove Category shortcut card");
        assertFalse(dashboardHtml.contains("@{/admin/pricing}"),
                "Dashboard must remove Pricing shortcut card");

        // Verify rendered Admin Dashboard HTML (including shared fragments) contains ZERO occurrences of Configure Master Data or Pricing in sidebar
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Configure Master Data"))))
                .andExpect(content().string(containsString("Category")))
                .andExpect(content().string(not(containsString("<span>Pricing</span>"))));
    }

    @Test
    @DisplayName("Req 6: Category Page contains Add Categories modal, single category name field with emoji, and 4-column table")
    public void testCategoryPageContainsConfigureMasterDataModal() throws Exception {
        String workCategoriesHtml = loadTemplateContent("admin/work-categories.html");

        // 1. Configure Master Data button is present above category table
        assertTrue(workCategoriesHtml.contains("id=\"openConfigureMasterDataBtn\""),
                "Category page must have Configure Master Data button with id 'openConfigureMasterDataBtn'");

        // 2. Modal markup is present on the same page
        assertTrue(workCategoriesHtml.contains("id=\"masterDataModal\""),
                "Category page must contain modal container 'masterDataModal'");
        assertTrue(workCategoriesHtml.contains("id=\"categoryNameInput\""),
                "Modal must contain Category Name input field");
        assertTrue(workCategoriesHtml.contains("id=\"modalSaveCategoryBtn\""),
                "Modal must contain Save Category button");
        assertTrue(workCategoriesHtml.contains("id=\"modalCancelBtn\""),
                "Modal must contain Cancel button");

        // 3. Live preview element for name
        assertTrue(workCategoriesHtml.contains("id=\"previewName\""),
                "Modal must display live name preview");

        // 4. Table headers match: S.NO | CATEGORY NAME | STATUS | ACTIONS (no separate EMOJI/ICON column)
        assertTrue(workCategoriesHtml.contains("Category Name"),
                "Category table must render Category Name column");
        assertFalse(workCategoriesHtml.contains("<th class=\"col-icon\">"),
                "Category table must NOT contain separate EMOJI / ICON table header column");

        // 5. Actions column includes View button for work types
        assertTrue(workCategoriesHtml.contains("js-view-category-btn"),
                "Category table must contain View action button");

        // 6. Verify pagination method correctness
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

    @Test
    @DisplayName("Req 8: Edit category modal elements present and single category name field used")
    public void testEditCategoryModalAndSingleEmojiFieldMarkup() throws Exception {
        String html = loadTemplateContent("admin/work-categories.html");

        // 1. Edit button triggers modal on same page (button, not external anchor link)
        assertTrue(html.contains("js-edit-category-btn"),
                "Category table must have 'js-edit-category-btn' to trigger modal edit");
        assertTrue(html.contains("th:data-id=\"${category.id}\""),
                "Category row or edit button must bind category ID for modal population");

        // 2. Hidden category ID input present in modal form
        assertTrue(html.contains("id=\"categoryIdInput\""),
                "Modal form must contain hidden categoryIdInput to distinguish between create and update");

        // 3. Category Name input is the single field (including emoji)
        assertTrue(html.contains("id=\"categoryNameInput\""),
                "Modal must contain single categoryNameInput text input field");

        // 4. In-place table update function is defined for AJAX modal save
        assertTrue(html.contains("updateCategoryRowInTable"),
                "Category page script must define updateCategoryRowInTable for smooth in-place table updates");
    }

    @Test
    @DisplayName("Req 9: Editing category via AJAX updates database without creating duplicates")
    public void testEditCategoryUpdatesExistingWithoutDuplicates() {
        assertNotNull(workCategoryController, "WorkCategoryController must be autowired");

        // Create an initial category
        String initialName = "GUEST_ROOM_" + System.currentTimeMillis();
        WorkCategory initialCat = new WorkCategory();
        initialCat.setName(initialName);
        initialCat.setIcon("🛏️");
        initialCat.setDescription("Initial description");
        initialCat.setActive(true);
        WorkCategory savedInitial = workCategoryRepository.save(initialCat);
        Long catId = savedInitial.getId();
        long initialCount = workCategoryRepository.count();

        // 1. Verify GET /edit/{id} returns JSON when requested via AJAX
        org.springframework.mock.web.MockHttpServletRequest editReq = new org.springframework.mock.web.MockHttpServletRequest();
        editReq.addHeader("X-Requested-With", "XMLHttpRequest");
        Object getEditResponse = workCategoryController.editCategory(
                catId,
                editReq,
                new org.springframework.ui.ConcurrentModel()
        );
        assertTrue(getEditResponse instanceof org.springframework.http.ResponseEntity,
                "GET edit with AJAX must return ResponseEntity JSON");
        org.springframework.http.ResponseEntity<?> editEntity = (org.springframework.http.ResponseEntity<?>) getEditResponse;
        assertEquals(org.springframework.http.HttpStatus.OK, editEntity.getStatusCode());
        WorkCategory editCat = (WorkCategory) editEntity.getBody();
        assertNotNull(editCat);
        assertEquals(catId, editCat.getId());
        assertEquals("🛏️", editCat.getIcon());

        // 2. Perform Edit update with ID via saveCategory
        String updatedName = "LUXURY_GUEST_ROOM_" + System.currentTimeMillis();
        String updatedEmoji = "🚪";
        String updatedDesc = "Updated luxury guest room description";

        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        Object updateResponse = workCategoryController.saveCategory(
                catId,
                updatedName,
                updatedDesc,
                updatedEmoji,
                null,
                1,
                request,
                redirectAttributes
        );
        assertTrue(updateResponse instanceof org.springframework.http.ResponseEntity,
                "Save with existing ID via AJAX must return ResponseEntity JSON");
        org.springframework.http.ResponseEntity<?> updateEntity = (org.springframework.http.ResponseEntity<?>) updateResponse;
        assertEquals(org.springframework.http.HttpStatus.OK, updateEntity.getStatusCode());

        // 3. Confirm total database count did NOT increase (no duplicate created!)
        long countAfterUpdate = workCategoryRepository.count();
        assertEquals(initialCount, countAfterUpdate, "Editing an existing category must NOT create a duplicate record");

        // 4. Confirm entity updated in database
        WorkCategory reloaded = workCategoryRepository.findById(catId).orElse(null);
        assertNotNull(reloaded, "Updated category must exist in database");
        assertEquals(updatedName.toUpperCase(), reloaded.getName(), "Category name must be updated and uppercase");
        assertEquals("🚪", reloaded.getIcon(), "Category emoji must be updated to 🚪");
        assertEquals(updatedDesc, reloaded.getDescription(), "Category description must be updated");
    }

    @Autowired
    private com.example.pravin_quotation.repository.DivisionRepository divisionRepository;

    @Autowired
    private com.example.pravin_quotation.repository.ItemRepository itemRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Req 10: Category management page includes View button in Actions column alongside Edit and Delete")
    public void testCategoryPageHasViewButtonAndActions() throws Exception {
        Resource templateResource = resourceLoader.getResource("classpath:templates/admin/work-categories.html");
        assertTrue(templateResource.exists(), "work-categories.html template must exist");

        String html;
        try (InputStream is = templateResource.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 1. Actions column must contain View, Edit, and Delete
        assertTrue(html.contains("js-view-category-btn"),
                "Category page table must contain View button with class 'js-view-category-btn'");
        assertTrue(html.contains("view-btn"),
                "Category page table must contain 'view-btn' class on the View button");
        assertTrue(html.contains("js-edit-category-btn"),
                "Category page table must preserve 'js-edit-category-btn'");
        assertTrue(html.contains("delete-btn"),
                "Category page table must preserve 'delete-btn'");

        // 2. Both existing table template and dynamic row creation include View button
        assertTrue(html.contains("data-id=\"' + category.id + '\"") && html.contains("title=\"View Category Work Types\""),
                "Dynamic JavaScript row creation in addCategoryRowToTable must also include the View button");
    }

    @Test
    @DisplayName("Req 11: Category management page contains View Category Work Types modal popup")
    public void testCategoryPageHasViewWorkTypesModal() throws Exception {
        Resource templateResource = resourceLoader.getResource("classpath:templates/admin/work-categories.html");
        assertTrue(templateResource.exists(), "work-categories.html template must exist");

        String html;
        try (InputStream is = templateResource.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 1. View modal dialog exists
        assertTrue(html.contains("id=\"viewWorkTypesModal\""),
                "Category page must contain View Work Types modal with id 'viewWorkTypesModal'");

        // 2. Header contains title, emoji badge, and close button
        assertTrue(html.contains("id=\"viewModalTitle\""),
                "View modal must contain 'viewModalTitle'");
        assertTrue(html.contains("id=\"viewModalHeaderBadge\""),
                "View modal must contain 'viewModalHeaderBadge'");
        assertTrue(html.contains("id=\"viewModalCloseXBtn\""),
                "View modal must contain 'viewModalCloseXBtn'");

        // 3. Total count element exists
        assertTrue(html.contains("id=\"viewWorkTypesCount\""),
                "View modal must contain 'viewWorkTypesCount'");

        // 4. Grid and empty state exist
        assertTrue(html.contains("id=\"viewWorkTypesGrid\""),
                "View modal must contain 'viewWorkTypesGrid'");
        assertTrue(html.contains("id=\"viewWorkTypesEmpty\""),
                "View modal must contain 'viewWorkTypesEmpty'");
        assertTrue(html.contains("No work types found in this category"),
                "View modal must contain clear empty state message: 'No work types found in this category'");

        // 5. Footer Close button exists
        assertTrue(html.contains("id=\"viewModalCloseBtn\""),
                "View modal must contain 'viewModalCloseBtn'");
    }

    @Test
    @DisplayName("Req 12: GET /admin/work-categories/{id}/work-types returns dynamically loaded work types from MySQL")
    public void testViewCategoryWorkTypesEndpointReturnsAssociatedItems() {
        assertNotNull(workCategoryController, "WorkCategoryController must be autowired");

        // Use category Modular kitchen (or find an active category with divisions)
        WorkCategory cat = workCategoryRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().toLowerCase().contains("kitchen"))
                .findFirst()
                .orElseGet(() -> {
                    java.util.List<WorkCategory> all = workCategoryRepository.findAll();
                    if (!all.isEmpty()) {
                        return all.get(0);
                    }
                    WorkCategory newCat = new WorkCategory();
                    newCat.setName("🍳 MODULAR KITCHEN");
                    newCat.setActive(true);
                    return workCategoryRepository.save(newCat);
                });

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/work-categories/" + cat.getId() + "/work-types");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        Object response = workCategoryController.viewCategory(cat.getId(), request, new ConcurrentModel());
        assertTrue(response instanceof org.springframework.http.ResponseEntity,
                "viewCategory with AJAX/work-types route must return ResponseEntity");

        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(org.springframework.http.HttpStatus.OK, entity.getStatusCode());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) entity.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("success")));
        assertEquals(cat.getId(), body.get("categoryId"));
        assertNotNull(body.get("categoryName"));
        assertNotNull(body.get("totalCount"));
        assertTrue(body.get("workTypes") instanceof java.util.List);

        int totalCount = (int) body.get("totalCount");
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> workTypes = (java.util.List<java.util.Map<String, Object>>) body.get("workTypes");
        assertEquals(totalCount, workTypes.size(), "totalCount must match the actual workTypes list size");

        // Verify each work type has id, name, emoji, active
        for (java.util.Map<String, Object> wt : workTypes) {
            assertNotNull(wt.get("name"), "Work type must have a name");
            assertNotNull(wt.get("emoji"), "Work type must have an emoji");
        }
    }

    @Test
    @DisplayName("Req 13: View Category Work Types handles empty state for categories without items")
    public void testViewCategoryWorkTypesEmptyState() {
        assertNotNull(workCategoryController, "WorkCategoryController must be autowired");

        // Create empty category with no divisions/items
        WorkCategory emptyCat = new WorkCategory();
        emptyCat.setName("BALCONY_EMPTY_" + System.currentTimeMillis());
        emptyCat.setIcon("🌿");
        emptyCat.setActive(true);
        WorkCategory saved = workCategoryRepository.save(emptyCat);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/work-categories/" + saved.getId() + "/work-types");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        Object response = workCategoryController.viewCategory(saved.getId(), request, new ConcurrentModel());
        assertTrue(response instanceof org.springframework.http.ResponseEntity);

        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(org.springframework.http.HttpStatus.OK, entity.getStatusCode());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) entity.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("success")));
        assertEquals(0, body.get("totalCount"), "Empty category must have totalCount 0");
        @SuppressWarnings("unchecked")
        java.util.List<?> list = (java.util.List<?>) body.get("workTypes");
        assertTrue(list.isEmpty(), "Empty category workTypes list must be empty");
    }

    @Test
    @DisplayName("Req 14: Category View includes category emoji and work types display single number format")
    public void testViewCategoryIncludesEmojiAndSingleNumberWorkTypes() throws Exception {
        // Create category with emoji icon
        WorkCategory cat = new WorkCategory();
        cat.setName("PENTHOUSE_" + System.currentTimeMillis());
        cat.setIcon("🏢");
        cat.setActive(true);
        WorkCategory savedCat = workCategoryRepository.save(cat);

        // Add 2 work types (divisions)
        workCategoryService.createWorkType(savedCat.getId(), "LIVING AREA", "🛋️", "Living area description");
        workCategoryService.createWorkType(savedCat.getId(), "TERRACE", "🌿", "Terrace description");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/work-categories/" + savedCat.getId() + "/work-types");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        Object response = workCategoryController.viewCategory(savedCat.getId(), request, new ConcurrentModel());
        assertTrue(response instanceof org.springframework.http.ResponseEntity);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) entity.getBody();
        assertNotNull(body);

        String categoryName = (String) body.get("categoryName");
        assertNotNull(categoryName, "categoryName must not be null");
        assertTrue(categoryName.contains("🏢"), "categoryName returned for View Category must include the emoji 🏢");

        // Check work-categories.html template to ensure single number per work type and no work type icon
        String html = loadTemplateContent("admin/work-categories.html");
        assertTrue(html.contains("span class=\"work-type-number\""),
                "View modal must use a single work-type-number for sequence display");
        assertTrue(html.contains("var num = index + 1;"),
                "Work type numbering must be 1-indexed dynamic number");
    }

    @Test
    @DisplayName("Req 15: POST /update/{id} endpoint successfully updates category without creating duplicates")
    public void testUpdateCategoryViaUpdateEndpointNoDuplicates() {
        // Create category
        WorkCategory cat = new WorkCategory();
        String initialName = "STUDY_" + System.currentTimeMillis();
        cat.setName(initialName);
        cat.setIcon("📚");
        cat.setDescription("Initial study description");
        cat.setActive(true);
        WorkCategory saved = workCategoryRepository.save(cat);
        Long id = saved.getId();
        long countBefore = workCategoryRepository.count();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String updatedName = "📚 EXECUTIVE STUDY";
        String updatedDesc = "Updated study description";

        Object updateResponse = workCategoryController.updateCategory(
                id,
                updatedName,
                updatedDesc,
                null,
                null,
                0,
                request,
                redirectAttributes
        );

        assertTrue(updateResponse instanceof org.springframework.http.ResponseEntity);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) updateResponse;
        assertEquals(org.springframework.http.HttpStatus.OK, entity.getStatusCode());

        // Count must remain the same
        long countAfter = workCategoryRepository.count();
        assertEquals(countBefore, countAfter, "Updating category via /update/{id} must not create duplicate records");

        // Reload from database
        WorkCategory updated = workCategoryRepository.findById(id).orElse(null);
        assertNotNull(updated);
        assertEquals(updatedName.toUpperCase(), updated.getName());
        assertEquals("📚", updated.getIcon(), "Icon should be extracted from leading emoji if present");
        assertEquals(updatedDesc, updated.getDescription());
    }

    @Test
    @DisplayName("Req 16: Category template JavaScript contains valid syntax and event delegation")
    public void testTemplateJavaScriptSyntaxIntegrity() throws Exception {
        String html = loadTemplateContent("admin/work-categories.html");

        // Verify the fix for line 933
        assertFalse(html.contains("workTypeFormAlert.style.display             if"),
                "work-categories.html must not contain broken JavaScript syntax");
        assertTrue(html.contains("if (workTypeFormAlert) workTypeFormAlert.style.display = 'none';"),
                "work-categories.html must cleanly hide workTypeFormAlert on open");

        // Verify event delegation for View and Edit buttons
        assertTrue(html.contains("e.target.closest('.js-view-category-btn')"),
                "work-categories.html must delegate click events for View button");
        assertTrue(html.contains("e.target.closest('.js-edit-category-btn')"),
                "work-categories.html must delegate click events for Edit button");
    }

    @Test
    @DisplayName("Req 17: Work type activate/deactivate toggles directly without confirm or alert popups and persists in DB")
    public void testDirectStatusToggleNoAlerts() throws Exception {
        // 1. Verify in work-categories.html that work type toggle handler does NOT use confirm() or alert()
        String html = loadTemplateContent("admin/work-categories.html");
        int toggleHandlerStart = html.indexOf("var toggleBtn = e.target.closest('.js-toggle-work-type-btn');");
        assertTrue(toggleHandlerStart > 0, "work-categories.html must contain toggleBtn handler");
        int toggleHandlerEnd = html.indexOf("var delBtn = e.target.closest('.js-delete-work-type-btn');", toggleHandlerStart);
        assertTrue(toggleHandlerEnd > toggleHandlerStart, "delBtn handler must follow toggleBtn handler");

        String toggleHandlerBlock = html.substring(toggleHandlerStart, toggleHandlerEnd);
        assertFalse(toggleHandlerBlock.contains("confirm("), "Work type toggle must NOT display confirm() popup. Block was: " + toggleHandlerBlock);
        assertFalse(toggleHandlerBlock.contains("alert("), "Work type toggle must NOT display alert() popup");
        assertTrue(toggleHandlerBlock.contains("card.querySelector('.status-badge')"), "Work type toggle must update card status badge in-place");
        assertTrue(toggleHandlerBlock.contains("showViewModalAlert("), "Work type toggle must use non-blocking inline message");

        // 2. Verify backend toggles status and persists in database without creating duplicates
        WorkCategory cat = new WorkCategory();
        cat.setName("TOGGLE_TEST_CAT_" + System.currentTimeMillis());
        cat.setIcon("⚡");
        cat.setActive(true);
        WorkCategory savedCat = workCategoryRepository.save(cat);

        Division division = workCategoryService.createWorkType(savedCat.getId(), "KITCHEN ISLAND", "🏝️", "Center cooking counter");
        Long divisionId = division.getId();
        assertTrue(Boolean.TRUE.equals(division.getActive()), "Initial work type status must be Active");

        long divisionCountBefore = divisionRepository.count();

        // 3. Toggle from Active -> Inactive
        org.springframework.http.ResponseEntity<?> resp1 = (org.springframework.http.ResponseEntity<?>)
                workCategoryController.toggleWorkType(savedCat.getId(), divisionId, null);
        assertEquals(org.springframework.http.HttpStatus.OK, resp1.getStatusCode());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body1 = (java.util.Map<String, Object>) resp1.getBody();
        assertNotNull(body1);
        assertEquals(Boolean.TRUE, body1.get("success"));
        assertEquals(Boolean.FALSE, body1.get("active"), "After first toggle, work type status must be Inactive");

        // Check DB
        Division reloaded1 = divisionRepository.findById(divisionId).orElse(null);
        assertNotNull(reloaded1);
        assertFalse(reloaded1.getActive(), "Database record must reflect Inactive status");
        assertEquals(divisionCountBefore, divisionRepository.count(), "Toggling status must not create new records");

        // 4. Toggle from Inactive -> Active
        org.springframework.http.ResponseEntity<?> resp2 = (org.springframework.http.ResponseEntity<?>)
                workCategoryController.toggleWorkType(savedCat.getId(), divisionId, null);
        assertEquals(org.springframework.http.HttpStatus.OK, resp2.getStatusCode());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body2 = (java.util.Map<String, Object>) resp2.getBody();
        assertNotNull(body2);
        assertEquals(Boolean.TRUE, body2.get("success"));
        assertEquals(Boolean.TRUE, body2.get("active"), "After second toggle, work type status must be Active again");

        // Check DB
        Division reloaded2 = divisionRepository.findById(divisionId).orElse(null);
        assertNotNull(reloaded2);
        assertTrue(reloaded2.getActive(), "Database record must reflect Active status");
        assertEquals(divisionCountBefore, divisionRepository.count(), "Toggling status must not create new records");
    }

    @Test
    @DisplayName("Verify Category Emoji Picker, Emoji in Category Name, Persistence, and Update without Duplication")
    void testCategoryEmojiPickerAndNamePersistence() throws Exception {
        // 1. Verify Template UI elements
        String template = loadTemplateContent("admin/work-categories.html");
        assertTrue(template.contains("openEmojiPickerBtn"), "Template must contain openEmojiPickerBtn");
        assertTrue(template.contains("Choose Emoji"), "Template must contain 'Choose Emoji' button label");
        assertTrue(template.contains("categoryEmojiPickerPopup"), "Template must contain categoryEmojiPickerPopup");
        assertTrue(template.contains("data-category=\"KITCHEN\""), "Emoji picker must contain Kitchen category");
        assertTrue(template.contains("data-category=\"ROOMS\""), "Emoji picker must contain Rooms category");
        assertTrue(template.contains("data-category=\"FURNITURE\""), "Emoji picker must contain Furniture category");
        assertTrue(template.contains("data-category=\"STORAGE\""), "Emoji picker must contain Storage category");
        assertTrue(template.contains("data-category=\"LIGHTING\""), "Emoji picker must contain Lighting category");
        assertTrue(template.contains("data-category=\"BATHROOM\""), "Emoji picker must contain Bathroom category");
        assertTrue(template.contains("data-category=\"DECOR\""), "Emoji picker must contain Decor category");
        assertTrue(template.contains("data-category=\"CONSTRUCTION\""), "Emoji picker must contain Construction category");
        assertTrue(template.contains("data-category=\"OFFICE\""), "Emoji picker must contain Office category");
        assertTrue(template.contains("data-category=\"GENERAL\""), "Emoji picker must contain General category");

        // Verify table headers (no separate icon/emoji column)
        assertTrue(template.contains("col-sno"), "Table must contain S.No column");
        assertTrue(template.contains("col-name"), "Table must contain Category Name column");
        assertTrue(template.contains("col-status"), "Table must contain Status column");
        assertTrue(template.contains("col-actions"), "Table must contain Actions column");
        assertFalse(template.contains("<th class=\"col-icon\">"), "Table must NOT contain a separate col-icon column");
        assertFalse(template.contains("<th class=\"col-emoji\">"), "Table must NOT contain a separate col-emoji column");

        // 2. Verify emoji extraction logic for requested groups
        assertEquals("🍳", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("🍳 MODULAR KITCHEN"));
        assertEquals("🏠", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("🏠 FOYER"));
        assertEquals("🛏️", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("🛏️ BEDROOM"));
        assertEquals("🛋️", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("🛋️ LIVING ROOM"));
        assertEquals("🚪", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("🚪 DOORS & WINDOWS"));
        assertEquals("🪴", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("🪴 INTERIOR DECOR"));
        assertEquals("⭐", com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("⭐ SPECIAL CATEGORY"));
        assertNull(com.example.pravin_quotation.service.WorkCategoryService.extractLeadingEmoji("MODULAR KITCHEN"));

        // 3. Test creating category with emoji directly in category name
        String suffix = String.valueOf(System.currentTimeMillis());
        WorkCategory cat1 = workCategoryService.createCategory("🍳 MODULAR KITCHEN " + suffix, "Kitchen accessories and cabinets", null, 1);
        assertNotNull(cat1.getId());
        assertEquals("🍳 MODULAR KITCHEN " + suffix, cat1.getName());
        assertEquals("🍳", cat1.getIcon());

        WorkCategory cat2 = workCategoryService.createCategory("🏠 FOYER " + suffix, "Entrance and shoe storage", null, 2);
        assertNotNull(cat2.getId());
        assertEquals("🏠 FOYER " + suffix, cat2.getName());
        assertEquals("🏠", cat2.getIcon());

        // 4. Test updating category with a new emoji (replacing leading emoji without duplication)
        WorkCategory updatedCat1 = workCategoryService.updateCategory(cat1.getId(), "🥘 MODULAR KITCHEN " + suffix, "Updated kitchen", null, 1);
        assertEquals("🥘 MODULAR KITCHEN " + suffix, updatedCat1.getName());
        assertEquals("🥘", updatedCat1.getIcon());
        assertFalse(updatedCat1.getName().contains("🍳"), "Previous emoji must be replaced without leaving duplicates");

        // Check database reload
        WorkCategory reloaded = workCategoryRepository.findById(cat1.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals("🥘 MODULAR KITCHEN " + suffix, reloaded.getName());
        assertEquals("🥘", reloaded.getIcon());
    }

    @Autowired
    private com.example.pravin_quotation.repository.BranchRepository branchRepository;

    @Autowired
    private com.example.pravin_quotation.repository.DistrictPricingRepository districtPricingRepository;

    @Autowired
    private com.example.pravin_quotation.service.AdminQuotationBuilderService adminQuotationBuilderService;

    @Test
    @DisplayName("Verify Branch-Specific Material Pricing: Dynamic branches, distinct branch rates, strict branch lookup, error on unconfigured rate, and duplicate prevention")
    void testBranchSpecificMaterialPricing() throws Exception {
        // 1. Verify Template UI elements
        String template = loadTemplateContent("admin/work-categories.html");
        assertTrue(template.contains("branch-pricing-table"), "Template must contain branch pricing table");
        assertTrue(template.contains("branchPricingTableBody"), "Template must contain branchPricingTableBody tbody");
        assertTrue(template.contains("branchCountBadge"), "Template must contain branchCountBadge");
        assertTrue(template.contains("branchRateColumnHeader"), "Template must contain dynamic single-grade rate column header");
        assertTrue(template.contains("materials-grade-tabs"), "Template must contain top grade tabs bar");
        assertTrue(template.contains("switchGradeTab"), "Template must contain switchGradeTab function for dynamic tab switching");
        assertTrue(template.contains("syncVisibleInputsToState"), "Template must contain syncVisibleInputsToState function to preserve input values across tabs");
        assertTrue(template.contains("Premium Rate (₹)"), "Template must contain Premium rate column title");
        assertTrue(template.contains("Middle / Standard Rate (₹)"), "Template must contain Middle / Standard rate column title");
        assertTrue(template.contains("Economy Rate (₹)"), "Template must contain Economy rate column title");
        assertFalse(template.contains("branch-grade-switch-btn"), "Duplicate grade switch buttons inside pricing section must be removed");

        // 2. Ensure test branches exist
        com.example.pravin_quotation.model.Branch nagercoil = branchRepository.findByName("Nagercoil")
                .orElseGet(() -> {
                    com.example.pravin_quotation.model.Branch b = new com.example.pravin_quotation.model.Branch();
                    b.setName("Nagercoil");
                    b.setActive(true);
                    return branchRepository.save(b);
                });

        com.example.pravin_quotation.model.Branch thuckalay = branchRepository.findByName("Thuckalay")
                .orElseGet(() -> {
                    com.example.pravin_quotation.model.Branch b = new com.example.pravin_quotation.model.Branch();
                    b.setName("Thuckalay");
                    b.setActive(true);
                    return branchRepository.save(b);
                });

        com.example.pravin_quotation.model.Branch marthandam = branchRepository.findByName("Marthandam")
                .orElseGet(() -> {
                    com.example.pravin_quotation.model.Branch b = new com.example.pravin_quotation.model.Branch();
                    b.setName("Marthandam");
                    b.setActive(true);
                    return branchRepository.save(b);
                });

        // 3. Create a work category and work type
        String bpSuffix = String.valueOf(System.currentTimeMillis());
        WorkCategory cat = workCategoryService.createCategory("🍳 KITCHEN PRICING TEST " + bpSuffix, "Kitchen for branch test", null, 1);
        Division workType = workCategoryService.createWorkType(cat.getId(), "BASE UNITS " + bpSuffix, "Base units test", "🗄️");

        // 4. Verify getWorkTypeMaterials returns dynamic branches from MySQL
        java.util.Map<String, Object> workTypeData = workCategoryService.getWorkTypeMaterials(workType.getId());
        assertNotNull(workTypeData.get("branches"), "getWorkTypeMaterials must return branches list");
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> returnedBranches = (java.util.List<java.util.Map<String, Object>>) workTypeData.get("branches");
        assertFalse(returnedBranches.isEmpty(), "Returned branches must not be empty");
        assertTrue(returnedBranches.stream().anyMatch(b -> "Nagercoil".equals(b.get("name"))), "Nagercoil branch must be present");
        assertTrue(returnedBranches.stream().anyMatch(b -> "Thuckalay".equals(b.get("name"))), "Thuckalay branch must be present");

        // 5. Build branch rates map:
        // Nagercoil: Premium 250, Middle 200, Economy 150
        // Thuckalay: Premium 260, Middle 210, Economy 160
        // Marthandam: unconfigured initially
        java.util.Map<Long, java.util.Map<PricingMode, BigDecimal>> branchRates = new java.util.LinkedHashMap<>();
        java.util.Map<PricingMode, BigDecimal> nglRates = new java.util.LinkedHashMap<>();
        nglRates.put(PricingMode.PREMIUM, new BigDecimal("250.00"));
        nglRates.put(PricingMode.MIDDLE, new BigDecimal("200.00"));
        nglRates.put(PricingMode.ECONOMY, new BigDecimal("150.00"));
        branchRates.put(nagercoil.getId(), nglRates);

        java.util.Map<PricingMode, BigDecimal> tckRates = new java.util.LinkedHashMap<>();
        tckRates.put(PricingMode.PREMIUM, new BigDecimal("260.00"));
        tckRates.put(PricingMode.MIDDLE, new BigDecimal("210.00"));
        tckRates.put(PricingMode.ECONOMY, new BigDecimal("160.00"));
        branchRates.put(thuckalay.getId(), tckRates);

        com.example.pravin_quotation.model.Material material = workCategoryService.createWorkTypeMaterial(
                workType.getId(),
                PricingMode.PREMIUM,
                "BWP Plywood",
                "IS:710 Marine Grade",
                "Sq.ft",
                new BigDecimal("250.00"),
                "L × W",
                branchRates
        );

        assertNotNull(material.getId(), "Material must be saved with an ID");

        // 6. Test Rate lookup by branch and grade
        // Nagercoil Premium should be 250
        BigDecimal nglPremRate = adminQuotationBuilderService.resolveRate(nagercoil.getId(), material.getId(), null, PricingMode.PREMIUM);
        assertEquals(new BigDecimal("250.00"), nglPremRate, "Nagercoil Premium rate must be exactly 250.00");

        // Thuckalay Premium should be 260 (different from Nagercoil)
        BigDecimal tckPremRate = adminQuotationBuilderService.resolveRate(thuckalay.getId(), material.getId(), null, PricingMode.PREMIUM);
        assertEquals(new BigDecimal("260.00"), tckPremRate, "Thuckalay Premium rate must be exactly 260.00");
        assertNotEquals(nglPremRate, tckPremRate, "Nagercoil and Thuckalay must have distinct rates");

        // Nagercoil Middle should be 200
        BigDecimal nglMidRate = adminQuotationBuilderService.resolveRate(nagercoil.getId(), material.getId(), null, PricingMode.MIDDLE);
        assertEquals(new BigDecimal("200.00"), nglMidRate);

        // Thuckalay Middle should be 210
        BigDecimal tckMidRate = adminQuotationBuilderService.resolveRate(thuckalay.getId(), material.getId(), null, PricingMode.MIDDLE);
        assertEquals(new BigDecimal("210.00"), tckMidRate);

        // 7. Verify unconfigured branch rate throws clear validation message and does NOT silently default or substitute
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminQuotationBuilderService.resolveRate(marthandam.getId(), material.getId(), null, PricingMode.PREMIUM);
        });
        assertTrue(ex.getMessage().contains("No rate has been configured for branch 'Marthandam'"),
                "Validation message must clearly mention branch name: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("PREMIUM"),
                "Validation message must clearly mention pricing grade: " + ex.getMessage());

        // 8. Test Edit Material updates branch rates without duplicate pricing records
        long initialDistrictPricingCount = districtPricingRepository.count();

        // Update Thuckalay rate to 275 and configure Marthandam rate 255
        java.util.Map<PricingMode, BigDecimal> mrmRates = new java.util.LinkedHashMap<>();
        mrmRates.put(PricingMode.PREMIUM, new BigDecimal("255.00"));
        mrmRates.put(PricingMode.MIDDLE, new BigDecimal("205.00"));
        mrmRates.put(PricingMode.ECONOMY, new BigDecimal("155.00"));
        branchRates.put(marthandam.getId(), mrmRates);

        tckRates.put(PricingMode.PREMIUM, new BigDecimal("275.00"));

        com.example.pravin_quotation.model.Material updatedMat = workCategoryService.updateWorkTypeMaterial(
                material.getId(),
                "BWP Plywood Superior",
                "IS:710 Marine Grade Updated",
                "Sq.ft",
                new BigDecimal("250.00"),
                "L × W",
                branchRates
        );

        assertEquals("BWP Plywood Superior", updatedMat.getName());

        // Check updated rates
        BigDecimal updatedTckRate = adminQuotationBuilderService.resolveRate(thuckalay.getId(), material.getId(), null, PricingMode.PREMIUM);
        assertEquals(new BigDecimal("275.00"), updatedTckRate, "Thuckalay rate must be updated to 275.00");

        BigDecimal newMrmRate = adminQuotationBuilderService.resolveRate(marthandam.getId(), material.getId(), null, PricingMode.PREMIUM);
        assertEquals(new BigDecimal("255.00"), newMrmRate, "Marthandam rate must now be 255.00");

        // Verify no duplicate records created for existing branches
        long finalDistrictPricingCount = districtPricingRepository.count();
        // Added 3 records for Marthandam (PREMIUM, MIDDLE, ECONOMY) and 0 duplicates for NGL/TCK
        assertEquals(initialDistrictPricingCount + 3, finalDistrictPricingCount,
                "Only newly configured branch pricing records should be added without duplicates");
    }
}

