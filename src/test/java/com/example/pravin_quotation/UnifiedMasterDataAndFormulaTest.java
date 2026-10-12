package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.PricingController;
import com.example.pravin_quotation.controller.QuotationItemController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.Locale;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UnifiedMasterDataAndFormulaTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private PricingController pricingController;

    @Autowired
    private QuotationItemController quotationItemController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private WorkCategory category;
    private Division division;
    private Item item;
    private Material material;
    private MaterialOption option;
    private Branch branch;
    private Customer customer;
    private User employee;
    private Quotation quotation;
    private QuotationRoom room;

    @BeforeEach
    public void setup() {
        category = new WorkCategory();
        category.setName("Kitchen Master Test");
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("Base Cabinets Test");
        division.setWorkCategory(category);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Modular Kitchen Unit Test");
        item.setDivision(division);
        item = itemRepository.save(item);

        material = new Material();
        material.setName("Plywood Test");
        material.setItem(item);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("BWP 710 Grade");
        option.setMaterial(material);
        option.setFormula(CalculationFormula.FORMULA_L_X_W);
        option = materialOptionRepository.save(option);

        branch = new Branch();
        branch.setName("RS Puram Test " + System.currentTimeMillis());
        branch.setBranchAddress("100 DB Road");
        branch = branchRepository.save(branch);

        employee = new User();
        employee.setName("Test Employee");
        employee.setEmail("emp" + System.currentTimeMillis() + "@test.com");
        employee.setPassword("pass");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee = userRepository.save(employee);

        customer = new Customer();
        customer.setName("Test Customer");
        customer.setPhone("9876543210");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setEmployee(employee);
        quotation.setQuotationNumber("QT-TEST-" + System.currentTimeMillis());
        quotation.setPricingMode(PricingMode.ECONOMY);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation = quotationRepository.save(quotation);

        room = new QuotationRoom();
        room.setQuotation(quotation);
        room.setFloor("Ground Floor");
        room.setRoom("Kitchen");
        room = quotationRoomRepository.save(room);
    }

    // =========================================================================
    // 1. CANONICAL FORMULA & CALCULATION UTILITY TESTS
    // =========================================================================

    @Test
    public void testCalculationFormulaNormalization() {
        assertEquals("L × W", CalculationFormula.normalizeFormula("L × W"));
        assertEquals("L × W", CalculationFormula.normalizeFormula("l*w"));
        assertEquals("L × W", CalculationFormula.normalizeFormula("LxW"));
        assertEquals("L × W", CalculationFormula.normalizeFormula(null));

        assertEquals("(L + W) × H", CalculationFormula.normalizeFormula("(L + W) × H"));
        assertEquals("(L + W) × H", CalculationFormula.normalizeFormula("(l+w)*h"));
        assertEquals("(L + W) × H", CalculationFormula.normalizeFormula("l+(w*h)"));
        assertEquals("(L + W) × H", CalculationFormula.normalizeFormula("l+w*h"));
        assertEquals("(L + W) × H", CalculationFormula.normalizeFormula("L+(WxH)"));
        assertEquals("(L + W) × H", CalculationFormula.normalizeFormula("((L + W) * H) / 144"));

        assertEquals("L × H", CalculationFormula.normalizeFormula("L × H"));
        assertEquals("L × H", CalculationFormula.normalizeFormula("l*h"));
        assertEquals("L × H", CalculationFormula.normalizeFormula("lxH"));
    }

    @Test
    public void testCalculationFormulaAreaInFeet() {
        // Formula 1: L × W
        BigDecimal area1 = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_X_W,
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                null,
                "ft"
        );
        assertEquals(new BigDecimal("50.00"), area1);

        // Formula 2: (L + W) × H in Feet -> (10 + 4) * 3 = 42.00
        BigDecimal area2 = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("10.00"),
                new BigDecimal("4.00"),
                new BigDecimal("3.00"),
                "ft"
        );
        assertEquals(new BigDecimal("42.00"), area2);

        // Formula 3: L × H -> 10 * 6 = 60
        BigDecimal area3 = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_X_H,
                new BigDecimal("10.00"),
                null,
                new BigDecimal("6.00"),
                "ft"
        );
        assertEquals(new BigDecimal("60.00"), area3);
    }

    @Test
    public void testCalculationFormulaAreaInInches() {
        // Dimensions in inches: length=120 in (10 ft), width=60 in (5 ft)
        // Formula 1: 10 * 5 = 50.00
        BigDecimal area1 = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_X_W,
                new BigDecimal("120.00"),
                new BigDecimal("60.00"),
                null,
                "in"
        );
        assertEquals(new BigDecimal("50.00"), area1);

        // Formula 2: length=120 in (10 ft), width=48 in (4 ft), height=36 in (3 ft)
        // ((120 + 48) * 36) / 144 = 42.00
        BigDecimal area2 = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("120.00"),
                new BigDecimal("48.00"),
                new BigDecimal("36.00"),
                "in"
        );
        assertEquals(new BigDecimal("42.00"), area2);

        // Formula 2 Verification test: L = 23, W = 12, H = 15
        // ((23 + 12) * 15) / 144 = 525 / 144 = 3.645833... -> 3.65 sq.ft
        BigDecimal areaVerification = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("23"),
                new BigDecimal("12"),
                new BigDecimal("15"),
                "in"
        );
        assertEquals(new BigDecimal("3.65"), areaVerification);

        // Formula 3: length=120 in (10 ft), height=72 in (6 ft)
        // 10 * 6 = 60.00
        BigDecimal area3 = CalculationFormula.calculateArea(
                CalculationFormula.FORMULA_L_X_H,
                new BigDecimal("120.00"),
                null,
                new BigDecimal("72.00"),
                "in"
        );
        assertEquals(new BigDecimal("60.00"), area3);
    }

    @Test
    public void testCalculateItemAmount() {
        BigDecimal area = new BigDecimal("50.00");
        BigDecimal rate = new BigDecimal("1250.00");
        BigDecimal expectedAmount = new BigDecimal("62500.00");
        BigDecimal actualAmount = CalculationFormula.calculateItemAmount(area, rate);
        assertEquals(expectedAmount, actualAmount);
    }

    @Test
    public void testValidateDimensions() {
        // L x W requires Length and Width
        assertDoesNotThrow(() -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_X_W, BigDecimal.TEN, BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_X_W, BigDecimal.TEN, null, null));
        assertThrows(IllegalArgumentException.class, () -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_X_W, BigDecimal.ZERO, BigDecimal.ONE, null));

        // L + (W x H) requires all three
        assertDoesNotThrow(() -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_PLUS_W_X_H, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_PLUS_W_X_H, BigDecimal.TEN, BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_PLUS_W_X_H, BigDecimal.TEN, null, BigDecimal.ONE));

        // L x H requires Length and Height
        assertDoesNotThrow(() -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_X_H, BigDecimal.TEN, null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_X_H, BigDecimal.TEN, null, null));
        assertThrows(IllegalArgumentException.class, () -> CalculationFormula.validateDimensions(CalculationFormula.FORMULA_L_X_H, null, null, BigDecimal.ONE));
    }

    // =========================================================================
    // 2. UNIFIED MASTER DATA PRICING CONFIGURATION & VALIDATION
    // =========================================================================

    @Test
    public void testUnifiedPricingServiceSaveAndFormulaPersistence() {
        Pricing saved = pricingService.saveUnifiedMasterData(
                null,
                category.getId(),
                null,
                division.getId(),
                null,
                material.getId(),
                null,
                option.getId(),
                null,
                PricingMode.PREMIUM,
                new BigDecimal("1850.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                "Test description"
        );

        assertNotNull(saved.getId());
        assertEquals(new BigDecimal("1850.00"), saved.getRate());
        assertEquals(CalculationFormula.FORMULA_L_PLUS_W_X_H, saved.getFormula());
        assertEquals(PricingMode.PREMIUM, saved.getPricingMode());

        // Verify material option updated
        MaterialOption reloadedOption = materialOptionRepository.findById(option.getId()).orElseThrow();
        assertEquals(CalculationFormula.FORMULA_L_PLUS_W_X_H, reloadedOption.getFormula());
    }

    @Test
    public void testUnifiedPricingRejectsZeroOrNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.saveUnifiedMasterData(
                    null,
                    category.getId(),
                    null,
                    division.getId(),
                    null,
                    material.getId(),
                    null,
                    option.getId(),
                    null,
                    PricingMode.ECONOMY,
                    BigDecimal.ZERO,
                    "SQFT",
                    CalculationFormula.FORMULA_L_X_W,
                    null
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.saveUnifiedMasterData(
                    null,
                    category.getId(),
                    null,
                    division.getId(),
                    null,
                    material.getId(),
                    null,
                    option.getId(),
                    null,
                    PricingMode.ECONOMY,
                    new BigDecimal("-100.00"),
                    "SQFT",
                    CalculationFormula.FORMULA_L_X_W,
                    null
            );
        });
    }

    // =========================================================================
    // 3. ADMIN CONTROLLER ENDPOINTS
    // =========================================================================

    @Test
    public void testAdminUnifiedPricingFormController() {
        Model model = new ConcurrentModel();
        String view = pricingController.newPricing(model);
        assertEquals("admin/pricing-form", view);
        assertTrue(model.containsAttribute("workCategories"));
        assertTrue(model.containsAttribute("formulas"));
        assertTrue(model.containsAttribute("pricingModes"));

        // Save valid pricing with formula
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = pricingController.savePricing(
                null,
                category.getId(),
                null,
                division.getId(),
                null,
                material.getId(),
                null,
                option.getId(),
                null,
                PricingMode.ECONOMY,
                new BigDecimal("1450.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Test save",
                redirectAttributes
        );
        assertEquals("redirect:/admin/pricing", redirect);

        // Verify saved
        Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), PricingMode.ECONOMY).orElseThrow();
        assertEquals(new BigDecimal("1450.00"), pricing.getRate());
        assertEquals(CalculationFormula.FORMULA_L_X_H, pricing.getFormula());

        // Save invalid non-positive rate -> redirect with error
        redirectAttributes = new RedirectAttributesModelMap();
        String errorRedirect = pricingController.savePricing(
                null,
                category.getId(),
                null,
                division.getId(),
                null,
                material.getId(),
                null,
                option.getId(),
                null,
                PricingMode.ECONOMY,
                BigDecimal.ZERO,
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                null,
                redirectAttributes
        );
        assertEquals("redirect:/admin/pricing/new", errorRedirect);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("errorMessage"));
    }

    @Test
    public void testPricingFormTemplateRenderingForNewAndEdit() {
        var application = JakartaServletWebApplication.buildApplication(new MockServletContext());

        // 1. Test rendering pricing-form for 'new / configure' (no TemplateInputException)
        Model newModel = new ConcurrentModel();
        pricingController.newPricing(newModel);

        var newExchange = application.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse());
        WebContext newContext = new WebContext(newExchange, Locale.getDefault(), newModel.asMap());

        String renderedNewHtml = templateEngine.process("admin/pricing-form", newContext);
        assertNotNull(renderedNewHtml);
        assertTrue(renderedNewHtml.contains("divisionId"));
        assertTrue(renderedNewHtml.contains("workCategoryId"));

        // 2. Test rendering pricing-form for 'edit'
        Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), PricingMode.ECONOMY)
                .orElseGet(() -> {
                    Pricing p = new Pricing();
                    p.setMaterialOption(option);
                    p.setPricingMode(PricingMode.ECONOMY);
                    p.setRate(new BigDecimal("1200.00"));
                    p.setUnit("SQFT");
                    return pricingRepository.save(p);
                });

        Model editModel = new ConcurrentModel();
        pricingController.editPricing(pricing.getId(), editModel);

        var editExchange = application.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse());
        WebContext editContext = new WebContext(editExchange, Locale.getDefault(), editModel.asMap());

        String renderedEditHtml = templateEngine.process("admin/pricing-form", editContext);
        assertNotNull(renderedEditHtml);
        assertTrue(renderedEditHtml.contains("divisionId"));
    }

    @Test
    public void testAdminDependentDropdownApis() {
        List<Map<String, Object>> divisions = pricingController.getDivisions(category.getId());
        assertFalse(divisions.isEmpty());
        assertEquals(division.getId(), divisions.get(0).get("id"));

        List<Map<String, Object>> materials = pricingController.getMaterials(division.getId());
        assertFalse(materials.isEmpty());
        assertEquals(material.getId(), materials.get(0).get("id"));

        List<Map<String, Object>> options = pricingController.getMaterialOptions(material.getId());
        assertFalse(options.isEmpty());
        assertEquals(option.getId(), options.get(0).get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAdminResolveRateReturnsRateAndFormula() {
        // Configure pricing
        pricingService.saveUnifiedMasterData(
                null,
                category.getId(),
                null,
                division.getId(),
                null,
                material.getId(),
                null,
                option.getId(),
                null,
                PricingMode.ECONOMY,
                new BigDecimal("1600.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                null
        );

        ResponseEntity<?> response = quotationItemController.resolveRate(room.getId(), option.getId());
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(new BigDecimal("1600.00"), body.get("rate"));
        assertEquals(CalculationFormula.FORMULA_L_PLUS_W_X_H, body.get("formula"));
    }

    // =========================================================================
    // 4. EMPLOYEE CONTROLLER ENDPOINTS
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    public void testEmployeeResolveRateReturnsRateAndFormula() {
        // Configure pricing
        pricingService.saveUnifiedMasterData(
                null,
                category.getId(),
                null,
                division.getId(),
                null,
                material.getId(),
                null,
                option.getId(),
                null,
                PricingMode.ECONOMY,
                new BigDecimal("1750.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                null
        );

        ResponseEntity<?> response = employeeQuotationController.resolveItemRate(quotation.getId(), option.getId());
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(new BigDecimal("1750.00"), body.get("rate"));
        assertEquals(CalculationFormula.FORMULA_L_X_H, body.get("formula"));
        assertEquals("ECONOMY", body.get("pricingMode"));
    }

    // =========================================================================
    // 5. DIRECT TEXT INPUT & OPTIONAL SPECIFICATION TESTS (TASK 10)
    // =========================================================================

    @Test
    public void testAddNewConfigurationWithDirectTextInputs() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String catName = "Direct Cat " + uniqueSuffix;
        String divName = "Direct Div " + uniqueSuffix;
        String matName = "Direct Mat " + uniqueSuffix;
        String optName = "Direct 18mm Specification";

        // Save with direct text inputs (no dropdown IDs supplied)
        String redirect = pricingController.savePricing(
                null,
                null,
                catName,
                null,
                null,
                divName,
                null,
                null,
                matName,
                null,
                null,
                optName,
                null,
                null,
                PricingMode.PREMIUM,
                new BigDecimal("2100.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                "Direct text input test",
                redirectAttributes
        );

        assertEquals("redirect:/admin/pricing", redirect);
        assertEquals("Master data and pricing configuration saved successfully.", redirectAttributes.getFlashAttributes().get("successMessage"));

        // Verify entities were created and linked properly
        WorkCategory wc = workCategoryRepository.findByNameIgnoreCase(catName).orElseThrow();
        assertEquals(catName, wc.getName());

        List<Division> divs = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(wc.getId());
        assertFalse(divs.isEmpty());
        assertEquals(divName, divs.get(0).getName());

        List<Item> items = itemRepository.findByDivisionIdOrderByDisplayOrderAsc(divs.get(0).getId());
        assertFalse(items.isEmpty());

        List<Material> mats = materialRepository.findByItemIdOrderByDisplayOrderAsc(items.get(0).getId());
        assertFalse(mats.isEmpty());
        assertEquals(matName, mats.get(0).getName());

        List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mats.get(0).getId());
        assertFalse(opts.isEmpty());
        assertEquals(optName, opts.get(0).getName());
        assertEquals(CalculationFormula.FORMULA_L_PLUS_W_X_H, opts.get(0).getFormula());

        Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(opts.get(0).getId(), PricingMode.PREMIUM).orElseThrow();
        assertEquals(new BigDecimal("2100.00"), pricing.getRate());
        assertEquals(CalculationFormula.FORMULA_L_PLUS_W_X_H, pricing.getFormula());
    }

    @Test
    public void testAddConfigurationWithEmptyMaterialOptionSucceeds() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        String catName = "Optional Cat " + uniqueSuffix;
        String divName = "Optional Div " + uniqueSuffix;
        String matName = "Optional Mat " + uniqueSuffix;

        // Specification left blank / empty
        String redirect = pricingController.savePricing(
                null,
                null,
                catName,
                null,
                null,
                divName,
                null,
                null,
                matName,
                null,
                null,
                "", // empty specification
                null,
                null,
                PricingMode.MIDDLE,
                new BigDecimal("1350.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Optional specification test",
                redirectAttributes
        );

        assertEquals("redirect:/admin/pricing", redirect);

        Material mat = materialRepository.findAll().stream()
                .filter(m -> m.getName().equalsIgnoreCase(matName))
                .findFirst().orElseThrow();

        // Option should gracefully default to "Standard"
        List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId());
        assertFalse(opts.isEmpty());
        assertEquals("Standard", opts.get(0).getName());

        Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(opts.get(0).getId(), PricingMode.MIDDLE).orElseThrow();
        assertEquals(new BigDecimal("1350.00"), pricing.getRate());
        assertEquals(CalculationFormula.FORMULA_L_X_W, pricing.getFormula());
    }

    @Test
    public void testEditConfigurationUpdatesValuesWithoutDuplication() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Create initial pricing
        Pricing initialPricing = pricingService.saveUnifiedMasterData(
                null,
                category.getId(),
                null,
                division.getId(),
                null,
                material.getId(),
                null,
                option.getId(),
                null,
                PricingMode.MIDDLE,
                new BigDecimal("1500.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Initial note"
        );

        Long pricingId = initialPricing.getId();
        int initialPricingCount = pricingRepository.findAll().size();

        // Update pricing with updated rate and formula
        String redirect = pricingController.updatePricing(
                pricingId,
                category.getId(),
                category.getName(),
                null,
                division.getId(),
                division.getName(),
                null,
                material.getId(),
                material.getName(),
                null,
                option.getId(),
                option.getName(),
                null,
                null,
                PricingMode.MIDDLE,
                new BigDecimal("1950.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                "Updated note",
                redirectAttributes
        );

        assertEquals("redirect:/admin/pricing", redirect);
        assertEquals(initialPricingCount, pricingRepository.findAll().size()); // No new record duplicated

        Pricing updatedPricing = pricingService.getPricingById(pricingId);
        assertEquals(new BigDecimal("1950.00"), updatedPricing.getRate());
        assertEquals(CalculationFormula.FORMULA_L_PLUS_W_X_H, updatedPricing.getFormula());
        assertEquals("Updated note", updatedPricing.getDescription());
    }

    @Test
    public void testPricingFormTemplateRendersDirectTextInputsAndNoMandatoryOption() {
        var application = JakartaServletWebApplication.buildApplication(new MockServletContext());

        // 1. New mode
        Model newModel = new ConcurrentModel();
        pricingController.newPricing(newModel);
        var newExchange = application.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse());
        WebContext newContext = new WebContext(newExchange, Locale.getDefault(), newModel.asMap());
        String htmlNew = templateEngine.process("admin/pricing-form", newContext);

        assertNotNull(htmlNew);
        assertTrue(htmlNew.contains("Configure Master Data"));
        assertTrue(htmlNew.contains("name=\"workCategoryName\""));
        assertTrue(htmlNew.contains("name=\"divisionName\""));
        assertTrue(htmlNew.contains("name=\"materialName\""));
        assertTrue(htmlNew.contains("name=\"materialOptionName\""));
        assertTrue(htmlNew.contains("Optional"));
        assertTrue(htmlNew.contains("Save Configuration"));

        // 2. Edit mode
        Pricing pricing = pricingRepository.findByMaterialOptionIdAndPricingMode(option.getId(), PricingMode.MIDDLE)
                .orElseGet(() -> {
                    Pricing p = new Pricing();
                    p.setMaterialOption(option);
                    p.setPricingMode(PricingMode.MIDDLE);
                    p.setRate(new BigDecimal("1800.00"));
                    p.setUnit("SQFT");
                    return pricingRepository.save(p);
                });

        Model editModel = new ConcurrentModel();
        pricingController.editPricing(pricing.getId(), editModel);
        var editExchange = application.buildExchange(new MockHttpServletRequest(), new MockHttpServletResponse());
        WebContext editContext = new WebContext(editExchange, Locale.getDefault(), editModel.asMap());
        String htmlEdit = templateEngine.process("admin/pricing-form", editContext);

        assertNotNull(htmlEdit);
        assertTrue(htmlEdit.contains("Edit Master Data Configuration"));
        assertTrue(htmlEdit.contains("Update Configuration"));
        assertTrue(htmlEdit.contains(category.getName()));
        assertTrue(htmlEdit.contains(division.getName()));
        assertTrue(htmlEdit.contains(material.getName()));
    }
}
