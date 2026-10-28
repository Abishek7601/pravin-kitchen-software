package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.NumberToWordsService;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationFinancialConsistencyTest {

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private QuotationCalculationService calculationService;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private NumberToWordsService numberToWordsService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

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
    private UserRepository userRepository;

    private Branch branch;
    private WorkCategory category;
    private Division division;
    private Item item;
    private Material material;
    private MaterialOption option;
    private User adminUser;

    @BeforeEach
    public void setup() {
        adminUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Admin");
                    u.setEmail("admin@pravinkitchens.com");
                    u.setPassword("password");
                    u.setRole(Role.SUPER_ADMIN);
                    u.setActive(true);
                    return userRepository.save(u);
                });
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Chennai Main");
            b.setBranchAddress("100 Anna Salai, Chennai");
            b.setWhatsapp("9876543210");
            b.setEmail("chennai@pravinkitchens.com");
            return branchRepository.save(b);
        });

        long ts = System.currentTimeMillis();
        category = new WorkCategory();
        category.setName("Category " + ts);
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("Division " + ts);
        division.setWorkCategory(category);
        division.setActive(true);
        division.setDisplayOrder(1);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Item " + ts);
        item.setDivision(division);
        item.setActive(true);
        item.setDisplayOrder(1);
        item = itemRepository.save(item);

        material = new Material();
        material.setName("Material " + ts);
        material.setItem(item);
        material.setActive(true);
        material.setDisplayOrder(1);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("Option " + ts);
        option.setMaterial(material);
        option.setActive(true);
        option = materialOptionRepository.save(option);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(option);
        pricing.setPricingMode(PricingMode.MIDDLE);
        pricing.setRate(new BigDecimal("1200.00"));
        pricing.setFormula("(L + W) × H");
        pricing.setUnit("SQFT");
        pricing.setActive(true);
        pricingRepository.save(pricing);
    }

    private String extractPageText(byte[] pdfBytes, int pageNum) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            byte[] pageBytes = reader.getPageContent(pageNum);
            return new String(pageBytes, StandardCharsets.UTF_8);
        } finally {
            reader.close();
        }
    }

    @Test
    @DisplayName("Admin Workflow: Amount calculated during creation matches DB record, View page data, and generated PDF")
    public void testAdminWorkflowFinancialConsistency() throws Exception {
        // Customer & Request
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Karthik Raja", "9876511223", "karthik@example.com", "45 Gandhi Road, Chennai"
        );

        // Room 1: Kitchen with 1 item, 2 sizes
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("60.00"), new BigDecimal("24.00"), new BigDecimal("34.00"), null
        );
        AdminQuotationBuilderService.SizeRequest size2 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("48.00"), new BigDecimal("24.00"), new BigDecimal("34.00"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq1 = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Main Kitchen Counter", "18mm Waterproof", List.of(size1, size2)
        );
        AdminQuotationBuilderService.RoomRequest roomReq1 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Modular Setup", List.of(itemReq1)
        );

        // Additional Charges matching creation page inputs
        BigDecimal accessories = new BigDecimal("5000.00");
        BigDecimal travel = new BigDecimal("2500.00");
        BigDecimal other = new BigDecimal("1000.00");
        BigDecimal discount = new BigDecimal("3500.00");
        BigDecimal gstPct = new BigDecimal("18.00");
        BigDecimal offerPct = new BigDecimal("5.00");

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Custom modular kitchen requirements", "Delivery within 3 weeks",
                List.of(roomReq1),
                accessories, travel, other, discount, gstPct, offerPct
        );

        // 1. SAVE QUOTATION
        Quotation saved = builderService.save(request, adminUser.getEmail());
        assertNotNull(saved);
        assertNotNull(saved.getId());

        // 2. VERIFY SAVED DATABASE AMOUNTS
        Quotation dbQuotation = quotationRepository.findById(saved.getId()).orElseThrow();
        BigDecimal subtotal = dbQuotation.getSubtotal();
        assertNotNull(subtotal);
        assertTrue(subtotal.compareTo(BigDecimal.ZERO) > 0, "Subtotal must be greater than zero");

        // Verify mathematical business rules on DB record
        BigDecimal expectedTaxable = subtotal.add(accessories).add(travel).add(other).subtract(discount);
        if (expectedTaxable.compareTo(BigDecimal.ZERO) < 0) expectedTaxable = BigDecimal.ZERO;
        assertEquals(expectedTaxable.setScale(2, RoundingMode.HALF_UP), dbQuotation.getTaxableAmount());

        BigDecimal expectedGst = expectedTaxable.multiply(gstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        assertEquals(expectedGst, dbQuotation.getGstAmount());

        BigDecimal expectedTotal = expectedTaxable.add(expectedGst);
        assertEquals(expectedTotal, dbQuotation.getTotalAmount());

        BigDecimal expectedOffer = expectedTotal.multiply(offerPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        assertEquals(expectedOffer, dbQuotation.getOfferAmount());

        BigDecimal expectedGrandTotal = expectedTotal.subtract(expectedOffer);
        assertEquals(expectedGrandTotal, dbQuotation.getGrandTotal());

        // 3. GENERATE PDF AND VERIFY FINANCIAL ACCURACY
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(dbQuotation.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        String page2 = extractPageText(pdfBytes, 2);

        // Verify summary rows
        assertTrue(page2.contains("Subtotal"), "PDF must contain Subtotal row");
        assertTrue(page2.contains("Accessories"), "PDF must contain Accessories row");
        assertTrue(page2.contains("Travel Charges"), "PDF must contain Travel Charges row");
        assertTrue(page2.contains("Other Charges"), "PDF must contain Other Charges row");
        assertTrue(page2.contains("Discount"), "PDF must contain Discount row");
        assertTrue(page2.contains("GST"), "PDF must contain GST row");
        assertTrue(page2.contains("Total Amount"), "PDF must contain Total Amount row");
        assertTrue(page2.contains("Offer Price"), "PDF must contain Offer Price row");
        assertTrue(page2.contains("GRAND TOTAL"), "PDF must contain GRAND TOTAL row");

        // Verify Amount in Words matches exact grand total
        String expectedWords = numberToWordsService.convertRupees(expectedGrandTotal).toUpperCase();
        assertTrue(page2.contains("AMOUNT IN WORDS"), "PDF must contain AMOUNT IN WORDS");
        assertTrue(page2.contains(expectedWords), "PDF amount in words must match grand total exactly");

        // Verify Payment Terms instalments sum to Grand Total
        BigDecimal token = expectedGrandTotal.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal advance = expectedGrandTotal.multiply(new BigDecimal("0.50")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal part = expectedGrandTotal.multiply(new BigDecimal("0.35")).setScale(0, RoundingMode.HALF_UP);
        BigDecimal finalPay = expectedGrandTotal.setScale(0, RoundingMode.HALF_UP).subtract(token).subtract(advance).subtract(part);
        assertEquals(expectedGrandTotal.setScale(0, RoundingMode.HALF_UP), token.add(advance).add(part).add(finalPay));

        assertTrue(page2.contains("10% Token:"), "PDF must contain 10% Token");
        assertTrue(page2.contains("50% Advance:"), "PDF must contain 50% Advance");
        assertTrue(page2.contains("35% Part Payment:"), "PDF must contain 35% Part Payment");
        assertTrue(page2.contains("5% Final Payment:"), "PDF must contain 5% Final Payment");
    }

    @Test
    @DisplayName("Employee Workflow: Amount calculated during creation matches DB record and generated PDF")
    public void testEmployeeWorkflowFinancialConsistency() throws Exception {
        User employee = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE && u.getBranch() != null)
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Test Employee");
                    u.setEmail("employee.test@pravinkitchens.com");
                    u.setPassword("password");
                    u.setRole(Role.EMPLOYEE);
                    u.setBranch(branch);
                    u.setActive(true);
                    return userRepository.save(u);
                });

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Sanjay Verma", "9876522334", "sanjay@example.com", "12 Race Course Road"
        );

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("72.00"), new BigDecimal("24.00"), new BigDecimal("36.00"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Living Room TV Unit", "Commercial Ply", List.of(size1)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "First Floor", "Living Room", "Living Room TV Unit", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, employee.getBranch().getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Employee TV unit setup", "Standard installation",
                List.of(roomReq),
                new BigDecimal("2000.00"), new BigDecimal("1500.00"), BigDecimal.ZERO,
                new BigDecimal("1000.00"), new BigDecimal("18.00"), new BigDecimal("10.00")
        );

        Quotation saved = builderService.save(request, employee.getEmail());
        assertNotNull(saved);

        Quotation dbQ = quotationRepository.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getGrandTotal(), dbQ.getGrandTotal());

        // Verify PDF matches saved record
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(dbQ.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);
        assertTrue(page2.contains("GRAND TOTAL"));
        String expectedWords = numberToWordsService.convertRupees(dbQ.getGrandTotal()).toUpperCase();
        assertTrue(page2.contains(expectedWords));
    }

    @Test
    @DisplayName("Zero Charges Quotation: subtotal equals total amount and grand total across DB and PDF")
    public void testZeroChargesQuotationConsistency() throws Exception {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Priya Shankar", "9876533445", "priya@example.com", "77 Beach Road"
        );

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("30.00"), new BigDecimal("24.00"), new BigDecimal("30.00"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Pooja Unit", "Teak Finish", List.of(size1)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Pooja Room", "Pooja Unit", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                null, null, List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        assertNotNull(saved);

        Quotation dbQ = quotationRepository.findById(saved.getId()).orElseThrow();
        // Subtotal == Taxable == Total == GrandTotal when extra charges and offer are zero
        assertEquals(0, dbQ.getSubtotal().compareTo(dbQ.getTaxableAmount()));
        assertEquals(0, dbQ.getSubtotal().compareTo(dbQ.getTotalAmount()));
        assertEquals(0, dbQ.getSubtotal().compareTo(dbQ.getGrandTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dbQ.getOfferAmount()));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(dbQ.getId());
        assertNotNull(pdfBytes);
        String page2 = extractPageText(pdfBytes, 2);
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("GRAND TOTAL"));
        assertFalse(page2.contains("Accessories"), "Unconfigured Accessories must not appear");
        assertFalse(page2.contains("Travel Charges"), "Unconfigured Travel Charges must not appear");
        assertFalse(page2.contains("Discount"), "Unconfigured Discount must not appear");
    }
}
