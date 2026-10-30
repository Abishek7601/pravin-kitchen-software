package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.QuotationService;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationPdfSpecificationCapitalizationTest {

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    private Customer customer;
    private User employee;
    private Branch branch;
    private WorkCategory category;
    private Division division;
    private Material material;
    private MaterialOption option;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setName("PDF Test Branch " + System.nanoTime());
        branch.setActive(true);
        branch = branchRepository.save(branch);

        employee = new User();
        employee.setName("PDF Tester");
        employee.setEmail("pdftester_" + System.nanoTime() + "@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);
        employee = userRepository.save(employee);

        customer = new Customer();
        customer.setName("Karthik Raja");
        customer.setPhone("9876543210");
        customer.setEmail("karthik@example.com");
        customer.setAddress("Anna Nagar, Chennai");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        category = new WorkCategory();
        category.setName("FOYER");
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("SHOE RACK");
        division.setWorkCategory(category);
        division.setActive(true);
        division = divisionRepository.save(division);

        material = new Material();
        material.setName("BWP PLYWOOD");
        material.setUnit("Sq.ft");
        material.setMeasurementType(MeasurementType.SQFT);
        material.setDivision(division);
        material.setActive(true);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("16MM SHEET");
        option.setMaterial(material);
        option.setActive(true);
        option = materialOptionRepository.save(option);
    }

    private String extractPage2PdfText(byte[] pdfBytes) throws IOException {
        PdfReader reader = new PdfReader(pdfBytes);
        byte[] pageContent = reader.getPageContent(2);
        reader.close();
        return pageContent != null ? new String(pageContent, StandardCharsets.ISO_8859_1) : "";
    }

    @Test
    @DisplayName("1. Unit Test: formatSpecificationForPdf converts specifications to Title Case matching all required examples")
    void testFormatSpecificationForPdfExamples() {
        // Example 1: MARINE PLY 710 GRADE -> Marine Ply 710 Grade
        assertEquals("Marine Ply 710 Grade",
                QuotationPdfService.formatSpecificationForPdf("MARINE PLY 710 GRADE"));

        // Example 2: 16MM THICKNESS -> 16mm Thickness
        assertEquals("16mm Thickness",
                QuotationPdfService.formatSpecificationForPdf("16MM THICKNESS"));

        // Example 3: BRAND: FINE PLY/SABURI -> Brand: Fine Ply/Saburi
        assertEquals("Brand: Fine Ply/Saburi",
                QuotationPdfService.formatSpecificationForPdf("BRAND: FINE PLY/SABURI"));

        // Example 4: INNER FINISH: FABRIC/OFF WHITE -> Inner Finish: Fabric/Off White
        assertEquals("Inner Finish: Fabric/Off White",
                QuotationPdfService.formatSpecificationForPdf("INNER FINISH: FABRIC/OFF WHITE"));

        // Multiline specification preservation
        String multilineInput = "MARINE PLY 710 GRADE\n16MM THICKNESS\nBRAND: FINE PLY/SABURI\nINNER FINISH: FABRIC/OFF WHITE";
        String expectedMultiline = "Marine Ply 710 Grade\n16mm Thickness\nBrand: Fine Ply/Saburi\nInner Finish: Fabric/Off White";
        assertEquals(expectedMultiline,
                QuotationPdfService.formatSpecificationForPdf(multilineInput));

        // Mixed measurements, special characters, and hyphens
        assertEquals("18mm Thickness, Is:710 Marine Grade Bwp (Grade-A) + Laminate Finish",
                QuotationPdfService.formatSpecificationForPdf("18MM THICKNESS, IS:710 MARINE GRADE BWP (GRADE-A) + LAMINATE FINISH"));

        // Null and blank strings
        assertNull(QuotationPdfService.formatSpecificationForPdf(null));
        assertEquals("", QuotationPdfService.formatSpecificationForPdf(""));
        assertEquals("   ", QuotationPdfService.formatSpecificationForPdf("   "));
    }

    @Test
    @DisplayName("2. PDF Generation: Material Name is FULL UPPERCASE, Specification is Title Case, Database is Unchanged")
    void testPdfGenerationRendersMaterialNameUppercaseAndSpecificationTitleCase() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 30);
        Quotation q = quotationService.create(
                "QT-SPEC-" + System.nanoTime(),
                customer.getId(),
                employee.getId(),
                branch.getId(),
                today,
                PricingMode.PREMIUM,
                "Foyer Shoe Rack Setup",
                "Internal notes"
        );

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setFloor("Ground Floor");
        room.setRoom("1");
        room.setWorkDescription("Foyer Area");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        String originalDbSpec = "MARINE PLY 710 GRADE\n16MM THICKNESS\nBRAND: FINE PLY/SABURI\nINNER FINISH: FABRIC/OFF WHITE";

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setDivision(division);
        item.setMaterial(material);
        item.setMaterialOption(option);
        item.setSpecification(originalDbSpec);
        item.setMeasurementType(MeasurementType.SQFT);
        item.setFormula("L × W");
        item.setLengthValue(new BigDecimal("32.00"));
        item.setWidthValue(new BigDecimal("23.00"));
        item.setCalculatedSqft(new BigDecimal("5.11"));
        item.setRate(new BigDecimal("300.00"));
        item.setAmount(new BigDecimal("1533.00"));
        item.setActive(true);
        quotationItemRepository.save(item);

        QuotationItem item2 = new QuotationItem();
        item2.setQuotationRoom(room);
        item2.setWorkCategory(category);
        item2.setDivision(division);
        item2.setMaterial(material);
        item2.setSpecification("");
        item2.setMeasurementType(MeasurementType.SQFT);
        item2.setFormula("L × W");
        item2.setLengthValue(new BigDecimal("23.00"));
        item2.setWidthValue(new BigDecimal("12.00"));
        item2.setCalculatedSqft(new BigDecimal("1.92"));
        item2.setRate(new BigDecimal("250.00"));
        item2.setAmount(new BigDecimal("480.00"));
        item2.setActive(true);
        quotationItemRepository.save(item2);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        java.nio.file.Files.write(java.nio.file.Path.of("target/test-quotation.pdf"), pdfBytes);

        String page2 = extractPage2PdfText(pdfBytes);

        // 1. Material Name must remain FULL UPPERCASE
        assertTrue(page2.contains("BWP PLYWOOD"), "Material Name must remain FULL UPPERCASE in generated PDF");

        // 2. Material Specification must be formatted in Title Case
        assertTrue(page2.contains("Marine Ply 710 Grade"), "Specification 'Marine Ply 710 Grade' must be Title Case in PDF");
        assertTrue(page2.contains("16mm Thickness"), "Specification '16mm Thickness' must be Title Case in PDF");
        assertTrue(page2.contains("Brand: Fine Ply/Saburi"), "Specification 'Brand: Fine Ply/Saburi' must be Title Case in PDF");
        assertTrue(page2.contains("Inner Finish: Fabric/Off White"), "Specification 'Inner Finish: Fabric/Off White' must be Title Case in PDF");

        // 3. Confirm uppercase specifications are NOT rendered in the PDF
        assertFalse(page2.contains("MARINE PLY 710 GRADE"), "PDF must NOT contain uppercase specification");
        assertFalse(page2.contains("16MM THICKNESS"), "PDF must NOT contain uppercase specification");

        // 4. Confirm Database original specification was NOT modified
        QuotationItem itemInDb = quotationItemRepository.findById(item.getId()).orElseThrow();
        assertEquals(originalDbSpec, itemInDb.getSpecification(), "Database original specification must NOT be modified");
    }

    @Test
    @DisplayName("3. Material Spacing: Vertical spacing gap exists between Material Name and Specification")
    void testMaterialSpacingBetweenNameAndSpecification() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 30);
        Quotation q = quotationService.create(
                "QT-SPACE-" + System.nanoTime(),
                customer.getId(),
                employee.getId(),
                branch.getId(),
                today,
                PricingMode.PREMIUM,
                "Spacing Verification",
                "Internal notes"
        );

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setFloor("Ground Floor");
        room.setRoom("1");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setWorkCategory(category);
        item.setDivision(division);
        item.setMaterial(material);
        item.setSpecification("Line One Spec\nLine Two Spec");
        item.setMeasurementType(MeasurementType.SQFT);
        item.setFormula("L × W");
        item.setLengthValue(new BigDecimal("30.00"));
        item.setWidthValue(new BigDecimal("20.00"));
        item.setCalculatedSqft(new BigDecimal("4.17"));
        item.setRate(new BigDecimal("300.00"));
        item.setAmount(new BigDecimal("1251.00"));
        item.setActive(true);
        quotationItemRepository.save(item);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);

        String page2 = extractPage2PdfText(pdfBytes);
        assertTrue(page2.contains("BWP PLYWOOD"));
        assertTrue(page2.contains("Line One Spec"));
        assertTrue(page2.contains("Line Two Spec"));
    }

    @Test
    @DisplayName("4. Material Image: Uploaded image replaces PHOTO text, renders without errors, preserves other material names")
    void testMaterialImageReplacesPhotoTextInPdf() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 30);
        Quotation q = quotationService.create(
                "QT-IMG-" + System.nanoTime(),
                customer.getId(),
                employee.getId(),
                branch.getId(),
                today,
                PricingMode.PREMIUM,
                "Photo Material Test",
                "Internal notes"
        );

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setFloor("Ground Floor");
        room.setRoom("Room 1");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        // Material with name "PHOTO" and unit "Files"
        Material photoMat = new Material();
        photoMat.setName("PHOTO");
        photoMat.setUnit("Files");
        photoMat.setMeasurementType(MeasurementType.FILES);
        photoMat.setDivision(division);
        photoMat.setActive(true);
        photoMat = materialRepository.save(photoMat);

        // Item 1: "PHOTO" material WITH uploaded image
        QuotationItem itemWithImage = new QuotationItem();
        itemWithImage.setQuotationRoom(room);
        itemWithImage.setWorkCategory(category);
        itemWithImage.setDivision(division);
        itemWithImage.setMaterial(photoMat);
        itemWithImage.setImageUrl("/uploads/quotation-items/aa420e66-5fe2-4a33-97dc-4b010a0db40e.jpg");
        itemWithImage.setMeasurementType(MeasurementType.FILES);
        itemWithImage.setQuantity(new BigDecimal("2"));
        itemWithImage.setRate(new BigDecimal("400.00"));
        itemWithImage.setAmount(new BigDecimal("800.00"));
        itemWithImage.setActive(true);
        quotationItemRepository.save(itemWithImage);

        // Item 2: Named material "GYPSUM CEILING" WITH uploaded image
        Material gypsumMat = new Material();
        gypsumMat.setName("GYPSUM CEILING");
        gypsumMat.setUnit("Files");
        gypsumMat.setMeasurementType(MeasurementType.FILES);
        gypsumMat.setDivision(division);
        gypsumMat.setActive(true);
        gypsumMat = materialRepository.save(gypsumMat);

        QuotationItem item2WithImage = new QuotationItem();
        item2WithImage.setQuotationRoom(room);
        item2WithImage.setWorkCategory(category);
        item2WithImage.setDivision(division);
        item2WithImage.setMaterial(gypsumMat);
        item2WithImage.setImageUrl("/uploads/quotation-items/aa420e66-5fe2-4a33-97dc-4b010a0db40e.jpg");
        item2WithImage.setMeasurementType(MeasurementType.FILES);
        item2WithImage.setQuantity(new BigDecimal("1"));
        item2WithImage.setRate(new BigDecimal("500.00"));
        item2WithImage.setAmount(new BigDecimal("500.00"));
        item2WithImage.setActive(true);
        quotationItemRepository.save(item2WithImage);

        // Item 3: "PHOTO" material WITHOUT image (fallback: show text)
        QuotationItem itemNoImage = new QuotationItem();
        itemNoImage.setQuotationRoom(room);
        itemNoImage.setWorkCategory(category);
        itemNoImage.setDivision(division);
        itemNoImage.setMaterial(photoMat);
        itemNoImage.setImageUrl(null);
        itemNoImage.setMeasurementType(MeasurementType.FILES);
        itemNoImage.setQuantity(new BigDecimal("1"));
        itemNoImage.setRate(new BigDecimal("300.00"));
        itemNoImage.setAmount(new BigDecimal("300.00"));
        itemNoImage.setActive(true);
        quotationItemRepository.save(itemNoImage);

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        java.nio.file.Files.write(java.nio.file.Path.of("target/test-photo-quotation.pdf"), pdfBytes);

        String page2 = extractPage2PdfText(pdfBytes);

        // Item 2 named material must be displayed in uppercase
        assertTrue(page2.contains("GYPSUM CEILING"), "Named material 'GYPSUM CEILING' must be displayed in PDF");

        // Item 3 (photo material with no image) displays "PHOTO" as fallback
        assertTrue(page2.contains("PHOTO"), "PHOTO text should appear as fallback when image is absent");
    }
}
