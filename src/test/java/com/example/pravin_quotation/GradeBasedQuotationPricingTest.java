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
        assertEquals(patternBMaterialMiddle.getId(), resMiddle.materialId(), "Should map to Middle material ID");
        assertEquals(0, new BigDecimal("250.00").compareTo(resMiddle.rate()), "Should load Middle grade rate of 250");

        // Now switch from Middle material ID to ECONOMY grade
        AdminQuotationBuilderService.ResolvedMaterialRate resEconomy =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternBMaterialMiddle.getId(), null, PricingMode.ECONOMY);

        assertTrue(resEconomy.available(), "Counterpart material should be available in ECONOMY grade");
        assertEquals(patternBMaterialEconomy.getId(), resEconomy.materialId(), "Should map to Economy material ID");
        assertEquals(0, new BigDecimal("200.00").compareTo(resEconomy.rate()), "Should load Economy grade rate of 200");

        // Now switch back from Economy material ID to PREMIUM grade
        AdminQuotationBuilderService.ResolvedMaterialRate resPremium =
                adminQuotationBuilderService.resolveMaterialAndRate(branch.getId(), division.getId(), patternBMaterialEconomy.getId(), null, PricingMode.PREMIUM);

        assertTrue(resPremium.available(), "Counterpart material should be available in PREMIUM grade");
        assertEquals(patternBMaterialPremium.getId(), resPremium.materialId(), "Should map to Premium material ID");
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
        assertTrue(res.message().contains("ECONOMY"), "Message should mention target grade");
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
        assertEquals(patternBMaterialMiddle.getId(), updatedSqftItem.getMaterial().getId(), "Item should be linked to Middle material counterpart");

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
        assertEquals(patternBMaterialMiddle.getId(), body.get("materialId"));
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
        assertEquals(patternBMaterialEconomy.getId(), empRateBody.get("materialId"));

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
}
