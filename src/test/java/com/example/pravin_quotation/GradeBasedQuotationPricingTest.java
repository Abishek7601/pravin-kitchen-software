package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GradeBasedQuotationPricingTest {

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

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

    private User adminUser;
    private User employeeUser;
    private Authentication employeeAuth;
    private Branch branch;
    private Customer customer;
    private WorkCategory workCategory;
    private Division division;
    private Item item;

    // Pattern A: Single material with options having Pricing per grade
    private Material patternAMaterial;
    private MaterialOption patternAOption;

    // Pattern B: Separate materials per grade in same division
    private Material patternBMaterialPremium;
    private Material patternBMaterialMiddle;
    private Material patternBMaterialEconomy;

    // Material with Files unit
    private Material filesMaterial;
    private MaterialOption filesOption;

    // Material available ONLY in Premium grade
    private Material exclusivePremiumMaterial;

    @BeforeEach
    void setUp() {
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Chennai Main");
            b.setActive(true);
            return branchRepository.save(b);
        });

        adminUser = userRepository.findByEmail("admin@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Tester");
            u.setEmail("admin@pravin.com");
            u.setPassword("password");
            u.setRole(Role.SUPER_ADMIN);
            u.setBranch(branch);
            u.setActive(true);
            return userRepository.save(u);
        });

        employeeUser = userRepository.findByEmail("employee@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Employee Tester");
            u.setEmail("employee@pravin.com");
            u.setPassword("password");
            u.setRole(Role.EMPLOYEE);
            u.setBranch(branch);
            u.setActive(true);
            return userRepository.save(u);
        });

        customer = new Customer();
        customer.setName("Grade Test Customer");
        customer.setPhone("9876543299");
        customer.setEmail("gradetest@example.com");
        customer.setAddress("123 Test Street");
        customer.setBranch(branch);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        workCategory = new WorkCategory();
        workCategory.setName("Living Area");
        workCategory.setActive(true);
        workCategory = workCategoryRepository.save(workCategory);

        division = new Division();
        division.setName("TV Unit Cabinet");
        division.setWorkCategory(workCategory);
        division.setActive(true);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("TV Storage");
        item.setDivision(division);
        item.setActive(true);
        item = itemRepository.save(item);

        // --- Pattern A: Multi-grade option pricing ---
        patternAMaterial = new Material();
        patternAMaterial.setName("Marine Plywood Multi");
        patternAMaterial.setUnit("Sq.ft");
        patternAMaterial.setFormula("L × H");
        patternAMaterial.setMeasurementType(MeasurementType.SQFT);
        patternAMaterial.setDivision(division);
        patternAMaterial.setItem(item);
        patternAMaterial.setActive(true);
        patternAMaterial = materialRepository.save(patternAMaterial);

        patternAOption = new MaterialOption();
        patternAOption.setName("18mm Sheet");
        patternAOption.setMaterial(patternAMaterial);
        patternAOption.setActive(true);
        patternAOption = materialOptionRepository.save(patternAOption);

        Pricing pPremium = new Pricing();
        pPremium.setMaterialOption(patternAOption);
        pPremium.setPricingMode(PricingMode.PREMIUM);
        pPremium.setRate(BigDecimal.valueOf(350.00));
        pPremium.setUnit("Sq.ft");
        pPremium.setActive(true);
        pricingRepository.save(pPremium);

        Pricing pMiddle = new Pricing();
        pMiddle.setMaterialOption(patternAOption);
        pMiddle.setPricingMode(PricingMode.MIDDLE);
        pMiddle.setRate(BigDecimal.valueOf(280.00));
        pMiddle.setUnit("Sq.ft");
        pMiddle.setActive(true);
        pricingRepository.save(pMiddle);

        Pricing pEconomy = new Pricing();
        pEconomy.setMaterialOption(patternAOption);
        pEconomy.setPricingMode(PricingMode.ECONOMY);
        pEconomy.setRate(BigDecimal.valueOf(220.00));
        pEconomy.setUnit("Sq.ft");
        pEconomy.setActive(true);
        pricingRepository.save(pEconomy);

        // --- Pattern B: Grade-specific Material entities ---
        patternBMaterialPremium = new Material();
        patternBMaterialPremium.setName("BWP Plywood + Laminate");
        patternBMaterialPremium.setPricingGrade(PricingMode.PREMIUM);
        patternBMaterialPremium.setRate(BigDecimal.valueOf(300.00));
        patternBMaterialPremium.setUnit("Sq.ft");
        patternBMaterialPremium.setFormula("L × H");
        patternBMaterialPremium.setMeasurementType(MeasurementType.SQFT);
        patternBMaterialPremium.setDivision(division);
        patternBMaterialPremium.setItem(null);
        patternBMaterialPremium.setActive(true);
        patternBMaterialPremium = materialRepository.save(patternBMaterialPremium);

        MaterialOption optBPremium = new MaterialOption();
        optBPremium.setName("Default");
        optBPremium.setMaterial(patternBMaterialPremium);
        optBPremium.setActive(true);
        optBPremium = materialOptionRepository.save(optBPremium);

        Pricing pricingBPremium = new Pricing();
        pricingBPremium.setMaterialOption(optBPremium);
        pricingBPremium.setPricingMode(PricingMode.PREMIUM);
        pricingBPremium.setRate(BigDecimal.valueOf(300.00));
        pricingBPremium.setUnit("Sq.ft");
        pricingBPremium.setActive(true);
        pricingRepository.save(pricingBPremium);

        patternBMaterialMiddle = new Material();
        patternBMaterialMiddle.setName("BWP Plywood + Laminate");
        patternBMaterialMiddle.setPricingGrade(PricingMode.MIDDLE);
        patternBMaterialMiddle.setRate(BigDecimal.valueOf(250.00));
        patternBMaterialMiddle.setUnit("Sq.ft");
        patternBMaterialMiddle.setFormula("L × H");
        patternBMaterialMiddle.setMeasurementType(MeasurementType.SQFT);
        patternBMaterialMiddle.setDivision(division);
        patternBMaterialMiddle.setItem(null);
        patternBMaterialMiddle.setActive(true);
        patternBMaterialMiddle = materialRepository.save(patternBMaterialMiddle);

        MaterialOption optBMiddle = new MaterialOption();
        optBMiddle.setName("Default");
        optBMiddle.setMaterial(patternBMaterialMiddle);
        optBMiddle.setActive(true);
        optBMiddle = materialOptionRepository.save(optBMiddle);

        Pricing pricingBMiddle = new Pricing();
        pricingBMiddle.setMaterialOption(optBMiddle);
        pricingBMiddle.setPricingMode(PricingMode.MIDDLE);
        pricingBMiddle.setRate(BigDecimal.valueOf(250.00));
        pricingBMiddle.setUnit("Sq.ft");
        pricingBMiddle.setActive(true);
        pricingRepository.save(pricingBMiddle);

        patternBMaterialEconomy = new Material();
        patternBMaterialEconomy.setName("BWP Plywood + Laminate");
        patternBMaterialEconomy.setPricingGrade(PricingMode.ECONOMY);
        patternBMaterialEconomy.setRate(BigDecimal.valueOf(200.00));
        patternBMaterialEconomy.setUnit("Sq.ft");
        patternBMaterialEconomy.setFormula("L × H");
        patternBMaterialEconomy.setMeasurementType(MeasurementType.SQFT);
        patternBMaterialEconomy.setDivision(division);
        patternBMaterialEconomy.setItem(null);
        patternBMaterialEconomy.setActive(true);
        patternBMaterialEconomy = materialRepository.save(patternBMaterialEconomy);

        MaterialOption optBEconomy = new MaterialOption();
        optBEconomy.setName("Default");
        optBEconomy.setMaterial(patternBMaterialEconomy);
        optBEconomy.setActive(true);
        optBEconomy = materialOptionRepository.save(optBEconomy);

        Pricing pricingBEconomy = new Pricing();
        pricingBEconomy.setMaterialOption(optBEconomy);
        pricingBEconomy.setPricingMode(PricingMode.ECONOMY);
        pricingBEconomy.setRate(BigDecimal.valueOf(200.00));
        pricingBEconomy.setUnit("Sq.ft");
        pricingBEconomy.setActive(true);
        pricingRepository.save(pricingBEconomy);

        // --- Files unit Material ---
        filesMaterial = new Material();
        filesMaterial.setName("3D Architectural Drawings");
        filesMaterial.setUnit("Files");
        filesMaterial.setMeasurementType(MeasurementType.FILES);
        filesMaterial.setDivision(division);
        filesMaterial.setItem(item);
        filesMaterial.setRate(BigDecimal.valueOf(1000.00));
        filesMaterial.setActive(true);
        filesMaterial = materialRepository.save(filesMaterial);

        filesOption = new MaterialOption();
        filesOption.setName("Standard Set");
        filesOption.setMaterial(filesMaterial);
        filesOption.setActive(true);
        filesOption = materialOptionRepository.save(filesOption);

        Pricing pFiles = new Pricing();
        pFiles.setMaterialOption(filesOption);
        pFiles.setPricingMode(PricingMode.PREMIUM);
        pFiles.setRate(BigDecimal.valueOf(1000.00));
        pFiles.setUnit("Files");
        pFiles.setActive(true);
        pricingRepository.save(pFiles);

        // --- Exclusive Premium Material (no middle or economy available) ---
        exclusivePremiumMaterial = new Material();
        exclusivePremiumMaterial.setName("Italian Exotic Veneer");
        exclusivePremiumMaterial.setPricingGrade(PricingMode.PREMIUM);
        exclusivePremiumMaterial.setRate(BigDecimal.valueOf(900.00));
        exclusivePremiumMaterial.setUnit("Sq.ft");
        exclusivePremiumMaterial.setMeasurementType(MeasurementType.SQFT);
        exclusivePremiumMaterial.setDivision(division);
        exclusivePremiumMaterial.setItem(item);
        exclusivePremiumMaterial.setActive(true);
        exclusivePremiumMaterial = materialRepository.save(exclusivePremiumMaterial);
    }

    @Test
    @DisplayName("1. Pattern A: Rate resolution across grades for single material with options")
    void testPatternA_MultiGradeOptionsResolution() {
        // Direct option pricing lookup for PREMIUM, MIDDLE, ECONOMY
        AdminQuotationBuilderService.ResolvedMaterialRate resPremium =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternAMaterial.getId(), patternAOption.getId(), PricingMode.PREMIUM);
        assertTrue(resPremium.available());
        assertEquals(0, new BigDecimal("350.00").compareTo(resPremium.rate()));

        AdminQuotationBuilderService.ResolvedMaterialRate resMiddle =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternAMaterial.getId(), patternAOption.getId(), PricingMode.MIDDLE);
        assertTrue(resMiddle.available());
        assertEquals(0, new BigDecimal("280.00").compareTo(resMiddle.rate()));

        AdminQuotationBuilderService.ResolvedMaterialRate resEconomy =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternAMaterial.getId(), patternAOption.getId(), PricingMode.ECONOMY);
        assertTrue(resEconomy.available());
        assertEquals(0, new BigDecimal("220.00").compareTo(resEconomy.rate()));
    }

    @Test
    @DisplayName("2. Pattern B: Cross-grade counterpart resolution when switching grades")
    void testPatternB_CrossGradeCounterpartResolution() {
        // Starting from the Premium material ID, switch to MIDDLE grade
        AdminQuotationBuilderService.ResolvedMaterialRate resMiddle =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternBMaterialPremium.getId(), null, PricingMode.MIDDLE);

        assertTrue(resMiddle.available(), "Counterpart material should be available in MIDDLE grade");
        assertEquals(patternBMaterialPremium.getId(), resMiddle.materialId(), "Original material ID must be preserved when switching to Middle grade");
        assertEquals(0, new BigDecimal("250.00").compareTo(resMiddle.rate()), "Should load Middle grade rate of 250");

        // Now switch from Middle material ID to ECONOMY grade
        AdminQuotationBuilderService.ResolvedMaterialRate resEconomy =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternBMaterialMiddle.getId(), null, PricingMode.ECONOMY);

        assertTrue(resEconomy.available(), "Counterpart material should be available in ECONOMY grade");
        assertEquals(patternBMaterialMiddle.getId(), resEconomy.materialId(), "Original material ID must be preserved when switching to Economy grade");
        assertEquals(0, new BigDecimal("200.00").compareTo(resEconomy.rate()), "Should load Economy grade rate of 200");

        // Now switch back from Economy material ID to PREMIUM grade
        AdminQuotationBuilderService.ResolvedMaterialRate resPremium =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternBMaterialEconomy.getId(), null, PricingMode.PREMIUM);

        assertTrue(resPremium.available(), "Counterpart material should be available in PREMIUM grade");
        assertEquals(patternBMaterialEconomy.getId(), resPremium.materialId(), "Original material ID must be preserved when switching to Premium grade");
        assertEquals(0, new BigDecimal("300.00").compareTo(resPremium.rate()), "Should restore Premium grade rate of 300");
    }

    @Test
    @DisplayName("3. Material unavailable in target grade is handled cleanly without crashing")
    void testMaterialUnavailableInTargetGrade() {
        AdminQuotationBuilderService.ResolvedMaterialRate res =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), exclusivePremiumMaterial.getId(), null, PricingMode.ECONOMY);

        assertFalse(res.available(), "Material exclusive to Premium should not be available in Economy");
        assertNull(res.rate(), "Rate should be null when unavailable");
        assertNotNull(res.message(), "Should provide a clear explanation message");
        assertTrue(res.message().equalsIgnoreCase("No material available in Economy Grade") || res.message().toUpperCase().contains("ECONOMY"), "Message should mention target grade");
        assertEquals("No material available in Economy Grade", res.message());
    }

    @Test
    @DisplayName("4. Admin Quotation Edit: Switch grade from Premium to Middle and verify rates, amounts and totals recalculation")
    void testAdminQuotationEdit_ChangeGradeToMiddle() {
        // Step 1: Create initial quotation in PREMIUM grade
        // Item 1: BWP Plywood @ Premium (₹300), Size 10 x 0 x 1 (sqft = 10), amount = ₹3000
        // Item 2: Files unit material @ custom rate ₹1500, qty 2, amount = ₹3000
        // Total = ₹6000
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(120), BigDecimal.valueOf(12), BigDecimal.valueOf(12), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq1 = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                patternBMaterialPremium.getId(), null, null,
                "TV Storage Unit", "Premium Spec", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(300.00)
        );

        AdminQuotationBuilderService.SizeRequest sizeFiles = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq2 = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                filesMaterial.getId(), filesOption.getId(), null,
                "3D Renderings", "Files Spec", List.of(sizeFiles),
                MeasurementType.FILES, BigDecimal.valueOf(2), null, BigDecimal.valueOf(1500.00)
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Hall", "Living room woodwork", List.of(itemReq1, itemReq2)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM,
                "Initial Requirements", "Initial Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation createdQuotation = adminQuotationBuilderService.save(createReq, adminUser.getEmail());
        assertNotNull(createdQuotation.getId());
        Long quotationId = createdQuotation.getId();
        assertEquals(PricingMode.PREMIUM, createdQuotation.getPricingMode());

        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotationId);
        assertEquals(1, rooms.size());
        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(rooms.get(0).getId());
        assertEquals(2, items.size());

        QuotationItem initialSqftItem = items.get(0);
        assertEquals(0, new BigDecimal("300.00").compareTo(initialSqftItem.getRate()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(initialSqftItem.getAmount()));

        QuotationItem initialFilesItem = items.get(1);
        assertEquals(0, new BigDecimal("1500.00").compareTo(initialFilesItem.getRate()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(initialFilesItem.getAmount()));
        assertEquals(0, new BigDecimal("6000.00").compareTo(createdQuotation.getGrandTotal()));

        // Step 2: Edit Builder - change grade to MIDDLE
        // Sqft material rate should change from 300 to 250 -> Amount = 10 * 250 = ₹2500
        // Files unit material must preserve custom rate ₹1500 -> Amount = 2 * 1500 = ₹3000
        // New grand total = ₹2500 + ₹3000 = ₹5500
        AdminQuotationBuilderService.ItemRequest editItem1 = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                patternBMaterialPremium.getId(), null, null, // Even if old material ID is passed
                "TV Storage Unit", "Middle Spec", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, null // Rate resolved from Middle grade
        );

        AdminQuotationBuilderService.ItemRequest editItem2 = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                filesMaterial.getId(), filesOption.getId(), null,
                "3D Renderings", "Files Spec", List.of(sizeFiles),
                MeasurementType.FILES, BigDecimal.valueOf(2), null, BigDecimal.valueOf(1500.00) // Preserved rate
        );

        AdminQuotationBuilderService.RoomRequest editRoomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Hall", "Living room woodwork updated", List.of(editItem1, editItem2)
        );

        AdminQuotationBuilderService.BuilderRequest updateReq = new AdminQuotationBuilderService.BuilderRequest(
                quotationId, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Updated Requirements", "Updated Notes", List.of(editRoomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation updatedQuotation = adminQuotationBuilderService.save(updateReq, adminUser.getEmail());

        // Verify quotation ID preserved - NO duplicate!
        assertEquals(quotationId, updatedQuotation.getId());
        assertEquals(PricingMode.MIDDLE, updatedQuotation.getPricingMode());

        List<QuotationRoom> updatedRooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotationId);
        assertEquals(1, updatedRooms.size());
        List<QuotationItem> updatedItems = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(updatedRooms.get(0).getId());
        assertEquals(2, updatedItems.size());

        QuotationItem updatedSqftItem = updatedItems.get(0);
        // Verify Middle grade rate ₹250 and amount ₹2500
        assertEquals(0, new BigDecimal("250.00").compareTo(updatedSqftItem.getRate()));
        assertEquals(0, new BigDecimal("2500.00").compareTo(updatedSqftItem.getAmount()));
        assertEquals(patternBMaterialPremium.getId(), updatedSqftItem.getMaterial().getId(), "Item should preserve its original material ID");

        QuotationItem updatedFilesItem = updatedItems.get(1);
        // Verify Files rate preserved at ₹1500 and amount ₹3000
        assertEquals(0, new BigDecimal("1500.00").compareTo(updatedFilesItem.getRate()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(updatedFilesItem.getAmount()));

        // Grand total should now be ₹5500
        assertEquals(0, new BigDecimal("5500.00").compareTo(updatedQuotation.getGrandTotal()));

        // Step 3: Verify Controller rate endpoint for grade change
        ResponseEntity<?> rateResp = adminQuotationBuilderController.rate(
                branch.getId(), division.getId(), null, patternBMaterialPremium.getId(), PricingMode.MIDDLE
        );
        assertTrue(rateResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) rateResp.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("available")));
        assertEquals(0, new BigDecimal("250.00").compareTo(new BigDecimal(body.get("rate").toString())));
        assertEquals(patternBMaterialPremium.getId(), body.get("materialId"), "Controller must return original material ID");
    }

    @Test
    @DisplayName("5. Employee Quotation Edit: Switch grade from Premium to Economy and verify parity")
    void testEmployeeQuotationEdit_ChangeGradeToEconomy() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        // Step 1: Create initial quotation via Employee
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(120), BigDecimal.valueOf(12), BigDecimal.valueOf(12), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq1 = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                patternBMaterialPremium.getId(), null, null,
                "TV Storage Unit", "Premium Spec", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(300.00)
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Hall", "Living room woodwork", List.of(itemReq1)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM,
                "Employee Req", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept", "application/json");
        Object saveResp = employeeQuotationController.saveBuilderQuotation(createReq, request, auth, null);
        assertNotNull(saveResp);
        assertTrue(saveResp instanceof ResponseEntity<?>);
        @SuppressWarnings("unchecked")
        Map<String, Object> createRespBody = (Map<String, Object>) ((ResponseEntity<?>) saveResp).getBody();
        assertNotNull(createRespBody);
        Long quoteId = ((Number) createRespBody.get("quotationId")).longValue();

        Quotation empQuotation = quotationRepository.findById(quoteId).orElseThrow();
        assertEquals(PricingMode.PREMIUM, empQuotation.getPricingMode());
        assertEquals(0, new BigDecimal("3000.00").compareTo(empQuotation.getGrandTotal()));

        // Step 2: Employee switches grade to ECONOMY in Edit Builder
        // Verify Employee builderRate endpoint returns Economy rate (₹200)
        ResponseEntity<?> empRateResp = employeeQuotationController.builderRate(
                branch.getId(), division.getId(), null, patternBMaterialPremium.getId(), PricingMode.ECONOMY
        );
        assertTrue(empRateResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> empRateBody = (Map<String, Object>) empRateResp.getBody();
        assertNotNull(empRateBody);
        assertTrue(Boolean.TRUE.equals(empRateBody.get("available")));
        assertEquals(0, new BigDecimal("200.00").compareTo(new BigDecimal(empRateBody.get("rate").toString())));
        assertEquals(patternBMaterialPremium.getId(), empRateBody.get("materialId"), "Controller must return original material ID");

        // Step 3: Save updated quotation with ECONOMY grade
        AdminQuotationBuilderService.ItemRequest editItem = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                patternBMaterialPremium.getId(), null, null,
                "TV Storage Unit", "Economy Spec", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, null // Resolved from Economy grade
        );

        AdminQuotationBuilderService.RoomRequest editRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Hall", "Updated woodwork", List.of(editItem)
        );

        AdminQuotationBuilderService.BuilderRequest updateReq = new AdminQuotationBuilderService.BuilderRequest(
                quoteId, custReq, branch.getId(), LocalDate.now(), PricingMode.ECONOMY,
                "Employee Req Updated", "Notes", List.of(editRoom),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        employeeQuotationController.saveBuilderQuotation(updateReq, request, auth, null);

        Quotation updatedEmpQuote = quotationRepository.findById(quoteId).orElseThrow();
        assertEquals(PricingMode.ECONOMY, updatedEmpQuote.getPricingMode());
        // Rate is 200 * 10 sqft = 2000
        assertEquals(0, new BigDecimal("2000.00").compareTo(updatedEmpQuote.getGrandTotal()));

        // Step 4: Verify PDF generation succeeds with the updated Economy quotation
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quoteId);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "Generated PDF should not be empty");
    }

    @Test
    @DisplayName("6. Edit Builder Load: Formula and measurements preserved, preventing zero sqft or amounts")
    void testEditBuilderLoadPreservesFormulaAndMeasurements_NoZeroAmounts() {
        // Create an item with L x H formula (like Foyer Shoe Rack: L=23, W=0, H=12, rate=250)
        Material shoeRackMat = new Material();
        shoeRackMat.setName("Shoe Rack Ply");
        shoeRackMat.setFormula("L × H");
        shoeRackMat.setUnit("Sq.ft");
        shoeRackMat.setMeasurementType(MeasurementType.SQFT);
        shoeRackMat.setDivision(division);
        shoeRackMat.setPricingGrade(PricingMode.MIDDLE);
        shoeRackMat.setRate(BigDecimal.valueOf(250.00));
        shoeRackMat.setActive(true);
        shoeRackMat = materialRepository.save(shoeRackMat);

        AdminQuotationBuilderService.SizeRequest sizeShoeRack = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(23), BigDecimal.ZERO, BigDecimal.valueOf(12), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                shoeRackMat.getId(), null, null,
                "Shoe Rack", "BWR Plywood", List.of(sizeShoeRack),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(250.00)
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Foyer woodwork", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Foyer woodwork", "", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation quotation = adminQuotationBuilderService.save(createReq, adminUser.getEmail());
        assertNotNull(quotation.getId());
        quotation.setEmployee(employeeUser);
        quotation = quotationRepository.save(quotation);

        // Verify Admin controller loadQuotation
        ResponseEntity<?> adminLoadResp = adminQuotationBuilderController.loadQuotation(quotation.getId());
        assertTrue(adminLoadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> adminData = (Map<String, Object>) adminLoadResp.getBody();
        assertNotNull(adminData);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> adminRooms = (List<Map<String, Object>>) adminData.get("rooms");
        assertEquals(1, adminRooms.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> adminItems = (List<Map<String, Object>>) adminRooms.get(0).get("items");
        assertEquals(1, adminItems.size());
        Map<String, Object> adminItem = adminItems.get(0);

        assertEquals("L × H", adminItem.get("formula"), "Formula must be returned in loadQuotation");
        assertEquals(division.getId(), adminItem.get("divisionId"), "Division ID must be returned");
        assertEquals(shoeRackMat.getId(), adminItem.get("materialId"), "Material ID must be preserved");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        // Verify Employee controller loadQuotation
        ResponseEntity<?> empLoadResp = employeeQuotationController.builderLoadQuotation(quotation.getId(), auth);
        assertTrue(empLoadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> empData = (Map<String, Object>) empLoadResp.getBody();
        assertNotNull(empData);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> empRooms = (List<Map<String, Object>>) empData.get("rooms");
        assertEquals(1, empRooms.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> empItems = (List<Map<String, Object>>) empRooms.get(0).get("items");
        assertEquals(1, empItems.size());
        Map<String, Object> empItem = empItems.get(0);

        assertEquals("L × H", empItem.get("formula"), "Employee loadQuotation must return formula");
        assertEquals(division.getId(), empItem.get("divisionId"), "Employee loadQuotation must return divisionId");
        assertEquals(shoeRackMat.getId(), empItem.get("materialId"), "Material ID must be preserved");
    }

    @Test
    @DisplayName("7. Preserving items on grade change: Material ID, specifications, dimensions, quantity preserved")
    void testGradeChangePreservesAllItemAttributes_UpdatesOnlyRates() {
        // Create quote in Middle grade with specific dimensions, quantity, specs
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(48), BigDecimal.valueOf(24), BigDecimal.valueOf(36), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                patternBMaterialMiddle.getId(), null, null,
                "Custom Unit", "Century Club Prime 16mm", List.of(size),
                MeasurementType.SQFT, BigDecimal.valueOf(3), null, BigDecimal.valueOf(250.00)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living", "Detailed description", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Req", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation quotation = adminQuotationBuilderService.save(createReq, adminUser.getEmail());
        Long quotationId = quotation.getId();

        // Switch to PREMIUM grade
        // Resolve rate for patternBMaterialMiddle in PREMIUM grade -> should return 300.00 and keep patternBMaterialMiddle.getId()
        AdminQuotationBuilderService.ResolvedMaterialRate resolved =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternBMaterialMiddle.getId(), null, PricingMode.PREMIUM);
        assertTrue(resolved.available());
        assertEquals(patternBMaterialMiddle.getId(), resolved.materialId(), "Material ID must NOT change when resolving rate");
        assertEquals(0, new BigDecimal("300.00").compareTo(resolved.rate()), "Rate should be updated to Premium rate of 300");

        // Save update with PREMIUM grade
        AdminQuotationBuilderService.ItemRequest editItem = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                patternBMaterialMiddle.getId(), null, null,
                "Custom Unit", "Century Club Prime 16mm", List.of(size),
                MeasurementType.SQFT, BigDecimal.valueOf(3), null, null // will resolve 300.00
        );
        AdminQuotationBuilderService.RoomRequest editRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living", "Detailed description", List.of(editItem)
        );
        AdminQuotationBuilderService.BuilderRequest updateReq = new AdminQuotationBuilderService.BuilderRequest(
                quotationId, custReq, branch.getId(), LocalDate.now(), PricingMode.PREMIUM,
                "Req", "Notes", List.of(editRoom),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation updatedQuote = adminQuotationBuilderService.save(updateReq, adminUser.getEmail());
        assertEquals(quotationId, updatedQuote.getId());
        assertEquals(PricingMode.PREMIUM, updatedQuote.getPricingMode());

        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotationId);
        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(rooms.get(0).getId());
        assertEquals(1, items.size());
        QuotationItem itemSaved = items.get(0);

        // Verify preserved attributes
        assertEquals(patternBMaterialMiddle.getId(), itemSaved.getMaterial().getId(), "Material ID preserved");
        assertEquals(division.getId(), itemSaved.getDivision().getId(), "Division ID preserved");
        assertEquals(workCategory.getId(), itemSaved.getWorkCategory().getId(), "Work Category ID preserved");
        assertEquals("Century Club Prime 16mm", itemSaved.getSpecification(), "Specification preserved");
        assertEquals(0, BigDecimal.valueOf(3).compareTo(itemSaved.getQuantity()), "Quantity preserved");

        // Verify rate updated to 300.00
        assertEquals(0, new BigDecimal("300.00").compareTo(itemSaved.getRate()), "Unit rate updated to Premium grade rate");
    }

    @Test
    @DisplayName("8. Verify exact required message format for unavailable materials in Middle and Economy grades")
    void testExactUnavailableMessageFormat() {
        // Exclusive Premium material requested in Middle grade
        AdminQuotationBuilderService.ResolvedMaterialRate resMiddle =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), exclusivePremiumMaterial.getId(), null, PricingMode.MIDDLE);
        assertFalse(resMiddle.available());
        assertNull(resMiddle.rate());
        assertEquals("No material available in Middle Grade", resMiddle.message());

        // Exclusive Premium material requested in Economy grade
        AdminQuotationBuilderService.ResolvedMaterialRate resEconomy =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), exclusivePremiumMaterial.getId(), null, PricingMode.ECONOMY);
        assertFalse(resEconomy.available());
        assertNull(resEconomy.rate());
        assertEquals("No material available in Economy Grade", resEconomy.message());
    }

    @Test
    @DisplayName("9. Controller parity: Admin and Employee rate endpoints return correct grade rates and unavailable messages")
    void testControllerRateParityAdminAndEmployee() {
        Authentication employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), employeeUser.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        // Available material in Middle grade
        ResponseEntity<?> adminResp = adminQuotationBuilderController.rate(branch.getId(), division.getId(), null, patternBMaterialPremium.getId(), PricingMode.MIDDLE);
        assertTrue(adminResp.getStatusCode().is2xxSuccessful());
        Map<?, ?> adminData = (Map<?, ?>) adminResp.getBody();
        assertNotNull(adminData);
        assertEquals(true, adminData.get("available"));
        assertEquals(0, new BigDecimal("250.00").compareTo(new BigDecimal(adminData.get("rate").toString())));

        ResponseEntity<?> empResp = employeeQuotationController.builderRate(branch.getId(), division.getId(), null, patternBMaterialPremium.getId(), PricingMode.MIDDLE, employeeAuth);
        assertTrue(empResp.getStatusCode().is2xxSuccessful());
        Map<?, ?> empData = (Map<?, ?>) empResp.getBody();
        assertNotNull(empData);
        assertEquals(true, empData.get("available"));
        assertEquals(0, new BigDecimal("250.00").compareTo(new BigDecimal(empData.get("rate").toString())));

        // Unavailable material in Middle grade
        ResponseEntity<?> adminUnavail = adminQuotationBuilderController.rate(branch.getId(), division.getId(), null, exclusivePremiumMaterial.getId(), PricingMode.MIDDLE);
        assertTrue(adminUnavail.getStatusCode().is2xxSuccessful());
        Map<?, ?> adminUnavailData = (Map<?, ?>) adminUnavail.getBody();
        assertNotNull(adminUnavailData);
        assertEquals(false, adminUnavailData.get("available"));
        assertNull(adminUnavailData.get("rate"));
        assertEquals("No material available in Middle Grade", adminUnavailData.get("message"));

        ResponseEntity<?> empUnavail = employeeQuotationController.builderRate(branch.getId(), division.getId(), null, exclusivePremiumMaterial.getId(), PricingMode.MIDDLE, employeeAuth);
        assertTrue(empUnavail.getStatusCode().is2xxSuccessful());
        Map<?, ?> empUnavailData = (Map<?, ?>) empUnavail.getBody();
        assertNotNull(empUnavailData);
        assertEquals(false, empUnavailData.get("available"));
        assertNull(empUnavailData.get("rate"));
        assertEquals("No material available in Middle Grade", empUnavailData.get("message"));
    }
}
