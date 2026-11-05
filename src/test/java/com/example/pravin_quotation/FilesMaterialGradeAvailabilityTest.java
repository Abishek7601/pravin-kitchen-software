package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
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
public class FilesMaterialGradeAvailabilityTest {

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

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

    private Branch branchA;
    private Branch branchB;
    private User adminUser;
    private User employeeUser;
    private Authentication employeeAuth;
    private WorkCategory workCategory;
    private Division division;
    private Item item;
    private Material filesMaterial;
    private MaterialOption filesOption;
    private Customer customer;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branchA = new Branch();
        branchA.setName("Branch Alpha " + ts);
        branchA.setBranchAddress("Alpha St");
        branchA.setActive(true);
        branchA = branchRepository.save(branchA);

        branchB = new Branch();
        branchB.setName("Branch Beta " + ts);
        branchB.setBranchAddress("Beta St");
        branchB.setActive(true);
        branchB = branchRepository.save(branchB);

        adminUser = new User();
        adminUser.setName("Admin " + ts);
        adminUser.setEmail("admin_" + ts + "@test.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Employee " + ts);
        employeeUser.setEmail("emp_" + ts + "@test.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branchA);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        workCategory = new WorkCategory();
        workCategory.setName("Category " + ts);
        workCategory.setActive(true);
        workCategory = workCategoryRepository.save(workCategory);

        division = new Division();
        division.setName("Division Elevation " + ts);
        division.setWorkCategory(workCategory);
        division.setActive(true);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Design Item " + ts);
        item.setDivision(division);
        item.setActive(true);
        item = itemRepository.save(item);

        filesMaterial = new Material();
        filesMaterial.setName("3D Perspective Render " + ts);
        filesMaterial.setItem(item);
        filesMaterial.setDivision(division);
        filesMaterial.setUnit("Files");
        filesMaterial.setMeasurementType(MeasurementType.FILES);
        filesMaterial.setImageUrl("/uploads/materials/test-3d-render.jpg");
        filesMaterial.setSpecification("High resolution 3D render files");
        filesMaterial.setActive(true);
        filesMaterial = materialRepository.save(filesMaterial);

        filesOption = new MaterialOption();
        filesOption.setName("Perspective Option " + ts);
        filesOption.setMaterial(filesMaterial);
        filesOption.setActive(true);
        filesOption = materialOptionRepository.save(filesOption);

        // Configure Pricing:
        // PREMIUM: 2500.00
        Pricing pPrem = new Pricing();
        pPrem.setMaterialOption(filesOption);
        pPrem.setPricingMode(PricingMode.PREMIUM);
        pPrem.setRate(new BigDecimal("2500.00"));
        pPrem.setUnit("Files");
        pPrem.setActive(true);
        pPrem = pricingRepository.save(pPrem);

        // MIDDLE: 1800.00
        Pricing pMid = new Pricing();
        pMid.setMaterialOption(filesOption);
        pMid.setPricingMode(PricingMode.MIDDLE);
        pMid.setRate(new BigDecimal("1800.00"));
        pMid.setUnit("Files");
        pMid.setActive(true);
        pMid = pricingRepository.save(pMid);

        // ECONOMY: No pricing configured (or inactive / 0.00)

        // Customer
        customer = new Customer();
        customer.setName("John Customer");
        customer.setPhone("9876543210");
        customer.setBranch(branchA);
        customer.setActive(true);
        customer = customerRepository.save(customer);
    }

    @Test
    void testFilesMaterialGradeAvailabilityInMaterialsEndpoint() {
        // Admin - PREMIUM
        List<Map<String, Object>> adminPremMats = adminQuotationBuilderController.materials(
                branchA.getId(), division.getId(), null, "PREMIUM"
        );
        Map<String, Object> premRow = adminPremMats.stream()
                .filter(m -> filesMaterial.getId().equals(m.get("id")))
                .findFirst().orElseThrow();
        assertEquals(true, premRow.get("available"), "Files material should be available in Premium");
        assertNotNull(premRow.get("rate"));
        assertEquals(0, new BigDecimal("2500.00").compareTo(new BigDecimal(premRow.get("rate").toString())));
        assertNull(premRow.get("unavailableMessage"));
        assertEquals("/uploads/materials/test-3d-render.jpg", premRow.get("imageUrl"), "Image must be displayed when available");

        // Admin - MIDDLE
        List<Map<String, Object>> adminMidMats = adminQuotationBuilderController.materials(
                branchA.getId(), division.getId(), null, "MIDDLE"
        );
        Map<String, Object> midRow = adminMidMats.stream()
                .filter(m -> filesMaterial.getId().equals(m.get("id")))
                .findFirst().orElseThrow();
        assertEquals(true, midRow.get("available"), "Files material should be available in Middle");
        assertNotNull(midRow.get("rate"));
        assertEquals(0, new BigDecimal("1800.00").compareTo(new BigDecimal(midRow.get("rate").toString())));
        assertEquals("/uploads/materials/test-3d-render.jpg", midRow.get("imageUrl"));

        // Admin - ECONOMY (NOT available)
        List<Map<String, Object>> adminEcoMats = adminQuotationBuilderController.materials(
                branchA.getId(), division.getId(), null, "ECONOMY"
        );
        Map<String, Object> ecoRow = adminEcoMats.stream()
                .filter(m -> filesMaterial.getId().equals(m.get("id")))
                .findFirst().orElseThrow();
        assertEquals(false, ecoRow.get("available"), "Files material should be UNAVAILABLE in Economy");
        assertNull(ecoRow.get("rate"));
        assertEquals("No material available in Economy Grade", ecoRow.get("unavailableMessage"));
        assertNull(ecoRow.get("imageUrl"), "Image must NOT be displayed when unavailable");

        // Employee Parity - ECONOMY
        List<Map<String, Object>> empEcoMats = employeeQuotationController.builderMaterials(
                branchA.getId(), division.getId(), null, "ECONOMY"
        );
        Map<String, Object> empEcoRow = empEcoMats.stream()
                .filter(m -> filesMaterial.getId().equals(m.get("id")))
                .findFirst().orElseThrow();
        assertEquals(false, empEcoRow.get("available"), "Employee should also see Files material UNAVAILABLE in Economy");
        assertEquals("No material available in Economy Grade", empEcoRow.get("unavailableMessage"));
        assertNull(empEcoRow.get("imageUrl"), "Employee: Image must NOT be displayed when unavailable");
    }

