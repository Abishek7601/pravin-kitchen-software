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

    @Autowired
    private DivisionRepository divisionRepository;

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
        String text = null;
        try {
            com.lowagie.text.pdf.parser.PdfTextExtractor extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
            text = extractor.getTextFromPage(pageNum);
        } catch (Exception ignored) {
        }
        if (text == null || text.isBlank()) {
            byte[] pageContent = reader.getPageContent(pageNum);
            text = pageContent != null ? new String(pageContent, StandardCharsets.ISO_8859_1) : "";
        }
        reader.close();
        return text;
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

        // Verification 1: Summary rows - Total Amount and OFFER PRICE header must be present
        assertTrue(page2.contains("Total Amount"), "Summary must contain Total Amount row");
        assertTrue(page2.contains("OFFER PRICE"), "Summary table must contain OFFER PRICE header");
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
        assertTrue(page2.contains("OFFER PRICE"));
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
        assertTrue(page2.contains("OFFER PRICE"));
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
        assertTrue(page2.contains("OFFER PRICE"));
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
        assertTrue(page2.contains("OFFER PRICE"));
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
        assertTrue(page2.contains("OFFER PRICE"));
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

        // Verification 2: Total Amount and Grand Total are present in summary
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.toUpperCase().contains("GRAND TOTAL"));

        // Verification 3: PDF Grand Total matches saved database grand total: 70,623
        assertTrue(page2.contains("SEVENTY THOUSAND SIX HUNDRED TWENTY THREE"),
                "Amount in words must match saved quotation grand total 70,623");
    }

    @Test
    @DisplayName("Quote Summary table includes OFFER PRICE column header and preserves summary Total Amount and Grand Total rows")
    public void testQuoteSummaryTableRemovesOfferPriceColumnHeader() throws Exception {
        BigDecimal total = new BigDecimal("50000.00");
        Quotation q = createQuotationWithItems("QT-COL-001", adminUser, total, BigDecimal.ZERO);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Header check: Required headers present including Offer Price
        assertTrue(page2.toUpperCase().contains("WORK TYPE"), "Table header must have WORK TYPE");
        assertTrue(page2.toUpperCase().contains("MATERIALS"), "Table header must have MATERIALS");
        assertTrue(page2.toUpperCase().contains("SQFT") || page2.toUpperCase().contains("SQ.FT"), "Table header must have SQFT");
        assertTrue(page2.toUpperCase().contains("OFFER PRICE"), "Table header must have OFFER PRICE column header");

        // Footer check: Footer summary rows retain Total Amount and GRAND TOTAL
        assertTrue(page2.contains("Total Amount"), "Footer summary row must retain Total Amount");
        assertTrue(page2.toUpperCase().contains("GRAND TOTAL"), "Footer must retain GRAND TOTAL");
    }

    @Test
    @DisplayName("PDF renders material name once even if option name is identical and cleans specification")
    public void testMaterialNameDeduplicationInPdf() throws Exception {
        Material plyMat = new Material();
        plyMat.setName("Commercial Ply");
        plyMat.setActive(true);
        plyMat.setSpecification("Commercial Ply Thickness 16mm with mica");
        plyMat = materialRepository.save(plyMat);

        MaterialOption dupOption = new MaterialOption();
        dupOption.setMaterial(plyMat);
        dupOption.setName("Commercial Ply");
        dupOption.setActive(true);
        dupOption = materialOptionRepository.save(dupOption);

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-DEDUP-001");
        q.setCustomer(customer);
        q.setBranch(branch);
        q.setPricingMode(PricingMode.MIDDLE);
        q.setQuotationDate(LocalDate.now());
        q.setEmployee(adminUser);
        q.setStatus(QuotationStatus.WORKING);
        q.setSubtotal(new BigDecimal("25000.00"));
        q.setTotalAmount(new BigDecimal("25000.00"));
        q.setOfferPercentage(BigDecimal.ZERO);
        q.setOfferAmount(BigDecimal.ZERO);
        q.setGrandTotal(new BigDecimal("25000.00"));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setRoom("Master Bedroom");
        room.setFloor("Ground Floor");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setMaterial(plyMat);
        item.setMaterialOption(dupOption);
        item.setSpecification("Commercial Ply Thickness 16mm with mica");
        item.setLengthValue(new BigDecimal("8.00"));
        item.setWidthValue(BigDecimal.ZERO);
        item.setHeightValue(new BigDecimal("6.00"));
        item.setCalculatedSqft(new BigDecimal("48.00"));
        item.setRate(new BigDecimal("500.00"));
        item.setAmount(new BigDecimal("24000.00"));
        item.setActive(true);
        quotationItemRepository.save(item);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPageText(pdfBytes, 2);

        // Material name should appear only once, NOT duplicate as COMMERCIAL PLY - COMMERCIAL PLY
        assertFalse(page2.contains("COMMERCIAL PLY - COMMERCIAL PLY"), "Must not duplicate material name");
        assertTrue(page2.contains("COMMERCIAL PLY"), "Must contain material name");
        assertTrue(page2.contains("Thickness 16mm With Mica"), "Specification must be formatted in Title Case without duplicate material name prefix");
    }

    @Test
    @DisplayName("Verify Work Type grouping with row-spanning, separate Price & Offer Price Total, and financial calculation sequence")
    public void testWorkTypeGroupingAndPricingCalculationSequence() throws Exception {
        WorkCategory foyerCat = workCategoryRepository.findByNameIgnoreCase("Foyer")
                .orElseGet(() -> {
                    WorkCategory c = new WorkCategory();
                    c.setName("Foyer");
                    c.setActive(true);
                    return workCategoryRepository.save(c);
                });

        WorkCategory ceilingCat = workCategoryRepository.findByNameIgnoreCase("False Ceiling")
                .orElseGet(() -> {
                    WorkCategory c = new WorkCategory();
                    c.setName("False Ceiling");
                    c.setActive(true);
                    return workCategoryRepository.save(c);
                });

        Division shoeRackDiv = new Division();
        shoeRackDiv.setName("Shoe Rack");
        shoeRackDiv.setWorkCategory(foyerCat);
        shoeRackDiv.setActive(true);
        shoeRackDiv = divisionRepository.save(shoeRackDiv);

        Division ceilingDiv = new Division();
        ceilingDiv.setName("Gypsum Ceiling");
        ceilingDiv.setWorkCategory(ceilingCat);
        ceilingDiv.setActive(true);
        ceilingDiv = divisionRepository.save(ceilingDiv);

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-WORKTYPE-001");
        q.setCustomer(customer);
        q.setBranch(branch);
        q.setPricingMode(PricingMode.MIDDLE);
        q.setQuotationDate(LocalDate.now());
        q.setEmployee(adminUser);
        q.setStatus(QuotationStatus.WORKING);
        q.setTravelCharge(new BigDecimal("2000.00"));
        q.setOtherCharges(new BigDecimal("1000.00"));
        q.setOtherChargesDescription("Food charges");
        q.setGstPercentage(new BigDecimal("18.00"));
        q.setGstAmount(new BigDecimal("8460.00"));
        q.setSubtotal(new BigDecimal("44000.00"));
        q.setTotalAmount(new BigDecimal("47000.00"));
        q.setGrandTotal(new BigDecimal("55460.00"));
        q = quotationRepository.save(q);

        // Floor 1: Ground Floor -> Foyer -> Work Type: Shoe Rack with 3 materials
        QuotationRoom room1 = new QuotationRoom();
        room1.setQuotation(q);
        room1.setRoom("Entrance");
        room1.setFloor("Ground Floor");
        room1.setActive(true);
        room1 = quotationRoomRepository.save(room1);

        // Material 1 in Shoe Rack: Price 10000, Offer 9000
        QuotationItem item1 = new QuotationItem();
        item1.setQuotationRoom(room1);
        item1.setWorkCategory(foyerCat);
        item1.setDivision(shoeRackDiv);
        item1.setMaterial(material);
        item1.setMaterialOption(option);
        item1.setLengthValue(new BigDecimal("10.00"));
        item1.setWidthValue(BigDecimal.ZERO);
        item1.setHeightValue(new BigDecimal("10.00"));
        item1.setCalculatedSqft(new BigDecimal("100.00"));
        item1.setRate(new BigDecimal("100.00"));
        item1.setAmount(new BigDecimal("10000.00"));
        item1.setOfferPrice(new BigDecimal("9000.00"));
        item1.setActive(true);
        quotationItemRepository.save(item1);

        // Material 2 in Shoe Rack: Price 20000, Offer 18000
        QuotationItem item2 = new QuotationItem();
        item2.setQuotationRoom(room1);
        item2.setWorkCategory(foyerCat);
        item2.setDivision(shoeRackDiv);
        item2.setMaterial(material);
        item2.setMaterialOption(option);
        item2.setLengthValue(new BigDecimal("20.00"));
        item2.setWidthValue(BigDecimal.ZERO);
        item2.setHeightValue(new BigDecimal("10.00"));
        item2.setCalculatedSqft(new BigDecimal("200.00"));
        item2.setRate(new BigDecimal("100.00"));
        item2.setAmount(new BigDecimal("20000.00"));
        item2.setOfferPrice(new BigDecimal("18000.00"));
        item2.setActive(true);
        quotationItemRepository.save(item2);

        // Material 3 in Shoe Rack: Price 5000, Offer null (Offer Price equals Price = 5000)
        QuotationItem item3 = new QuotationItem();
        item3.setQuotationRoom(room1);
        item3.setWorkCategory(foyerCat);
        item3.setDivision(shoeRackDiv);
        item3.setMaterial(material);
        item3.setMaterialOption(option);
        item3.setLengthValue(new BigDecimal("5.00"));
        item3.setWidthValue(BigDecimal.ZERO);
        item3.setHeightValue(new BigDecimal("10.00"));
        item3.setCalculatedSqft(new BigDecimal("50.00"));
        item3.setRate(new BigDecimal("100.00"));
        item3.setAmount(new BigDecimal("5000.00"));
        item3.setOfferPrice(null);
        item3.setActive(true);
        quotationItemRepository.save(item3);

        // Floor 2: 4th Floor -> False Ceiling -> Work Type: Gypsum Ceiling with 1 material: Price 15000, Offer 12000
        QuotationRoom room2 = new QuotationRoom();
        room2.setQuotation(q);
        room2.setRoom("Living");
        room2.setFloor("4th Floor");
        room2.setActive(true);
        room2 = quotationRoomRepository.save(room2);

        QuotationItem item4 = new QuotationItem();
        item4.setQuotationRoom(room2);
        item4.setWorkCategory(ceilingCat);
        item4.setDivision(ceilingDiv);
        item4.setMaterial(material);
        item4.setMaterialOption(option);
        item4.setLengthValue(new BigDecimal("15.00"));
        item4.setWidthValue(BigDecimal.ZERO);
        item4.setHeightValue(new BigDecimal("10.00"));
        item4.setCalculatedSqft(new BigDecimal("150.00"));
        item4.setRate(new BigDecimal("100.00"));
        item4.setAmount(new BigDecimal("15000.00"));
        item4.setOfferPrice(new BigDecimal("12000.00"));
        item4.setActive(true);
        quotationItemRepository.save(item4);

        // Test Admin Generated PDF
        byte[] adminPdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(adminPdfBytes);
        String adminText = extractPageText(adminPdfBytes, 2);

        // Verify Floor headings
        assertTrue(adminText.contains("Ground Floor"), "Must contain Ground Floor heading");
        assertTrue(adminText.contains("4th Floor"), "Must contain 4th Floor heading");

        // Verify Work Types
        assertTrue(adminText.contains("Shoe Rack"), "Must contain Shoe Rack work type");
        assertTrue(adminText.contains("Gypsum Ceiling"), "Must contain Gypsum Ceiling work type");

        // Verify Total row displays both Price Total (50,000) and Offer Price Total (44,000)
        assertTrue(adminText.contains("50,000.00"), "Must contain Price Total 50,000.00");
        assertTrue(adminText.contains("44,000.00"), "Must contain Offer Price Total / Subtotal 44,000.00");

        // Verify Subtotal, Travel, Other Charges, Total Amount, GST, Grand Total
        assertTrue(adminText.contains("Subtotal"), "Must contain Subtotal row");
        assertTrue(adminText.contains("Travel Charges"), "Must contain Travel Charges row");
        assertTrue(adminText.contains("2,000.00"), "Must contain Travel Charges 2,000.00");
        assertTrue(adminText.contains("Other Charges (Food charges)"), "Must contain Other Charges (Food charges)");
        assertTrue(adminText.contains("1,000.00"), "Must contain Other Charges 1,000.00");
        assertTrue(adminText.contains("Total Amount"), "Must contain Total Amount row");
        assertTrue(adminText.contains("47,000.00"), "Must contain Total Amount 47,000.00");
        assertTrue(adminText.contains("GST (18%)"), "Must contain GST (18%) row");
        assertTrue(adminText.contains("8,460.00"), "Must contain GST amount 8,460.00");
        assertTrue(adminText.toUpperCase().contains("GRAND TOTAL"), "Must contain GRAND TOTAL row");
        assertTrue(adminText.contains("55,460.00"), "Must contain Grand Total 55,460.00");

        // Verify Amount in Words
        assertTrue(adminText.contains("FIFTY FIVE THOUSAND FOUR HUNDRED SIXTY"), "Amount in words must match Grand Total 55,460");

        // Test Employee Generated PDF parity
        q.setEmployee(employeeUser);
        q = quotationRepository.save(q);
        byte[] empPdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(empPdfBytes);
        String empText = extractPageText(empPdfBytes, 2);

        assertTrue(empText.contains("Shoe Rack"));
        assertTrue(empText.contains("Gypsum Ceiling"));
        assertTrue(empText.contains("50,000.00"));
        assertTrue(empText.contains("44,000.00"));
        assertTrue(empText.contains("47,000.00"));
        assertTrue(empText.contains("55,460.00"));
        assertTrue(empText.contains("FIFTY FIVE THOUSAND FOUR HUNDRED SIXTY"));
    }

    @Test
    @DisplayName("Multi-page quotation keeps entire amount summary together on one page and shows table header only once")
    public void testMultiPagePaginationSummaryKeptTogetherAndHeaderOnlyOnce() throws Exception {
        Quotation q = new Quotation();
        q.setQuotationNumber("QT-PAGE-001");
        q.setQuotationDate(LocalDate.now());
        q.setValidUntil(LocalDate.now().plusDays(30));
        q.setCustomer(customer);
        q.setBranch(branch);
        q.setEmployee(adminUser);
        q.setStatus(QuotationStatus.WORKING);
        q.setPricingMode(PricingMode.MIDDLE);
        q.setSelectedGrade(PricingMode.MIDDLE);
        q.setShowSize(true);
        q.setShowSqft(true);
        q.setShowSqftSize(true);
        q.setTravelCharge(new BigDecimal("1500.00"));
        q.setOtherCharges(new BigDecimal("500.00"));
        q.setOtherChargesDescription("Loading & unloading");
        q.setDiscountAmount(BigDecimal.ZERO);
        q.setGstPercentage(new BigDecimal("18.00"));
        q = quotationRepository.save(q);

        WorkCategory foyerCat = workCategoryRepository.findByNameIgnoreCase("Foyer")
                .orElseGet(() -> {
                    WorkCategory c = new WorkCategory();
                    c.setName("Foyer");
                    c.setActive(true);
                    return workCategoryRepository.save(c);
                });

        WorkCategory kitchenCat = workCategoryRepository.findByNameIgnoreCase("Modular Kitchen")
                .orElseGet(() -> {
                    WorkCategory c = new WorkCategory();
                    c.setName("Modular Kitchen");
                    c.setActive(true);
                    return workCategoryRepository.save(c);
                });

        WorkCategory ceilingCat = workCategoryRepository.findByNameIgnoreCase("False Ceiling")
                .orElseGet(() -> {
                    WorkCategory c = new WorkCategory();
                    c.setName("False Ceiling");
                    c.setActive(true);
                    return workCategoryRepository.save(c);
                });

        Division shoeRackDiv = new Division();
        shoeRackDiv.setName("Shoe Rack Unit");
        shoeRackDiv.setActive(true);
        shoeRackDiv.setWorkCategory(foyerCat);
        shoeRackDiv = divisionRepository.save(shoeRackDiv);

        Division baseCabDiv = new Division();
        baseCabDiv.setName("Base Cabinet");
        baseCabDiv.setActive(true);
        baseCabDiv.setWorkCategory(kitchenCat);
        baseCabDiv = divisionRepository.save(baseCabDiv);

        Division wallCabDiv = new Division();
        wallCabDiv.setName("Wall Cabinet");
        wallCabDiv.setActive(true);
        wallCabDiv.setWorkCategory(kitchenCat);
        wallCabDiv = divisionRepository.save(wallCabDiv);

        Division ceilingDiv = new Division();
        ceilingDiv.setName("Gypsum Ceiling");
        ceilingDiv.setActive(true);
        ceilingDiv.setWorkCategory(ceilingCat);
        ceilingDiv = divisionRepository.save(ceilingDiv);

        // Ground Floor - Room 1 (Foyer with 3 items)
        QuotationRoom room1 = new QuotationRoom();
        room1.setQuotation(q);
        room1.setRoom("Foyer Area");
        room1.setFloor("Ground Floor");
        room1.setActive(true);
        room1 = quotationRoomRepository.save(room1);

        for (int i = 1; i <= 3; i++) {
            QuotationItem item = new QuotationItem();
            item.setQuotationRoom(room1);
            item.setWorkCategory(foyerCat);
            item.setDivision(shoeRackDiv);
            item.setMaterial(material);
            item.setMaterialOption(option);
            item.setSpecification("Marine Ply 710 Grade, 16mm Thickness, Sleeka/Hettich hinges specification details " + i);
            item.setLengthValue(new BigDecimal("30.00"));
            item.setWidthValue(new BigDecimal("20.00"));
            item.setHeightValue(new BigDecimal("15.00"));
            item.setCalculatedSqft(new BigDecimal("10.00"));
            item.setRate(new BigDecimal("500.00"));
            item.setAmount(new BigDecimal("5000.00"));
            item.setOfferPrice(new BigDecimal("4500.00"));
            item.setActive(true);
            quotationItemRepository.save(item);
        }

        // Ground Floor - Room 2 (Kitchen with 3 items)
        QuotationRoom room2 = new QuotationRoom();
        room2.setQuotation(q);
        room2.setRoom("Kitchen Area");
        room2.setFloor("Ground Floor");
        room2.setActive(true);
        room2 = quotationRoomRepository.save(room2);

        for (int i = 1; i <= 3; i++) {
            QuotationItem item = new QuotationItem();
            item.setQuotationRoom(room2);
            item.setWorkCategory(kitchenCat);
            item.setDivision(i % 2 == 0 ? wallCabDiv : baseCabDiv);
            item.setMaterial(material);
            item.setMaterialOption(option);
            item.setSpecification("BWP Grade plywood with glossy laminate outer finish and soft close hinges " + i);
            item.setLengthValue(new BigDecimal("40.00"));
            item.setWidthValue(new BigDecimal("25.00"));
            item.setHeightValue(new BigDecimal("30.00"));
            item.setCalculatedSqft(new BigDecimal("15.00"));
            item.setRate(new BigDecimal("800.00"));
            item.setAmount(new BigDecimal("12000.00"));
            item.setOfferPrice(new BigDecimal("11000.00"));
            item.setActive(true);
            quotationItemRepository.save(item);
        }

        // 1st Floor - Room 3 (Living False Ceiling with 3 items)
        QuotationRoom room3 = new QuotationRoom();
        room3.setQuotation(q);
        room3.setRoom("Living Room");
        room3.setFloor("1st Floor");
        room3.setActive(true);
        room3 = quotationRoomRepository.save(room3);

        for (int i = 1; i <= 3; i++) {
            QuotationItem item = new QuotationItem();
            item.setQuotationRoom(room3);
            item.setWorkCategory(ceilingCat);
            item.setDivision(ceilingDiv);
            item.setMaterial(material);
            item.setMaterialOption(option);
            item.setSpecification("Gypsum Board False Ceiling with Saint-Gobain channel framing and LED profile groove " + i);
            item.setLengthValue(new BigDecimal("50.00"));
            item.setWidthValue(new BigDecimal("40.00"));
            item.setHeightValue(new BigDecimal("10.00"));
            item.setCalculatedSqft(new BigDecimal("20.00"));
            item.setRate(new BigDecimal("300.00"));
            item.setAmount(new BigDecimal("6000.00"));
            item.setOfferPrice(new BigDecimal("5500.00"));
            item.setActive(true);
            quotationItemRepository.save(item);
        }

        // Generate Admin PDF
        byte[] adminPdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(adminPdfBytes);

        PdfReader reader = new PdfReader(adminPdfBytes);
        int totalPages = reader.getNumberOfPages();
        // Since there are 9 items with rich specifications across multiple floors and categories,
        // the quotation table spans across 2 pages (Page 2 and Page 3), making total pages >= 8
        assertTrue(totalPages >= 8, "Multi-page quotation should have at least 8 pages (got " + totalPages + ")");

        // Page 2: First page of quotation
        String page2Text = extractPageText(adminPdfBytes, 2);
        assertTrue(page2Text.contains("Quote Summary"), "Page 2 must contain Quote Summary title");
        assertTrue(page2Text.contains("WORK TYPE"), "Page 2 must contain table header WORK TYPE");
        assertTrue(page2Text.contains("MATERIALS"), "Page 2 must contain table header MATERIALS");
        assertTrue(page2Text.contains("Ground Floor"), "Page 2 must contain Ground Floor");

        // Page 3: Continuation page
        String page3Text = extractPageText(adminPdfBytes, 3);
        // Table header row must NOT be repeated on Page 3
        assertFalse(page3Text.contains("WORK TYPE"), "Page 3 must NOT repeat the table header WORK TYPE");

        // Find which page contains the financial summary block
        // The complete summary block (Total, Subtotal, Travel, Other Charges, Total Amount, GST, Grand Total, Amount in Words)
        // must be together on that same page and NEVER split
        String summaryPageText;
        if (page3Text.contains("GRAND TOTAL")) {
            summaryPageText = page3Text;
            // Ensure no partial summary rows leaked onto Page 2
            assertFalse(page2Text.contains("Total Amount"), "Total Amount must not be on Page 2 if summary is on Page 3");
            assertFalse(page2Text.contains("GRAND TOTAL"), "GRAND TOTAL must not be on Page 2 if summary is on Page 3");
        } else {
            summaryPageText = page2Text;
        }

        // Verify the entire amount summary block is together on summaryPageText
        assertTrue(summaryPageText.contains("Total"), "Summary page must contain Total row");
        assertTrue(summaryPageText.contains("Subtotal"), "Summary page must contain Subtotal row");
        assertTrue(summaryPageText.contains("Travel Charges"), "Summary page must contain Travel Charges");
        assertTrue(summaryPageText.contains("Other Charges (Loading & unloading)"), "Summary page must contain Other Charges");
        assertTrue(summaryPageText.contains("Total Amount"), "Summary page must contain Total Amount");
        assertTrue(summaryPageText.contains("GST (18%)"), "Summary page must contain GST");
        assertTrue(summaryPageText.toUpperCase().contains("GRAND TOTAL"), "Summary page must contain GRAND TOTAL");
        assertTrue(summaryPageText.contains("AMOUNT IN WORDS -"), "Summary page must contain AMOUNT IN WORDS immediately below summary");
        assertTrue(summaryPageText.contains("Payment Terms:"), "Summary page must contain Payment Terms");

        // Parity test: Employee PDF
        q.setEmployee(employeeUser);
        q = quotationRepository.save(q);
        byte[] empPdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(empPdfBytes);
        PdfReader empReader = new PdfReader(empPdfBytes);
        assertEquals(totalPages, empReader.getNumberOfPages(), "Admin and Employee PDFs must have identical page count");

        String empPage3Text = extractPageText(empPdfBytes, 3);
        assertFalse(empPage3Text.contains("WORK TYPE"), "Employee PDF Page 3 must NOT repeat the table header WORK TYPE");
    }

    @Test
    @DisplayName("Verify Category and Work Type page break behavior and continuation page Category association")
    public void testCategoryAndWorkTypePageBreaksAndCategoryAssociationOnContinuationPage() throws Exception {
        Quotation q = new Quotation();
        q.setQuotationNumber("QT-PAGEBREAK-01");
        q.setCustomer(customer);
        q.setBranch(branch);
        q.setEmployee(adminUser);
        q.setQuotationDate(LocalDate.now());
        q.setValidUntil(LocalDate.now().plusDays(30));
        q.setStatus(QuotationStatus.WORKING);
        q.setPricingMode(PricingMode.MIDDLE);
        q.setSelectedGrade(PricingMode.MIDDLE);
        q.setShowSize(true);
        q.setShowSqft(true);
        q.setShowSqftSize(true);
        q.setTravelCharge(new BigDecimal("1000.00"));
        q.setOtherCharges(new BigDecimal("500.00"));
        q.setDiscountAmount(BigDecimal.ZERO);
        q.setGstPercentage(new BigDecimal("18.00"));
        q = quotationRepository.save(q);

        WorkCategory kitchenCat = workCategoryRepository.findByNameIgnoreCase("Modular Kitchen")
                .orElseGet(() -> {
                    WorkCategory c = new WorkCategory();
                    c.setName("Modular Kitchen");
                    c.setActive(true);
                    return workCategoryRepository.save(c);
                });

        Division baseCabDiv = new Division();
        baseCabDiv.setName("Base Cabinet");
        baseCabDiv.setActive(true);
        baseCabDiv.setWorkCategory(kitchenCat);
        baseCabDiv = divisionRepository.save(baseCabDiv);

        Division wallCabDiv = new Division();
        wallCabDiv.setName("Wall Cabinet");
        wallCabDiv.setActive(true);
        wallCabDiv.setWorkCategory(kitchenCat);
        wallCabDiv = divisionRepository.save(wallCabDiv);

        Division tallUnitDiv = new Division();
        tallUnitDiv.setName("Tall Unit Pantry");
        tallUnitDiv.setActive(true);
        tallUnitDiv.setWorkCategory(kitchenCat);
        tallUnitDiv = divisionRepository.save(tallUnitDiv);

        QuotationRoom room1 = new QuotationRoom();
        room1.setQuotation(q);
        room1.setRoom("Kitchen Area");
        room1.setFloor("Ground Floor");
        room1.setActive(true);
        room1 = quotationRoomRepository.save(room1);

        // Add 3 items for Base Cabinet
        for (int i = 1; i <= 3; i++) {
            QuotationItem item = new QuotationItem();
            item.setQuotationRoom(room1);
            item.setWorkCategory(kitchenCat);
            item.setDivision(baseCabDiv);
            item.setMaterial(material);
            item.setMaterialOption(option);
            item.setSpecification("Marine Ply 710 Grade, 16mm Thickness, Sleeka/Hettich hinges specification details " + i);
            item.setLengthValue(new BigDecimal("30.00"));
            item.setWidthValue(new BigDecimal("20.00"));
            item.setHeightValue(new BigDecimal("15.00"));
            item.setCalculatedSqft(new BigDecimal("10.00"));
            item.setRate(new BigDecimal("500.00"));
            item.setAmount(new BigDecimal("5000.00"));
            item.setOfferPrice(new BigDecimal("4500.00"));
            item.setActive(true);
            quotationItemRepository.save(item);
        }

        // Add 3 items for Wall Cabinet
        for (int i = 1; i <= 3; i++) {
            QuotationItem item = new QuotationItem();
            item.setQuotationRoom(room1);
            item.setWorkCategory(kitchenCat);
            item.setDivision(wallCabDiv);
            item.setMaterial(material);
            item.setMaterialOption(option);
            item.setSpecification("BWP Grade plywood with glossy laminate outer finish and soft close hinges " + i);
            item.setLengthValue(new BigDecimal("40.00"));
            item.setWidthValue(new BigDecimal("25.00"));
            item.setHeightValue(new BigDecimal("30.00"));
            item.setCalculatedSqft(new BigDecimal("15.00"));
            item.setRate(new BigDecimal("800.00"));
            item.setAmount(new BigDecimal("12000.00"));
            item.setOfferPrice(new BigDecimal("11000.00"));
            item.setActive(true);
            quotationItemRepository.save(item);
        }

        // Add 3 items for Tall Unit Pantry
        for (int i = 1; i <= 3; i++) {
            QuotationItem item = new QuotationItem();
            item.setQuotationRoom(room1);
            item.setWorkCategory(kitchenCat);
            item.setDivision(tallUnitDiv);
            item.setMaterial(material);
            item.setMaterialOption(option);
            item.setSpecification("Tall pantry unit with chrome wire basket accessories and hydraulic lift system " + i);
            item.setLengthValue(new BigDecimal("50.00"));
            item.setWidthValue(new BigDecimal("40.00"));
            item.setHeightValue(new BigDecimal("80.00"));
            item.setCalculatedSqft(new BigDecimal("25.00"));
            item.setRate(new BigDecimal("1200.00"));
            item.setAmount(new BigDecimal("30000.00"));
            item.setOfferPrice(new BigDecimal("28000.00"));
            item.setActive(true);
            quotationItemRepository.save(item);
        }

        // Generate Admin PDF
        byte[] adminPdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(adminPdfBytes);

        PdfReader reader = new PdfReader(adminPdfBytes);
        int totalPages = reader.getNumberOfPages();
        assertTrue(totalPages >= 8, "Expected at least 8 total pages (got " + totalPages + ")");

        // Page 2: First page of quotation summary table
        String page2Text = extractPageText(adminPdfBytes, 2);
        assertTrue(page2Text.contains("Quote Summary"), "Page 2 must contain Quote Summary heading");
        assertTrue(page2Text.contains("WORK TYPE"), "Page 2 must contain WORK TYPE table header");
        assertTrue(page2Text.contains("Ground Floor"), "Page 2 must contain Ground Floor heading");
        assertTrue(page2Text.contains("Modular Kitchen"), "Page 2 must contain Category heading Modular Kitchen");
        assertTrue(page2Text.contains("Base Cabinet"), "Page 2 must contain Work Type Base Cabinet");

        // Page 3: Continuation page
        String page3Text = extractPageText(adminPdfBytes, 3);
        // Table header row must NOT be repeated on Page 3
        assertFalse(page3Text.contains("WORK TYPE"), "Page 3 must NOT repeat the table header WORK TYPE");
        // Category heading must be repeated at the top of Page 3 if Work Types continue onto Page 3
        if (page3Text.contains("Tall Unit Pantry") || page3Text.contains("Wall Cabinet")) {
            assertTrue(page3Text.contains("Modular Kitchen"), "Page 3 must repeat Modular Kitchen category heading for continued work types");
        }

        // Employee parity test
        q.setEmployee(employeeUser);
        q = quotationRepository.save(q);
        byte[] empPdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(empPdfBytes);
        PdfReader empReader = new PdfReader(empPdfBytes);
        assertEquals(totalPages, empReader.getNumberOfPages(), "Admin and Employee PDFs must have identical page count");
    }

    @Test
    @DisplayName("Verify OFFER PRICE column header in Quote Summary table for Admin and Employee PDFs")
    void testOfferPriceColumnHeaderInAdminAndEmployeePdf() throws Exception {
        BigDecimal total = new BigDecimal("45000.00");
        // 1. Admin PDF
        Quotation adminQ = createQuotationWithItems("QT-ADM-OFFER", adminUser, total, BigDecimal.ZERO);
        byte[] adminPdfBytes = quotationPdfService.generateQuotationPdf(adminQ.getId());
        assertNotNull(adminPdfBytes);
        String adminText = extractPageText(adminPdfBytes, 2);
        assertTrue(adminText.contains("OFFER PRICE"), "Admin PDF table header must contain OFFER PRICE in uppercase");

        // 2. Employee PDF
        Quotation empQ = createQuotationWithItems("QT-EMP-OFFER", employeeUser, total, BigDecimal.ZERO);
        byte[] empPdfBytes = quotationPdfService.generateQuotationPdf(empQ.getId());
        assertNotNull(empPdfBytes);
        String empText = extractPageText(empPdfBytes, 2);
        assertTrue(empText.contains("OFFER PRICE"), "Employee PDF table header must contain OFFER PRICE in uppercase");

        // Save sample outputs for visual verification
        java.io.File outDir = new java.io.File("target/test-output");
        outDir.mkdirs();
        java.nio.file.Files.write(new java.io.File(outDir, "Sample_Admin_Quotation.pdf").toPath(), adminPdfBytes);
        java.nio.file.Files.write(new java.io.File(outDir, "Sample_Employee_Quotation.pdf").toPath(), empPdfBytes);
        assertTrue(new java.io.File(outDir, "Sample_Admin_Quotation.pdf").exists());
        assertTrue(new java.io.File(outDir, "Sample_Employee_Quotation.pdf").exists());
    }
}
