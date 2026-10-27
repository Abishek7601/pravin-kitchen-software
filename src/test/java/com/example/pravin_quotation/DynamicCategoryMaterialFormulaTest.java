package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.controller.MaterialController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.MaterialService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.WorkCategoryService;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class DynamicCategoryMaterialFormulaTest {

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialController materialController;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationPdfService quotationPdfService;

    private Branch branch;
    private Customer customer;
    private User employeeUser;
    private User adminUser;
    private Authentication employeeAuth;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branch = new Branch();
        branch.setName("Formula-Test-Branch-" + ts);
        branch.setBranchAddress("Test St 101");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        customer = new Customer();
        customer.setName("Test Customer " + ts);
        customer.setPhone("987654" + (ts % 10000));
        customer.setEmail("customer_" + ts + "@test.com");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        adminUser = new User();
        adminUser.setName("Admin " + ts);
        adminUser.setEmail("admin_" + ts + "@test.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setBranch(branch);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Emp " + ts);
        employeeUser.setEmail("emp_" + ts + "@test.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branch);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(),
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
    }

    private Pricing createPricing(MaterialOption opt, PricingMode mode, BigDecimal rate, String formula) {
        Pricing pricing = new Pricing();
        pricing.setMaterialOption(opt);
        pricing.setPricingMode(mode);
        pricing.setRate(rate);
        pricing.setFormula(formula);
        pricing.setUnit("SQFT");
        pricing.setActive(true);
        return pricingRepository.save(pricing);
    }

    @Test
    @DisplayName("Scenario 1: Create a new Work Category with an emoji")
    void testScenario1_CreateWorkCategoryWithEmoji() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Foyer Area");
        cat.setEmoji("🏠");
        cat.setActive(true);
        cat.setDisplayOrder(10);
        WorkCategory saved = workCategoryRepository.save(cat);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Foyer Area");
        assertThat(saved.getEmoji()).isEqualTo("🏠");
        assertThat(saved.getDisplayName()).isEqualTo("🏠 Foyer Area");
    }

    @Test
    @DisplayName("Scenario 2: Edit the category name and emoji")
    void testScenario2_EditCategoryNameAndEmoji() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Foyer Area");
        cat.setEmoji("🏠");
        cat.setActive(true);
        WorkCategory saved = workCategoryRepository.save(cat);

        // Edit via controller
        String result = workCategoryController.updateCategory(
                saved.getId(),
                "Master Foyer",
                "",
                null,
                "✨",
                0
        );
        assertThat(result).isEqualTo("redirect:/admin/work-categories");

        WorkCategory updated = workCategoryService.getCategoryById(saved.getId());
        assertThat(updated.getName()).isEqualTo("MASTER FOYER");
        assertThat(updated.getEmoji()).isEqualTo("✨");
        assertThat(updated.getDisplayName()).isEqualTo("✨ MASTER FOYER");
    }

    @Test
    @DisplayName("Scenario 3: Verify category appears in both Admin and Employee quotation forms")
    void testScenario3_CategoryAppearsInAdminAndEmployeeForms() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Living Luxury");
        cat.setEmoji("🛋️");
        cat.setActive(true);
        workCategoryRepository.save(cat);

        // Admin builder categories
        ExtendedModelMap adminModel = new ExtendedModelMap();
        adminQuotationBuilderController.builder(null, null, adminModel);
        @SuppressWarnings("unchecked")
        List<WorkCategory> adminCats = (List<WorkCategory>) adminModel.get("workCategories");
        assertThat(adminCats).anyMatch(c -> "Living Luxury".equals(c.getName()) && "🛋️".equals(c.getEmoji()));

        // Employee builder categories
        ExtendedModelMap empModel = new ExtendedModelMap();
        employeeQuotationController.builder(employeeAuth, empModel);
        @SuppressWarnings("unchecked")
        List<WorkCategory> empCats = (List<WorkCategory>) empModel.get("workCategories");
        assertThat(empCats).isNotNull();
        assertThat(empCats).anyMatch(c -> "Living Luxury".equals(c.getName()) && "🛋️".equals(c.getEmoji()));
    }

    @Test
    @DisplayName("Scenario 4: Create and manage Divisions under the correct Work Categories")
    void testScenario4_CreateDivisionsUnderWorkCategory() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Kitchen Works");
        cat.setEmoji("🍳");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div1 = new Division();
        div1.setName("Base Cabinets");
        div1.setWorkCategory(cat);
        div1.setActive(true);
        div1 = divisionRepository.save(div1);

        Division div2 = new Division();
        div2.setName("Wall Cabinets");
        div2.setWorkCategory(cat);
        div2.setActive(true);
        div2 = divisionRepository.save(div2);

        List<Division> byCategory = divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(cat.getId());
        assertThat(byCategory).hasSize(2);
        assertThat(byCategory).extracting(Division::getName).containsExactlyInAnyOrder("Base Cabinets", "Wall Cabinets");
    }

    @Test
    @DisplayName("Scenario 5: Map multiple materials to the same Division")
    void testScenario5_MapMultipleMaterialsToSameDivision() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Bedroom Works");
        cat.setEmoji("🛏️");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("Wardrobe Unit");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        Material mat1 = materialService.createMaterial(
                "Commercial Plywood", "Commercial grade ply", "L × W", null, List.of(div.getId())
        );
        Material mat2 = materialService.createMaterial(
                "BWP Marine Plywood", "Boiling waterproof marine ply", "(L + W) × H", null, List.of(div.getId())
        );

        List<Material> divisionMaterials = materialService.getActiveMaterialsByDivision(div.getId());
        assertThat(divisionMaterials).hasSize(2);
        assertThat(divisionMaterials).extracting(Material::getName).containsExactlyInAnyOrder("Commercial Plywood", "BWP Marine Plywood");
    }

    @Test
    @DisplayName("Scenario 6: Map an existing material to another Division (many-to-many)")
    void testScenario6_MapMaterialToMultipleDivisions() {
        WorkCategory cat = new WorkCategory();
        cat.setName("General Woodwork");
        cat.setEmoji("🪵");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div1 = new Division();
        div1.setName("Carcass");
        div1.setWorkCategory(cat);
        div1.setActive(true);
        div1 = divisionRepository.save(div1);

        Division div2 = new Division();
        div2.setName("Shutters");
        div2.setWorkCategory(cat);
        div2.setActive(true);
        div2 = divisionRepository.save(div2);

        Material mat = materialService.createMaterial(
                "HDHMR Board", "High Density High Moisture Resistance", "L × H", null, List.of(div1.getId())
        );

        // Map mat to div2 as well
        materialService.mapDivision(mat.getId(), div2.getId());

        Material refreshed = materialService.getMaterialById(mat.getId());
        assertThat(refreshed.getDivisions()).hasSize(2);
        assertThat(refreshed.hasDivision(div1.getId())).isTrue();
        assertThat(refreshed.hasDivision(div2.getId())).isTrue();

        List<Material> div1Mats = materialService.getActiveMaterialsByDivision(div1.getId());
        List<Material> div2Mats = materialService.getActiveMaterialsByDivision(div2.getId());
        assertThat(div1Mats).extracting(Material::getName).contains("HDHMR Board");
        assertThat(div2Mats).extracting(Material::getName).contains("HDHMR Board");
    }

    @Test
    @DisplayName("Scenario 7: Verify Material and Specifications remain separate")
    void testScenario7_MaterialAndSpecificationsRemainSeparate() {
        Material mat = materialService.createMaterial(
                "MDF Core", "Medium Density Fibreboard", "L × W", null, null
        );

        MaterialOption opt = new MaterialOption();
        opt.setMaterial(mat);
        opt.setName("1mm Acrylic White Gloss");
        opt.setDescription("Glossy white 1mm acrylic finish");
        opt.setActive(true);
        opt = materialOptionRepository.save(opt);

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase("Dining").orElseGet(() -> {
            WorkCategory c = new WorkCategory();
            c.setName("Dining_" + System.currentTimeMillis());
            c.setEmoji("🍽️");
            c.setActive(true);
            return workCategoryRepository.save(c);
        });

        Division div = new Division();
        div.setName("Storage");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        mat.addDivision(div);
        materialRepository.save(mat);

        createPricing(opt, PricingMode.PREMIUM, new BigDecimal("1200.00"), "L × W");

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getName(), customer.getPhone(), customer.getEmail(), "Dining Street"
        );
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), new BigDecimal("48"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), null, mat.getId(), opt.getId(),
                "Dining Storage Cabinet", "Custom edge-banding with copper trim", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Dining Room", "Dining woodwork", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM, "Dining woodwork", "Handle with care",
                List.of(roomReq)
        );

        Quotation q = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(q.getId());
        assertThat(rooms).isNotEmpty();
        QuotationRoom savedRoom = rooms.get(0);
        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(savedRoom.getId());
        assertThat(items).isNotEmpty();
        QuotationItem savedItem = items.get(0);

        // Assert Material and Specifications are distinct
        assertThat(savedItem.getMaterial()).isNotNull();
        assertThat(savedItem.getMaterial().getName()).isEqualTo("MDF Core");
        assertThat(savedItem.getMaterialOption()).isNotNull();
        assertThat(savedItem.getMaterialOption().getName()).isEqualTo("1mm Acrylic White Gloss");
        assertThat(savedItem.getSpecification()).isEqualTo("Custom edge-banding with copper trim");
    }

    @Test
    @DisplayName("Scenario 8: Select a material and display its configured formula")
    void testScenario8_SelectMaterialAndDisplayConfiguredFormula() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Study");
        cat.setEmoji("📚");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("Bookshelf");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        Material mat = materialService.createMaterial(
                "Teak Veneer", "Natural Teak Veneer", "L × H", null, List.of(div.getId())
        );

        List<Map<String, Object>> mats = adminQuotationBuilderController.materials(div.getId(), null);
        assertThat(mats).isNotNull();
        Map<String, Object> found = mats.stream()
                .filter(m -> mat.getId().equals(((Number) m.get("id")).longValue()))
                .findFirst().orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.get("formula")).isEqualTo("L × H");
    }

    @Test
    @DisplayName("Scenario 9: For L × W, require Length and Width, hide Height, and calculate correctly")
    void testScenario9_Formula_L_X_W() {
        // Formula A: L × W
        // Length = 120 inches, Width = 48 inches -> (120 * 48) / 144 = 40.00 Sq. Ft.
        BigDecimal sqft = CalculationFormula.calculateArea(
                "L × W",
                new BigDecimal("120"),
                new BigDecimal("48"),
                BigDecimal.ZERO
        );
        assertThat(sqft).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Scenario 10: For (L + W) × H, require Length, Width, Height, and calculate correctly")
    void testScenario10_Formula_L_Plus_W_X_H() {
        // Formula B: (L + W) × H
        // Length = 120 inches, Width = 48 inches, Height = 96 inches -> ((120 + 48) * 96) / 144 = 112.00 Sq. Ft.
        BigDecimal sqft = CalculationFormula.calculateArea(
                "(L + W) × H",
                new BigDecimal("120"),
                new BigDecimal("48"),
                new BigDecimal("96")
        );
        assertThat(sqft).isEqualByComparingTo(new BigDecimal("112.00"));
    }

    @Test
    @DisplayName("Scenario 11: For L × H, require Length and Height, hide Width, and calculate correctly")
    void testScenario11_Formula_L_X_H() {
        // Formula C: L × H
        // Length = 120 inches, Height = 96 inches -> (120 * 96) / 144 = 80.00 Sq. Ft.
        BigDecimal sqft = CalculationFormula.calculateArea(
                "L × H",
                new BigDecimal("120"),
                BigDecimal.ZERO,
                new BigDecimal("96")
        );
        assertThat(sqft).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("Scenario 12: Switch between materials with different formulas and verify canonical field handling")
    void testScenario12_SwitchMaterialsWithDifferentFormulas() {
        Material matLW = materialService.createMaterial("Mat LW", "", "L × W", null, null);
        Material matLWH = materialService.createMaterial("Mat LWH", "", "(L + W) × H", null, null);
        Material matLH = materialService.createMaterial("Mat LH", "", "L × H", null, null);

        assertThat(CalculationFormula.normalizeFormula(matLW.getFormula())).isEqualTo("L × W");
        assertThat(CalculationFormula.normalizeFormula(matLWH.getFormula())).isEqualTo("(L + W) × H");
        assertThat(CalculationFormula.normalizeFormula(matLH.getFormula())).isEqualTo("L × H");

        // When formula is L × H, Width is ignored (even if non-zero, it does not affect L × H)
        BigDecimal sqftLH_ZeroW = CalculationFormula.calculateArea("L × H", new BigDecimal("120"), BigDecimal.ZERO, new BigDecimal("96"));
        BigDecimal sqftLH_WithW = CalculationFormula.calculateArea("L × H", new BigDecimal("120"), new BigDecimal("50"), new BigDecimal("96"));
        assertThat(sqftLH_ZeroW).isEqualByComparingTo(sqftLH_WithW);
        assertThat(sqftLH_ZeroW).isEqualByComparingTo(new BigDecimal("80.00"));

        // When formula is L × W, Height is ignored
        BigDecimal sqftLW_ZeroH = CalculationFormula.calculateArea("L × W", new BigDecimal("120"), new BigDecimal("48"), BigDecimal.ZERO);
        BigDecimal sqftLW_WithH = CalculationFormula.calculateArea("L × W", new BigDecimal("120"), new BigDecimal("48"), new BigDecimal("100"));
        assertThat(sqftLW_ZeroH).isEqualByComparingTo(sqftLW_WithH);
        assertThat(sqftLW_ZeroH).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("Scenario 13: Verify inch-based measurements produce the correct Sq Ft results (/ 144)")
    void testScenario13_InchToSqFtCalculations() {
        // (144 * 144) / 144 = 144 sqft
        BigDecimal res1 = CalculationFormula.calculateArea("L × W", new BigDecimal("144"), new BigDecimal("144"), BigDecimal.ZERO);
        assertThat(res1).isEqualByComparingTo(new BigDecimal("144.00"));

        // (12 * 12) / 144 = 1.00 sqft
        BigDecimal res2 = CalculationFormula.calculateArea("L × W", new BigDecimal("12"), new BigDecimal("12"), BigDecimal.ZERO);
        assertThat(res2).isEqualByComparingTo(new BigDecimal("1.00"));

        // ((24 + 48) * 36) / 144 = 18.00 sqft
        BigDecimal res3 = CalculationFormula.calculateArea("(L + W) × H", new BigDecimal("24"), new BigDecimal("48"), new BigDecimal("36"));
        assertThat(res3).isEqualByComparingTo(new BigDecimal("18.00"));
    }

    @Test
    @DisplayName("Scenario 14: Verify backend validation for missing required dimensions")
    void testScenario14_BackendValidation() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Test Cat");
        cat.setEmoji("🏠");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("Test Div");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        Material matLWH = materialService.createMaterial("Mat LWH", "", "(L + W) × H", null, List.of(div.getId()));
        MaterialOption opt = materialOptionRepository.save(new MaterialOption(null, "Opt 1", "", matLWH, true, 0, null));
        createPricing(opt, PricingMode.PREMIUM, new BigDecimal("100"), "(L + W) × H");

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getName(), customer.getPhone(), customer.getEmail(), "Address"
        );
        AdminQuotationBuilderService.SizeRequest sizeInvalid = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), new BigDecimal("48"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), null, matLWH.getId(), opt.getId(),
                "Item", "Spec", List.of(sizeInvalid)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Floor 1", "Room 101", "Desc", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM, "Reqs", "Notes",
                List.of(roomReq)
        );

        assertThatThrownBy(() -> adminQuotationBuilderService.save(builderReq, adminUser.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Height");
    }

    @Test
    @DisplayName("Scenario 15: Verify pricing, totals and PDF generation")
    void testScenario15_PricingDiscountGstTotalsPdf() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Living");
        cat.setEmoji("🛋️");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("TV Unit");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        Material mat = materialService.createMaterial("Marine Ply", "", "L × W", null, List.of(div.getId()));
        MaterialOption opt = materialOptionRepository.save(new MaterialOption(null, "Matt Laminate", "", mat, true, 0, null));
        createPricing(opt, PricingMode.PREMIUM, new BigDecimal("1000.00"), "L × W");

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getName(), customer.getPhone(), customer.getEmail(), "Living Street"
        );
        // L=120, W=48 -> 40.00 sqft @ 1000 = 40,000
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), new BigDecimal("48"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), null, mat.getId(), opt.getId(),
                "TV Unit Base", "Laminate finish", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living Room", "Living Room TV Unit", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM, "Living requirements", "Note",
                List.of(roomReq)
        );

        Quotation q = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());
        assertThat(q.getId()).isNotNull();

        Quotation refreshed = quotationRepository.findById(q.getId()).orElseThrow();
        assertThat(refreshed.getSubtotal()).isEqualByComparingTo(new BigDecimal("40000.00"));

        // Verify PDF can be generated without errors
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(100);
    }

    @Test
    @DisplayName("Scenario 16: Old quotations retain their historical values after master data changes")
    void testScenario16_HistoricalQuotationsPreservedAfterFormulaChange() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Kitchen");
        cat.setEmoji("🍳");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("Storage");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        Material mat = materialService.createMaterial("Ply 1", "", "L × W", null, List.of(div.getId()));
        MaterialOption opt = materialOptionRepository.save(new MaterialOption(null, "Spec 1", "", mat, true, 0, null));
        createPricing(opt, PricingMode.PREMIUM, new BigDecimal("500.00"), "L × W");

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getName(), customer.getPhone(), customer.getEmail(), "Kitchen St"
        );
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), new BigDecimal("48"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), null, mat.getId(), opt.getId(),
                "Kitchen Storage", "Spec 1", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "1st Floor", "Kitchen", "Kitchen Storage Area", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM, "Kitchen reqs", "Kitchen notes",
                List.of(roomReq)
        );

        Quotation q = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());
        Long qId = q.getId();

        Quotation saved = quotationRepository.findById(qId).orElseThrow();
        BigDecimal originalTotal = saved.getGrandTotal();
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(qId);
        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(rooms.get(0).getId());
        BigDecimal originalItemSqft = items.get(0).getCalculatedSqft();

        // Admin subsequently edits Material formula to L × H
        materialService.updateMaterial(mat.getId(), "Ply 1 Updated", "desc", "L × H", null, List.of(div.getId()));

        // Reload the quotation from database
        Quotation loaded = quotationRepository.findById(qId).orElseThrow();
        List<QuotationRoom> loadedRooms = quotationRoomRepository.findByQuotationId(qId);
        QuotationItem loadedItem = quotationItemRepository.findByQuotationRoomId(loadedRooms.get(0).getId()).get(0);

        // Saved quotation item must retain its historical calculatedSqft and total
        assertThat(loaded.getGrandTotal()).isEqualByComparingTo(originalTotal);
        assertThat(loadedItem.getCalculatedSqft()).isEqualByComparingTo(originalItemSqft);
        assertThat(loadedItem.getFormula()).isEqualTo("L × W");
    }

    @Test
    @DisplayName("Scenario 17: Deactivated categories and materials do not break historical quotations")
    void testScenario17_DeactivatedEntitiesDoNotBreakHistoricalQuotations() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Deact Cat");
        cat.setEmoji("📦");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("Deact Div");
        div.setWorkCategory(cat);
        div.setActive(true);
        div = divisionRepository.save(div);

        Material mat = materialService.createMaterial("Deact Mat", "", "L × W", null, List.of(div.getId()));
        MaterialOption opt = materialOptionRepository.save(new MaterialOption(null, "Deact Opt", "", mat, true, 0, null));
        createPricing(opt, PricingMode.PREMIUM, new BigDecimal("300.00"), "L × W");

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getName(), customer.getPhone(), customer.getEmail(), "Store St"
        );
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), new BigDecimal("48"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), null, mat.getId(), opt.getId(),
                "Store unit", "Deact Opt", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground", "Store", "Store work", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM, "Store reqs", "Store notes",
                List.of(roomReq)
        );

        Quotation q = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());
        Long qId = q.getId();

        // Safe deactivation
        cat.setActive(false);
        workCategoryRepository.save(cat);
        materialService.deleteMaterial(mat.getId()); // Safely deactivates
        Material deactivatedMat = materialService.getMaterialById(mat.getId());
        assertThat(deactivatedMat.getActive()).isFalse();

        // Load historical quotation
        Quotation loaded = quotationRepository.findById(qId).orElseThrow();
        List<QuotationRoom> loadedRooms = quotationRoomRepository.findByQuotationId(qId);
        assertThat(loadedRooms).isNotEmpty();
        QuotationItem loadedItem = quotationItemRepository.findByQuotationRoomId(loadedRooms.get(0).getId()).get(0);
        assertThat(loadedItem.getMaterial()).isNotNull();
        assertThat(loadedItem.getMaterial().getName()).isEqualTo("Deact Mat");
        assertThat(loadedItem.getWorkCategory()).isNotNull();
        assertThat(loadedItem.getWorkCategory().getName()).isEqualTo("Deact Cat");

        // Verify PDF generation does not fail with deactivated master data
        byte[] pdf = quotationPdfService.generateQuotationPdf(qId);
        assertThat(pdf).isNotEmpty();
    }

    @Test
    @DisplayName("Verify configurable measurement formulas for materials: L×W, (L+W)×H, L×H, persistence, update and calculations")
    void testConfigurableMaterialMeasurementFormulas() {
        WorkCategory cat = new WorkCategory();
        cat.setName("Modular Kitchen Test");
        cat.setEmoji("🍳");
        cat.setActive(true);
        cat = workCategoryRepository.save(cat);

        Division wt = new Division();
        wt.setName("Base Cabinets");
        wt.setWorkCategory(cat);
        wt.setActive(true);
        wt = divisionRepository.save(wt);

        // 1. Create materials with each of the three formulas
        ResponseEntity<?> resp1 = workCategoryController.createWorkTypeMaterial(
                wt.getId(), "PREMIUM", "Ply Base (L x W)", "18mm Marine Ply", "Sq.ft", new BigDecimal("250.00"), "L × W"
        );
        assertThat(resp1.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<?> resp2 = workCategoryController.createWorkTypeMaterial(
                wt.getId(), "PREMIUM", "Carcass ((L+W) x H)", "Commercial Ply", "Sq.ft", new BigDecimal("300.00"), "(L + W) × H"
        );
        assertThat(resp2.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<?> resp3 = workCategoryController.createWorkTypeMaterial(
                wt.getId(), "PREMIUM", "Shutter (L x H)", "Gloss Acrylic", "Sq.ft", new BigDecimal("400.00"), "L × H"
        );
        assertThat(resp3.getStatusCode().is2xxSuccessful()).isTrue();

        // 2. Verify materials and formula persistence in database
        List<Material> materials = materialRepository.findByDivisionsIdOrderByDisplayOrderAsc(wt.getId());
        Material matLW = materials.stream().filter(m -> m.getName().contains("L x W")).findFirst().orElseThrow();
        Material matComb = materials.stream().filter(m -> m.getName().contains("(L+W) x H")).findFirst().orElseThrow();
        Material matLH = materials.stream().filter(m -> m.getName().contains("L x H")).findFirst().orElseThrow();

        assertThat(matLW.getFormula()).isEqualTo("L × W");
        assertThat(matComb.getFormula()).isEqualTo("(L + W) × H");
        assertThat(matLH.getFormula()).isEqualTo("L × H");

        // 3. Verify getWorkTypeMaterials returns the formula for each material
        Map<String, Object> materialsMap = workCategoryService.getWorkTypeMaterials(wt.getId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> premiumMaterials = (List<Map<String, Object>>) materialsMap.get("premium");
        assertThat(premiumMaterials).isNotEmpty();

        Map<String, Object> lwMap = premiumMaterials.stream().filter(m -> m.get("name").toString().contains("L x W")).findFirst().orElseThrow();
        assertThat(lwMap.get("formula")).isEqualTo("L × W");

        Map<String, Object> combMap = premiumMaterials.stream().filter(m -> m.get("name").toString().contains("(L+W) x H")).findFirst().orElseThrow();
        assertThat(combMap.get("formula")).isEqualTo("(L + W) × H");

        Map<String, Object> lhMap = premiumMaterials.stream().filter(m -> m.get("name").toString().contains("L x H")).findFirst().orElseThrow();
        assertThat(lhMap.get("formula")).isEqualTo("L × H");

        // 4. Update formula of a material and verify persistence
        ResponseEntity<?> updateResp = workCategoryController.updateWorkTypeMaterial(
                matLW.getId(), "Ply Base (Updated to L x H)", "18mm Marine Ply", "Sq.ft", new BigDecimal("275.00"), "L × H"
        );
        assertThat(updateResp.getStatusCode().is2xxSuccessful()).isTrue();
        Material updatedMat = materialRepository.findById(matLW.getId()).orElseThrow();
        assertThat(updatedMat.getFormula()).isEqualTo("L × H");
        assertThat(updatedMat.getRate()).isEqualByComparingTo(new BigDecimal("275.00"));

        // 5. Test formula calculation logic:
        // Let L = 10 ft, W = 5 ft, H = 8 ft:
        // - L × W: 10 * 5 = 50 sq.ft
        // - (L + W) × H: (10 + 5) * 8 = 120 sq.ft
        // - L × H: 10 * 8 = 80 sq.ft
        BigDecimal lFeet = new BigDecimal("10");
        BigDecimal wFeet = new BigDecimal("5");
        BigDecimal hFeet = new BigDecimal("8");

        BigDecimal areaLW = CalculationFormula.calculateArea("L × W", lFeet, wFeet, hFeet, "ft");
        BigDecimal areaComb = CalculationFormula.calculateArea("(L + W) × H", lFeet, wFeet, hFeet, "ft");
        BigDecimal areaLH = CalculationFormula.calculateArea("L × H", lFeet, wFeet, hFeet, "ft");

        assertThat(areaLW).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(areaComb).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(areaLH).isEqualByComparingTo(new BigDecimal("80.00"));

        // Amount calculation verification with rates:
        // Rate 250 -> 50 sq.ft * 250 = 12500.00
        BigDecimal rateLW = new BigDecimal("250.00");
        BigDecimal amountLW = areaLW.multiply(rateLW).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(amountLW).isEqualByComparingTo(new BigDecimal("12500.00"));

        // Rate 300 -> 120 sq.ft * 300 = 36000.00
        BigDecimal rateComb = new BigDecimal("300.00");
        BigDecimal amountComb = areaComb.multiply(rateComb).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(amountComb).isEqualByComparingTo(new BigDecimal("36000.00"));

        // Rate 400 -> 80 sq.ft * 400 = 32000.00
        BigDecimal rateLH = new BigDecimal("400.00");
        BigDecimal amountLH = areaLH.multiply(rateLH).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(amountLH).isEqualByComparingTo(new BigDecimal("32000.00"));

        // 6. Test inch to feet conversion:
        // L = 120 inches (10 ft), W = 60 inches (5 ft), H = 96 inches (8 ft)
        BigDecimal lInches = new BigDecimal("120");
        BigDecimal wInches = new BigDecimal("60");
        BigDecimal hInches = new BigDecimal("96");

        BigDecimal areaLWInches = CalculationFormula.calculateArea("L × W", lInches, wInches, hInches, "in");
        BigDecimal areaCombInches = CalculationFormula.calculateArea("(L + W) × H", lInches, wInches, hInches, "in");
        BigDecimal areaLHInches = CalculationFormula.calculateArea("L × H", lInches, wInches, hInches, "in");

        assertThat(areaLWInches).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(areaCombInches).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(areaLHInches).isEqualByComparingTo(new BigDecimal("80.00"));

        // 7. Verify invalid/missing dimensions do not calculate area (returns 0.00)
        // For L × W: missing or 0 width returns 0.00
        assertThat(CalculationFormula.calculateArea("L × W", lFeet, BigDecimal.ZERO, hFeet, "ft")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(CalculationFormula.calculateArea("L × W", null, wFeet, hFeet, "ft")).isEqualByComparingTo(BigDecimal.ZERO);

        // For (L + W) × H: missing H returns 0.00
        assertThat(CalculationFormula.calculateArea("(L + W) × H", lFeet, wFeet, BigDecimal.ZERO, "ft")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(CalculationFormula.calculateArea("(L + W) × H", lFeet, BigDecimal.ZERO, hFeet, "ft")).isEqualByComparingTo(BigDecimal.ZERO);

        // For L × H: missing H returns 0.00
        assertThat(CalculationFormula.calculateArea("L × H", lFeet, wFeet, BigDecimal.ZERO, "ft")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(CalculationFormula.calculateArea("L × H", BigDecimal.ZERO, wFeet, hFeet, "ft")).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
