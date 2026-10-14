package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.controller.MasterDataHierarchyController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import com.example.pravin_quotation.util.CalculationFormula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MasterDataQuotationIntegrationTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private DivisionService divisionService;

    @Autowired
    private MaterialOptionService materialOptionService;

    @Autowired
    private MaterialSpecificationService materialSpecificationService;

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private MasterDataHierarchyController masterDataHierarchyController;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

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
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    private Branch testBranch;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        testBranch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Test Branch");
            b.setEmail("testbranch@example.com");
            b.setActive(true);
            return branchRepository.save(b);
        });

        testAdmin = userRepository.findByEmail("admin@example.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin User");
            u.setEmail("admin@example.com");
            u.setPassword("password");
            u.setRole(Role.SUPER_ADMIN);
            u.setBranch(testBranch);
            u.setActive(true);
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("1. Master Data Hierarchy Creation: Work Category with emoji, Division, Material with formulas, Option, Specification")
    void testCompleteHierarchyCreation() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String catName = "Living Room " + suffix;
        String emoji = "🛋️";
        String divName = "TV Unit " + suffix;
        String matName = "HDHMR Premium " + suffix;
        String optName = "18mm Matte Acrylic";
        String specName = "Anti-scratch Polyurethane Coating";

        Pricing savedPricing = pricingService.saveUnifiedMasterData(
                null,
                null,
                catName,
                emoji,
                null,
                divName,
                null,
                matName,
                null,
                optName,
                specName,
                PricingMode.PREMIUM,
                new BigDecimal("1850.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Living Room TV unit premium material"
        );

        assertNotNull(savedPricing);
        assertNotNull(savedPricing.getId());

        // Verify Work Category with emoji
        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase(catName).orElse(null);
        assertNotNull(cat);
        assertEquals(emoji, cat.getIcon());
        assertTrue(cat.getActive());

        // Verify Division under Category
        Division div = divisionRepository.findByNameIgnoreCaseAndWorkCategoryId(divName, cat.getId()).orElse(null);
        assertNotNull(div);
        assertEquals(cat.getId(), div.getWorkCategory().getId());

        // Verify Material under Division with formula
        List<Material> mats = materialRepository.findActiveByDivisionId(div.getId());
        assertFalse(mats.isEmpty());
        Material mat = mats.stream().filter(m -> m.getName().equalsIgnoreCase(matName)).findFirst().orElse(null);
        assertNotNull(mat);
        assertEquals(CalculationFormula.FORMULA_L_X_W, mat.getFormula());

        // Verify Material Option under Material
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId())
                .stream().filter(o -> o.getName().equalsIgnoreCase(optName)).findFirst().orElse(null);
        assertNotNull(opt);
        assertEquals(mat.getId(), opt.getMaterial().getId());

        // Verify Material Specification under Option (Level 5)
        List<MaterialSpecification> specs = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId());
        assertFalse(specs.isEmpty());
        MaterialSpecification spec = specs.stream().filter(s -> s.getName().equalsIgnoreCase(specName)).findFirst().orElse(null);
        assertNotNull(spec);
        assertEquals(opt.getId(), spec.getMaterialOption().getId());
        assertEquals(mat.getId(), spec.getMaterial().getId());
        assertTrue(spec.getActive());
    }

    @Test
    @DisplayName("2. Master Data Hierarchy View Controller returns 5-level hierarchy tree")
    void testMasterDataHierarchyViewController() {
        String suffix = String.valueOf(System.currentTimeMillis());
        pricingService.saveUnifiedMasterData(
                null,
                null,
                "Foyer Area " + suffix,
                "🚪",
                null,
                "Shoe Cabinet " + suffix,
                null,
                "Marine Ply " + suffix,
                null,
                "Gloss Laminate",
                "1mm Edge Banded",
                PricingMode.MIDDLE,
                new BigDecimal("1450.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_H,
                "Foyer shoe rack"
        );

        Model model = new ConcurrentModel();
        String viewName = masterDataHierarchyController.showHierarchy(null, model);

        assertEquals("admin/master-data-hierarchy", viewName);
        assertTrue(model.containsAttribute("categories"));
        @SuppressWarnings("unchecked")
        List<MasterDataHierarchyController.HierarchyCategoryDto> categories =
                (List<MasterDataHierarchyController.HierarchyCategoryDto>) model.getAttribute("categories");

        assertNotNull(categories);
        assertFalse(categories.isEmpty());

        MasterDataHierarchyController.HierarchyCategoryDto foundCat = categories.stream()
                .filter(c -> c.getCategory().getName().equalsIgnoreCase("Foyer Area " + suffix))
                .findFirst().orElse(null);

        assertNotNull(foundCat);
        assertEquals("🚪", foundCat.getCategory().getIcon());
        assertFalse(foundCat.getDivisions().isEmpty());

        MasterDataHierarchyController.HierarchyDivisionDto foundDiv = foundCat.getDivisions().stream()
                .filter(d -> d.getDivision().getName().equalsIgnoreCase("Shoe Cabinet " + suffix))
                .findFirst().orElse(null);
        assertNotNull(foundDiv);
        assertFalse(foundDiv.getMaterials().isEmpty());

        MasterDataHierarchyController.HierarchyMaterialDto foundMat = foundDiv.getMaterials().stream()
                .filter(m -> m.getMaterial().getName().equalsIgnoreCase("Marine Ply " + suffix))
                .findFirst().orElse(null);
        assertNotNull(foundMat);
        assertEquals(CalculationFormula.FORMULA_L_X_H, foundMat.getMaterial().getFormula());
        assertFalse(foundMat.getOptions().isEmpty());

        MasterDataHierarchyController.HierarchyOptionDto foundOpt = foundMat.getOptions().stream()
                .filter(o -> o.getOption().getName().equalsIgnoreCase("Gloss Laminate"))
                .findFirst().orElse(null);
        assertNotNull(foundOpt);
        assertFalse(foundOpt.getSpecifications().isEmpty());
        assertTrue(foundOpt.getSpecifications().stream().anyMatch(s -> s.getName().equalsIgnoreCase("1mm Edge Banded")));
    }

    @Test
    @DisplayName("3. Admin and Employee Quotation Builder endpoints parity for divisions, materials, options, specifications")
    void testBuilderEndpointsParity() {
        String suffix = String.valueOf(System.currentTimeMillis());
        pricingService.saveUnifiedMasterData(
                null,
                null,
                "Kitchen Space " + suffix,
                "🍳",
                null,
                "Base Cabinets " + suffix,
                null,
                "BWP Plywood " + suffix,
                null,
                "Acrylic Finish",
                "Soft-close Hettich Channels",
                PricingMode.PREMIUM,
                new BigDecimal("2200.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_PLUS_W_X_H,
                "Kitchen base unit"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase("Kitchen Space " + suffix).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()).get(0);

        // Test Admin endpoints
        List<Map<String, Object>> adminDivs = adminQuotationBuilderController.divisions(cat.getId());
        List<Map<String, Object>> adminMats = adminQuotationBuilderController.materials(div.getId(), null);
        List<Map<String, Object>> adminOpts = adminQuotationBuilderController.materialOptions(mat.getId());
        List<Map<String, Object>> adminSpecs = adminQuotationBuilderController.materialSpecifications(opt.getId(), null);

        // Test Employee endpoints
        List<Map<String, Object>> empDivs = employeeQuotationController.builderDivisions(cat.getId());
        List<Map<String, Object>> empMats = employeeQuotationController.builderMaterials(div.getId(), null);
        List<Map<String, Object>> empOpts = employeeQuotationController.builderMaterialOptions(mat.getId());
        List<Map<String, Object>> empSpecs = employeeQuotationController.builderMaterialSpecifications(opt.getId(), null);

        // Verify parity
        assertEquals(adminDivs.size(), empDivs.size());
        assertEquals(adminMats.size(), empMats.size());
        assertEquals(adminOpts.size(), empOpts.size());
        assertEquals(adminSpecs.size(), empSpecs.size());

        assertFalse(adminSpecs.isEmpty());
        assertEquals("SOFT-CLOSE HETTICH CHANNELS", adminSpecs.get(0).get("name"));
        assertEquals("SOFT-CLOSE HETTICH CHANNELS", empSpecs.get(0).get("name"));
    }

    @Test
    @DisplayName("4. Formula Calculation: L x W, L x H, (L + W) x H from inches to Sq.Ft & QuotationItem creation")
    void testFormulaCalculationsAndQuotationSave() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // Create Master Data with L × W
        Pricing pricingLW = pricingService.saveUnifiedMasterData(
                null,
                null,
                "Bedroom Space " + suffix,
                "🛏️",
                null,
                "Wardrobe " + suffix,
                null,
                "MDF Core " + suffix,
                null,
                "Laminate 1mm",
                "Internal LED Strip Channel",
                PricingMode.PREMIUM,
                new BigDecimal("1500.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Wardrobe sliding"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase("Bedroom Space " + suffix).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()).get(0);
        MaterialSpecification spec = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId()).get(0);

        // Verify L × W conversion: Length = 72 in, Width = 48 in -> Area = (72 * 48) / 144 = 24.00 Sq.Ft
        BigDecimal sqftLW = CalculationFormula.calculateArea(CalculationFormula.FORMULA_L_X_W,
                new BigDecimal("72"), new BigDecimal("48"), BigDecimal.ZERO, "in");
        assertEquals(new BigDecimal("24.00"), sqftLW);

        // Verify L × H conversion: Length = 96 in, Height = 48 in -> Area = (96 * 48) / 144 = 32.00 Sq.Ft
        BigDecimal sqftLH = CalculationFormula.calculateArea(CalculationFormula.FORMULA_L_X_H,
                new BigDecimal("96"), BigDecimal.ZERO, new BigDecimal("48"), "in");
        assertEquals(new BigDecimal("32.00"), sqftLH);

        // Verify (L + W) × H conversion: L = 60 in, W = 30 in, H = 96 in -> (90 * 96) / 144 = 60.00 Sq.Ft
        BigDecimal sqftLWH = CalculationFormula.calculateArea(CalculationFormula.FORMULA_L_PLUS_W_X_H,
                new BigDecimal("60"), new BigDecimal("30"), new BigDecimal("96"), "in");
        assertEquals(new BigDecimal("60.00"), sqftLWH);

        // Create complete quotation with MaterialSpecification
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Jane Designer", "9876501234", "jane@example.com", "Flat 4B, Residency"
        );
        AdminQuotationBuilderService.SizeRequest sizeReq = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("72"), new BigDecimal("48"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(),
                div.getId(),
                null,
                mat.getId(),
                opt.getId(),
                spec.getId(),
                "Master Bedroom Wardrobe",
                "Custom polyurethane coat",
                List.of(sizeReq)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "First Floor", "Master Bedroom", "Complete bedroom interior", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                testBranch.getId(),
                LocalDate.now(),
                PricingMode.PREMIUM,
                "Contemporary style",
                "Client requested sample before final assembly",
                List.of(roomReq)
        );

        Quotation savedQuotation = builderService.save(builderReq, testAdmin.getEmail());
        assertNotNull(savedQuotation);
        assertNotNull(savedQuotation.getId());

        List<QuotationItem> items = quotationItemRepository.findByMaterialSpecificationIdOrderByCreatedAtDesc(spec.getId());
        assertEquals(1, items.size());
        QuotationItem item = items.get(0);

        // Verify item references MaterialSpecification (Level 5)
        assertNotNull(item.getMaterialSpecification());
        assertEquals(spec.getId(), item.getMaterialSpecification().getId());
        assertEquals("INTERNAL LED STRIP CHANNEL", item.getMaterialSpecification().getName());

        // Verify calculated Sq.Ft is 24.00 and amount = 24.00 * 1500 = 36000.00
        assertEquals(new BigDecimal("24.00"), item.getCalculatedSqft());
        assertEquals(new BigDecimal("36000.00"), item.getAmount());

        // Verify builder load endpoint returns materialSpecificationId and name
        ResponseEntity<?> loadResp = adminQuotationBuilderController.loadQuotation(savedQuotation.getId());
        assertTrue(loadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResp.getBody();
        assertNotNull(data);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("rooms");
        assertFalse(rooms.isEmpty());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> loadedItems = (List<Map<String, Object>>) rooms.get(0).get("items");
        assertEquals(1, loadedItems.size());
        assertEquals(spec.getId(), loadedItems.get(0).get("materialSpecificationId"));
        assertEquals("INTERNAL LED STRIP CHANNEL", loadedItems.get(0).get("materialSpecificationName"));
    }

    @Test
    @DisplayName("5. Safe Deactivation: records referenced by quotations are deactivated rather than hard-deleted")
    void testSafeDeactivationWhenReferenced() {
        String suffix = String.valueOf(System.currentTimeMillis());

        pricingService.saveUnifiedMasterData(
                null,
                null,
                "Office Space " + suffix,
                "💼",
                null,
                "Workstation " + suffix,
                null,
                "Pre-lam Particle " + suffix,
                null,
                "Classic Oak",
                "Cable Grommet Precut",
                PricingMode.MIDDLE,
                new BigDecimal("950.00"),
                "SQFT",
                CalculationFormula.FORMULA_L_X_W,
                "Modular office desk"
        );

        WorkCategory cat = workCategoryRepository.findByNameIgnoreCase("Office Space " + suffix).orElseThrow();
        Division div = divisionRepository.findByWorkCategoryIdOrderByDisplayOrderAsc(cat.getId()).get(0);
        Material mat = materialRepository.findActiveByDivisionId(div.getId()).get(0);
        MaterialOption opt = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(mat.getId()).get(0);
        MaterialSpecification spec = materialSpecificationRepository.findByMaterialOptionIdOrderByDisplayOrderAsc(opt.getId()).get(0);

        // Create quotation referencing these records
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Corporate Client", "9112233445", "corp@client.com", "Tech Park"
        );
        AdminQuotationBuilderService.SizeRequest sizeReq = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("60"), new BigDecimal("30"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), null, mat.getId(), opt.getId(), spec.getId(),
                "Office Workstation", "Standard cable grommet", List.of(sizeReq)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Open Workspace", "Modular desk setup", List.of(itemReq)
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, testBranch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                null, null, List.of(roomReq)
        );

        Quotation quotation = builderService.save(builderReq, testAdmin.getEmail());
        assertNotNull(quotation);

        // 1. Delete Work Category -> should be safely deactivated
        workCategoryService.deleteCategory(cat.getId());
        WorkCategory reloadedCat = workCategoryRepository.findById(cat.getId()).orElse(null);
        assertNotNull(reloadedCat);
        assertFalse(reloadedCat.getActive(), "WorkCategory must be deactivated, not deleted from database");

        // 2. Delete Division -> should be safely deactivated
        divisionService.deleteDivision(div.getId());
        Division reloadedDiv = divisionRepository.findById(div.getId()).orElse(null);
        assertNotNull(reloadedDiv);
        assertFalse(reloadedDiv.getActive(), "Division must be deactivated, not deleted from database");

        // 3. Delete Material Option -> should be safely deactivated
        materialOptionService.deleteOption(opt.getId());
        MaterialOption reloadedOpt = materialOptionRepository.findById(opt.getId()).orElse(null);
        assertNotNull(reloadedOpt);
        assertFalse(reloadedOpt.getActive(), "MaterialOption must be deactivated, not deleted from database");

        // 4. Delete Material Specification -> should be safely deactivated
        materialSpecificationService.deleteSpecification(spec.getId());
        MaterialSpecification reloadedSpec = materialSpecificationRepository.findById(spec.getId()).orElse(null);
        assertNotNull(reloadedSpec);
        assertFalse(reloadedSpec.getActive(), "MaterialSpecification must be deactivated, not deleted from database");

        // Verify historical quotation and its item are completely preserved
        Quotation reloadedQuotation = quotationRepository.findById(quotation.getId()).orElse(null);
        assertNotNull(reloadedQuotation);
        List<QuotationItem> reloadedItems = quotationItemRepository.findByMaterialSpecificationIdOrderByCreatedAtDesc(spec.getId());
        assertEquals(1, reloadedItems.size());
        assertEquals(cat.getId(), reloadedItems.get(0).getWorkCategory().getId());
        assertEquals(div.getId(), reloadedItems.get(0).getDivision().getId());
        assertEquals(mat.getId(), reloadedItems.get(0).getMaterial().getId());
        assertEquals(opt.getId(), reloadedItems.get(0).getMaterialOption().getId());
        assertEquals(spec.getId(), reloadedItems.get(0).getMaterialSpecification().getId());
    }
}
