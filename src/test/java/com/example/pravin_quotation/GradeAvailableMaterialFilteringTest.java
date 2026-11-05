package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GradeAvailableMaterialFilteringTest {

    @Autowired
    private AdminQuotationBuilderController adminController;

    @Autowired
    private EmployeeQuotationController employeeController;

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private QuotationRepository quotationRepository;

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
    private DistrictPricingRepository districtPricingRepository;

    private Branch branchAlpha;
    private Branch branchBeta;
    private User employeeUser;
    private Authentication employeeAuth;
    private WorkCategory category;
    private Division division;
    private Item item;
    private Customer customer;

    // Materials
    private Material matA; // Premium only
    private Material matB; // Premium & Middle
    private Material matC; // Middle & Economy
    private Material matD; // Economy only
    private Material matE; // Branch Alpha Premium only (District Pricing)
    private Material matFiles; // Files material - Premium only
    private Material matRoll; // Roll material - Middle & Economy

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branchAlpha = new Branch();
        branchAlpha.setName("Branch Alpha " + ts);
        branchAlpha.setBranchAddress("Street 1");
        branchAlpha.setActive(true);
        branchAlpha = branchRepository.save(branchAlpha);

        branchBeta = new Branch();
        branchBeta.setName("Branch Beta " + ts);
        branchBeta.setBranchAddress("Street 2");
        branchBeta.setActive(true);
        branchBeta = branchRepository.save(branchBeta);

        employeeUser = new User();
        employeeUser.setName("Emp " + ts);
        employeeUser.setEmail("emp_" + ts + "@test.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branchAlpha);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        category = new WorkCategory();
        category.setName("Category " + ts);
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("Division " + ts);
        division.setWorkCategory(category);
        division.setActive(true);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Item " + ts);
        item.setDivision(division);
        item.setActive(true);
        item = itemRepository.save(item);

        customer = new Customer();
        customer.setName("Customer " + ts);
        customer.setPhone("9988776655");
        customer.setBranch(branchAlpha);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        // Material A: Premium only
        matA = createMaterial("MATERIAL A - PREMIUM ONLY", "Sq.ft", MeasurementType.SQFT, "(L + W) × H");
        addPricing(matA, PricingMode.PREMIUM, new BigDecimal("1000.00"));

        // Material B: Premium & Middle
        matB = createMaterial("MATERIAL B - PREMIUM AND MIDDLE", "Sq.ft", MeasurementType.SQFT, "(L + W) × H");
        addPricing(matB, PricingMode.PREMIUM, new BigDecimal("1200.00"));
        addPricing(matB, PricingMode.MIDDLE, new BigDecimal("900.00"));

        // Material C: Middle & Economy
        matC = createMaterial("MATERIAL C - MIDDLE AND ECONOMY", "Sq.ft", MeasurementType.SQFT, "L × W");
        addPricing(matC, PricingMode.MIDDLE, new BigDecimal("800.00"));
        addPricing(matC, PricingMode.ECONOMY, new BigDecimal("600.00"));

        // Material D: Economy only
        matD = createMaterial("MATERIAL D - ECONOMY ONLY", "Quantity", MeasurementType.QUANTITY, null);
        addPricing(matD, PricingMode.ECONOMY, new BigDecimal("500.00"));

        // Material E: Branch-specific (District pricing on Branch Alpha for Premium only)
        matE = createMaterial("MATERIAL E - BRANCH ALPHA ONLY", "Sq.ft", MeasurementType.SQFT, "L × H");
        Pricing pE = addPricing(matE, PricingMode.PREMIUM, null); // Base pricing inactive/null rate
        DistrictPricing dpE = new DistrictPricing();
        dpE.setBranch(branchAlpha);
        dpE.setPricing(pE);
        dpE.setRate(new BigDecimal("1500.00"));
        dpE.setActive(true);
        districtPricingRepository.save(dpE);

        // Files Material: Premium only
        matFiles = createMaterial("MATERIAL FILES - 3D ELEVATION", "Files", MeasurementType.FILES, null);
        matFiles.setImageUrl("/uploads/materials/test-3d.jpg");
        matFiles = materialRepository.save(matFiles);
        addPricing(matFiles, PricingMode.PREMIUM, new BigDecimal("2500.00"));

        // Roll Material: Middle & Economy
        matRoll = createMaterial("MATERIAL ROLL - WALLPAPER", "Roll", MeasurementType.ROLL, "L × W");
        matRoll.setCoveragePerRoll(new BigDecimal("50.00"));
        matRoll = materialRepository.save(matRoll);
        addPricing(matRoll, PricingMode.MIDDLE, new BigDecimal("1400.00"));
        addPricing(matRoll, PricingMode.ECONOMY, new BigDecimal("1100.00"));
    }

    private Material createMaterial(String name, String unit, MeasurementType measurementType, String formula) {
        Material m = new Material();
        m.setName(name);
        m.setItem(item);
        m.setDivision(division);
        m.setUnit(unit);
        m.setMeasurementType(measurementType);
        m.setFormula(formula);
        m.setActive(true);
        return materialRepository.save(m);
    }

    private Pricing addPricing(Material material, PricingMode mode, BigDecimal rate) {
        MaterialOption opt = new MaterialOption();
        opt.setName(material.getName() + " - " + mode.name());
        opt.setMaterial(material);
        opt.setActive(true);
        opt = materialOptionRepository.save(opt);

        Pricing p = new Pricing();
        p.setMaterialOption(opt);
        p.setPricingMode(mode);
        p.setRate(rate != null ? rate : BigDecimal.ZERO);
        p.setUnit(material.getUnit());
        p.setActive(true);
        return pricingRepository.save(p);
    }

    @Test
    @DisplayName("Admin: Premium Grade returns ONLY Premium-available materials when onlyAvailable=true")
    void testAdminPremiumGradeFiltering() {
        List<Map<String, Object>> materials = adminController.materials(
                branchAlpha.getId(), division.getId(), null, "PREMIUM", true
        );

        assertNotNull(materials);
        List<Long> ids = materials.stream().map(m -> (Long) m.get("id")).toList();

        // Must include: matA, matB, matE (branchAlpha), matFiles
        assertTrue(ids.contains(matA.getId()), "Material A must be in Premium");
        assertTrue(ids.contains(matB.getId()), "Material B must be in Premium");
        assertTrue(ids.contains(matE.getId()), "Material E must be in Premium for Branch Alpha");
        assertTrue(ids.contains(matFiles.getId()), "Files Material must be in Premium");

        // Must NOT include: matC, matD, matRoll
        assertFalse(ids.contains(matC.getId()), "Material C (Middle & Economy) must NOT be in Premium");
        assertFalse(ids.contains(matD.getId()), "Material D (Economy only) must NOT be in Premium");
        assertFalse(ids.contains(matRoll.getId()), "Material Roll (Middle & Economy) must NOT be in Premium");
    }

    @Test
    @DisplayName("Admin: Middle / Standard Grade returns ONLY Middle-available materials when onlyAvailable=true")
    void testAdminMiddleGradeFiltering() {
        List<Map<String, Object>> materials = adminController.materials(
                branchAlpha.getId(), division.getId(), null, "MIDDLE", true
        );

        assertNotNull(materials);
        List<Long> ids = materials.stream().map(m -> (Long) m.get("id")).toList();

        // Must include: matB, matC, matRoll
        assertTrue(ids.contains(matB.getId()), "Material B must be in Middle");
        assertTrue(ids.contains(matC.getId()), "Material C must be in Middle");
        assertTrue(ids.contains(matRoll.getId()), "Roll Material must be in Middle");

        // Must NOT include: matA, matD, matE, matFiles
        assertFalse(ids.contains(matA.getId()), "Material A (Premium only) must NOT be in Middle");
        assertFalse(ids.contains(matD.getId()), "Material D (Economy only) must NOT be in Middle");
        assertFalse(ids.contains(matE.getId()), "Material E (Premium only) must NOT be in Middle");
        assertFalse(ids.contains(matFiles.getId()), "Files Material (Premium only) must NOT be in Middle");
    }

    @Test
    @DisplayName("Admin: Economy Grade returns ONLY Economy-available materials when onlyAvailable=true")
    void testAdminEconomyGradeFiltering() {
        List<Map<String, Object>> materials = adminController.materials(
                branchAlpha.getId(), division.getId(), null, "ECONOMY", true
        );

        assertNotNull(materials);
        List<Long> ids = materials.stream().map(m -> (Long) m.get("id")).toList();

        // Must include: matC, matD, matRoll
        assertTrue(ids.contains(matC.getId()), "Material C must be in Economy");
        assertTrue(ids.contains(matD.getId()), "Material D must be in Economy");
        assertTrue(ids.contains(matRoll.getId()), "Roll Material must be in Economy");

        // Must NOT include: matA, matB, matE, matFiles
        assertFalse(ids.contains(matA.getId()), "Material A (Premium only) must NOT be in Economy");
        assertFalse(ids.contains(matB.getId()), "Material B (Premium & Middle) must NOT be in Economy");
        assertFalse(ids.contains(matE.getId()), "Material E must NOT be in Economy");
        assertFalse(ids.contains(matFiles.getId()), "Files Material must NOT be in Economy");
    }

    @Test
    @DisplayName("Branch-specific rate availability: Material E is excluded on Branch Beta")
    void testBranchSpecificFiltering() {
        List<Map<String, Object>> betaMats = adminController.materials(
                branchBeta.getId(), division.getId(), null, "PREMIUM", true
        );
        List<Long> betaIds = betaMats.stream().map(m -> (Long) m.get("id")).toList();
        assertFalse(betaIds.contains(matE.getId()), "Material E has pricing only for Branch Alpha and must NOT appear on Branch Beta");
    }

    @Test
    @DisplayName("Employee Parity: Employee builderMaterials returns identical filtered materials")
    void testEmployeeParityGradeFiltering() {
        List<Map<String, Object>> empPremMats = employeeController.builderMaterials(
                branchAlpha.getId(), division.getId(), null, "PREMIUM", true, employeeAuth
        );
        List<Long> ids = empPremMats.stream().map(m -> (Long) m.get("id")).toList();

        assertTrue(ids.contains(matA.getId()));
        assertTrue(ids.contains(matB.getId()));
        assertTrue(ids.contains(matFiles.getId()));
        assertFalse(ids.contains(matC.getId()));
        assertFalse(ids.contains(matD.getId()));
        assertFalse(ids.contains(matRoll.getId()));
    }

    @Test
    @DisplayName("Empty work type in selected grade: Division with no available materials returns empty list")
    void testNoMaterialsAvailableInGrade() {
        Division emptyDiv = new Division();
        emptyDiv.setName("Empty Division");
        emptyDiv.setWorkCategory(category);
        emptyDiv.setActive(true);
        emptyDiv = divisionRepository.save(emptyDiv);

        Material onlyPrem = new Material();
        onlyPrem.setName("ONLY PREMIUM");
        onlyPrem.setDivision(emptyDiv);
        onlyPrem.setUnit("Sq.ft");
        onlyPrem.setActive(true);
        onlyPrem = materialRepository.save(onlyPrem);
        addPricing(onlyPrem, PricingMode.PREMIUM, new BigDecimal("2000.00"));

        // In ECONOMY, this division has NO materials available
        List<Map<String, Object>> ecoMats = adminController.materials(
                branchAlpha.getId(), emptyDiv.getId(), null, "ECONOMY", true
        );
        assertTrue(ecoMats.isEmpty(), "When no materials have pricing in this grade, returned list must be empty");
    }

    @Test
    @DisplayName("Backend save validation blocks saving quotation if material is unavailable in selected grade")
    void testBackendSaveValidationBlocksUnavailableMaterial() {
        // Build quotation request with Material A in Economy grade (Material A is Premium only)
        AdminQuotationBuilderService.SizeRequest sizeReq = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("10.0"), null
        );

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(),
                division.getId(),
                item.getId(),
                matA.getId(),
                null,
                null,
                "Item desc",
                "Spec",
                List.of(sizeReq),
                MeasurementType.SQFT,
                BigDecimal.ONE,
                null,
                new BigDecimal("1000.00"),
                null
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living Room", "Work", List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), "", ""
        );

        AdminQuotationBuilderService.BuilderRequest saveReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branchAlpha.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                PricingMode.ECONOMY, // Notice: ECONOMY! matA is PREMIUM ONLY
                null,
                null,
                List.of(roomReq),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builderService.save(saveReq, employeeUser.getEmail());
        });

        assertTrue(ex.getMessage().contains("No material available in Economy Grade"),
                "Save must be rejected when material is unavailable in selected grade: " + ex.getMessage());
    }
}

