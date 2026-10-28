package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationPdfService;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationPdfAmountSummaryTest {

    @Autowired
    private QuotationPdfService quotationPdfService;

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
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private UserRepository userRepository;

    private Branch branch;
    private Customer customer;
    private WorkCategory category;
    private Material material;
    private MaterialOption option;
    private User adminUser;
    private User employeeUser;

    @BeforeEach
    public void setup() {
        branch = new Branch();
        branch.setName("Test Branch");
        branch.setBranchAddress("123 Main Street");
        branch.setWhatsapp("9876543210");
        branch.setEmail("branch@example.com");
        branch = branchRepository.save(branch);

        customer = new Customer();
        customer.setName("Suresh Raina");
        customer.setPhone("9876500000");
        customer.setEmail("suresh@example.com");
        customer.setAddress("10 Stadium Road");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        category = workCategoryRepository.findByNameIgnoreCase("Modular Kitchen")
                .orElseGet(() -> {
                    WorkCategory wc = new WorkCategory();
                    wc.setName("Modular Kitchen");
                    wc.setActive(true);
                    return workCategoryRepository.save(wc);
                });

        material = materialRepository.findAll().stream()
                .filter(m -> "BWP Marine Ply".equalsIgnoreCase(m.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Material m = new Material();
                    m.setName("BWP Marine Ply");
                    m.setActive(true);
                    return materialRepository.save(m);
                });

        option = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(material.getId()).stream()
                .findFirst()
                .orElseGet(() -> {
                    MaterialOption mo = new MaterialOption();
                    mo.setMaterial(material);
                    mo.setName("18MM BWP");
                    mo.setActive(true);
                    return materialOptionRepository.save(mo);
                });

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin_test_summary_" + System.currentTimeMillis() + "@example.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);
        adminUser.setBranch(branch);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Employee User");
        employeeUser.setEmail("emp_test_summary_" + System.currentTimeMillis() + "@example.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setActive(true);
        employeeUser.setBranch(branch);
        employeeUser = userRepository.save(employeeUser);
    }

    private String extractPageText(byte[] pdfBytes, int pageNum) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        byte[] pageContent = reader.getPageContent(pageNum);
        reader.close();
        return pageContent != null ? new String(pageContent, StandardCharsets.ISO_8859_1) : "";
    }

    private Quotation createQuotationWithItems(String qNum, User creator, BigDecimal totalAmount, BigDecimal offerPercent) {
        Quotation q = new Quotation();
        q.setQuotationNumber(qNum);
        q.setCustomer(customer);
        q.setBranch(branch);
        q.setPricingMode(PricingMode.PREMIUM);
        q.setQuotationDate(LocalDate.now());
        q.setEmployee(creator);
        q.setStatus(QuotationStatus.WORKING);
        q.setSubtotal(totalAmount);
        q.setTotalAmount(totalAmount);
        q.setOfferPercentage(offerPercent);

        BigDecimal offerAmt = totalAmount.multiply(offerPercent).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        q.setOfferAmount(offerAmt);
        q.setGrandTotal(totalAmount.subtract(offerAmt));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setRoom("Kitchen");
        room.setFloor("1st Floor");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setMaterial(material);
        item.setMaterialOption(option);
        item.setLengthValue(new BigDecimal("10.00"));
        item.setWidthValue(BigDecimal.ZERO);
        item.setHeightValue(new BigDecimal("10.00"));
        item.setCalculatedSqft(new BigDecimal("100.00"));
        item.setRate(totalAmount.divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
        item.setAmount(totalAmount);
        item.setOfferPrice(totalAmount.subtract(offerAmt));
        item.setActive(true);
        quotationItemRepository.save(item);

        return q;
    }

    @Test
    @DisplayName("Admin PDF with 0% offer: Total Amount, Offer Price, Grand Total is full total, unwanted rows absent")
    public void testAdminQuotationWithZeroPercentOffer() throws Exception {
        BigDecimal total = new BigDecimal("100000.00");
        BigDecimal offerPct = BigDecimal.ZERO;
        Quotation q = createQuotationWithItems("QT-ADM-001", adminUser, total, offerPct);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Summary rows - Total Amount and Offer Price must be present
        assertTrue(page2.contains("Total Amount"), "Summary must contain Total Amount row");
        assertTrue(page2.contains("Offer Price"), "Summary must contain Offer Price row");
        assertTrue(page2.contains("GRAND TOTAL"), "Summary must contain GRAND TOTAL row");

        // Verification 2: Offer Percentage must NOT be displayed in summary
        assertFalse(page2.contains("0.00%"), "Summary must not display offer percentage");

        // Verification 3: Unwanted rows must NOT appear in summary
        assertFalse(page2.contains("Accessories"), "Summary must not contain Accessories row");
        assertFalse(page2.contains("Taxable Amount"), "Summary must not contain Taxable Amount row");
        assertFalse(page2.contains("Travel Charge"), "Summary must not contain Travel Charge row");
        assertFalse(page2.contains("Other Charges"), "Summary must not contain Other Charges row");
        assertFalse(page2.contains("Discount"), "Summary must not contain Discount row");

        // Verification 4: Words match PDF Grand Total (1,00,000)
        assertTrue(page2.contains("AMOUNT IN WORDS"), "Must contain AMOUNT IN WORDS");
        assertTrue(page2.contains("ONE LAKH"), "Amount in words must match full grand total (1,00,000)");
    }

    @Test
    @DisplayName("Admin PDF with 10% offer: Total Amount, Offer Price, Grand Total is 90000.00, unwanted rows absent")
    public void testAdminQuotationWithTenPercentOffer() throws Exception {
        BigDecimal total = new BigDecimal("100000.00");
        BigDecimal offerPct = new BigDecimal("10.00");
        Quotation q = createQuotationWithItems("QT-ADM-002", adminUser, total, offerPct);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Summary rows
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertTrue(page2.contains("GRAND TOTAL"));

        // Verification 2: Offer percentage not in summary
        assertFalse(page2.contains("10.00%"));

        // Verification 3: Unwanted rows absent
        assertFalse(page2.contains("Accessories"));
        assertFalse(page2.contains("Taxable Amount"));
        assertFalse(page2.contains("Travel Charge"));
        assertFalse(page2.contains("Other Charges"));
        assertFalse(page2.contains("Discount"));

        // Verification 4: Amount in words is Ninety Thousand
        assertTrue(page2.contains("AMOUNT IN WORDS"));
        assertTrue(page2.contains("NINETY THOUSAND"), "Amount in words must match payable grand total 90,000");
    }

    @Test
    @DisplayName("Admin PDF with 20% offer: Total Amount, Offer Price, Grand Total is 80000.00, unwanted rows absent")
    public void testAdminQuotationWithTwentyPercentOffer() throws Exception {
        BigDecimal total = new BigDecimal("100000.00");
        BigDecimal offerPct = new BigDecimal("20.00");
        Quotation q = createQuotationWithItems("QT-ADM-003", adminUser, total, offerPct);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Summary rows
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertTrue(page2.contains("GRAND TOTAL"));

        // Verification 2: Offer percentage not in summary
        assertFalse(page2.contains("20.00%"));

        // Verification 3: Unwanted rows absent
        assertFalse(page2.contains("Accessories"));
        assertFalse(page2.contains("Taxable Amount"));
        assertFalse(page2.contains("Travel Charge"));
        assertFalse(page2.contains("Other Charges"));
        assertFalse(page2.contains("Discount"));

        // Verification 4: Amount in words is Eighty Thousand
        assertTrue(page2.contains("AMOUNT IN WORDS"));
        assertTrue(page2.contains("EIGHTY THOUSAND"), "Amount in words must match payable grand total 80,000");
    }

    @Test
    @DisplayName("Employee PDF with 0% offer: Total Amount, Offer Price, Grand Total is 50000.00")
    public void testEmployeeQuotationWithZeroPercentOffer() throws Exception {
        BigDecimal total = new BigDecimal("50000.00");
        BigDecimal offerPct = BigDecimal.ZERO;
        Quotation q = createQuotationWithItems("QT-EMP-001", employeeUser, total, offerPct);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Summary rows
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertTrue(page2.contains("GRAND TOTAL"));

        // Verification 2: Offer percentage not in summary
        assertFalse(page2.contains("0.00%"));

        // Verification 3: Unwanted rows absent
        assertFalse(page2.contains("Accessories"));
        assertFalse(page2.contains("Taxable Amount"));
        assertFalse(page2.contains("Travel Charge"));
        assertFalse(page2.contains("Other Charges"));
        assertFalse(page2.contains("Discount"));

        // Verification 4: Words match
        assertTrue(page2.contains("AMOUNT IN WORDS"));
        assertTrue(page2.contains("FIFTY THOUSAND"), "Amount in words must match payable grand total 50,000");
    }

    @Test
    @DisplayName("Employee PDF with 10% offer: Total Amount, Offer Price, Grand Total is 45000.00")
    public void testEmployeeQuotationWithTenPercentOffer() throws Exception {
        BigDecimal total = new BigDecimal("50000.00");
        BigDecimal offerPct = new BigDecimal("10.00");
        Quotation q = createQuotationWithItems("QT-EMP-002", employeeUser, total, offerPct);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Summary rows
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertTrue(page2.contains("GRAND TOTAL"));

        // Verification 2: Offer percentage not in summary
        assertFalse(page2.contains("10.00%"));

        // Verification 3: Unwanted rows absent
        assertFalse(page2.contains("Accessories"));
        assertFalse(page2.contains("Taxable Amount"));
        assertFalse(page2.contains("Travel Charge"));
        assertFalse(page2.contains("Other Charges"));
        assertFalse(page2.contains("Discount"));

        // Verification 4: Words match
        assertTrue(page2.contains("AMOUNT IN WORDS"));
        assertTrue(page2.contains("FORTY FIVE THOUSAND"), "Amount in words must match payable grand total 45,000");
    }

    @Test
    @DisplayName("Employee PDF with 15% offer: Total Amount, Offer Price, Grand Total is 42500.00")
    public void testEmployeeQuotationWithFifteenPercentOffer() throws Exception {
        BigDecimal total = new BigDecimal("50000.00");
        BigDecimal offerPct = new BigDecimal("15.00");
        Quotation q = createQuotationWithItems("QT-EMP-003", employeeUser, total, offerPct);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Summary rows
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertTrue(page2.contains("GRAND TOTAL"));

        // Verification 2: Offer percentage not in summary
        assertFalse(page2.contains("15.00%"));

        // Verification 3: Unwanted rows absent
        assertFalse(page2.contains("Accessories"));
        assertFalse(page2.contains("Taxable Amount"));
        assertFalse(page2.contains("Travel Charge"));
        assertFalse(page2.contains("Other Charges"));
        assertFalse(page2.contains("Discount"));

        // Verification 4: Words match
        assertTrue(page2.contains("AMOUNT IN WORDS"));
        assertTrue(page2.contains("FORTY TWO THOUSAND FIVE HUNDRED"), "Amount in words must match payable grand total 42,500");
    }

    @Test
    @DisplayName("Quotation with Travel Charge, Other Charges, Discount, and GST: PDF includes configured charges, GST, and matches Grand Total")
    public void testPdfIncludesConfiguredChargesAndMatchesSavedGrandTotal() throws Exception {
        BigDecimal itemsTotal = new BigDecimal("64000.00");
        BigDecimal offerPct = new BigDecimal("10.00");
        Quotation q = createQuotationWithItems("QT-CHARGES-001", adminUser, itemsTotal, offerPct);

        // Add additional charges to the stored Quotation entity
        q.setTravelCharge(new BigDecimal("3000.00"));
        q.setOtherCharges(new BigDecimal("1500.00"));
        q.setDiscountAmount(new BigDecimal("2000.00"));
        q.setGstPercentage(new BigDecimal("18.00"));
        // subtotal = 64,000, travel = 3,000, other = 1,500, discount = 2,000 => taxable = 66,500
        // gst 18% = 11,970 => totalAmount = 78,470
        // offer 10% = 7,847 => grandTotal = 70,623
        q.setTaxableAmount(new BigDecimal("66500.00"));
        q.setGstAmount(new BigDecimal("11970.00"));
        q.setTotalAmount(new BigDecimal("78470.00"));
        q.setOfferAmount(new BigDecimal("7847.00"));
        q.setGrandTotal(new BigDecimal("70623.00"));
        q = quotationRepository.save(q);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Verification 1: Configured rows are present
        assertTrue(page2.contains("Subtotal"), "Summary must contain Subtotal row");
        assertTrue(page2.contains("Travel Charges"), "Summary must contain Travel Charges row");
        assertTrue(page2.contains("Other Charges"), "Summary must contain Other Charges row");
        assertTrue(page2.contains("Discount"), "Summary must contain Discount row");
        assertTrue(page2.contains("GST (18%)") || page2.contains("GST"), "Summary must contain GST row");
        assertFalse(page2.contains("Taxable Amount"), "Intermediate Taxable Amount must not appear");

        // Verification 2: Total Amount, Offer Price, and GRAND TOTAL are present
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertTrue(page2.contains("GRAND TOTAL"));

        // Verification 3: PDF Grand Total matches saved database grand total: 70,623
        assertTrue(page2.contains("SEVENTY THOUSAND SIX HUNDRED TWENTY THREE"),
                "Amount in words must match saved quotation grand total 70,623");
    }
}
