package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.DivisionController;
import com.example.pravin_quotation.controller.MasterDataHierarchyController;
import com.example.pravin_quotation.controller.MaterialOptionController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.WorkCategoryService;
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
public class MasterDataDeleteOperationsTest {

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private DivisionController divisionController;

    @Autowired
    private MaterialOptionController materialOptionController;

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
    private PricingRepository pricingRepository;

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
    @DisplayName("Verify UI Templates contain Delete buttons and confirmation with item name")
    public void testUiTemplatesContainDeleteButtonsAndConfirmations() throws Exception {
        // 1. work-categories.html
        Resource wcRes = resourceLoader.getResource("classpath:templates/admin/work-categories.html");
        assertTrue(wcRes.exists());
        String wcHtml = new String(wcRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(wcHtml.contains("/admin/work-categories/delete/"), "Must contain delete action pointing to /admin/work-categories/delete/");
        assertTrue(wcHtml.contains("confirmDeleteCategory"), "Must contain confirmDeleteCategory handler");
        assertTrue(wcHtml.contains("data-category-name"), "Must pass category name to confirmation");

        // 2. divisions.html
        Resource divRes = resourceLoader.getResource("classpath:templates/admin/divisions.html");
        assertTrue(divRes.exists());
        String divHtml = new String(divRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(divHtml.contains("/admin/divisions/delete/"), "Must contain delete action pointing to /admin/divisions/delete/");
        assertTrue(divHtml.contains("confirmDeleteDivision"), "Must contain confirmDeleteDivision handler");
        assertTrue(divHtml.contains("data-division-name"), "Must pass division name to confirmation");

        // 3. material-options.html
        Resource optRes = resourceLoader.getResource("classpath:templates/admin/material-options.html");
        assertTrue(optRes.exists());
        String optHtml = new String(optRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(optHtml.contains("/admin/material-options/delete/"), "Must contain delete action pointing to /admin/material-options/delete/");
        assertTrue(optHtml.contains("data-option-name"), "Must pass option name to confirmation");

        // 4. work-category-view.html
        Resource viewRes = resourceLoader.getResource("classpath:templates/admin/work-category-view.html");
        assertTrue(viewRes.exists());
        String viewHtml = new String(viewRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(viewHtml.contains("/admin/divisions/delete/"), "work-category-view.html must contain Division Delete action");
        assertTrue(viewHtml.contains("/admin/material-options/delete/"), "work-category-view.html must contain Material Option Delete action");
        assertTrue(viewHtml.contains("/admin/materials/delete/"), "work-category-view.html must contain Material Delete action");
    }

    @Test
    @DisplayName("Work Category: Safe permanent deletion when no quotations reference it")
    public void testDeleteWorkCategoryPermanently() {
        long timestamp = System.currentTimeMillis();
        String catName = "TEST CAT DEL " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🏷️",
                null, "TEST DIV DEL " + timestamp,
                null, "TEST MAT DEL " + timestamp,
                null, "OPT DEL 1",
                "SPEC DEL 1",
                PricingMode.PREMIUM,
                new BigDecimal("1500.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Test cat deletion"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Long catId = cat.getId();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = workCategoryController.deleteCategory(catId, request, redirectAttributes);
        assertEquals("redirect:/admin/work-categories", result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        Object flashError = redirectAttributes.getFlashAttributes().get("errorMessage");
        assertNotNull(flashSuccess, "Expected flash successMessage, but got error: " + flashError);
        assertTrue(flashSuccess.toString().contains("permanently deleted"));

        assertTrue(workCategoryRepository.findById(catId).isEmpty(), "Category must be permanently removed from DB");
        assertFalse(workCategoryService.getAllCategories().stream().anyMatch(c -> c.getId().equals(catId)),
                "Permanently deleted category must not appear in normal categories list");
    }

    @Test
    @DisplayName("Work Category: Inactive status alone does not block permanent deletion if never used in quotations")
    public void testDeleteInactiveWorkCategoryWithZeroQuotationsPermanentlyDeletes() {
        long timestamp = System.currentTimeMillis();
        String catName = "INACTIVE CAT DEL " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🏷️",
                null, "INACTIVE DIV DEL " + timestamp,
                null, "INACTIVE MAT DEL " + timestamp,
                null, "INACTIVE OPT DEL",
                "SPEC DEL 1",
                PricingMode.PREMIUM,
                new BigDecimal("1500.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Test inactive cat deletion"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Long catId = cat.getId();

        // Deactivate category
        cat.setActive(false);
        workCategoryRepository.saveAndFlush(cat);
        assertFalse(workCategoryRepository.findById(catId).orElseThrow().getActive());

        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = workCategoryController.deleteCategory(catId, request, redirectAttributes);
        assertEquals("redirect:/admin/work-categories", result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        assertNotNull(flashSuccess);
        assertTrue(flashSuccess.toString().contains("permanently deleted"));

        assertTrue(workCategoryRepository.findById(catId).isEmpty(), "Inactive category with 0 quotations must be permanently removed from DB");
        assertFalse(workCategoryService.getAllCategories().stream().anyMatch(c -> c.getId().equals(catId)),
                "Permanently deleted category must not appear in normal categories list");
    }

    @Test
    @DisplayName("Work Category: Soft-delete archive when referenced in quotation")
    public void testDeleteWorkCategoryReferencedInQuotationSafelyArchives() {
        long timestamp = System.currentTimeMillis();
        String catName = "QUOTE CAT DEL " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🏢",
                null, "QUOTE DIV DEL " + timestamp,
                null, "QUOTE MAT DEL " + timestamp,
                null, "QUOTE OPT DEL",
                "QUOTE SPEC DEL",
                PricingMode.PREMIUM,
                new BigDecimal("2200.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Test quotation category"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()).get(0);

        // Create quotation referencing this category
        Customer cust = new Customer();
        cust.setName("Cat Cust " + timestamp);
        cust.setPhone("9812345670");
        cust = customerRepository.save(cust);

        Branch br = new Branch();
        br.setName("Cat Branch " + timestamp);
        br.setBranchAddress("Address");
        br = branchRepository.save(br);

        User u = userRepository.findAll().stream().findFirst().orElseThrow();

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-CAT-" + timestamp);
        q.setCustomer(cust);
        q.setBranch(br);
        q.setEmployee(u);
        q.setDivision(div);
        q.setPricingMode(PricingMode.PREMIUM);
        q.setQuotationDate(LocalDate.now());
        q.setStatus(QuotationStatus.PENDING);
        q.setSubtotal(new BigDecimal("5000.00"));
        q.setGrandTotal(new BigDecimal("5900.00"));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setRoom("Office");
        room.setFloor("1st Floor");
        room.setSubtotal(new BigDecimal("5000.00"));
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(cat);
        item.setDivision(div);
        item.setMaterial(mat);
        item.setMaterialOption(opt);
        item.setItemDescription("Test item");
        item.setRate(new BigDecimal("2200.00"));
        item.setAmount(new BigDecimal("5000.00"));
        quotationItemRepository.save(item);

        // Attempt delete
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = workCategoryController.deleteCategory(cat.getId(), request, redirectAttributes);
        assertEquals("redirect:/admin/work-categories", result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        assertNotNull(flashSuccess);
        assertTrue(flashSuccess.toString().contains("referenced by existing quotations and cannot be permanently deleted"));
        assertTrue(flashSuccess.toString().contains("archived"));

        // Category must not be permanently removed, but archived = true and active = false
        WorkCategory reloaded = workCategoryRepository.findById(cat.getId()).orElseThrow();
        assertTrue(reloaded.isArchived(), "Category must be marked as archived");
        assertFalse(reloaded.getActive(), "Archived category must have active = false");

        // Verify category is removed from normal categories list
        assertFalse(workCategoryService.getAllCategories().stream().anyMatch(c -> c.getId().equals(cat.getId())),
                "Archived category must be removed from normal categories list");

        // Verify category is accessible in archived categories list
        assertTrue(workCategoryService.getArchivedCategories().stream().anyMatch(c -> c.getId().equals(cat.getId())),
                "Archived category must be present in archived categories list");

        // Verify PDF generation works
        byte[] pdf = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Work Category: Activate/Deactivate toggle is separate from Delete and keeps category in normal list")
    public void testToggleWorkCategoryKeepsCategoryInNormalList() {
        long timestamp = System.currentTimeMillis();
        String catName = "TOGGLE CAT " + timestamp;

        WorkCategory cat = workCategoryService.createCategory(catName, "Toggle test", "📁", 100);
        Long catId = cat.getId();

        assertTrue(cat.getActive());
        assertFalse(cat.isArchived());
        assertTrue(workCategoryService.getAllCategories().stream().anyMatch(c -> c.getId().equals(catId)));

        // 1. Toggle to Inactive
        workCategoryController.toggleCategory(catId);
        WorkCategory toggled = workCategoryRepository.findById(catId).orElseThrow();
        assertFalse(toggled.getActive(), "Status must now be Inactive");
        assertFalse(toggled.isArchived(), "Archived must remain false");
        assertTrue(workCategoryService.getAllCategories().stream().anyMatch(c -> c.getId().equals(catId)),
                "Inactive category must still remain in the normal categories list");

        // 2. Toggle back to Active
        workCategoryController.toggleCategory(catId);
        WorkCategory reactivated = workCategoryRepository.findById(catId).orElseThrow();
        assertTrue(reactivated.getActive(), "Status must now be Active");
        assertFalse(reactivated.isArchived(), "Archived must remain false");
        assertTrue(workCategoryService.getAllCategories().stream().anyMatch(c -> c.getId().equals(catId)),
                "Active category must remain in the normal categories list");
    }

    @Test
    @DisplayName("Division: Safe permanent deletion when no quotations exist")
    public void testDeleteDivisionPermanently() {
        long timestamp = System.currentTimeMillis();
        String catName = "PARENT CAT " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🏠",
                null, "DELETE DIV " + timestamp,
                null, "DIV MAT " + timestamp,
                null, "DIV OPT",
                "DIV SPEC",
                PricingMode.PREMIUM,
                new BigDecimal("1100.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Test division permanent delete"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Long divId = div.getId();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirectUrl = "/admin/work-categories/view/" + cat.getId();

        Object result = divisionController.deleteDivision(divId, redirectUrl, request, redirectAttributes);
        assertEquals("redirect:" + redirectUrl, result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        Object flashError = redirectAttributes.getFlashAttributes().get("errorMessage");
        assertNotNull(flashSuccess, "Expected flash successMessage, but got error: " + flashError);
        assertTrue(flashSuccess.toString().contains("deleted successfully"));

        assertTrue(divisionRepository.findById(divId).isEmpty(), "Division must be permanently removed from DB");

        // Verify category is still present
        assertTrue(workCategoryRepository.findById(cat.getId()).isPresent(), "Parent category must remain intact");

        // Verify view page no longer displays the division
        Model viewModel = new ConcurrentModel();
        masterDataHierarchyController.viewCategoryDetails(cat.getId(), viewModel);
        MasterDataHierarchyController.HierarchyCategoryDto catDto =
                (MasterDataHierarchyController.HierarchyCategoryDto) viewModel.getAttribute("catDto");
        assertNotNull(catDto);
        assertTrue(catDto.getDivisions().isEmpty(), "Division must not appear in view page hierarchy");
    }

    @Test
    @DisplayName("Division: Soft-delete archive when referenced in quotation")
    public void testDeleteDivisionReferencedInQuotationSafelyArchives() {
        long timestamp = System.currentTimeMillis();
        String catName = "DIV QUOTE CAT " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "🏢",
                null, "QUOTED DIV " + timestamp,
                null, "QUOTED DIV MAT " + timestamp,
                null, "OPT",
                "SPEC",
                PricingMode.PREMIUM,
                new BigDecimal("1800.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Quoted division test"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);

        // Create quotation referencing division
        Customer cust = new Customer();
        cust.setName("Div Cust " + timestamp);
        cust.setPhone("9812345671");
        cust = customerRepository.save(cust);

        Branch br = new Branch();
        br.setName("Div Branch " + timestamp);
        br.setBranchAddress("Address");
        br = branchRepository.save(br);

        User u = userRepository.findAll().stream().findFirst().orElseThrow();

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-DIV-" + timestamp);
        q.setCustomer(cust);
        q.setBranch(br);
        q.setEmployee(u);
        q.setDivision(div);
        q.setPricingMode(PricingMode.PREMIUM);
        q.setQuotationDate(LocalDate.now());
        q.setStatus(QuotationStatus.PENDING);
        q.setSubtotal(new BigDecimal("3000.00"));
        q.setGrandTotal(new BigDecimal("3540.00"));
        q = quotationRepository.save(q);

        // Attempt delete
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = divisionController.deleteDivision(div.getId(), null, request, redirectAttributes);
        assertEquals("redirect:/admin/divisions", result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        assertNotNull(flashSuccess);
        assertTrue(flashSuccess.toString().contains("archived") || flashSuccess.toString().contains("deactivated"));

        // Division must still exist but active = false
        Division reloadedDiv = divisionRepository.findById(div.getId()).orElseThrow();
        assertFalse(reloadedDiv.getActive());

        // Verify PDF generation works
        byte[] pdf = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("Material Option: Safe permanent deletion when no quotations exist")
    public void testDeleteMaterialOptionPermanently() {
        long timestamp = System.currentTimeMillis();
        String catName = "OPT CAT " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "📦",
                null, "OPT DIV " + timestamp,
                null, "OPT MAT " + timestamp,
                null, "UNWANTED OPT " + timestamp,
                "UNWANTED SPEC",
                PricingMode.PREMIUM,
                new BigDecimal("1300.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Material option permanent delete test"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()).get(0);
        Long optId = opt.getId();

        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirectUrl = "/admin/work-categories/view/" + cat.getId();

        Object result = materialOptionController.deleteOption(optId, redirectUrl, request, redirectAttributes);
        assertEquals("redirect:" + redirectUrl, result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        Object flashError = redirectAttributes.getFlashAttributes().get("errorMessage");
        assertNotNull(flashSuccess, "Expected flash successMessage, but got error: " + flashError);
        assertTrue(flashSuccess.toString().contains("deleted successfully"));

        assertTrue(materialOptionRepository.findById(optId).isEmpty(), "Option must be permanently removed from DB");

        // Verify parent material still exists
        assertTrue(materialRepository.findById(mat.getId()).isPresent(), "Parent material must remain intact");

        // Verify view page no longer displays the option
        Model viewModel = new ConcurrentModel();
        masterDataHierarchyController.viewCategoryDetails(cat.getId(), viewModel);
        MasterDataHierarchyController.HierarchyCategoryDto catDto =
                (MasterDataHierarchyController.HierarchyCategoryDto) viewModel.getAttribute("catDto");
        assertNotNull(catDto);
        assertTrue(catDto.getDivisions().get(0).getMaterials().get(0).getOptions().isEmpty(),
                "Option must not appear in view page hierarchy");
    }

    @Test
    @DisplayName("Material Option: Soft-delete archive when referenced in quotation")
    public void testDeleteMaterialOptionReferencedInQuotationSafelyArchives() {
        long timestamp = System.currentTimeMillis();
        String catName = "OPT QUOTE CAT " + timestamp;

        pricingService.saveUnifiedMasterData(
                null,
                null, catName, "📦",
                null, "OPT QUOTE DIV " + timestamp,
                null, "OPT QUOTE MAT " + timestamp,
                null, "QUOTED OPT " + timestamp,
                "QUOTED SPEC",
                PricingMode.PREMIUM,
                new BigDecimal("1600.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Quoted option test"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()).get(0);

        // Create quotation referencing this option
        Customer cust = new Customer();
        cust.setName("Opt Cust " + timestamp);
        cust.setPhone("9812345672");
        cust = customerRepository.save(cust);

        Branch br = new Branch();
        br.setName("Opt Branch " + timestamp);
        br.setBranchAddress("Address");
        br = branchRepository.save(br);

        User u = userRepository.findAll().stream().findFirst().orElseThrow();

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-OPT-" + timestamp);
        q.setCustomer(cust);
        q.setBranch(br);
        q.setEmployee(u);
        q.setDivision(div);
        q.setPricingMode(PricingMode.PREMIUM);
        q.setQuotationDate(LocalDate.now());
        q.setStatus(QuotationStatus.PENDING);
        q.setSubtotal(new BigDecimal("4000.00"));
        q.setGrandTotal(new BigDecimal("4720.00"));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setRoom("Bedroom");
        room.setFloor("Ground");
        room.setSubtotal(new BigDecimal("4000.00"));
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(cat);
        item.setDivision(div);
        item.setMaterial(mat);
        item.setMaterialOption(opt);
        item.setItemDescription("Test item option");
        item.setRate(new BigDecimal("1600.00"));
        item.setAmount(new BigDecimal("4000.00"));
        quotationItemRepository.save(item);

        // Attempt delete
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = materialOptionController.deleteOption(opt.getId(), null, request, redirectAttributes);
        assertEquals("redirect:/admin/material-options", result);

        Object flashSuccess = redirectAttributes.getFlashAttributes().get("successMessage");
        assertNotNull(flashSuccess);
        assertTrue(flashSuccess.toString().contains("archived") || flashSuccess.toString().contains("deactivated"));

        // Option must still exist but active = false
        MaterialOption reloadedOpt = materialOptionRepository.findById(opt.getId()).orElseThrow();
        assertFalse(reloadedOpt.getActive());

        // Verify PDF generation works
        byte[] pdf = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
