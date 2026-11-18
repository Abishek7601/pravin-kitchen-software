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

public class QuotationPdfTypeMatchingGradeTest {

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

    private Quotation createSampleQuotation(Long id, PricingMode pricingMode) {
        Branch branch = branchStore.get(1L);
        Customer customer = new Customer();
        customer.setName("Abishek S R");
        customer.setAddress("Arumanai");
        customer.setBranch(branch);

        Quotation quotation = new Quotation();
        quotation.setId(id);
        quotation.setQuotationNumber("QT-00053");
        quotation.setQuotationDate(LocalDate.of(2026, 11, 14));
        quotation.setValidUntil(LocalDate.of(2027, 11, 14));
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setPricingMode(pricingMode);
        quotation.setShowSqftSize(true);
        quotation.setSubtotal(new BigDecimal("7350.00"));
        quotation.setTotalAmount(new BigDecimal("7350.00"));
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("7350.00"));

        quotationStore.put(id, quotation);

        QuotationRoom room = new QuotationRoom();
        room.setId(id * 10);
        room.setQuotation(quotation);
        room.setRoom("1");
        room.setFloor("GROUND FLOOR");
        room.setActive(true);
        roomStoreByQuotation.put(id, List.of(room));

        Material mat = new Material();
        mat.setName(pricingMode == PricingMode.ECONOMY ? "ENGINEERING WOOD BOX TYPE" : "MARINE PLY BOX TYPE");
        mat.setMeasurementType(MeasurementType.SQFT);

        QuotationItem item = new QuotationItem();
        item.setId(id * 100);
        item.setQuotationRoom(room);
        item.setMaterial(mat);
        item.setPricingMode(pricingMode);
        item.setSpecification("16mm Thickness");
        item.setLengthValue(new BigDecimal("23.00"));
        item.setWidthValue(new BigDecimal("23.00"));
        item.setHeightValue(new BigDecimal("23.00"));
        item.setCalculatedSqft(new BigDecimal("7.35"));
        item.setMeasurementType(MeasurementType.SQFT);
        item.setRate(new BigDecimal("1000.00"));
        item.setAmount(new BigDecimal("7350.00"));
        item.setOfferPrice(new BigDecimal("7350.00"));
        item.setActive(true);
        itemStoreByRoom.put(room.getId(), List.of(item));

        return quotation;
    }

    @Test
    @DisplayName("PDF generated with targetGrade=ECONOMY displays 'ECONOMY GRADE QUOTATION' and 'Quotation Type: Economy'")
    void testPdfGenerationWithEconomyGrade() throws Exception {
        createSampleQuotation(201L, PricingMode.ECONOMY);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(201L, PricingMode.ECONOMY);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);
        String normalizedText = page2Text.replaceAll("\\s+", " ");

        assertTrue(normalizedText.contains("ECONOMY GRADE QUOTATION") || (page2Text.contains("ECONOMY GRADE") && page2Text.contains("QUOTATION")), "Title must be ECONOMY GRADE QUOTATION");
        assertTrue(page2Text.contains("Quotation Type:") && page2Text.contains("Economy"), "Quotation Type must be Economy");
        assertTrue(page2Text.contains("Economy Grade Subtotal"), "Footer must contain Economy Grade Subtotal");
    }

    @Test
    @DisplayName("PDF generated with targetGrade=MIDDLE displays 'MIDDLE GRADE QUOTATION' and 'Quotation Type: Middle / Standard'")
    void testPdfGenerationWithMiddleGrade() throws Exception {
        createSampleQuotation(202L, PricingMode.MIDDLE);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(202L, PricingMode.MIDDLE);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        assertTrue(page2Text.contains("MIDDLE GRADE QUOTATION"), "Title must be MIDDLE GRADE QUOTATION");
        assertTrue(page2Text.contains("Quotation Type:") && page2Text.contains("Middle / Standard"), "Quotation Type must be Middle / Standard");
        assertTrue(page2Text.contains("Middle Grade Subtotal"), "Footer must contain Middle Grade Subtotal");
    }

    @Test
    @DisplayName("PDF generated with targetGrade=PREMIUM displays 'PREMIUM GRADE QUOTATION' and 'Quotation Type: Premium'")
    void testPdfGenerationWithPremiumGrade() throws Exception {
        createSampleQuotation(203L, PricingMode.PREMIUM);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(203L, PricingMode.PREMIUM);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);

        assertTrue(page2Text.contains("PREMIUM GRADE QUOTATION"), "Title must be PREMIUM GRADE QUOTATION");
        assertTrue(page2Text.contains("Quotation Type:") && page2Text.contains("Premium"), "Quotation Type must be Premium");
        assertTrue(page2Text.contains("Premium Grade Subtotal"), "Footer must contain Premium Grade Subtotal");
    }

    @Test
    @DisplayName("PDF generated without explicit targetGrade respects quotation.selectedGrade")
    void testPdfGenerationSelectedGradeFallback() throws Exception {
        Quotation q = createSampleQuotation(204L, PricingMode.PREMIUM);
        q.setSelectedGrade(PricingMode.ECONOMY);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(204L);
        assertNotNull(pdfBytes);

        PdfReader reader = new PdfReader(pdfBytes);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page2Text = extractor.getTextFromPage(2);
        String normalizedText = page2Text.replaceAll("\\s+", " ");

        assertTrue(normalizedText.contains("ECONOMY GRADE QUOTATION") || (page2Text.contains("ECONOMY GRADE") && page2Text.contains("QUOTATION")), "Title must adapt to selectedGrade ECONOMY");
        assertTrue(page2Text.contains("Quotation Type:") && page2Text.contains("Economy"), "Quotation Type must adapt to selectedGrade Economy");
    }
}
