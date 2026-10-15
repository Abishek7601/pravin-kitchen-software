package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.MasterDataHierarchyController;
import com.example.pravin_quotation.controller.MaterialController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WorkCategoryMaterialDeleteTest {

    @Autowired
    private MaterialController materialController;

    @Autowired
    private MasterDataHierarchyController masterDataHierarchyController;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private MaterialSpecificationRepository materialSpecificationRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @DisplayName("Req 1 & 3: Template contains Delete button, form action, CSRF token, and confirmation dialog")
    public void testTemplateContainsDeleteButtonAndConfirmation() throws Exception {
        Resource res = resourceLoader.getResource("classpath:templates/admin/work-category-view.html");
        assertTrue(res.exists(), "work-category-view.html must exist");

        String html;
        try (InputStream is = res.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(html.contains("/admin/materials/delete/"), "Must contain delete form action pointing to /admin/materials/delete/");
        assertTrue(html.contains("btn-action-danger"), "Must contain btn-action-danger class for delete button");
        assertTrue(html.contains("Delete"), "Must contain Delete text");
        assertTrue(html.contains("confirm("), "Must contain onsubmit confirmation dialog");
        assertTrue(html.contains("data-material-name"), "Must contain material name attribute for confirmation");
        assertTrue(html.contains("redirectUrl"), "Must contain redirectUrl input to return to the View page");
        assertTrue(html.contains("Edit Material"), "Existing Edit Material option must be preserved");
    }

    @Test
    @DisplayName("Req 2, 4, 6: Delete unwanted material permanently from DB and refresh view page without affecting division or category")
    public void testDeleteUnwantedMaterialPermanently() {
        long timestamp = System.currentTimeMillis();
        String catName = "TEST CAT " + timestamp;

        // Create category, division, and two materials
        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🏠",
                null, "TEST DIV " + timestamp,
                null, "UNWANTED MAT " + timestamp,
                null, "OPT 1",
                "SPEC 1",
                PricingMode.PREMIUM,
                new BigDecimal("1200.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Unwanted material"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material unwantedMat = materialRepository.findActiveByDivisionId(div.getId()).get(0);

        // Add a second material under the same division
        pricingService.saveUnifiedMasterData(
                null,
                cat.getId(), catName, "🏠",
                div.getId(), div.getName(),
                null, "KEEP MAT " + timestamp,
                null, "OPT 2",
                "SPEC 2",
                PricingMode.PREMIUM,
                new BigDecimal("1500.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Material to keep"
        );

        List<Material> initialMats = materialRepository.findActiveByDivisionId(div.getId());
        assertEquals(2, initialMats.size(), "Division should initially have 2 materials");

        // Delete Unwanted Material directly from View page context
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirectUrl = "/admin/work-categories/view/" + cat.getId();

        Object result = materialController.deleteMaterial(unwantedMat.getId(), redirectUrl, request, redirectAttributes);
        assertEquals("redirect:" + redirectUrl, result, "Should redirect directly back to the work category view page");
        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        Object flashError = redirectAttributes.getFlashAttributes().get("errorMessage");
        if (flashSuccess == null) {
            fail("Delete failed with error: " + flashError);
        }
        assertTrue(flashSuccess.toString().contains("deleted successfully"));

        // Verify Unwanted Material is PERMANENTLY deleted from DB
        assertTrue(materialRepository.findById(unwantedMat.getId()).isEmpty(), "Unwanted material must be permanently removed from DB");

        // Verify remaining material, division, and work category stay intact
        assertEquals(1, materialRepository.findActiveByDivisionId(div.getId()).size(), "Only 1 material should remain");
        assertEquals("KEEP MAT " + timestamp, materialRepository.findActiveByDivisionId(div.getId()).get(0).getName());
        assertTrue(divisionRepository.findById(div.getId()).isPresent(), "Division must NOT be deleted");
        assertTrue(workCategoryRepository.findById(cat.getId()).isPresent(), "Work Category must NOT be deleted");

        // Verify refreshed view model contains only the remaining material
        Model viewModel = new ConcurrentModel();
        String viewName = masterDataHierarchyController.viewCategoryDetails(cat.getId(), viewModel);
        assertEquals("admin/work-category-view", viewName);

        MasterDataHierarchyController.HierarchyCategoryDto catDto =
                (MasterDataHierarchyController.HierarchyCategoryDto) viewModel.getAttribute("catDto");
        assertNotNull(catDto);
        List<MasterDataHierarchyController.HierarchyMaterialDto> displayedMaterials = catDto.getDivisions().get(0).getMaterials();
        assertEquals(1, displayedMaterials.size(), "View page must display only the remaining material");
        assertEquals("KEEP MAT " + timestamp, displayedMaterials.get(0).getMaterial().getName());
    }

    @Test
    @DisplayName("Req 5: Material linked to quotation is safely deactivated and removed from view, preserving quotation data")
    public void testDeleteMaterialUsedInQuotationSafelyDeactivatesAndPreservesQuotation() {
        long timestamp = System.currentTimeMillis();
        String catName = "QUOTE CAT " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "📦",
                null, "QUOTE DIV " + timestamp,
                null, "QUOTED MAT " + timestamp,
                null, "QUOTED OPT",
                "QUOTED SPEC",
                PricingMode.PREMIUM,
                new BigDecimal("2000.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Material used in quotation"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material quotedMat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(quotedMat.getId()).get(0);

        // Create Customer, Branch, Quotation, and QuotationItem referencing this material
        Customer customer = new Customer();
        customer.setName("Test Customer " + timestamp);
        customer.setPhone("9876543210");
        customer = customerRepository.save(customer);

        Branch branch = new Branch();
        branch.setName("Test Branch " + timestamp);
        branch.setBranchAddress("123 Test Street");
        branch = branchRepository.save(branch);

        User user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = new User();
            u.setName("Admin User");
            u.setEmail("admin_" + timestamp + "@test.com");
            u.setPassword("pass");
            u.setRole(Role.SUPER_ADMIN);
            return userRepository.save(u);
        });

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-DEL-" + timestamp);
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setEmployee(user);
        quotation.setDivision(div);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setSubtotal(new BigDecimal("10000.00"));
        quotation.setGrandTotal(new BigDecimal("11800.00"));
        quotation = quotationRepository.save(quotation);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(quotation);
        room.setRoom("Living Room");
        room.setFloor("Ground Floor");
        room.setSubtotal(new BigDecimal("10000.00"));
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(cat);
        item.setDivision(div);
        item.setMaterial(quotedMat);
        item.setMaterialOption(opt);
        item.setItemDescription("Custom unit");
        item.setRate(new BigDecimal("2000.00"));
        item.setAmount(new BigDecimal("10000.00"));
        quotationItemRepository.save(item);

        // Now attempt to delete this material via MaterialController
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirectUrl = "/admin/work-categories/view/" + cat.getId();

        Object result = materialController.deleteMaterial(quotedMat.getId(), redirectUrl, request, redirectAttributes);
        assertEquals("redirect:" + redirectUrl, result);
        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        Object flashError = redirectAttributes.getFlashAttributes().get("errorMessage");
        assertNotNull(flashSuccess, "Expected flash successMessage, but got flash error: " + flashError);
        assertTrue(flashSuccess.toString().contains("archived")
                || flashSuccess.toString().contains("deactivated"));

        // Verify material was NOT deleted from DB, but deactivated (active = false)
        Material reloadedMat = materialRepository.findById(quotedMat.getId()).orElseThrow();
        assertFalse(reloadedMat.getActive(), "Material must be deactivated/soft-deleted to preserve quotation history");

        // Verify historical quotation data remains intact
        Quotation loadedQuotation = quotationRepository.findById(quotation.getId()).orElseThrow();
        assertEquals("QT-DEL-" + timestamp, loadedQuotation.getQuotationNumber());
        List<QuotationItem> loadedItems = quotationItemRepository.findByQuotationRoomIdOrderByIdAsc(room.getId());
        assertEquals(1, loadedItems.size());
        assertEquals(quotedMat.getId(), loadedItems.get(0).getMaterial().getId(), "Quotation item material link must be intact");

        // Verify PDF generation does not fail
        byte[] pdf = quotationPdfService.generateQuotationPdf(quotation.getId());
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        // Verify refreshed view page removes the deactivated material from displayed list
        Model viewModel = new ConcurrentModel();
        masterDataHierarchyController.viewCategoryDetails(cat.getId(), viewModel);
        MasterDataHierarchyController.HierarchyCategoryDto catDto =
                (MasterDataHierarchyController.HierarchyCategoryDto) viewModel.getAttribute("catDto");
        assertNotNull(catDto);
        List<MasterDataHierarchyController.HierarchyMaterialDto> displayedMaterials = catDto.getDivisions().get(0).getMaterials();
        assertTrue(displayedMaterials.isEmpty(), "View page must not display deactivated material");
    }

    @Test
    @DisplayName("Req 2 & 7: AJAX / JSON delete request returns success JSON response")
    public void testDeleteMaterialJsonApi() {
        long timestamp = System.currentTimeMillis();
        String catName = "JSON CAT " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "📄",
                null, "JSON DIV " + timestamp,
                null, "JSON MAT " + timestamp,
                null, "JSON OPT",
                "JSON SPEC",
                PricingMode.PREMIUM,
                new BigDecimal("1000.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "JSON API test"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = materialController.deleteMaterial(mat.getId(), null, request, redirectAttributes);
        assertTrue(result instanceof ResponseEntity<?>);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map<?, ?>);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
        assertTrue(body.get("message").toString().contains("deleted successfully"));
    }
}
