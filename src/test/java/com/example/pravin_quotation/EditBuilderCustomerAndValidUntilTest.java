package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EditBuilderCustomerAndValidUntilTest {

    @Autowired
    private AdminQuotationBuilderService builderService;

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
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    private Branch branchA;
    private Branch branchB;
    private User admin;
    private User employeeA;
    private User employeeB;
    private Customer customer;
    private WorkCategory category;
    private Division division;
    private Material material;

    @BeforeEach
    void setUp() {
        branchA = new Branch();
        branchA.setName("Branch Chennai " + System.nanoTime());
        branchA.setActive(true);
        branchA = branchRepository.save(branchA);

        branchB = new Branch();
        branchB.setName("Branch Madurai " + System.nanoTime());
        branchB.setActive(true);
        branchB = branchRepository.save(branchB);

        admin = new User();
        admin.setName("Super Admin");
        admin.setEmail("admin-" + System.nanoTime() + "@pravin.com");
        admin.setPassword("pass");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setBranch(branchA);
        admin.setActive(true);
        admin = userRepository.save(admin);

        employeeA = new User();
        employeeA.setName("Emp A");
        employeeA.setEmail("empA-" + System.nanoTime() + "@pravin.com");
        employeeA.setPassword("pass");
        employeeA.setRole(Role.EMPLOYEE);
        employeeA.setBranch(branchA);
        employeeA.setActive(true);
        employeeA = userRepository.save(employeeA);

        employeeB = new User();
        employeeB.setName("Emp B");
        employeeB.setEmail("empB-" + System.nanoTime() + "@pravin.com");
        employeeB.setPassword("pass");
        employeeB.setRole(Role.EMPLOYEE);
        employeeB.setBranch(branchB);
        employeeB.setActive(true);
        employeeB = userRepository.save(employeeB);

        category = new WorkCategory();
        category.setName("Living Room Decor " + System.nanoTime());
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("Interior Work " + System.nanoTime());
        division.setWorkCategory(category);
        division.setActive(true);
        division = divisionRepository.save(division);

        material = new Material();
        material.setName("Premium Teak Wood");
        material.setUnit("Sq.ft");
        material.setMeasurementType(MeasurementType.SQFT);
        material.setFormula("(L + W) × H");
        material.setRate(new BigDecimal("1200.00"));
        material.setPricingGrade(PricingMode.PREMIUM);
        material.setDivision(division);
        material.setActive(true);
        material = materialRepository.save(material);

        customer = new Customer();
        customer.setName("Karthik Raja");
        customer.setPhone("98765" + (System.currentTimeMillis() % 100000));
        customer.setEmail("karthik-" + System.nanoTime() + "@example.com");
        customer.setAddress("Anna Nagar, Chennai");
        customer.setBranch(branchA);
        customer.setActive(true);
        customer = customerRepository.save(customer);
    }

    private Authentication authFor(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    private AdminQuotationBuilderService.RoomRequest createSampleRoomRequest() {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("10.0"),
                new BigDecimal("12.0"),
                new BigDecimal("9.0"),
                null
        );
        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                category.getId(),
                division.getId(),
                null,
                material.getId(),
                null,
                null,
                "Teak Item",
                "Spec",
                List.of(size),
                MeasurementType.SQFT,
                BigDecimal.ONE,
                null,
                new BigDecimal("1200.00")
        );
        return new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor",
                "Hall",
                "Living Area",
                List.of(item)
        );
    }

    @Test
    @DisplayName("1. Admin: Edit customer details and valid until date updates existing customer and quotation")
    void testAdminEditBuilderCustomerAndValidUntil() {
        LocalDate qDate = LocalDate.of(2026, 11, 1);
        LocalDate vUntil = LocalDate.of(2026, 12, 15);

        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                vUntil,
                PricingMode.PREMIUM,
                "Requirements initial",
                "Notes initial",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        Quotation created = builderService.save(createReq, admin.getEmail());
        assertNotNull(created.getId());
        assertEquals(vUntil, created.getValidUntil());
        Long initialCustId = created.getCustomer().getId();

        // Now Edit Builder saves updated customer info and updated valid until
        LocalDate newValidUntil = LocalDate.of(2027, 3, 31);
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                created.getId(),
                new AdminQuotationBuilderService.CustomerRequest(initialCustId, "Karthik Updated", customer.getPhone(), "karthik.new@example.com", "T Nagar, Chennai"),
                branchA.getId(),
                qDate,
                newValidUntil,
                PricingMode.PREMIUM,
                "Requirements updated",
                "Notes updated",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        Quotation updated = builderService.save(editReq, admin.getEmail());
        assertEquals(created.getId(), updated.getId());
        assertEquals(newValidUntil, updated.getValidUntil());

        Customer reloadedCustomer = customerRepository.findById(initialCustId).orElseThrow();
        assertEquals("Karthik Updated", reloadedCustomer.getName());
        assertEquals("karthik.new@example.com", reloadedCustomer.getEmail());
        assertEquals("T Nagar, Chennai", reloadedCustomer.getAddress());
    }

    @Test
    @DisplayName("2. Employee: Edit customer details in own branch quotation successfully")
    void testEmployeeEditBuilderOwnBranch() {
        LocalDate qDate = LocalDate.of(2026, 11, 1);
        LocalDate vUntil = LocalDate.of(2027, 1, 10);

        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                vUntil,
                PricingMode.PREMIUM,
                "Emp requirements",
                "Emp notes",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        Quotation created = builderService.save(createReq, employeeA.getEmail());
        assertEquals(vUntil, created.getValidUntil());

        LocalDate newValidUntil = LocalDate.of(2027, 2, 28);
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                created.getId(),
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), "Karthik Raja Verified", customer.getPhone(), customer.getEmail(), "Velachery, Chennai"),
                branchA.getId(),
                qDate,
                newValidUntil,
                PricingMode.PREMIUM,
                "Emp req updated",
                "Emp notes updated",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        Quotation updated = builderService.save(editReq, employeeA.getEmail());
        assertEquals(newValidUntil, updated.getValidUntil());

        Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals("Karthik Raja Verified", reloaded.getName());
        assertEquals("Velachery, Chennai", reloaded.getAddress());
    }

    @Test
    @DisplayName("3. Security: Employee cannot modify customer belonging to another branch")
    void testEmployeeCannotModifyOtherBranchCustomer() {
        Customer otherBranchCust = new Customer();
        otherBranchCust.setName("Madurai Client");
        otherBranchCust.setPhone("94431" + (System.currentTimeMillis() % 100000));
        otherBranchCust.setEmail("madurai-" + System.nanoTime() + "@example.com");
        otherBranchCust.setAddress("Madurai Town");
        otherBranchCust.setBranch(branchB); // Branch B
        otherBranchCust.setActive(true);
        otherBranchCust = customerRepository.save(otherBranchCust);

        // Employee A belongs to branch A and attempts to update otherBranchCust
        AdminQuotationBuilderService.BuilderRequest reqTampered = new AdminQuotationBuilderService.BuilderRequest(
                null,
                new AdminQuotationBuilderService.CustomerRequest(otherBranchCust.getId(), "Tampered Name", otherBranchCust.getPhone(), otherBranchCust.getEmail(), otherBranchCust.getAddress()),
                branchA.getId(),
                LocalDate.now(),
                null,
                PricingMode.MIDDLE,
                "Req", "Note",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        assertThrows(AccessDeniedException.class, () -> {
            builderService.save(reqTampered, employeeA.getEmail());
        });
    }

    @Test
    @DisplayName("4. Validation: Valid Until date cannot be earlier than Quotation Date")
    void testValidUntilCannotBeEarlierThanQuotationDate() {
        LocalDate qDate = LocalDate.of(2026, 11, 10);
        LocalDate invalidValidUntil = LocalDate.of(2026, 11, 5); // Earlier than qDate

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null,
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                invalidValidUntil,
                PricingMode.PREMIUM,
                "Req", "Note",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builderService.save(req, admin.getEmail());
        });
        assertTrue(ex.getMessage().toLowerCase().contains("valid until date cannot be earlier than quotation date"));
    }

    @Test
    @DisplayName("5. Uniqueness: Duplicate phone number belonging to another customer is rejected")
    void testCustomerPhoneDuplicateRejection() {
        Customer existingOther = new Customer();
        existingOther.setName("Existing User");
        existingOther.setPhone("9988776655");
        existingOther.setEmail("existing-" + System.nanoTime() + "@example.com");
        existingOther.setBranch(branchA);
        existingOther.setActive(true);
        customerRepository.save(existingOther);

        LocalDate qDate = LocalDate.of(2026, 11, 1);
        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                qDate.plusDays(30),
                PricingMode.PREMIUM,
                "Req", "Note",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        Quotation quotation = builderService.save(createReq, admin.getEmail());

        // Attempt to update customer to existingOther's phone number
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                quotation.getId(),
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), "9988776655", customer.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                qDate.plusDays(30),
                PricingMode.PREMIUM,
                "Req", "Note",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builderService.save(editReq, admin.getEmail());
        });
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("6. Uniqueness: Duplicate email address belonging to another customer is rejected")
    void testCustomerEmailDuplicateRejection() {
        Customer existingOther = new Customer();
        existingOther.setName("Existing Email User");
        existingOther.setPhone("91234" + (System.currentTimeMillis() % 100000));
        existingOther.setEmail("duplicate-" + System.nanoTime() + "@example.com");
        existingOther.setBranch(branchA);
        existingOther.setActive(true);
        customerRepository.save(existingOther);

        LocalDate qDate = LocalDate.of(2026, 11, 1);
        AdminQuotationBuilderService.BuilderRequest createReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                qDate.plusDays(30),
                PricingMode.PREMIUM,
                "Req", "Note",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        Quotation quotation = builderService.save(createReq, admin.getEmail());

        // Attempt to update customer to existingOther's email
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                quotation.getId(),
                new AdminQuotationBuilderService.CustomerRequest(customer.getId(), customer.getName(), customer.getPhone(), existingOther.getEmail(), customer.getAddress()),
                branchA.getId(),
                qDate,
                qDate.plusDays(30),
                PricingMode.PREMIUM,
                "Req", "Note",
                List.of(createSampleRoomRequest()),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            builderService.save(editReq, admin.getEmail());
        });
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Autowired
    private com.example.pravin_quotation.service.QuotationPdfService quotationPdfService;

    @Test
    @DisplayName("7. Diagnostic: Inspect QT-00019 in database and test PDF generation")
    void inspectQt00019() throws Exception {
        var opt = quotationRepository.findAll().stream()
                .filter(q -> "QT-00019".equalsIgnoreCase(q.getQuotationNumber()))
                .findFirst();
        if (opt.isPresent()) {
            Quotation q = opt.get();
            System.out.println("=== FOUND QT-00019 ===");
            System.out.println("Id: " + q.getId());
            var rooms = quotationRoomRepository.findByQuotationIdOrderByIdAsc(q.getId());
            for (var r : rooms) {
                System.out.println("Room: " + r.getRoom() + ", floor: " + r.getFloor());
                var items = quotationItemRepository.findByQuotationRoomIdOrderByIdAsc(r.getId());
                for (var it : items) {
                    System.out.println("Item: id=" + it.getId()
                            + ", mat=" + (it.getMaterial() != null ? it.getMaterial().getName() : "null")
                            + ", matId=" + (it.getMaterial() != null ? it.getMaterial().getId() : "null")
                            + ", matImageUrl=" + (it.getMaterial() != null ? it.getMaterial().getImageUrl() : "null")
                            + ", itemImageUrl=" + it.getImageUrl()
                            + ", measurementType=" + it.getMeasurementType()
                            + ", spec=" + it.getSpecification());
                }
            }

            byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
            System.out.println("Generated PDF bytes: " + pdfBytes.length);
            java.nio.file.Files.write(java.nio.file.Path.of("target/debug-qt00019.pdf"), pdfBytes);
        } else {
            System.out.println("=== QT-00019 NOT FOUND ===");
        }
    }
}
