package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
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
public class QuotationPdf7PageTest {

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
    private PricingRepository pricingRepository;

    @Autowired
    private UserRepository userRepository;

    private Branch branchCoimbatore;
    private Customer customerAnand;
    private Customer customerBala;
    private WorkCategory kitchenCategory;
    private WorkCategory wardrobeCategory;
    private Material marinePly;
    private MaterialOption option18mm;
    private User adminUser;
    private User employeeUser;

    @BeforeEach
    public void setup() {
        branchCoimbatore = new Branch();
        branchCoimbatore.setName("Coimbatore Prime Branch");
        branchCoimbatore.setBranchAddress("100 Cross Cut Road, Gandhipuram, Coimbatore 641012");
        branchCoimbatore.setWhatsapp("9876543210");
        branchCoimbatore.setEmail("cbe@pravinkitchens.com");
        branchCoimbatore = branchRepository.save(branchCoimbatore);

        customerAnand = new Customer();
        customerAnand.setName("Anand Kumar");
        customerAnand.setPhone("9840123456");
        customerAnand.setEmail("anand@example.com");
        customerAnand.setAddress("42 Palm Meadows, Race Course, Coimbatore");
        customerAnand.setBranch(branchCoimbatore);
        customerAnand = customerRepository.save(customerAnand);

        customerBala = new Customer();
        customerBala.setName("Bala Subramanian");
        customerBala.setPhone("9840654321");
        customerBala.setEmail("bala@example.com");
        customerBala.setAddress("15 Whispering Palms, RS Puram, Coimbatore");
        customerBala.setBranch(branchCoimbatore);
        customerBala = customerRepository.save(customerBala);

        kitchenCategory = workCategoryRepository.findByNameIgnoreCase("Modular Kitchen")
                .orElseGet(() -> {
                    WorkCategory wc = new WorkCategory();
                    wc.setName("Modular Kitchen");
                    return workCategoryRepository.save(wc);
                });

        wardrobeCategory = workCategoryRepository.findByNameIgnoreCase("Master Wardrobe")
                .orElseGet(() -> {
                    WorkCategory wc = new WorkCategory();
                    wc.setName("Master Wardrobe");
                    return workCategoryRepository.save(wc);
                });

        marinePly = materialRepository.findAll().stream()
                .filter(m -> "BWP Marine Ply".equalsIgnoreCase(m.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Material m = new Material();
                    m.setName("BWP Marine Ply");
                    return materialRepository.save(m);
                });

        option18mm = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(marinePly.getId()).stream()
                .findFirst()
                .orElseGet(() -> {
                    MaterialOption mo = new MaterialOption();
                    mo.setMaterial(marinePly);
                    mo.setName("18mm Calibrated");
                    return materialOptionRepository.save(mo);
                });

        if (pricingRepository.findByMaterialOptionIdAndPricingMode(option18mm.getId(), PricingMode.PREMIUM).isEmpty()) {
            Pricing pricing = new Pricing();
            pricing.setMaterialOption(option18mm);
            pricing.setPricingMode(PricingMode.PREMIUM);
            pricing.setRate(new BigDecimal("1200.00"));
            pricing.setUnit("SQFT");
            pricing.setActive(true);
            pricingRepository.save(pricing);
        }

        adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin_" + System.currentTimeMillis() + "@pravin.com");
        adminUser.setPassword("pass");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Employee User");
        employeeUser.setEmail("emp_" + System.currentTimeMillis() + "@pravin.com");
        employeeUser.setPassword("pass");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branchCoimbatore);
        employeeUser = userRepository.save(employeeUser);
    }

    private String extractPageText(byte[] pdfBytes, int pageNum) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        byte[] pageContent = reader.getPageContent(pageNum);
        reader.close();
        return pageContent != null ? new String(pageContent, StandardCharsets.ISO_8859_1) : "";
    }

    @Test
    public void testSevenPagePdfGenerationForAdminQuotation() throws Exception {
        // Create Admin Quotation
        Quotation q = new Quotation();
        q.setQuotationNumber("QT-ADM-7001");
        q.setCustomer(customerAnand);
        q.setBranch(branchCoimbatore);
        q.setPricingMode(PricingMode.PREMIUM);
        q.setQuotationDate(LocalDate.now());
        q.setEmployee(adminUser);
        q.setStatus(QuotationStatus.PENDING);
        q.setSubtotal(new BigDecimal("100000.00"));
        q.setTotalAmount(new BigDecimal("100000.00"));
        q.setOfferPercentage(new BigDecimal("10.00"));
        q.setOfferAmount(new BigDecimal("10000.00"));
        q.setGrandTotal(new BigDecimal("90000.00"));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setRoom("Kitchen Area");
        room.setFloor("Ground Floor");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(kitchenCategory);
        item.setMaterial(marinePly);
        item.setMaterialOption(option18mm);
        item.setLengthValue(new BigDecimal("10.00"));
        item.setWidthValue(BigDecimal.ZERO);
        item.setHeightValue(new BigDecimal("8.00"));
        item.setCalculatedSqft(new BigDecimal("80.00"));
        item.setRate(new BigDecimal("1250.00"));
        item.setAmount(new BigDecimal("100000.00"));
        item.setOfferPrice(new BigDecimal("90000.00"));
        item.setActive(true);
        quotationItemRepository.save(item);

        // Generate PDF
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // Verify Page Count is EXACTLY 7
        PdfReader reader = new PdfReader(pdfBytes);
        assertEquals(7, reader.getNumberOfPages(), "Quotation PDF must have exactly 7 pages");
        reader.close();

        // Verify Page 1 (Cover) contains branding & guidelines text
        String page1 = extractPageText(pdfBytes, 1);
        assertTrue(page1.contains("PREMIUM INTERIOR ESTIMATE & GUIDELINES"), "Page 1 must contain title");
        assertTrue(page1.contains("PRAVIN KITCHENS & INTERIORS"), "Page 1 must contain brand");

        // Verify Page 2 (Dynamic Marketing Quotation) contains live quotation details
        String page2 = extractPageText(pdfBytes, 2);
        assertTrue(page2.contains("MARKETING QUOTATION"), "Page 2 must contain MARKETING QUOTATION header");
        assertTrue(page2.contains("QT-ADM-7001"), "Page 2 must contain quotation number");
        assertTrue(page2.contains("Anand Kumar"), "Page 2 must contain customer name");
        assertTrue(page2.contains("Race Course") || page2.contains("42 Palm Meadows"), "Page 2 must contain customer address");
        assertTrue(page2.contains("MODULAR") || page2.contains("KITCHEN"), "Page 2 must contain work category");
        assertTrue(page2.contains("KITCHEN") && page2.contains("AREA"), "Page 2 must contain room");
        assertTrue(page2.contains("GROUND") && page2.contains("FLOOR"), "Page 2 must contain floor");
        assertTrue(page2.contains("BWP") && page2.contains("MARINE PLY"), "Page 2 must contain material name");
        assertTrue(page2.contains("18MM"), "Page 2 must contain material option");
        assertTrue(page2.contains("SQ.FT"), "Page 2 must contain unit");
        assertTrue(page2.contains("Total Amount"), "Page 2 summary must contain Total Amount");
        assertFalse(page2.contains("Taxable Amount"), "Page 2 summary must not contain Taxable Amount");
        assertFalse(page2.contains("Accessories"), "Page 2 summary must not contain Accessories");
        assertFalse(page2.contains("Travel Charge"), "Page 2 summary must not contain Travel Charge");
        assertFalse(page2.contains("Other Charges"), "Page 2 summary must not contain Other Charges");
        assertFalse(page2.contains("Discount"), "Page 2 summary must not contain Discount");
        assertTrue(page2.contains("Offer Price"), "Page 2 must contain Offer Price");
        assertFalse(page2.contains("10.00%"), "Page 2 summary must not contain offer percentage");
        assertTrue(page2.contains("GRAND TOTAL"), "Page 2 must contain GRAND TOTAL");
        assertTrue(page2.contains("AMOUNT IN WORDS"), "Page 2 must contain AMOUNT IN WORDS");
        assertTrue(page2.contains("NINETY THOUSAND"), "Page 2 must contain amount in words text");
        assertTrue(page2.contains("Payment Terms:"), "Page 2 must contain payment terms");
        assertTrue(page2.contains("10% Token:"), "Page 2 must contain 10% Token");
        assertTrue(page2.contains("50% Advance:"), "Page 2 must contain 50% Advance");
        assertTrue(page2.contains("35% Part Payment:"), "Page 2 must contain 35% Part Payment");
        assertTrue(page2.contains("5% Final Payment:"), "Page 2 must contain 5% Final Payment");
        assertTrue(page2.contains("Customer Signature"), "Page 2 must contain customer signature");
        assertTrue(page2.contains("Page 2 of 7"), "Page 2 must contain footer page numbering");

        // Verify Page 3 contains Payment Terms & Why Choose Us
        String page3 = extractPageText(pdfBytes, 3);
        assertTrue(page3.contains("Payment Terms"), "Page 3 must contain Payment Terms");
        assertTrue(page3.contains("Why Choose Us"), "Page 3 must contain Why Choose Us");

        // Verify Page 4 contains Estimate Includes/Excludes & Warranty
        String page4 = extractPageText(pdfBytes, 4);
        assertTrue(page4.contains("Estimate Includes"), "Page 4 must contain Estimate Includes");
        assertTrue(page4.contains("Quality Policy"), "Page 4 must contain Quality Policy");
        assertTrue(page4.contains("Warranty"), "Page 4 must contain Warranty");

        // Verify Page 5 contains Care Instructions
        String page5 = extractPageText(pdfBytes, 5);
        assertTrue(page5.contains("Care Instructions"), "Page 5 must contain Care Instructions");

        // Verify Page 6 contains Unexpected Delays & Handover
        String page6 = extractPageText(pdfBytes, 6);
        assertTrue(page6.contains("Unexpected Delays"), "Page 6 must contain Unexpected Delays");
        assertTrue(page6.contains("Handover & Final Inspection"), "Page 6 must contain Handover");

        // Verify Page 7 contains Instructions & Guidelines
        String page7 = extractPageText(pdfBytes, 7);
        assertTrue(page7.contains("Instructions & Guidelines"), "Page 7 must contain Instructions & Guidelines");
        assertTrue(page7.contains("3D Designs"), "Page 7 must contain 3D Designs");
    }

    @Test
    public void testTwoDifferentQuotationsHaveDifferentPageTwoContent() throws Exception {
        // Quotation 1: Anand Kumar
        Quotation q1 = new Quotation();
        q1.setQuotationNumber("QT-CUST-1001");
        q1.setCustomer(customerAnand);
        q1.setBranch(branchCoimbatore);
        q1.setEmployee(adminUser);
        q1.setPricingMode(PricingMode.PREMIUM);
        q1.setQuotationDate(LocalDate.now());
        q1.setStatus(QuotationStatus.PENDING);
        q1.setGrandTotal(new BigDecimal("50000.00"));
        q1 = quotationRepository.save(q1);

        QuotationRoom room1 = new QuotationRoom();
        room1.setQuotation(q1);
        room1.setRoom("Living Room");
        room1.setFloor("1st Floor");
        room1.setActive(true);
        room1 = quotationRoomRepository.save(room1);

        QuotationItem item1 = new QuotationItem();
        item1.setQuotationRoom(room1);
        item1.setWorkCategory(kitchenCategory);
        item1.setAmount(new BigDecimal("50000.00"));
        item1.setActive(true);
        quotationItemRepository.save(item1);

        // Quotation 2: Bala Subramanian
        Quotation q2 = new Quotation();
        q2.setQuotationNumber("QT-CUST-2002");
        q2.setCustomer(customerBala);
        q2.setBranch(branchCoimbatore);
        q2.setEmployee(adminUser);
        q2.setPricingMode(PricingMode.ECONOMY);
        q2.setQuotationDate(LocalDate.now());
        q2.setStatus(QuotationStatus.PENDING);
        q2.setGrandTotal(new BigDecimal("75000.00"));
        q2 = quotationRepository.save(q2);

        QuotationRoom room2 = new QuotationRoom();
        room2.setQuotation(q2);
        room2.setRoom("Master Bedroom");
        room2.setFloor("2nd Floor");
        room2.setActive(true);
        room2 = quotationRoomRepository.save(room2);

        QuotationItem item2 = new QuotationItem();
        item2.setQuotationRoom(room2);
        item2.setWorkCategory(wardrobeCategory);
        item2.setAmount(new BigDecimal("75000.00"));
        item2.setActive(true);
        quotationItemRepository.save(item2);

        // Generate both PDFs
        byte[] pdf1 = quotationPdfService.generateQuotationPdf(q1.getId());
        byte[] pdf2 = quotationPdfService.generateQuotationPdf(q2.getId());

        String page2_1 = extractPageText(pdf1, 2);
        String page2_2 = extractPageText(pdf2, 2);

        // Verify PDF 1 has Anand and Living Room, but NOT Bala or Master Bedroom
        assertTrue(page2_1.contains("Anand Kumar"));
        assertTrue(page2_1.contains("QT-CUST-1001"));
        assertTrue(page2_1.contains("LIVING") && page2_1.contains("ROOM"));
        assertFalse(page2_1.contains("Bala Subramanian"));
        assertFalse(page2_1.contains("QT-CUST-2002"));
        assertFalse(page2_1.contains("MASTER"));

        // Verify PDF 2 has Bala and Master Bedroom, but NOT Anand or Living Room
        assertTrue(page2_2.contains("Bala Subramanian"));
        assertTrue(page2_2.contains("QT-CUST-2002"));
        assertTrue(page2_2.contains("MASTER") && page2_2.contains("BEDROOM"));
        assertFalse(page2_2.contains("Anand Kumar"));
        assertFalse(page2_2.contains("QT-CUST-1001"));
        assertFalse(page2_2.contains("LIVING"));
    }

    @Test
    public void testEmployeeQuotationGeneratesSevenPagePdf() throws Exception {
        Quotation qEmp = new Quotation();
        qEmp.setQuotationNumber("QT-EMP-9001");
        qEmp.setCustomer(customerBala);
        qEmp.setBranch(branchCoimbatore);
        qEmp.setEmployee(employeeUser);
        qEmp.setPricingMode(PricingMode.MIDDLE);
        qEmp.setQuotationDate(LocalDate.now());
        qEmp.setStatus(QuotationStatus.SENT);
        qEmp.setSubtotal(new BigDecimal("120000.00"));
        qEmp.setTotalAmount(new BigDecimal("120000.00"));
        qEmp.setOfferPercentage(new BigDecimal("5.00"));
        qEmp.setOfferAmount(new BigDecimal("6000.00"));
        qEmp.setGrandTotal(new BigDecimal("114000.00"));
        qEmp = quotationRepository.save(qEmp);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(qEmp);
        room.setRoom("Dining Area");
        room.setFloor("Ground Floor");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(kitchenCategory);
        item.setAmount(new BigDecimal("120000.00"));
        item.setActive(true);
        quotationItemRepository.save(item);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(qEmp.getId());
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        assertEquals(7, reader.getNumberOfPages(), "Employee quotation must generate exactly 7 pages");
        reader.close();

        String page2 = extractPageText(pdfBytes, 2);
        assertTrue(page2.contains("QT-EMP-9001"));
        assertTrue(page2.contains("Bala Subramanian"));
        assertTrue(page2.contains("Middle / Standard"));
        assertTrue(page2.contains("Total Amount"));
        assertTrue(page2.contains("Offer Price"));
        assertFalse(page2.contains("5.00%"));
        assertTrue(page2.contains("ONE LAKH FOURTEEN THOUSAND"));
    }
}
