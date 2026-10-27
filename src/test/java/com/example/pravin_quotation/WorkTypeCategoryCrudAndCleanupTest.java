package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.DivisionController;
import com.example.pravin_quotation.controller.MaterialController;
import com.example.pravin_quotation.controller.MaterialOptionController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WorkTypeCategoryCrudAndCleanupTest {

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private DivisionController divisionController;

    @Autowired
    private MaterialController materialController;

    @Autowired
    private MaterialOptionController materialOptionController;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @DisplayName("Create work type under category with emoji, name, and description")
    void testCreateWorkTypeUnderCategory() {
        WorkCategory category = workCategoryService.createCategory(
                "TEST MODULAR KITCHEN", "Kitchen units", "🍳", 1
        );
        assertNotNull(category.getId());

        Division workType = workCategoryService.createWorkType(
                category.getId(), "Bottom Cabinet", "🗄️", "Base cabinet with soft close"
        );
        assertNotNull(workType.getId());
        assertEquals("BOTTOM CABINET", workType.getName());
        assertEquals("🗄️", workType.getIcon());
        assertEquals("🗄️", workType.getEmoji());
        assertEquals("Base cabinet with soft close", workType.getDescription());
        assertEquals(category.getId(), workType.getWorkCategory().getId());
        assertTrue(workType.getActive());

        Map<String, Object> data = workCategoryService.getCategoryWorkTypes(category.getId());
        assertTrue((Boolean) data.get("success"));
        assertEquals(1, data.get("totalCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("workTypes");
        assertEquals(1, list.size());
        assertEquals("BOTTOM CABINET", list.get(0).get("name"));
        assertEquals("🗄️", list.get(0).get("emoji"));
    }

    @Test
    @DisplayName("Duplicate work type names prevented within the same category")
    void testDuplicateWorkTypeNamePreventedUnderSameCategory() {
        WorkCategory category = workCategoryService.createCategory(
                "BEDROOM SUITE", "Bedroom items", "🛏️", 2
        );

        workCategoryService.createWorkType(category.getId(), "WARDROBE", "🚪", "Standard wardrobe");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                workCategoryService.createWorkType(category.getId(), "wardrobe", "🚪", "Another wardrobe")
        );
        assertTrue(ex.getMessage().contains("already exists in this category"));
    }

    @Test
    @DisplayName("Different categories can have work types with the same name")
    void testDifferentCategoriesCanHaveSameWorkTypeName() {
        WorkCategory cat1 = workCategoryService.createCategory("LIVING ROOM A", "Living area", "🛋️", 1);
        WorkCategory cat2 = workCategoryService.createCategory("BEDROOM B", "Bedroom area", "🛏️", 2);

        Division wt1 = workCategoryService.createWorkType(cat1.getId(), "LEDGE", "🪜", "Living ledge");
        Division wt2 = workCategoryService.createWorkType(cat2.getId(), "LEDGE", "🪜", "Bedroom ledge");

        assertNotNull(wt1.getId());
        assertNotNull(wt2.getId());
        assertNotEquals(wt1.getId(), wt2.getId());
        assertEquals(cat1.getId(), wt1.getWorkCategory().getId());
        assertEquals(cat2.getId(), wt2.getWorkCategory().getId());
    }

    @Test
    @DisplayName("Update work type updates existing record in place")
    void testUpdateWorkType() {
        WorkCategory category = workCategoryService.createCategory("KITCHEN STORAGE", "Storage items", "🍳", 1);
        Division created = workCategoryService.createWorkType(category.getId(), "LOFT", "🏠", "Original loft");
        Long id = created.getId();

        Division updated = workCategoryService.updateWorkType(
                category.getId(), id, "OVERHEAD LOFT", "📦", "Updated overhead loft description"
        );

        assertEquals(id, updated.getId());
        assertEquals("OVERHEAD LOFT", updated.getName());
        assertEquals("📦", updated.getIcon());
        assertEquals("Updated overhead loft description", updated.getDescription());

        Division reloaded = divisionRepository.findById(id).orElseThrow();
        assertEquals("OVERHEAD LOFT", reloaded.getName());
        assertEquals("📦", reloaded.getIcon());
    }

    @Test
    @DisplayName("Safe delete deactivates work type when used in quotations")
    void testSafeDeleteWorkTypeWhenUsedInQuotations() {
        long timestamp = System.currentTimeMillis();

        Customer customer = new Customer();
        customer.setName("WT Cust " + timestamp);
        customer.setPhone("9812345670");
        customer = customerRepository.save(customer);

        Branch branch = new Branch();
        branch.setName("WT Branch " + timestamp);
        branch.setBranchAddress("Address");
        branch = branchRepository.save(branch);

        User user = userRepository.findAll().stream().findFirst().orElseThrow();

        WorkCategory category = workCategoryService.createCategory("SAFE DELETE CAT " + timestamp, "Desc", "🍳", 1);
        Division workType = workCategoryService.createWorkType(category.getId(), "I-LAND CABINET", "🏝️", "Island");

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-WT-" + timestamp);
        q.setCustomer(customer);
        q.setBranch(branch);
        q.setEmployee(user);
        q.setDivision(workType);
        q.setPricingMode(PricingMode.PREMIUM);
        q.setQuotationDate(java.time.LocalDate.now());
        q.setStatus(QuotationStatus.PENDING);
        q.setSubtotal(new BigDecimal("5000.00"));
        q.setGrandTotal(new BigDecimal("5900.00"));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setRoom("Kitchen");
        room.setFloor("Ground Floor");
        room.setSubtotal(new BigDecimal("5000.00"));
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setDivision(workType);
        item.setItemDescription("Custom Island item");
        item.setActive(true);
        quotationItemRepository.save(item);

        String message = workCategoryService.deleteWorkType(category.getId(), workType.getId());

        assertTrue(message.contains("safely deactivated") || message.contains("referenced by existing quotations"));
        Division deactivated = divisionRepository.findById(workType.getId()).orElseThrow();
        assertFalse(deactivated.getActive());
        assertNotNull(deactivated.getWorkCategory());
    }

    @Test
    @DisplayName("Delete work type permanently removes record when not used in quotations")
    void testDeleteWorkTypeWhenNotUsedInQuotations() {
        WorkCategory category = workCategoryService.createCategory("UNUSED CAT", "Desc", "🍳", 1);
        Division workType = workCategoryService.createWorkType(category.getId(), "SPICE RACK", "🪜", "Spice rack unit");
        Long id = workType.getId();

        String message = workCategoryService.deleteWorkType(category.getId(), id);

        assertTrue(message.contains("deleted successfully"));
        assertTrue(divisionRepository.findById(id).isEmpty());
    }

    @Test
    @DisplayName("Old master data management routes redirect to /admin/work-categories")
    void testOldMasterDataSectionsRedirectToWorkCategories() {
        assertEquals("redirect:/admin/work-categories", divisionController.divisions(new ConcurrentModel()));
        assertEquals("redirect:/admin/work-categories", materialController.materials(new ConcurrentModel()));
        assertEquals("redirect:/admin/work-categories", materialOptionController.options(new ConcurrentModel()));
    }

    @Test
    @DisplayName("Quotation builder returns work types with emoji and falls back to active materials")
    void testQuotationBuilderIntegration() {
        WorkCategory category = workCategoryService.createCategory("BUILDER TEST CAT", "Desc", "🍳", 1);
        Division wt = workCategoryService.createWorkType(category.getId(), "BREAKFAST TABLE", "🪑", "Breakfast table");

        List<Map<String, Object>> builderDivisions = adminQuotationBuilderController.divisions(category.getId());
        assertFalse(builderDivisions.isEmpty());
        Map<String, Object> found = builderDivisions.stream()
                .filter(m -> wt.getId().equals(m.get("id")))
                .findFirst().orElseThrow();

        assertEquals("BREAKFAST TABLE", found.get("name"));
        assertEquals("🪑", found.get("emoji"));

        List<Map<String, Object>> materials = adminQuotationBuilderController.materials(wt.getId(), null);
        assertNotNull(materials);
    }

    @Test
    @DisplayName("WorkCategoryController AJAX endpoints support Work Type CRUD")
    void testWorkCategoryControllerWorkTypeEndpoints() {
        WorkCategory cat = workCategoryService.createCategory("AJAX CAT", "Desc", "🍳", 1);

        ResponseEntity<?> saveResp = (ResponseEntity<?>) workCategoryController.saveWorkType(
                cat.getId(), null, null, "HEADBOARD", "🛏️", null, "Bedroom headboard"
        );
        assertEquals(200, saveResp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> saveBody = (Map<String, Object>) saveResp.getBody();
        assertTrue((Boolean) saveBody.get("success"));
        Long newId = ((Number) saveBody.get("workTypeId")).longValue();

        ResponseEntity<?> updateResp = (ResponseEntity<?>) workCategoryController.updateWorkType(
                cat.getId(), newId, null, "UPHOLSTERED HEADBOARD", "🛏️", null, "Updated headboard"
        );
        assertEquals(200, updateResp.getStatusCode().value());

        ResponseEntity<?> deleteResp = (ResponseEntity<?>) workCategoryController.deleteWorkType(
                cat.getId(), newId, null
        );
        assertEquals(200, deleteResp.getStatusCode().value());
    }

    @Test
    @DisplayName("HTML templates contain work type management UI and remove obsolete workflow steps")
    void testTemplateContents() throws Exception {
        Resource catRes = resourceLoader.getResource("classpath:templates/admin/work-categories.html");
        assertTrue(catRes.exists());
        String catHtml;
        try (InputStream in = catRes.getInputStream()) {
            catHtml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(catHtml.contains("openAddWorkTypeBtn"));
        assertTrue(catHtml.contains("workTypeFormContainer"));
        assertTrue(catHtml.contains("workTypeNameInput"));
        assertTrue(catHtml.contains("saveWorkTypeBtn"));
        assertTrue(catHtml.contains("js-edit-work-type-btn"));
        assertTrue(catHtml.contains("js-delete-work-type-btn"));

        Resource formulaRes = resourceLoader.getResource("classpath:templates/admin/formula-calculation.html");
        assertTrue(formulaRes.exists());
        String formulaHtml;
        try (InputStream in = formulaRes.getInputStream()) {
            formulaHtml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertFalse(formulaHtml.contains("2. Divisions"));
        assertFalse(formulaHtml.contains("3. Materials"));
        assertFalse(formulaHtml.contains("4. Material Options"));
    }
}
