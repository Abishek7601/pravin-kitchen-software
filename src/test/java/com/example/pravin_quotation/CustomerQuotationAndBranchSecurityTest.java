package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.CustomerController;
import com.example.pravin_quotation.controller.EmployeeCustomerController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.dto.BranchCustomerCountDto;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.CustomerService;
import com.example.pravin_quotation.service.QuotationPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CustomerQuotationAndBranchSecurityTest {

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private AdminQuotationBuilderController adminBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private CustomerController customerController;

    @Autowired
    private EmployeeCustomerController employeeCustomerController;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

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
    private QuotationPdfService quotationPdfService;

    private Branch branchA;
    private Branch branchB;
    private User adminUser;
    private User employeeA;
    private User employeeB;
    private MaterialOption testOption;
    private WorkCategory testCat;
    private Division testDiv;
    private Item testItem;
    private Material testMat;

    @BeforeEach
    public void setup() {
        branchA = new Branch();
        branchA.setName("Branch Alpha - Chennai");
        branchA.setActive(true);
        branchA = branchRepository.save(branchA);

        branchB = new Branch();
        branchB.setName("Branch Beta - Coimbatore");
        branchB.setActive(true);
        branchB = branchRepository.save(branchB);

        adminUser = new User();
        adminUser.setName("Super Admin");
        adminUser.setEmail("admin_test_" + System.currentTimeMillis() + "@test.com");
        adminUser.setPassword("password123");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        employeeA = new User();
        employeeA.setName("Employee Alpha");
        employeeA.setEmail("emp_alpha_" + System.currentTimeMillis() + "@test.com");
        employeeA.setPassword("password123");
        employeeA.setRole(Role.EMPLOYEE);
        employeeA.setBranch(branchA);
        employeeA.setActive(true);
        employeeA = userRepository.save(employeeA);

        employeeB = new User();
        employeeB.setName("Employee Beta");
        employeeB.setEmail("emp_beta_" + System.currentTimeMillis() + "@test.com");
        employeeB.setPassword("password123");
        employeeB.setRole(Role.EMPLOYEE);
        employeeB.setBranch(branchB);
        employeeB.setActive(true);
        employeeB = userRepository.save(employeeB);

        testCat = new WorkCategory();
        testCat.setName("KITCHEN");
        testCat.setDescription("Kitchen");
        testCat.setActive(true);
        testCat = workCategoryRepository.save(testCat);

        testDiv = new Division();
        testDiv.setName("Base Cabinets");
        testDiv.setWorkCategory(testCat);
        testDiv.setActive(true);
        testDiv = divisionRepository.save(testDiv);

        testItem = new Item();
        testItem.setName("2-Drawer Cabinet");
        testItem.setDivision(testDiv);
        testItem.setActive(true);
        testItem = itemRepository.save(testItem);

        testMat = new Material();
        testMat.setName("HDHMR Board");
        testMat.setFormula("L × W");
        testMat.setActive(true);
        testMat = materialRepository.save(testMat);

        testOption = new MaterialOption();
        testOption.setName("Glossy Acrylic");
        testOption.setMaterial(testMat);
        testOption.setActive(true);
        testOption = materialOptionRepository.save(testOption);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(testOption);
        pricing.setPricingMode(PricingMode.MIDDLE);
        pricing.setRate(new BigDecimal("1200.00"));
        pricing.setUnit("SQFT");
        pricing.setActive(true);
        pricingRepository.save(pricing);
    }

    private Authentication authFor(User user) {
        String roleStr = "ROLE_" + user.getRole().name();
        return new UsernamePasswordAuthenticationToken(user.getEmail(), "pass", List.of(new SimpleGrantedAuthority(roleStr)));
    }

    private AdminQuotationBuilderService.BuilderRequest createQuotationPayload(
            Long customerId, String name, String phone, String email, String address, Long branchId) {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customerId, name, phone, email, address
        );
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("10"), new BigDecimal("5"), BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                testCat.getId(), testDiv.getId(), testItem.getId(), testMat.getId(),
                testOption.getId(), null, "Cabinet Spec", "Custom Spec", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Main Kitchen", List.of(itemReq)
        );

        return new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branchId,
                LocalDate.now(),
                PricingMode.MIDDLE,
                "Need soft close hinges",
                "Delivered within 3 weeks",
                List.of(roomReq)
        );
    }

    @Test
    public void testAdminSelectsExistingCustomerAndCreatesQuotation() {
        Customer existing = new Customer();
        existing.setName("Ramesh Kumar");
        existing.setPhone("9841112233");
        existing.setEmail("ramesh@example.com");
        existing.setAddress("12 Anna Nagar, Chennai");
        existing.setBranch(branchA);
        existing.setActive(true);
        existing = customerRepository.save(existing);

        AdminQuotationBuilderService.BuilderRequest req = createQuotationPayload(
                existing.getId(), "Ramesh Kumar Modified Name", "9841112233", "ramesh@example.com", "12 Anna Nagar, Chennai", branchA.getId()
        );

        Quotation q = builderService.save(req, adminUser.getEmail());
        assertNotNull(q.getId());
        assertEquals(existing.getId(), q.getCustomer().getId());
        // Verify historical customer record in DB was reused without mutating the original customer entity profile
        Customer fromDb = customerRepository.findById(existing.getId()).orElseThrow();
        assertEquals("Ramesh Kumar", fromDb.getName());

        // Create a second quotation for the same customer
        AdminQuotationBuilderService.BuilderRequest req2 = createQuotationPayload(
                existing.getId(), "Ramesh Kumar", "9841112233", "ramesh@example.com", "12 Anna Nagar, Chennai", branchA.getId()
        );
        Quotation q2 = builderService.save(req2, adminUser.getEmail());
        assertNotNull(q2.getId());
        assertEquals(existing.getId(), q2.getCustomer().getId());

        // Check quotation count for customer
        long quotationCount = quotationRepository.countByCustomerId(existing.getId());
        assertTrue(quotationCount >= 2);
    }

    @Test
    public void testAdminCreatesNewCustomerAndVerifiesInCustomersAndDropdowns() {
        String uniquePhone = "9988776655";
        AdminQuotationBuilderService.BuilderRequest req = createQuotationPayload(
                null, "Anita Sharma", uniquePhone, "anita@example.com", "45 Gandhipuram, Coimbatore", branchB.getId()
        );

        Quotation q = builderService.save(req, adminUser.getEmail());
        assertNotNull(q.getId());
        assertNotNull(q.getCustomer());
        assertEquals(uniquePhone, q.getCustomer().getPhone());
        assertEquals(branchB.getId(), q.getCustomer().getBranch().getId());

        // Verify customer is in repository and active
        Customer savedCustomer = customerRepository.findByPhone(uniquePhone).orElse(null);
        assertNotNull(savedCustomer);
        assertTrue(savedCustomer.getActive());
        assertEquals("Anita Sharma", savedCustomer.getName());

        // Verify it appears in Admin customers endpoint for branchB
        List<Map<String, Object>> customersBranchB = adminBuilderController.customers(branchB.getId());
        assertTrue(customersBranchB.stream().anyMatch(c -> uniquePhone.equals(c.get("phone"))));

        // Verify it does NOT appear when filtered for branchA
        List<Map<String, Object>> customersBranchA = adminBuilderController.customers(branchA.getId());
        assertFalse(customersBranchA.stream().anyMatch(c -> uniquePhone.equals(c.get("phone"))));
    }

    @Test
    public void testEmployeeSelectsExistingCustomerFromOwnBranch() {
        Customer empCustomer = new Customer();
        empCustomer.setName("Alpha Customer");
        empCustomer.setPhone("9812345678");
        empCustomer.setEmail("alpha.cust@example.com");
        empCustomer.setAddress("Branch A Area");
        empCustomer.setBranch(branchA);
        empCustomer.setActive(true);
        empCustomer = customerRepository.save(empCustomer);

        AdminQuotationBuilderService.BuilderRequest req = createQuotationPayload(
                empCustomer.getId(), empCustomer.getName(), empCustomer.getPhone(), empCustomer.getEmail(), empCustomer.getAddress(), branchA.getId()
        );

        Quotation q = builderService.save(req, employeeA.getEmail());
        assertNotNull(q.getId());
        assertEquals(empCustomer.getId(), q.getCustomer().getId());
        assertEquals(branchA.getId(), q.getBranch().getId());

        // Check employee builder/customers endpoint returns only branchA customers
        final Long empCustId = empCustomer.getId();
        List<Map<String, Object>> empList = employeeQuotationController.builderCustomers(authFor(employeeA));
        assertTrue(empList.stream().anyMatch(c -> empCustId.equals(c.get("id"))));
    }

    @Test
    public void testEmployeeCreatesNewCustomerSavedUnderAssignedBranch() {
        String phone = "9123499999";
        AdminQuotationBuilderService.BuilderRequest req = createQuotationPayload(
                null, "New Branch A Client", phone, "client.a@example.com", "Client Address", branchA.getId()
        );

        Quotation q = builderService.save(req, employeeA.getEmail());
        assertNotNull(q.getId());
        assertNotNull(q.getCustomer());
        assertEquals(branchA.getId(), q.getCustomer().getBranch().getId());
        assertEquals(branchA.getId(), q.getBranch().getId());
    }

    @Test
    public void testEmployeeCannotAccessOrSelectAnotherBranchCustomer() {
        // Customer belongs to branchB
        Customer betaCustomer = new Customer();
        betaCustomer.setName("Beta Customer");
        betaCustomer.setPhone("9765432100");
        betaCustomer.setEmail("beta.cust@example.com");
        betaCustomer.setAddress("Coimbatore West");
        betaCustomer.setBranch(branchB);
        betaCustomer.setActive(true);
        betaCustomer = customerRepository.save(betaCustomer);

        // Employee A belongs to branchA and attempts to select Beta Customer (tampering request with betaCustomer.getId())
        AdminQuotationBuilderService.BuilderRequest reqTampered = createQuotationPayload(
                betaCustomer.getId(), betaCustomer.getName(), betaCustomer.getPhone(), betaCustomer.getEmail(), betaCustomer.getAddress(), branchA.getId()
        );

        assertThrows(AccessDeniedException.class, () -> {
            builderService.save(reqTampered, employeeA.getEmail());
        });

        // Also if employee A attempts to pass branchB as quotation branch
        AdminQuotationBuilderService.BuilderRequest reqTamperedBranch = createQuotationPayload(
                null, "Hacked Customer", "9111111111", "hacked@example.com", "Some Address", branchB.getId()
        );

        assertThrows(AccessDeniedException.class, () -> {
            builderService.save(reqTamperedBranch, employeeA.getEmail());
        });
    }

    @Test
    public void testDistinctCustomerCountingInStatistics() {
        Customer c1 = new Customer();
        c1.setName("Multi Quote Cust 1");
        c1.setPhone("9000000001");
        c1.setEmail("mq1@test.com");
        c1.setBranch(branchA);
        c1.setActive(true);
        c1 = customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setName("Multi Quote Cust 2");
        c2.setPhone("9000000002");
        c2.setEmail("mq2@test.com");
        c2.setBranch(branchA);
        c2.setActive(true);
        c2 = customerRepository.save(c2);

        Customer c3 = new Customer();
        c3.setName("Beta Branch Cust");
        c3.setPhone("9000000003");
        c3.setEmail("mq3@test.com");
        c3.setBranch(branchB);
        c3.setActive(true);
        c3 = customerRepository.save(c3);

        // Create multiple quotations for c1
        for (int i = 0; i < 3; i++) {
            Quotation q = new Quotation();
            q.setQuotationNumber("QT-TEST-C1-" + i);
            q.setCustomer(c1);
            q.setBranch(branchA);
            q.setEmployee(adminUser);
            q.setPricingMode(PricingMode.MIDDLE);
            q.setQuotationDate(LocalDate.now());
            q.setStatus(QuotationStatus.WORKING);
            q.setGrandTotal(new BigDecimal("50000"));
            quotationRepository.save(q);
        }

        // Verify statistics
        long totalCustomers = customerService.getTotalCustomerCount();
        assertTrue(totalCustomers >= 3);

        List<BranchCustomerCountDto> branchCounts = customerService.getBranchWiseCustomerCounts();
        BranchCustomerCountDto countBranchA = branchCounts.stream().filter(b -> b.branchId().equals(branchA.getId())).findFirst().orElseThrow();
        BranchCustomerCountDto countBranchB = branchCounts.stream().filter(b -> b.branchId().equals(branchB.getId())).findFirst().orElseThrow();

        // Customer c1 has 3 quotations, but customer count for Branch A must only count distinct customers!
        assertTrue(countBranchA.customerCount() >= 2);
        assertTrue(countBranchB.customerCount() >= 1);

        // Check Admin Customers controller populates these stats
        ExtendedModelMap model = new ExtendedModelMap();
        customerController.customers(null, model);
        assertTrue(model.containsAttribute("totalCustomers"));
        assertTrue(model.containsAttribute("activeCustomers"));
        assertTrue(model.containsAttribute("branchesWithCustomers"));
        assertTrue(model.containsAttribute("branchCustomerCounts"));
    }

    @Test
    public void testEmployeeCustomerControllerOnlyExposesOwnBranchCustomers() {
        Customer cAlpha = new Customer();
        cAlpha.setName("Emp Alpha Cust");
        cAlpha.setPhone("9100000001");
        cAlpha.setBranch(branchA);
        cAlpha.setActive(true);
        customerRepository.save(cAlpha);

        Customer cBeta = new Customer();
        cBeta.setName("Emp Beta Cust");
        cBeta.setPhone("9100000002");
        cBeta.setBranch(branchB);
        cBeta.setActive(true);
        customerRepository.save(cBeta);

        ExtendedModelMap model = new ExtendedModelMap();
        employeeCustomerController.customers(authFor(employeeA), model);

        @SuppressWarnings("unchecked")
        List<Customer> customers = (List<Customer>) model.get("customers");
        assertNotNull(customers);
        assertTrue(customers.stream().allMatch(c -> c.getBranch() != null && c.getBranch().getId().equals(branchA.getId())));
        assertFalse(customers.stream().anyMatch(c -> c.getBranch() != null && c.getBranch().getId().equals(branchB.getId())));

        // Ensure Admin-only cross-branch stats are NOT present on employee view
        assertFalse(model.containsAttribute("totalCustomers"));
        assertFalse(model.containsAttribute("branchesWithCustomers"));
        assertFalse(model.containsAttribute("branchCustomerCounts"));
    }

    @Test
    public void testQuotationPdfGenerationWorksWithCustomerSelection() {
        Customer cust = new Customer();
        cust.setName("PDF Test Customer");
        cust.setPhone("9888877777");
        cust.setEmail("pdf.cust@example.com");
        cust.setAddress("PDF Customer Lane");
        cust.setBranch(branchA);
        cust.setActive(true);
        cust = customerRepository.save(cust);

        AdminQuotationBuilderService.BuilderRequest req = createQuotationPayload(
                cust.getId(), cust.getName(), cust.getPhone(), cust.getEmail(), cust.getAddress(), branchA.getId()
        );

        Quotation q = builderService.save(req, adminUser.getEmail());
        assertNotNull(q.getId());

        // Generate PDF
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
