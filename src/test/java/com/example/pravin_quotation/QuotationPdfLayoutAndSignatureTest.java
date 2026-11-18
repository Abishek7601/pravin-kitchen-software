package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.NumberToWordsService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationPdfLayoutAndSignatureTest {

    private QuotationPdfService quotationPdfService;

    private Map<Long, Quotation> quotationStore;
    private Map<Long, Branch> branchStore;
    private Map<Long, List<QuotationRoom>> roomStoreByQuotation;
    private Map<Long, List<QuotationItem>> itemStoreByRoom;

    @BeforeEach
    void setUp() {
        quotationStore = new HashMap<>();
        branchStore = new HashMap<>();
        roomStoreByQuotation = new HashMap<>();
        itemStoreByRoom = new HashMap<>();

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Azhagiamandapam Branch");
        branch.setBranchAddress("Azhagiamandapam, Kanyakumari Dist - 629167");
        branch.setWhatsapp("9488081763");
        branch.setEmail("kk@gmail.com");
        branchStore.put(1L, branch);

        QuotationRepository quotationRepository = createMockRepository(QuotationRepository.class, quotationStore);
        BranchRepository branchRepository = createMockRepository(BranchRepository.class, branchStore);

        QuotationRoomRepository quotationRoomRepository = (QuotationRoomRepository) Proxy.newProxyInstance(
                QuotationRoomRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRoomRepository.class},
                (proxy, method, args) -> {
                    if ("findByQuotationId".equals(method.getName()) && args.length == 1) {
                        Long qId = (Long) args[0];
                        return roomStoreByQuotation.getOrDefault(qId, Collections.emptyList());
                    }
                    return null;
                }
        );

        QuotationItemRepository quotationItemRepository = (QuotationItemRepository) Proxy.newProxyInstance(
                QuotationItemRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemRepository.class},
                (proxy, method, args) -> {
                    if ("findByQuotationRoomId".equals(method.getName()) && args.length == 1) {
                        Long rId = (Long) args[0];
                        return itemStoreByRoom.getOrDefault(rId, Collections.emptyList());
                    }
                    return null;
                }
        );

        PricingRepository pricingRepository = (PricingRepository) Proxy.newProxyInstance(
                PricingRepository.class.getClassLoader(),
                new Class<?>[]{PricingRepository.class},
                (proxy, method, args) -> Optional.empty()
        );

        NumberToWordsService numberToWordsService = new NumberToWordsService();

        quotationPdfService = new QuotationPdfService(
                quotationRepository,
                quotationRoomRepository,
                quotationItemRepository,
                numberToWordsService,
                branchRepository,
                pricingRepository
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T createMockRepository(Class<T> repoInterface, Map<Long, ?> entityStore) {
        return (T) Proxy.newProxyInstance(
                repoInterface.getClassLoader(),
                new Class<?>[]{repoInterface},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName()) && args.length == 1) {
                        Long id = (Long) args[0];
                        Object val = entityStore.get(id);
                        return Optional.ofNullable(val);
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("PDF with 1 material in Economy Grade matches screenshot layout: Company Name, Quotation Title, Meta Details, Table, Payment Terms, and Signatures at Bottom")
    void testSingleMaterialEconomyGradeEntirePageLayout() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("Abishek S R");
        customer.setAddress("Arumanai");
        customer.setPhone("9876543210");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(100L);
        quotation.setQuotationNumber("QT-00053");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setValidUntil(LocalDate.of(2027, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.ECONOMY);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("7350.00"));
        quotation.setTotalAmount(new BigDecimal("7350.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("7350.00"));

        quotationStore.put(100L, quotation);

        WorkCategory modularKitchen = new WorkCategory();
        modularKitchen.setName("MODULAR KITCHEN");

        QuotationRoom room = new QuotationRoom();
        room.setId(200L);
        room.setQuotation(quotation);
        room.setRoom("1");
        room.setFloor("GROUND FLOOR");
        room.setActive(true);
        roomStoreByQuotation.put(100L, List.of(room));

        Material mat = new Material();
        mat.setName("ENGINEERING WOOD BOX TYPE");
        mat.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item = new QuotationItem();
        item.setId(300L);
        item.setQuotationRoom(room);
        item.setWorkCategory(modularKitchen);
        item.setMaterial(mat);
        item.setSpecification("Bye");
        item.setLengthValue(new BigDecimal("23.00"));
        item.setWidthValue(new BigDecimal("23.00"));
        item.setHeightValue(new BigDecimal("23.00"));
        item.setCalculatedSqft(new BigDecimal("7.35"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setRate(new BigDecimal("1000.00"));
        item.setAmount(new BigDecimal("7350.00"));
        item.setOfferPrice(new BigDecimal("7350.00"));
        item.setActive(true);
        itemStoreByRoom.put(200L, List.of(item));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(100L);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 10000, "PDF should have substantial size with 7 pages");

        PdfReader reader = new PdfReader(pdfBytes);
        assertEquals(7, reader.getNumberOfPages(), "Quotation PDF must have exactly 7 pages");

        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);
        assertNotNull(page2Text);

        // 1. Company name & Quotation title
        assertTrue(page2Text.contains("PRAVIN KITCHENS") && page2Text.contains("INTERIORS"), "Must contain company name");
        assertTrue(page2Text.contains("ECONOMY GRADE") && page2Text.contains("QUOTATION"), "Must contain quotation title");

        // 2. Company contact details
        assertTrue(page2Text.contains("Azhagiamandapam"), "Must contain branch address");
        assertTrue(page2Text.contains("9488081763"), "Must contain mobile number");
        assertTrue(page2Text.contains("kk@gmail.com"), "Must contain email");
        assertTrue(page2Text.contains("www.pravinkitchen.com"), "Must contain web URL");

        // 3. Quotation metadata details
        assertTrue(page2Text.contains("Quotation No:"), "Must contain Quotation No label");
        assertTrue(page2Text.contains("QT-00053"), "Must contain quotation number");
        assertTrue(page2Text.contains("Customer Name:"), "Must contain Customer Name label");
        assertTrue(page2Text.contains("Abishek S R"), "Must contain customer name");
        assertTrue(page2Text.contains("Customer Address:"), "Must contain Customer Address label");
        assertTrue(page2Text.contains("Arumanai"), "Must contain customer address");

        // 4. Quote Summary heading & table row content
        assertTrue(page2Text.contains("Quote Summary"), "Page 2 must contain 'Quote Summary'");
        assertTrue(page2Text.contains("WORK"), "Page 2 must contain table header WORK");
        assertTrue(page2Text.contains("CATEGORY"), "Page 2 must contain table header CATEGORY");
        assertTrue(page2Text.contains("TYPE"), "Page 2 must contain table header TYPE for WORK TYPE");
        assertTrue(page2Text.contains("ROOM"), "Page 2 must contain table header ROOM");
        assertTrue(page2Text.contains("FLOOR"), "Page 2 must contain table header FLOOR");
        assertTrue(page2Text.contains("MATERIAL"), "Page 2 must contain table header MATERIAL");
        assertTrue(page2Text.contains("MODULAR"), "Page 2 must contain category");
        assertTrue(page2Text.contains("ENGINEERING WOOD"), "Page 2 must contain material name");
        assertTrue(page2Text.contains("Bye"), "Page 2 must contain specification");
        assertTrue(page2Text.contains("7,350.00"), "Page 2 must contain item total amount");

        // 5. Totals
        assertTrue(page2Text.contains("Economy Grade Subtotal"), "Must contain Economy Grade Subtotal");
        assertTrue(page2Text.contains("Total Amount"), "Must contain Total Amount");
        assertTrue(page2Text.contains("Offer Price"), "Must contain Offer Price");
        assertTrue(page2Text.contains("GRAND TOTAL"), "Must contain GRAND TOTAL");

        // 6. Amount in words
        assertTrue(page2Text.contains("AMOUNT IN WORDS -"), "Must contain Amount in Words prefix");
        assertTrue(page2Text.contains("RUPEES SEVEN THOUSAND THREE HUNDRED FIFTY ONLY"), "Must contain amount in words text");

        // 7. Payment terms
        assertTrue(page2Text.contains("Payment Terms:"), "Must contain Payment Terms heading");
        assertTrue(page2Text.contains("10% Token:"), "Must contain 10% Token");
        assertTrue(page2Text.contains("50% Advance:"), "Must contain 50% Advance");
        assertTrue(page2Text.contains("35% Part Payment:"), "Must contain 35% Part Payment");
        assertTrue(page2Text.contains("5% Final Payment:"), "Must contain 5% Final Payment");

        // 8. Signatures at bottom
        assertTrue(page2Text.contains("Customer Signature"), "Page 2 must contain 'Customer Signature'");
        assertTrue(page2Text.contains("For Pravin KITCHENS & INTERIORS"), "Page 2 must contain 'For Pravin KITCHENS & INTERIORS'");

        // 9. Footer
        assertTrue(page2Text.contains("Page 2 of 7"), "Page 2 must contain footer 'Page 2 of 7'");

        reader.close();
    }

    @Test
    @DisplayName("PDF with 2 materials in Premium Grade generates 7 pages with complete layout hierarchy")
    void testTwoMaterialsPremiumGradePdfLayout() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setAddress("Coimbatore");
        customer.setPhone("9876500000");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(101L);
        quotation.setQuotationNumber("QT-00054");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("13304.00"));
        quotation.setTotalAmount(new BigDecimal("13304.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("13304.00"));

        quotationStore.put(101L, quotation);

        WorkCategory kitchenCat = new WorkCategory();
        kitchenCat.setName("MODULAR KITCHEN");

        QuotationRoom room1 = new QuotationRoom();
        room1.setId(201L);
        room1.setQuotation(quotation);
        room1.setRoom("1");
        room1.setFloor("GROUND FLOOR");
        room1.setActive(true);
        roomStoreByQuotation.put(101L, List.of(room1));

        Material mat1 = new Material();
        mat1.setName("MARINE PLY LAMINATION BOX TYPE Premium");
        mat1.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item1 = new QuotationItem();
        item1.setId(301L);
        item1.setQuotationRoom(room1);
        item1.setWorkCategory(kitchenCat);
        item1.setMaterial(mat1);
        item1.setSpecification("Hai Hello Bye");
        item1.setLengthValue(new BigDecimal("44.00"));
        item1.setWidthValue(new BigDecimal("22.00"));
        item1.setHeightValue(new BigDecimal("12.00"));
        item1.setCalculatedSqft(new BigDecimal("5.50"));
        item1.setMeasurementType(MeasurementType.SQFT);
        item1.setRate(new BigDecimal("1400.00"));
        item1.setAmount(new BigDecimal("7700.00"));
        item1.setOfferPrice(new BigDecimal("7700.00"));
        item1.setActive(true);

        Material mat2 = new Material();
        mat2.setName("MARINE PLY LAMINATION PARTITION TYPE Premium");
        mat2.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item2 = new QuotationItem();
        item2.setId(302L);
        item2.setQuotationRoom(room1);
        item2.setWorkCategory(kitchenCat);
        item2.setMaterial(mat2);
        item2.setSpecification("16 Mm Thickness");
        item2.setLengthValue(new BigDecimal("34.00"));
        item2.setWidthValue(new BigDecimal("22.00"));
        item2.setHeightValue(new BigDecimal("12.00"));
        item2.setCalculatedSqft(new BigDecimal("4.67"));
        item2.setMeasurementType(MeasurementType.SQFT);
        item2.setRate(new BigDecimal("1200.00"));
        item2.setAmount(new BigDecimal("5604.00"));
        item2.setOfferPrice(new BigDecimal("5604.00"));
        item2.setActive(true);

        itemStoreByRoom.put(201L, List.of(item1, item2));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(101L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        assertEquals(7, reader.getNumberOfPages());

        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        assertTrue(page2Text.contains("PREMIUM GRADE") && page2Text.contains("QUOTATION"));
        assertTrue(page2Text.contains("MARINE PLY"));
        assertTrue(page2Text.contains("Premium Grade Subtotal"));
        assertTrue(page2Text.contains("Customer Signature"));
        assertTrue(page2Text.contains("For Pravin KITCHENS & INTERIORS"));

        reader.close();
    }

    @Test
    @DisplayName("PDF with 5 materials in Middle Grade generates cleanly without overlapping or extra pages")
    void testMultipleMaterialsMiddleGradeLayout() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("Sarah Connor");
        customer.setAddress("Madurai");
        customer.setPhone("9844444444");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(102L);
        quotation.setQuotationNumber("QT-00055");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("35000.00"));
        quotation.setTotalAmount(new BigDecimal("35000.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("35000.00"));

        quotationStore.put(102L, quotation);

        WorkCategory cat1 = new WorkCategory();
        cat1.setName("MODULAR KITCHEN");
        WorkCategory cat2 = new WorkCategory();
        cat2.setName("BEDROOM");

        QuotationRoom room1 = new QuotationRoom();
        room1.setId(202L);
        room1.setQuotation(quotation);
        room1.setRoom("1");
        room1.setFloor("GROUND FLOOR");
        room1.setActive(true);

        QuotationRoom room2 = new QuotationRoom();
        room2.setId(203L);
        room2.setQuotation(quotation);
        room2.setRoom("2");
        room2.setFloor("FIRST FLOOR");
        room2.setActive(true);

        roomStoreByQuotation.put(102L, List.of(room1, room2));

        List<QuotationItem> items1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Material m = new Material();
            m.setName("MDF BOARD Middle " + i);
            m.setMeasurementType(MeasurementType.SQFT);

            QuotationItem it = new QuotationItem();
            it.setId(310L + i);
            it.setQuotationRoom(room1);
            it.setWorkCategory(cat1);
            it.setMaterial(m);
            it.setSpecification("High Gloss Exterior Spec " + i);
            it.setLengthValue(new BigDecimal("10.00"));
            it.setWidthValue(new BigDecimal("10.00"));
            it.setHeightValue(new BigDecimal("10.00"));
            it.setCalculatedSqft(new BigDecimal("10.00"));
            it.setMeasurementType(MeasurementType.SQFT);
            it.setRate(new BigDecimal("700.00"));
            it.setAmount(new BigDecimal("7000.00"));
            it.setOfferPrice(new BigDecimal("7000.00"));
            it.setActive(true);
            items1.add(it);
        }
        itemStoreByRoom.put(202L, items1);

        List<QuotationItem> items2 = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            Material m = new Material();
            m.setName("WARDROBE SLIDING Middle " + i);
            m.setMeasurementType(MeasurementType.SQFT);

            QuotationItem it = new QuotationItem();
            it.setId(320L + i);
            it.setQuotationRoom(room2);
            it.setWorkCategory(cat2);
            it.setMaterial(m);
            it.setSpecification("Soft Closing Hardware " + i);
            it.setLengthValue(new BigDecimal("8.00"));
            it.setWidthValue(new BigDecimal("8.00"));
            it.setHeightValue(new BigDecimal("8.00"));
            it.setCalculatedSqft(new BigDecimal("10.00"));
            it.setMeasurementType(MeasurementType.SQFT);
            it.setRate(new BigDecimal("700.00"));
            it.setAmount(new BigDecimal("7000.00"));
            it.setOfferPrice(new BigDecimal("7000.00"));
            it.setActive(true);
            items2.add(it);
        }
        itemStoreByRoom.put(203L, items2);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(102L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        assertEquals(7, reader.getNumberOfPages(), "Even with 5 items, PDF should stay at 7 pages");

        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        assertTrue(page2Text.contains("MIDDLE GRADE QUOTATION"));
        assertTrue(page2Text.contains("Middle Grade Subtotal"));
        assertTrue(page2Text.contains("Customer Signature"));
        assertTrue(page2Text.contains("For Pravin KITCHENS & INTERIORS"));

        reader.close();
    }

    @Test
    @DisplayName("Verify Grand Total ₹ 10,290.00 does not wrap or split currency and digits across multiple lines")
    void testGrandTotalTenThousandTwoNinetySingleLine() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setAddress("Nagercoil");
        customer.setPhone("9876543210");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(103L);
        quotation.setQuotationNumber("QT-00056");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.ECONOMY);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("10290.00"));
        quotation.setTotalAmount(new BigDecimal("10290.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("10290.00"));

        quotationStore.put(103L, quotation);

        WorkCategory cat = new WorkCategory();
        cat.setName("MODULAR KITCHEN");

        QuotationRoom room = new QuotationRoom();
        room.setId(204L);
        room.setQuotation(quotation);
        room.setRoom("1");
        room.setFloor("GROUND FLOOR");
        room.setActive(true);
        roomStoreByQuotation.put(103L, List.of(room));

        Material mat = new Material();
        mat.setName("TEST MATERIAL");
        mat.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item = new QuotationItem();
        item.setId(330L);
        item.setQuotationRoom(room);
        item.setWorkCategory(cat);
        item.setMaterial(mat);
        item.setSpecification("Test Spec");
        item.setLengthValue(new BigDecimal("10.00"));
        item.setWidthValue(new BigDecimal("10.00"));
        item.setHeightValue(new BigDecimal("10.00"));
        item.setCalculatedSqft(new BigDecimal("10.29"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setRate(new BigDecimal("1000.00"));
        item.setAmount(new BigDecimal("10290.00"));
        item.setOfferPrice(new BigDecimal("10290.00"));
        item.setActive(true);
        itemStoreByRoom.put(204L, List.of(item));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(103L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        // Verify the entire amount appears intact on a single line
        assertTrue(page2Text.contains("10,290.00"), "Page 2 must contain complete amount '10,290.00' on a single line");
        // Verify it didn't split into '10,290.0' and '0' on next line
        assertFalse(page2Text.contains("10,290.0\n0"), "Amount must NOT split digits across newlines");
        assertFalse(page2Text.contains("10,290.0\r\n0"), "Amount must NOT split digits across newlines");

        reader.close();
    }

    @Test
    @DisplayName("Verify Large Grand Total ₹ 1,59,530.63 and Multi-digit amounts remain on a single line without wrapping")
    void testLargeAmountSingleLineNoWrap() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("Corporate Client");
        customer.setAddress("Chennai");
        customer.setPhone("9876543210");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(104L);
        quotation.setQuotationNumber("QT-00057");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("159530.63"));
        quotation.setTotalAmount(new BigDecimal("159530.63"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("159530.63"));

        quotationStore.put(104L, quotation);

        WorkCategory cat = new WorkCategory();
        cat.setName("MODULAR KITCHEN");

        QuotationRoom room = new QuotationRoom();
        room.setId(205L);
        room.setQuotation(quotation);
        room.setRoom("1");
        room.setFloor("GROUND FLOOR");
        room.setActive(true);
        roomStoreByQuotation.put(104L, List.of(room));

        Material mat = new Material();
        mat.setName("PREMIUM ACRYLIC");
        mat.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item = new QuotationItem();
        item.setId(340L);
        item.setQuotationRoom(room);
        item.setWorkCategory(cat);
        item.setMaterial(mat);
        item.setSpecification("High Gloss German Acrylic");
        item.setLengthValue(new BigDecimal("50.00"));
        item.setWidthValue(new BigDecimal("20.00"));
        item.setHeightValue(new BigDecimal("10.00"));
        item.setCalculatedSqft(new BigDecimal("100.00"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setRate(new BigDecimal("1595.31"));
        item.setAmount(new BigDecimal("159530.63"));
        item.setOfferPrice(new BigDecimal("159530.63"));
        item.setActive(true);
        itemStoreByRoom.put(205L, List.of(item));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(104L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        assertTrue(page2Text.contains("1,59,530.63"), "Page 2 must contain large amount '1,59,530.63' intact on a single line");
        assertFalse(page2Text.contains("1,59,530.6\n3"), "Must not wrap last digit");

        reader.close();
    }

    @Test
    @DisplayName("Verify Quote Summary table columns: ROOM, FLOOR, SQ.FT, UNIT headers on one line, GROUND FLOOR does not break GROUN-D, and SQ.FT does not break SQ-FT")
    void testQuoteSummaryTableColumnsAlignmentAndNoWordSplit() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("Abishek S R");
        customer.setAddress("Arumanai");
        customer.setPhone("9488081763");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(105L);
        quotation.setQuotationNumber("QT-00058");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("10290.00"));
        quotation.setTotalAmount(new BigDecimal("10290.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("10290.00"));

        quotationStore.put(105L, quotation);

        WorkCategory cat = new WorkCategory();
        cat.setName("MODULAR KITCHEN");

        QuotationRoom room = new QuotationRoom();
        room.setId(206L);
        room.setQuotation(quotation);
        room.setRoom("1");
        room.setFloor("GROUND FLOOR");
        room.setActive(true);
        roomStoreByQuotation.put(105L, List.of(room));

        Material mat = new Material();
        mat.setName("MARINE PLY LAMINATION BOX TYPE");
        mat.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item = new QuotationItem();
        item.setId(350L);
        item.setQuotationRoom(room);
        item.setWorkCategory(cat);
        item.setMaterial(mat);
        item.setSpecification("Hai Hello Bye");
        item.setLengthValue(new BigDecimal("23.00"));
        item.setWidthValue(new BigDecimal("23.00"));
        item.setHeightValue(new BigDecimal("23.00"));
        item.setCalculatedSqft(new BigDecimal("7.35"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setRate(new BigDecimal("1400.00"));
        item.setAmount(new BigDecimal("10290.00"));
        item.setOfferPrice(new BigDecimal("10290.00"));
        item.setActive(true);
        itemStoreByRoom.put(206L, List.of(item));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(105L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        // 1. Column headers must be intact on a single line
        assertTrue(page2Text.contains("ROOM"), "Header ROOM must be intact");
        assertTrue(page2Text.contains("FLOOR"), "Header FLOOR must be intact");
        assertTrue(page2Text.contains("SQ.FT"), "Header SQ.FT must be intact");
        assertTrue(page2Text.contains("UNIT"), "Header UNIT must be intact");
        assertTrue(page2Text.contains("TOTAL AMOUNT"), "Header TOTAL AMOUNT must be intact");

        // Headers must not break into multiple lines or split words
        assertFalse(page2Text.contains("ROO\nM") || page2Text.contains("ROO M"), "Header ROOM must not split into ROO-M");
        assertFalse(page2Text.contains("SQ.F\nT") || page2Text.contains("SQ.F T"), "Header SQ.FT must not split into SQ.F-T");
        assertFalse(page2Text.contains("UNI\nT") || page2Text.contains("UNI T"), "Header UNIT must not split into UNI-T");

        // 2. Row values must be intact and not split awkwardly
        assertTrue(page2Text.contains("GROUND"), "Floor GROUND must be intact");
        assertFalse(page2Text.contains("GROUN\nD") || page2Text.contains("GROUN D"), "FLOOR value must not split into GROUN-D");
        assertFalse(page2Text.contains("SQ.\nFT") || page2Text.contains("SQ. FT"), "UNIT value SQ.FT must not split into SQ.-FT");

        // 3. Amount must be intact on a single line
        assertTrue(page2Text.contains("10,290.00"), "Amount 10,290.00 must be on a single line");

        reader.close();
    }

    @Test
    @DisplayName("PDF Quote Summary table displays WORK TYPE column with actual saved Work Type")
    void testWorkTypeColumnDisplayedInQuoteSummaryTable() throws Exception {
        Branch branch = branchStore.get(1L);

        Customer customer = new Customer();
        customer.setName("Abishek S R");
        customer.setAddress("Arumanai");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(109L);
        quotation.setQuotationNumber("QT-00059");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 18));
        quotation.setValidUntil(LocalDate.of(2027, 11, 18));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("28760.00"));
        quotation.setTotalAmount(new BigDecimal("28760.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("28760.00"));
        quotationStore.put(109L, quotation);

        WorkCategory category = new WorkCategory();
        category.setName("FOYER");

        Division baseCabinet = new Division();
        baseCabinet.setName("BASE CABINET");
        baseCabinet.setWorkCategory(category);

        QuotationRoom room = new QuotationRoom();
        room.setId(209L);
        room.setQuotation(quotation);
        room.setRoom("1");
        room.setFloor("GROUND FLOOR");
        room.setActive(true);
        roomStoreByQuotation.put(109L, List.of(room));

        Material mat = new Material();
        mat.setName("MARINE PLY LAMINATION BOX TYPE");
        mat.setMeasurementType(MeasurementType.SQFT);
        mat.setDivision(baseCabinet);

        QuotationItem item = new QuotationItem();
        item.setId(309L);
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setDivision(baseCabinet);
        item.setMaterial(mat);
        item.setSpecification("Marine Ply 710 Grade, 16mm Thickness");
        item.setLengthValue(new BigDecimal("45.00"));
        item.setWidthValue(new BigDecimal("45.00"));
        item.setHeightValue(new BigDecimal("23.00"));
        item.setCalculatedSqft(new BigDecimal("14.38"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setRate(new BigDecimal("2000.00"));
        item.setAmount(new BigDecimal("28760.00"));
        item.setOfferPrice(new BigDecimal("28760.00"));
        item.setActive(true);
        itemStoreByRoom.put(209L, List.of(item));

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(109L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);
        reader.close();

        assertNotNull(page2Text);
        String normalized = page2Text.replaceAll("\\s+", " ");

        // 1. Headers: WORK CATEGORY, WORK TYPE, ROOM, FLOOR, MATERIAL, SIZE, SQ.FT, UNIT, TOTAL AMOUNT
        assertTrue(normalized.contains("WORK CATEGORY"), "Table must contain WORK CATEGORY header");
        assertTrue(normalized.contains("WORK TYPE"), "Table must contain WORK TYPE header");
        assertTrue(normalized.contains("ROOM"), "Table must contain ROOM header");
        assertTrue(normalized.contains("FLOOR"), "Table must contain FLOOR header");
        assertTrue(normalized.contains("MATERIAL"), "Table must contain MATERIAL header");
        assertTrue(normalized.contains("SIZE"), "Table must contain SIZE header");
        assertTrue(normalized.contains("SQ.FT"), "Table must contain SQ.FT header");
        assertTrue(normalized.contains("UNIT"), "Table must contain UNIT header");
        assertTrue(normalized.contains("TOTAL AMOUNT"), "Table must contain TOTAL AMOUNT header");

        // Header order check: WORK CATEGORY before WORK TYPE, WORK TYPE before ROOM
        int catIdx = normalized.indexOf("WORK CATEGORY");
        int typeIdx = normalized.indexOf("WORK TYPE");
        int roomIdx = normalized.indexOf("ROOM");
        assertTrue(catIdx >= 0 && typeIdx > catIdx, "WORK TYPE header must come after WORK CATEGORY");
        assertTrue(roomIdx > typeIdx, "ROOM header must come after WORK TYPE");

        // 2. Data row values: Category FOYER, Work Type BASE CABINET
        assertTrue(normalized.contains("FOYER"), "Row must contain category FOYER");
        assertTrue(normalized.contains("BASE CABINET"), "Row must contain work type BASE CABINET");
        assertTrue(normalized.contains("MARINE PLY LAMINATION BOX TYPE"), "Row must contain material name");
        assertTrue(normalized.contains("28,760.00"), "Row must contain total amount");
    }
}
