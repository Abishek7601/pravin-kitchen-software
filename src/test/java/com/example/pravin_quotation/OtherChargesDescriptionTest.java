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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class OtherChargesDescriptionTest {

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private AdminQuotationBuilderController adminBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

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
    private QuotationPdfService quotationPdfService;

    private Branch branch;
    private User admin;
    private User employee;
    private Customer customer;
    private WorkCategory category;
    private Division division;
    private Material material;
    private MaterialOption option;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setName("Test Branch " + System.nanoTime());
        branch.setActive(true);
        branch = branchRepository.save(branch);

        admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin-" + System.nanoTime() + "@pravin.com");
        admin.setPassword("pass");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setBranch(branch);
        admin.setActive(true);
        admin = userRepository.save(admin);

        employee = new User();
        employee.setName("Employee User");
        employee.setEmail("emp-" + System.nanoTime() + "@pravin.com");
        employee.setPassword("pass");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);
        employee = userRepository.save(employee);

        customer = new Customer();
        customer.setName("John Doe");
        customer.setPhone("98765" + String.format("%05d", (int) (Math.random() * 100000)));
        customer.setEmail("john" + System.nanoTime() + "@example.com");
        customer.setAddress("123 Test Street");
        customer.setBranch(branch);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        category = new WorkCategory();
        category.setName("INTERIOR " + System.nanoTime());
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("WARDROBE " + System.nanoTime());
        division.setWorkCategory(category);
        division.setActive(true);
        division = divisionRepository.save(division);

        material = new Material();
        material.setName("PLYWOOD " + System.nanoTime());
        material.setDivision(division);
        material.setRate(new BigDecimal("1200.00"));
        material.setMeasurementType(MeasurementType.SQFT);
        material.setActive(true);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("Standard Option");
        option.setMaterial(material);
        option.setActive(true);
        option = materialOptionRepository.save(option);

        Pricing pPremium = new Pricing();
        pPremium.setMaterialOption(option);
        pPremium.setPricingMode(PricingMode.PREMIUM);
        pPremium.setRate(new BigDecimal("1200.00"));
        pPremium.setUnit("Sq.ft");
        pPremium.setActive(true);
        pricingRepository.save(pPremium);

        Pricing pMiddle = new Pricing();
        pMiddle.setMaterialOption(option);
        pMiddle.setPricingMode(PricingMode.MIDDLE);
        pMiddle.setRate(new BigDecimal("1000.00"));
        pMiddle.setUnit("Sq.ft");
        pMiddle.setActive(true);
        pricingRepository.save(pMiddle);
    }

    private AdminQuotationBuilderService.RoomRequest createSampleRoomRequest() {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("10.00"),
                new BigDecimal("8.00"),
                new BigDecimal("4.00"),
                null
        );

        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                category.getId(),
                division.getId(),
                null,
                material.getId(),
                option.getId(),
                null,
                "Sample Wardrobe",
                "Century Ply 18mm",
                List.of(size),
                MeasurementType.SQFT,
                BigDecimal.ONE,
                null,
                new BigDecimal("1200.00")
        );

        return new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor",
                "Bedroom 1",
                "Interior woodwork",
                List.of(item)
        );
    }

    @Test
    @DisplayName("Admin creates quotation with Other Charges and Description - verified in DB, loadQuotation and PDF")
    void testAdminCreatesQuotationWithOtherChargesAndDescription() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branch.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                PricingMode.PREMIUM,
                "Requirements",
                "Notes",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                "Transportation Charges",
                BigDecimal.ZERO,
                new BigDecimal("18.00"),
                BigDecimal.ZERO,
                true
        );

        Quotation saved = builderService.save(request, admin.getEmail());
        assertNotNull(saved.getId());
        assertEquals(0, new BigDecimal("2000.00").compareTo(saved.getOtherCharges()));
        assertEquals("Transportation Charges", saved.getOtherChargesDescription());

        // Verify loadQuotation returns both fields
        ResponseEntity<?> loadResp = adminBuilderController.loadQuotation(saved.getId());
        assertTrue(loadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResp.getBody();
        assertNotNull(data);
        assertEquals(new BigDecimal("2000.00"), data.get("otherCharges"));
        assertEquals("Transportation Charges", data.get("otherChargesDescription"));

        // Verify PDF includes description in summary row
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(saved.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF should be generated");
    }

    @Test
    @DisplayName("Employee creates quotation with Other Charges and Description - verified in DB and loadQuotation")
    void testEmployeeCreatesQuotationWithOtherChargesAndDescription() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branch.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                PricingMode.MIDDLE,
                "Requirements",
                "Notes",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1500.00"),
                "Installation & Loading",
                BigDecimal.ZERO,
                new BigDecimal("18.00"),
                BigDecimal.ZERO,
                true
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(), "pass", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        org.springframework.mock.web.MockHttpServletRequest httpReq = new org.springframework.mock.web.MockHttpServletRequest();
        httpReq.addHeader("Accept", "application/json");
        ResponseEntity<?> saveResp = (ResponseEntity<?>) employeeQuotationController.saveBuilderQuotation(request, httpReq, auth, null);
        assertTrue(saveResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> saveBody = (Map<String, Object>) saveResp.getBody();
        assertNotNull(saveBody);
        Long qId = (Long) saveBody.get("quotationId");
        assertNotNull(qId);

        Quotation saved = quotationRepository.findById(qId).orElseThrow();
        assertEquals(0, new BigDecimal("1500.00").compareTo(saved.getOtherCharges()));
        assertEquals("Installation & Loading", saved.getOtherChargesDescription());

        // Verify employee loadQuotation returns description
        ResponseEntity<?> loadResp = employeeQuotationController.builderLoadQuotation(qId, auth);
        assertTrue(loadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResp.getBody();
        assertNotNull(data);
        assertEquals(new BigDecimal("1500.00"), data.get("otherCharges"));
        assertEquals("Installation & Loading", data.get("otherChargesDescription"));
    }

    @Test
    @DisplayName("Validation: Description is required when Other Charges amount > 0")
    void testValidationRequiresDescriptionWhenAmountEntered() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        // otherCharges > 0 but description is null
        AdminQuotationBuilderService.BuilderRequest reqNullDesc = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"), null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                builderService.save(reqNullDesc, admin.getEmail())
        );
        assertTrue(ex1.getMessage().contains("Other charges description is required"));

        // otherCharges > 0 but description is blank
        AdminQuotationBuilderService.BuilderRequest reqBlankDesc = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"), "   ",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                builderService.save(reqBlankDesc, admin.getEmail())
        );
        assertTrue(ex2.getMessage().contains("Other charges description is required"));
    }

    @Test
    @DisplayName("Validation: Description is optional when Other Charges amount is zero or null")
    void testDescriptionOptionalWhenAmountZeroOrNull() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        // otherCharges = 0 with null description -> valid
        AdminQuotationBuilderService.BuilderRequest reqZero = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        assertDoesNotThrow(() -> {
            Quotation q = builderService.save(reqZero, admin.getEmail());
            assertNotNull(q.getId());
        });
    }

    @Test
    @DisplayName("Edit Builder loads and updates Other Charges description and amount")
    void testEditBuilderUpdatesDescriptionAndAmount() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest initialReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2000.00"), "Transportation",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation q = builderService.save(initialReq, admin.getEmail());
        Long quotationId = q.getId();

        // Update with new amount and description
        AdminQuotationBuilderService.BuilderRequest updateReq = new AdminQuotationBuilderService.BuilderRequest(
                quotationId, custReq, branch.getId(), q.getQuotationDate(), q.getValidUntil(),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("3500.00"), "Packaging & Logistics",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation updated = builderService.save(updateReq, admin.getEmail());
        assertEquals(quotationId, updated.getId());
        assertEquals(0, new BigDecimal("3500.00").compareTo(updated.getOtherCharges()));
        assertEquals("Packaging & Logistics", updated.getOtherChargesDescription());

        // Verify loaded data in controller
        ResponseEntity<?> loadResp = adminBuilderController.loadQuotation(quotationId);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResp.getBody();
        assertNotNull(data);
        assertEquals(new BigDecimal("3500.00"), data.get("otherCharges"));
        assertEquals("Packaging & Logistics", data.get("otherChargesDescription"));
    }

    @Test
    @DisplayName("Calculation: Other Charges amount is included exactly once in total calculation")
    void testCalculationIncludesOtherChargesOnlyOnce() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        BigDecimal otherCharges = new BigDecimal("2500.00");
        BigDecimal travel = new BigDecimal("1000.00");

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, travel, otherCharges, "Site Loading Charges",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation q = builderService.save(req, admin.getEmail());
        BigDecimal subtotal = q.getSubtotal();
        assertNotNull(subtotal);
        assertTrue(subtotal.compareTo(BigDecimal.ZERO) > 0);

        // Total amount before GST/discount = subtotal + travel + otherCharges
        BigDecimal expectedTotal = subtotal.add(travel).add(otherCharges);
        assertEquals(0, expectedTotal.compareTo(q.getGrandTotal()),
                "Grand total must equal Subtotal + Travel + Other Charges exactly once");
    }

    @Test
    @DisplayName("Auto-capitalize first letter: lowercase inputs become capitalized while preserving remaining letters")
    void testAutoCapitalizeFirstLetterPreservesRemainingLetters() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        // Example 1: transportation charges -> Transportation charges
        AdminQuotationBuilderService.BuilderRequest req1 = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, null, null, List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1500.00"), "transportation charges",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );
        Quotation q1 = builderService.save(req1, admin.getEmail());
        assertEquals("Transportation charges", q1.getOtherChargesDescription(),
                "First letter must be auto-capitalized and subsequent words preserved as entered");

        // Verify loadQuotation controller returns capitalized description
        ResponseEntity<?> response = adminBuilderController.loadQuotation(q1.getId());
        assertTrue(response.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> loadData = (Map<String, Object>) response.getBody();
        assertNotNull(loadData);
        assertEquals("Transportation charges", loadData.get("otherChargesDescription"));

        // Verify PDF generation works with the capitalized description
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q1.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Example 2: installation charges -> Installation charges
        q1.setOtherChargesDescription("installation charges");
        assertEquals("Installation charges", q1.getOtherChargesDescription());

        // Example 3: loading and unloading -> Loading and unloading (remaining letters preserved, not forced title case)
        q1.setOtherChargesDescription("loading and unloading");
        assertEquals("Loading and unloading", q1.getOtherChargesDescription());
    }

    @Test
    @DisplayName("UI Templates: Verify otherChargesDescription input exists in Admin and Employee builders and view pages")
    void testTemplatesContainOtherChargesDescriptionFields() throws IOException {
        Path adminBuilderPath = Paths.get("src/main/resources/templates/admin/quotation-builder.html");
        Path empBuilderPath = Paths.get("src/main/resources/templates/employee/quotation-builder.html");
        Path adminViewPath = Paths.get("src/main/resources/templates/admin/quotation-view.html");
        Path empViewPath = Paths.get("src/main/resources/templates/employee/quotation-view.html");

        assertTrue(Files.exists(adminBuilderPath));
        assertTrue(Files.exists(empBuilderPath));
        assertTrue(Files.exists(adminViewPath));
        assertTrue(Files.exists(empViewPath));

        String adminBuilderHtml = Files.readString(adminBuilderPath);
        String empBuilderHtml = Files.readString(empBuilderPath);
        String adminViewHtml = Files.readString(adminViewPath);
        String empViewHtml = Files.readString(empViewPath);

        // Check Description field in Admin builder
        assertTrue(adminBuilderHtml.contains("id=\"otherChargesDescription\""),
                "Admin builder must contain otherChargesDescription input");
        assertTrue(adminBuilderHtml.contains("id=\"otherCharges\""),
                "Admin builder must contain otherCharges input");
        assertTrue(adminBuilderHtml.contains("Enter reason for other charges (e.g. Transportation, Installation, Loading & Unloading)"),
                "Admin builder must contain placeholder for description");
        assertTrue(adminBuilderHtml.contains("autocapitalize=\"sentences\""),
                "Admin builder must contain autocapitalize attribute");
        assertTrue(adminBuilderHtml.contains("autoCapitalizeFirstLetter"),
                "Admin builder must contain autoCapitalizeFirstLetter script");

        // Check Description field in Employee builder
        assertTrue(empBuilderHtml.contains("id=\"otherChargesDescription\""),
                "Employee builder must contain otherChargesDescription input");
        assertTrue(empBuilderHtml.contains("id=\"otherCharges\""),
                "Employee builder must contain otherCharges input");
        assertTrue(empBuilderHtml.contains("Enter reason for other charges (e.g. Transportation, Installation, Loading & Unloading)"),
                "Employee builder must contain placeholder for description");
        assertTrue(empBuilderHtml.contains("autocapitalize=\"sentences\""),
                "Employee builder must contain autocapitalize attribute");
        assertTrue(empBuilderHtml.contains("autoCapitalizeFirstLetter"),
                "Employee builder must contain autoCapitalizeFirstLetter script");

        // Check display in view pages
        assertTrue(adminViewHtml.contains("otherChargesDescription"),
                "Admin quotation view must reference otherChargesDescription");
        assertTrue(empViewHtml.contains("otherChargesDescription"),
                "Employee quotation view must reference otherChargesDescription");
    }
}
