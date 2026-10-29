package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ShowSqftSizeToggleSyncTest {

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

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
    private DistrictPricingRepository districtPricingRepository;

    private User superAdminUser;
    private User employeeUser;
    private Branch branch;
    private Customer customer;
    private WorkCategory workCategory;
    private Division division;
    private Item item;
    private Material sqftMaterial;
    private MaterialOption sqftOption;

    @BeforeEach
    void setUp() {
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Chennai HQ");
            b.setBranchAddress("100 Anna Salai, Chennai");
            b.setEmail("chennai@pravinkitchens.com");
            b.setActive(true);
            return branchRepository.save(b);
        });

        superAdminUser = userRepository.findByEmail("admin_toggle_test@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Admin Toggle Tester");
            u.setEmail("admin_toggle_test@pravin.com");
            u.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
            u.setRole(Role.SUPER_ADMIN);
            u.setActive(true);
            u.setBranch(branch);
            return userRepository.save(u);
        });

        employeeUser = userRepository.findByEmail("emp_toggle_test@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Emp Toggle Tester");
            u.setEmail("emp_toggle_test@pravin.com");
            u.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
            u.setRole(Role.EMPLOYEE);
            u.setActive(true);
            u.setBranch(branch);
            return userRepository.save(u);
        });

        customer = new Customer();
        customer.setName("Mr. Anand Kumar");
        customer.setPhone("9876543210");
        customer.setEmail("anand_toggle_" + System.currentTimeMillis() + "@example.com");
        customer.setAddress("42 Lake View Road");
        customer.setBranch(branch);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        workCategory = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory wc = new WorkCategory();
            wc.setName("KITCHEN");
            wc.setActive(true);
            return workCategoryRepository.save(wc);
        });

        division = divisionRepository.findByWorkCategoryIdAndActiveTrueOrderByDisplayOrderAsc(workCategory.getId())
                .stream().findFirst().orElseGet(() -> {
                    Division d = new Division();
                    d.setName("BASE UNITS");
                    d.setWorkCategory(workCategory);
                    d.setActive(true);
                    return divisionRepository.save(d);
                });

        item = itemRepository.findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc(division.getId())
                .stream().findFirst().orElseGet(() -> {
                    Item i = new Item();
                    i.setName("SINK UNIT");
                    i.setDivision(division);
                    i.setActive(true);
                    return itemRepository.save(i);
                });

        sqftMaterial = new Material();
        sqftMaterial.setName("HDHMR Board");
        sqftMaterial.setUnit("Sq.ft");
        sqftMaterial.setMeasurementType(MeasurementType.SQFT);
        sqftMaterial.setFormula("(L + W) * H");
        sqftMaterial.setDivision(division);
        sqftMaterial.setActive(true);
        sqftMaterial = materialRepository.save(sqftMaterial);

        sqftOption = new MaterialOption();
        sqftOption.setName("18mm Matt Finish");
        sqftOption.setMaterial(sqftMaterial);
        sqftOption.setActive(true);
        sqftOption = materialOptionRepository.save(sqftOption);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(sqftOption);
        pricing.setPricingMode(PricingMode.MIDDLE);
        pricing.setRate(new BigDecimal("1200.00"));
        pricing.setUnit("Sq.ft");
        pricing.setActive(true);
        pricing = pricingRepository.save(pricing);

        DistrictPricing dp = new DistrictPricing();
        dp.setBranch(branch);
        dp.setPricing(pricing);
        dp.setRate(new BigDecimal("1200.00"));
        dp.setActive(true);
        districtPricingRepository.save(dp);
    }

    private String extractPage2PdfText(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        byte[] pageContent = reader.getPageContent(2);
        reader.close();
        return pageContent != null ? new String(pageContent, StandardCharsets.ISO_8859_1) : "";
    }

    @Test
    void testSaveQuotationWithToggleOn_PersistsAndSyncsToLoadAndPdf() throws Exception {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("5.0"), new BigDecimal("3.0"), new BigDecimal("2.5"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                sqftMaterial.getId(), sqftOption.getId(), null,
                "Sink Unit Base", "18mm Matt", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Cabinet work", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer specs", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true // showSqftSize = true
        );

        Quotation quotation = adminQuotationBuilderService.save(request, superAdminUser.getEmail());
        assertNotNull(quotation.getId());
        assertEquals(Boolean.TRUE, quotation.getShowSqftSize(), "Quotation should have showSqftSize = true");

        // Verify Load Endpoint
        ResponseEntity<?> loadResp = adminQuotationBuilderController.loadQuotation(quotation.getId());
        assertTrue(loadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResp.getBody();
        assertNotNull(data);
        assertEquals(true, data.get("showSqftSize"), "loadQuotation must return showSqftSize: true");

        // Verify PDF output includes SQ.FT and SIZE headers
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        assertNotNull(pdfBytes);
        String page2Text = extractPage2PdfText(pdfBytes);
        assertTrue(page2Text.contains("SQ.FT"), "PDF Page 2 must contain SQ.FT header when toggle is ON");
        assertTrue(page2Text.contains("SIZE"), "PDF Page 2 must contain SIZE header when toggle is ON");
    }

    @Test
    void testSaveQuotationWithToggleOff_PersistsAndSyncsToLoadAndPdf() throws Exception {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("5.0"), new BigDecimal("3.0"), new BigDecimal("2.5"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                sqftMaterial.getId(), sqftOption.getId(), null,
                "Sink Unit Base", "18mm Matt", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Cabinet work", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer specs", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false // showSqftSize = false
        );

        Quotation quotation = adminQuotationBuilderService.save(request, superAdminUser.getEmail());
        assertNotNull(quotation.getId());
        assertEquals(Boolean.FALSE, quotation.getShowSqftSize(), "Quotation should have showSqftSize = false");

        // Verify Load Endpoint
        ResponseEntity<?> loadResp = adminQuotationBuilderController.loadQuotation(quotation.getId());
        assertTrue(loadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResp.getBody();
        assertNotNull(data);
        assertEquals(false, data.get("showSqftSize"), "loadQuotation must return showSqftSize: false");

        // Verify PDF output omits SIZE column header and dimensions when toggle is OFF
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        assertNotNull(pdfBytes);
        String page2Text = extractPage2PdfText(pdfBytes);
        assertFalse(page2Text.contains("SIZE"), "PDF Page 2 must NOT contain SIZE header when toggle is OFF");
        assertFalse(page2Text.contains("5.00 \u00d7 3.00"), "PDF Page 2 must NOT contain size dimensions when toggle is OFF");
        assertTrue(page2Text.contains("WORK CATEGORY"), "PDF Page 2 must retain other columns like WORK CATEGORY");
        assertTrue(page2Text.contains("MATERIAL"), "PDF Page 2 must retain MATERIAL column");
        assertTrue(page2Text.contains("TOTAL AMOUNT"), "PDF Page 2 must retain TOTAL AMOUNT column");
    }

    @Test
    void testToggleStateCanBeUpdatedDuringEditing() throws Exception {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("4.0"), new BigDecimal("2.0"), new BigDecimal("2.0"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                sqftMaterial.getId(), sqftOption.getId(), null,
                "Sink Unit Base", "18mm Matt", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Cabinet work", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
        // Initially ON
        AdminQuotationBuilderService.BuilderRequest initialReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer specs", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                true
        );
        Quotation quotation = adminQuotationBuilderService.save(initialReq, superAdminUser.getEmail());
        assertEquals(Boolean.TRUE, quotation.getShowSqftSize());

        // Edit quotation: set toggle to OFF
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                quotation.getId(), custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer specs", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false
        );
        Quotation updatedQuotation = adminQuotationBuilderService.save(editReq, superAdminUser.getEmail());
        assertEquals(quotation.getId(), updatedQuotation.getId());
        assertEquals(Boolean.FALSE, updatedQuotation.getShowSqftSize(), "Updated quotation must have showSqftSize = false");

        // Verify PDF reflects update
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(updatedQuotation.getId());
        String page2Text = extractPage2PdfText(pdfBytes);
        assertFalse(page2Text.contains("SIZE"), "PDF Page 2 must NOT contain SIZE header when edited to toggle OFF");
        assertFalse(page2Text.contains("4.00 \u00d7 2.00"), "PDF Page 2 must NOT contain size dimensions when edited to toggle OFF");
    }

    @Test
    void testEmployeeWorkflowWithToggleSync() throws Exception {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("4.0"), new BigDecimal("2.0"), new BigDecimal("2.0"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                sqftMaterial.getId(), sqftOption.getId(), null,
                "Sink Unit Base", "18mm Matt", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Cabinet work", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        // Employee saves quotation with toggle OFF
        AdminQuotationBuilderService.BuilderRequest empReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer specs", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false
        );

        Quotation quotation = adminQuotationBuilderService.save(empReq, employeeUser.getEmail());
        assertEquals(Boolean.FALSE, quotation.getShowSqftSize());

        // Employee builder load endpoint returns showSqftSize: false
        Authentication auth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        ResponseEntity<?> empLoadResp = employeeQuotationController.builderLoadQuotation(quotation.getId(), auth);
        assertTrue(empLoadResp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) empLoadResp.getBody();
        assertNotNull(data);
        assertEquals(false, data.get("showSqftSize"), "Employee builder load must return showSqftSize: false");
    }

    @Test
    void testToggleOffWithFilesUnitMaterial_OmitsBothSizeAndSqftHeadersAndColumns() throws Exception {
        Material filesMaterial = new Material();
        filesMaterial.setName("3D Elevation Render");
        filesMaterial.setUnit("Files");
        filesMaterial.setMeasurementType(MeasurementType.FILES);
        filesMaterial.setDivision(division);
        filesMaterial.setActive(true);
        filesMaterial = materialRepository.save(filesMaterial);

        MaterialOption filesOption = new MaterialOption();
        filesOption.setName("4K Realistic");
        filesOption.setMaterial(filesMaterial);
        filesOption.setActive(true);
        filesOption = materialOptionRepository.save(filesOption);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(filesOption);
        pricing.setPricingMode(PricingMode.MIDDLE);
        pricing.setRate(new BigDecimal("1500.00"));
        pricing.setUnit("Files");
        pricing.setActive(true);
        pricing = pricingRepository.save(pricing);

        DistrictPricing dp = new DistrictPricing();
        dp.setBranch(branch);
        dp.setPricing(pricing);
        dp.setRate(new BigDecimal("1500.00"));
        dp.setActive(true);
        districtPricingRepository.save(dp);

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(),
                filesMaterial.getId(), filesOption.getId(), null,
                "Elevation Design", "4K", List.of(),
                MeasurementType.FILES, new BigDecimal("2"), null, new BigDecimal("1500.00")
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living", "Exterior render", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer specs", "Notes", List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false // Toggle OFF
        );

        Quotation quotation = adminQuotationBuilderService.save(req, superAdminUser.getEmail());
        assertEquals(Boolean.FALSE, quotation.getShowSqftSize());

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        String page2Text = extractPage2PdfText(pdfBytes);
        assertFalse(page2Text.contains("SIZE"), "PDF Page 2 must NOT contain SIZE header or column when toggle is OFF");
        assertFalse(page2Text.contains("SQ.FT"), "PDF Page 2 must NOT contain SQ.FT header or column when toggle is OFF");
        assertTrue(page2Text.contains("FILES"), "PDF Page 2 must contain FILES under UNIT column");
        assertTrue(page2Text.contains("TOTAL AMOUNT"), "PDF Page 2 must contain TOTAL AMOUNT header");
    }
}
