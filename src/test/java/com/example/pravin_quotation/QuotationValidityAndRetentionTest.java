package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationCleanupService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.QuotationService;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationValidityAndRetentionTest {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private QuotationCleanupService quotationCleanupService;

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private QuotationItemSizeRepository quotationItemSizeRepository;

    @Autowired
    private QuotationCommunicationRepository quotationCommunicationRepository;

    @Autowired
    private QuotationStatusHistoryRepository quotationStatusHistoryRepository;

    @Autowired
    private QuotationWorkflowRepository quotationWorkflowRepository;

    private Customer customer;
    private User employee;
    private User admin;
    private Branch branch;
    private Division division;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setName("Chennai Retention District " + System.nanoTime());
        branch.setActive(true);
        branch = branchRepository.save(branch);

        employee = new User();
        employee.setName("Retention Employee");
        employee.setEmail("retention_emp_" + System.nanoTime() + "@pravin.com");
        employee.setPassword("encoded");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);
        employee = userRepository.save(employee);

        admin = new User();
        admin.setName("Retention Admin");
        admin.setEmail("retention_admin_" + System.nanoTime() + "@pravin.com");
        admin.setPassword("encoded");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setBranch(branch);
        admin.setActive(true);
        admin = userRepository.save(admin);

        customer = new Customer();
        customer.setName("Karthik Raja");
        customer.setPhone("9876543210");
        customer.setEmail("karthik@example.com");
        customer.setAddress("Anna Nagar, Chennai");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        WorkCategory setupCat = new WorkCategory();
        setupCat.setName("Residential Cat " + System.nanoTime());
        setupCat.setActive(true);
        setupCat = workCategoryRepository.save(setupCat);

        division = new Division();
        division.setName("Residential Division " + System.nanoTime());
        division.setWorkCategory(setupCat);
        division.setActive(true);
        division = divisionRepository.save(division);
    }

    private String extractPage2PdfText(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        byte[] pageContent = reader.getPageContent(2);
        reader.close();
        return pageContent != null ? new String(pageContent, StandardCharsets.ISO_8859_1) : "";
    }

    @Test
    @DisplayName("1. Automatic Valid-Until Date Calculation: Exactly 365 Days Duration")
    void testAutomaticValidUntilCalculation() {
        LocalDate createdDate = LocalDate.of(2026, 10, 29);
        String qNumber = "QT-TEST-" + System.nanoTime();

        Quotation q = quotationService.create(
                qNumber,
                customer.getId(),
                employee.getId(),
                branch.getId(),
                createdDate,
                PricingMode.PREMIUM,
                "Kitchen modular setup",
                "Priority client"
        );

        assertNotNull(q.getId());
        assertEquals(createdDate, q.getQuotationDate());
        assertNotNull(q.getValidUntil());

        // Must be exactly createdDate.plusDays(365) -> 2027-10-29
        assertEquals(createdDate.plusDays(365), q.getValidUntil());
        assertEquals(LocalDate.of(2027, 10, 29), q.getValidUntil());

        // Verify leap year duration test
        // 2024 is a leap year (29 days in Feb). 2024-02-28 + 365 days = 2025-02-27 (not 2025-02-28!)
        LocalDate leapDate = LocalDate.of(2024, 2, 28);
        Quotation leapQ = quotationService.create(
                "QT-LEAP-" + System.nanoTime(),
                customer.getId(),
                employee.getId(),
                branch.getId(),
                leapDate,
                PricingMode.ECONOMY,
                "Leap test",
                "Leap year"
        );

        assertEquals(leapDate.plusDays(365), leapQ.getValidUntil());
        assertEquals(LocalDate.of(2025, 2, 27), leapQ.getValidUntil());
        assertNotEquals(leapDate.plusYears(1), leapQ.getValidUntil(), "Must use 365 days, not plusYears(1)!");
    }

    @Test
    @DisplayName("2. PDF Displays Quotation Date and Valid Until Clearly")
    void testPdfDisplaysQuotationDateAndValidUntil() throws Exception {
        LocalDate createdDate = LocalDate.of(2026, 10, 29);
        String qNumber = "QT-PDF-" + System.nanoTime();

        Quotation q = quotationService.create(
                qNumber,
                customer.getId(),
                employee.getId(),
                branch.getId(),
                createdDate,
                PricingMode.PREMIUM,
                "PDF validity test",
                "Ensure valid until in PDF"
        );

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        String page2Text = extractPage2PdfText(pdfBytes);
        assertTrue(page2Text.contains("Quotation Date:"), "PDF must contain 'Quotation Date:' label");
        assertTrue(page2Text.contains("29-10-2026"), "PDF must contain formatted creation date 29-10-2026");

        assertTrue(page2Text.contains("Valid Until:"), "PDF must contain 'Valid Until:' label");
        assertTrue(page2Text.contains("29-10-2027"), "PDF must contain calculated valid-until date 29-10-2027");
    }

    @Test
    @DisplayName("3. Reopening or Editing Quotation Preserves Original Creation Date and Validity Date")
    void testEditingQuotationPreservesDates() {
        LocalDate originalDate = LocalDate.of(2026, 4, 10);
        LocalDate expectedValidUntil = originalDate.plusDays(365); // 2027-04-10
        String qNumber = "QT-EDIT-" + System.nanoTime();

        Quotation q = quotationService.create(
                qNumber,
                customer.getId(),
                employee.getId(),
                branch.getId(),
                originalDate,
                PricingMode.MIDDLE,
                "Original requirements",
                "Original notes"
        );

        assertEquals(originalDate, q.getQuotationDate());
        assertEquals(expectedValidUntil, q.getValidUntil());

        // 1. Edit via updateBasicDetails with a different date
        LocalDate attemptedNewDate = LocalDate.of(2026, 11, 20);
        Quotation updated = quotationService.updateBasicDetails(
                q.getId(),
                customer.getId(),
                employee.getId(),
                branch.getId(),
                attemptedNewDate,
                PricingMode.PREMIUM,
                "Updated requirements",
                "Updated notes"
        );

        assertEquals(originalDate, updated.getQuotationDate(), "Editing must not reset original creation date!");
        assertEquals(expectedValidUntil, updated.getValidUntil(), "Editing must not extend valid until date!");

        // 2. Edit via AdminQuotationBuilderService with a different date
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                q.getId(),
                custReq,
                branch.getId(),
                LocalDate.of(2026, 12, 1),
                PricingMode.ECONOMY,
                "Builder edit",
                "Notes edit",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true
        );

        Quotation builderSaved = adminQuotationBuilderService.save(builderReq, admin.getEmail());
        assertEquals(originalDate, builderSaved.getQuotationDate(), "Builder edit must preserve original quotation date!");
        assertEquals(expectedValidUntil, builderSaved.getValidUntil(), "Builder edit must preserve original valid until!");
    }

    @Test
    @DisplayName("4. Automatic Cleanup Preserves Quotations Aged Under 365 Days (0, 60, 180, 364 days)")
    void testCleanupPreservesUnder365Days() {
        LocalDate today = LocalDate.of(2027, 10, 29);

        // 0 days old (today)
        Quotation qToday = quotationService.create("QT-0D-" + System.nanoTime(), customer.getId(), employee.getId(), branch.getId(), today, PricingMode.PREMIUM, null, null);
        // 60 days old
        Quotation q60 = quotationService.create("QT-60D-" + System.nanoTime(), customer.getId(), employee.getId(), branch.getId(), today.minusDays(60), PricingMode.PREMIUM, null, null);
        // 180 days old
        Quotation q180 = quotationService.create("QT-180D-" + System.nanoTime(), customer.getId(), employee.getId(), branch.getId(), today.minusDays(180), PricingMode.PREMIUM, null, null);
        // 364 days old
        Quotation q364 = quotationService.create("QT-364D-" + System.nanoTime(), customer.getId(), employee.getId(), branch.getId(), today.minusDays(364), PricingMode.PREMIUM, null, null);

        // Run retention cleanup as of `today` with 365 days retention
        int deleted = quotationCleanupService.cleanUpQuotationsOlderThan(today, 365);

        // None of these should be deleted
        assertTrue(quotationRepository.existsById(qToday.getId()), "0-day quotation must NOT be deleted");
        assertTrue(quotationRepository.existsById(q60.getId()), "60-day quotation must NOT be deleted");
        assertTrue(quotationRepository.existsById(q180.getId()), "180-day quotation must NOT be deleted");
        assertTrue(quotationRepository.existsById(q364.getId()), "364-day quotation must NOT be deleted");
    }

    @Test
    @DisplayName("5. Automatic Cleanup Deletes Quotations Aged >= 365 Days")
    void testCleanupDeletes365DaysOrMore() {
        LocalDate today = LocalDate.of(2027, 10, 29);

        // Exactly 365 days old
        Quotation q365 = quotationService.create("QT-365D-" + System.nanoTime(), customer.getId(), employee.getId(), branch.getId(), today.minusDays(365), PricingMode.PREMIUM, null, null);
        // 400 days old
        Quotation q400 = quotationService.create("QT-400D-" + System.nanoTime(), customer.getId(), employee.getId(), branch.getId(), today.minusDays(400), PricingMode.PREMIUM, null, null);

        // Run cleanup with reference date `today` and 365 days
        int deleted = quotationCleanupService.cleanUpQuotationsOlderThan(today, 365);
        assertTrue(deleted >= 2, "Should delete at least the 2 expired quotations");

        assertFalse(quotationRepository.existsById(q365.getId()), "Quotation with age >= 365 days MUST be deleted");
        assertFalse(quotationRepository.existsById(q400.getId()), "Quotation with age >= 400 days MUST be deleted");
    }

    @Test
    @DisplayName("6. Cascade Deletion Cleans Up All Dependent Records and Preserves Master Data")
    void testCascadeDeleteAndMasterDataPreservation() {
        LocalDate today = LocalDate.of(2027, 10, 29);
        LocalDate oldDate = today.minusDays(370);

        WorkCategory category = new WorkCategory();
        category.setName("Living Storage " + System.nanoTime());
        category.setActive(true);
        category = workCategoryRepository.save(category);

        Material material = new Material();
        material.setName("Marine Plywood " + System.nanoTime());
        material.setMeasurementType(MeasurementType.SQFT);
        material = materialRepository.save(material);

        Quotation q = quotationService.create(
                "QT-CASCADE-" + System.nanoTime(),
                customer.getId(),
                employee.getId(),
                branch.getId(),
                oldDate,
                PricingMode.PREMIUM,
                "Cascade test",
                "Verify cascading"
        );
        Long qId = q.getId();

        // Attach Room
        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setFloor("1st Floor");
        room.setRoom("Living Room");
        room.setActive(true);
        room = quotationRoomRepository.save(room);
        Long roomId = room.getId();

        // Attach Item
        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setMaterial(material);
        item.setItemDescription("TV Unit");
        item.setRate(new BigDecimal("450.00"));
        item.setAmount(new BigDecimal("4500.00"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setFormula("L × W");
        item.setQuantity(BigDecimal.ONE);
        item = quotationItemRepository.save(item);
        Long itemId = item.getId();

        // Attach Size
        QuotationItemSize size = new QuotationItemSize();
        size.setQuotationItem(item);
        size.setLengthValue(new BigDecimal("10.0"));
        size.setWidthValue(new BigDecimal("1.0"));
        size.setHeightValue(new BigDecimal("1.0"));
        size.setCalculatedSqft(new BigDecimal("10.0"));
        size.setDisplayOrder(1);
        size = quotationItemSizeRepository.save(size);
        Long sizeId = size.getId();

        // Attach Communication
        QuotationCommunication comm = new QuotationCommunication();
        comm.setQuotation(q);
        comm.setType(QuotationCommunication.CommunicationType.WHATSAPP);
        comm.setRecipient(customer.getPhone());
        comm.setStatus(QuotationCommunication.CommunicationStatus.SENT);
        comm.setSentAt(LocalDateTime.now().minusDays(360));
        comm = quotationCommunicationRepository.save(comm);
        Long commId = comm.getId();

        // Attach Status History
        QuotationStatusHistory hist = new QuotationStatusHistory();
        hist.setQuotation(q);
        hist.setNewStatus(QuotationStatus.WORKING);
        hist.setChangedAt(LocalDateTime.now().minusDays(360));
        hist = quotationStatusHistoryRepository.save(hist);
        Long histId = hist.getId();

        // Attach Workflow
        QuotationWorkflow wf = new QuotationWorkflow();
        wf.setQuotation(q);
        wf.setAction(QuotationWorkflow.WorkflowAction.VIEWED);
        wf.setCreatedAt(LocalDateTime.now().minusDays(360));
        wf = quotationWorkflowRepository.save(wf);
        Long wfId = wf.getId();

        // Execute cleanup
        quotationCleanupService.cleanUpQuotationsOlderThan(today, 365);

        // Verify Quotation and ALL dependent child records are deleted
        assertFalse(quotationRepository.existsById(qId), "Quotation must be deleted");
        assertFalse(quotationRoomRepository.existsById(roomId), "Room must be deleted");
        assertFalse(quotationItemRepository.existsById(itemId), "Item must be deleted");
        assertFalse(quotationItemSizeRepository.existsById(sizeId), "Size must be deleted");
        assertFalse(quotationCommunicationRepository.existsById(commId), "Communication must be deleted");
        assertFalse(quotationStatusHistoryRepository.existsById(histId), "Status history must be deleted");
        assertFalse(quotationWorkflowRepository.existsById(wfId), "Workflow must be deleted");

        // Verify Master Data remains intact
        assertTrue(customerRepository.existsById(customer.getId()), "Customer master data must NOT be deleted");
        assertTrue(userRepository.existsById(employee.getId()), "Employee master data must NOT be deleted");
        assertTrue(branchRepository.existsById(branch.getId()), "Branch master data must NOT be deleted");
        assertTrue(workCategoryRepository.existsById(category.getId()), "Category master data must NOT be deleted");
        assertTrue(materialRepository.existsById(material.getId()), "Material master data must NOT be deleted");
    }
}