    @Test
    void testFilesMaterialRateEndpointValidation() {
        // Admin /builder/rate
        ResponseEntity<?> respPrem = adminQuotationBuilderController.rate(
                branchA.getId(), division.getId(), null, filesMaterial.getId(), PricingMode.PREMIUM
        );
        Map<?, ?> premBody = (Map<?, ?>) respPrem.getBody();
        assertNotNull(premBody);
        assertEquals(true, premBody.get("available"));
        assertEquals(new BigDecimal("2500.00"), premBody.get("rate"));

        ResponseEntity<?> respEco = adminQuotationBuilderController.rate(
                branchA.getId(), division.getId(), null, filesMaterial.getId(), PricingMode.ECONOMY
        );
        Map<?, ?> ecoBody = (Map<?, ?>) respEco.getBody();
        assertNotNull(ecoBody);
        assertEquals(false, ecoBody.get("available"));
        assertEquals("No material available in Economy Grade", ecoBody.get("message"));
        assertNull(ecoBody.get("rate"));

        // Employee /builder/rate
        ResponseEntity<?> empRespEco = employeeQuotationController.builderRate(
                branchA.getId(), division.getId(), null, filesMaterial.getId(), PricingMode.ECONOMY, employeeAuth
        );
        Map<?, ?> empEcoBody = (Map<?, ?>) empRespEco.getBody();
        assertNotNull(empEcoBody);
        assertEquals(false, empEcoBody.get("available"));
        assertEquals("No material available in Economy Grade", empEcoBody.get("message"));
    }

    @Test
    void testBranchSpecificAvailabilityValidationForFilesMaterial() {
        // For branchB, add a DistrictPricing with rate 0 or active=false for Premium
        Pricing pPrem = pricingRepository.findByMaterialOptionIdAndPricingMode(filesOption.getId(), PricingMode.PREMIUM).orElseThrow();
        DistrictPricing dpB = new DistrictPricing();
        dpB.setBranch(branchB);
        dpB.setPricing(pPrem);
        dpB.setRate(BigDecimal.ZERO);
        dpB.setActive(false);
        districtPricingRepository.save(dpB);

        // BranchA has active pricing
        AdminQuotationBuilderService.ResolvedMaterialRate resA = adminQuotationBuilderService.resolveMaterialAndRate(
                branchA.getId(), division.getId(), filesMaterial.getId(), null, PricingMode.PREMIUM
        );
        assertTrue(resA.available(), "BranchA should have available rate");
        assertEquals(new BigDecimal("2500.00"), resA.rate());

        // BranchB has inactive override -> unavailable!
        AdminQuotationBuilderService.ResolvedMaterialRate resB = adminQuotationBuilderService.resolveMaterialAndRate(
                branchB.getId(), division.getId(), filesMaterial.getId(), null, PricingMode.PREMIUM
        );
        assertFalse(resB.available(), "BranchB should be unavailable due to inactive override");
        assertEquals("No material available in Premium Grade", resB.message());
    }

    @Test
    void testSaveQuotationBlockedWhenFilesMaterialIsUnavailableInSelectedGrade() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(),
                "John Customer",
                "9876543210",
                "john@test.com",
                "123 Street"
        );

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(),
                division.getId(),
                item.getId(),
                filesMaterial.getId(),
                null,
                null,
                "3D Item",
                "Render file",
                List.of(),
                MeasurementType.FILES,
                BigDecimal.ONE,
                null,
                new BigDecimal("500.00")
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor",
                "1",
                "Work desc",
                List.of(itemReq)
        );

        // Create builder request in ECONOMY mode (where filesMaterial has no pricing configured)
        AdminQuotationBuilderService.BuilderRequest reqEco = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branchA.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                PricingMode.ECONOMY,
                "Reqs",
                "Notes",
                List.of(roomReq),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true
        );

        // Expect IllegalArgumentException due to unavailability in Economy
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminQuotationBuilderService.save(reqEco, adminUser.getEmail());
        });
        assertTrue(ex.getMessage().contains("No material available in Economy Grade"),
                "Expected error message containing 'No material available in Economy Grade', but got: " + ex.getMessage());

        // Now create builder request in PREMIUM mode (where filesMaterial is available)
        AdminQuotationBuilderService.BuilderRequest reqPrem = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branchA.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(15),
                PricingMode.PREMIUM,
                "Reqs",
                "Notes",
                List.of(roomReq),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true
        );
        Quotation q = adminQuotationBuilderService.save(reqPrem, adminUser.getEmail());
        assertNotNull(q);
        assertNotNull(q.getId());
        assertEquals("PREMIUM", q.getPricingMode().name());
    }
}
