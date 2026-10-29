package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FileUnitMaterialRateEditingTest {

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
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

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

    @Autowired
    private ITemplateEngine templateEngine;

    private User adminUser;
    private User employeeUser;
    private Branch branch;
    private Authentication adminAuth;
    private Authentication employeeAuth;
    private WorkCategory workCategory;
    private Division division;
    private Item item;
    private Material filesMaterial;
    private MaterialOption filesOption;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branch = new Branch();
        branch.setName("Branch Files " + ts);
        branch.setBranchAddress("Files Road");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        adminUser = new User();
        adminUser.setName("Admin User " + ts);
        adminUser.setEmail("admin_" + ts + "@test.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Employee User " + ts);
        employeeUser.setEmail("emp_" + ts + "@test.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branch);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );

        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        workCategory = new WorkCategory();
        workCategory.setName("Design Category " + ts);
        workCategory.setActive(true);
        workCategory = workCategoryRepository.save(workCategory);

        division = new Division();
        division.setName("3D Elevation");
        division.setWorkCategory(workCategory);
        division.setActive(true);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Design Files");
        item.setDivision(division);
        item.setActive(true);
        item = itemRepository.save(item);

        filesMaterial = new Material();
        filesMaterial.setName("3D Render File");
        filesMaterial.setItem(item);
        filesMaterial.setDivision(division);
        filesMaterial.setUnit("Files");
        filesMaterial.setMeasurementType(MeasurementType.FILES);
        filesMaterial.setRate(new BigDecimal("500.00")); // Original master rate
        filesMaterial.setActive(true);
        filesMaterial = materialRepository.save(filesMaterial);

        filesOption = new MaterialOption();
        filesOption.setName("Standard Render");
        filesOption.setMaterial(filesMaterial);
        filesOption.setActive(true);
        filesOption = materialOptionRepository.save(filesOption);

        Pricing p = new Pricing();
        p.setMaterialOption(filesOption);
        p.setPricingMode(PricingMode.MIDDLE);
        p.setRate(new BigDecimal("500.00"));
        p.setUnit("Files");
        p.setActive(true);
        p = pricingRepository.save(p);

        DistrictPricing dp = new DistrictPricing();
        dp.setBranch(branch);
        dp.setPricing(p);
        dp.setRate(new BigDecimal("500.00"));
        dp.setActive(true);
        districtPricingRepository.save(dp);
    }

    @Test
    void testAdminCanSaveEditedRateForFilesUnitMaterial() {
        BigDecimal editedRate = new BigDecimal("750.00");
        BigDecimal qty = new BigDecimal("3");

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(),
                division.getId(),
                item.getId(),
                filesMaterial.getId(),
                filesOption.getId(),
                null,
                "Client 3D Renders",
                "High resolution interior renders",
                List.of(),
                MeasurementType.FILES,
                qty,
                null,
                editedRate
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "First Floor", "101", "Living Area", List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Pravin Client", "9876543210", "client@example.com", "Main Street"
        );

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Requirement text", "Internal notes", List.of(roomReq)
        );

        Quotation quotation = adminQuotationBuilderService.save(req, adminUser.getEmail());

        assertNotNull(quotation.getId());
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotation.getId());
        assertEquals(1, rooms.size());

        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(rooms.get(0).getId());
        assertEquals(1, items.size());

        QuotationItem savedItem = items.get(0);
        // Saved item should have the edited rate
        assertEquals(0, new BigDecimal("750.00").compareTo(savedItem.getRate()), "Quotation item must store edited rate");
        assertEquals(0, new BigDecimal("3").compareTo(savedItem.getQuantity()), "Quantity must be 3");
        // Amount = Rate * Quantity = 750.00 * 3 = 2250.00
        assertEquals(0, new BigDecimal("2250.00").compareTo(savedItem.getAmount()), "Amount must be Rate * Quantity = 2250.00");
        assertEquals(MeasurementType.FILES, savedItem.getMeasurementType());

        // Verify master material rate remains unchanged at 500.00
        Material masterMaterial = materialRepository.findById(filesMaterial.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(masterMaterial.getRate()), "Master material rate must NOT be modified");
    }

    @Test
    void testEmployeeCanSaveEditedRateForFilesUnitMaterial() {
        BigDecimal editedRate = new BigDecimal("820.00");
        BigDecimal qty = new BigDecimal("2");

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(),
                division.getId(),
                item.getId(),
                filesMaterial.getId(),
                filesOption.getId(),
                null,
                "Employee 3D Renders",
                "4K Renders",
                List.of(),
                MeasurementType.FILES,
                qty,
                null,
                editedRate
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "G-1", "Kitchen Area", List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Employee Client", "9876543212", "empclient@example.com", "Cross Road"
        );

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Emp requirements", "Emp notes", List.of(roomReq)
        );

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = employeeQuotationController.saveBuilderQuotation(req, employeeAuth, redirectAttributes);

        assertTrue(result.startsWith("redirect:/employee/quotations/view/"));
        Long quotationId = Long.parseLong(result.substring("redirect:/employee/quotations/view/".length()));

        Quotation savedQuote = quotationRepository.findById(quotationId).orElseThrow();
        assertEquals(employeeUser.getId(), savedQuote.getEmployee().getId());

        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(quotationId);
        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(rooms.get(0).getId());
        assertEquals(1, items.size());

        QuotationItem savedItem = items.get(0);
        assertEquals(0, new BigDecimal("820.00").compareTo(savedItem.getRate()), "Quotation item must store employee's edited rate");
        assertEquals(0, new BigDecimal("2").compareTo(savedItem.getQuantity()));
        // Amount = 820 * 2 = 1640.00
        assertEquals(0, new BigDecimal("1640.00").compareTo(savedItem.getAmount()));

        // Original master material rate must still be 500.00
        Material masterMaterial = materialRepository.findById(filesMaterial.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(masterMaterial.getRate()));
    }

    @Test
    void testNegativeRateThrowsValidationExceptionOnServer() {
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(),
                division.getId(),
                item.getId(),
                filesMaterial.getId(),
                filesOption.getId(),
                null,
                "Negative Rate Item",
                "Spec",
                List.of(),
                MeasurementType.FILES,
                BigDecimal.ONE,
                null,
                new BigDecimal("-50.00")
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Room", List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Cust", "9876543213", "cust@example.com", "City"
        );

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Req", "Notes", List.of(roomReq)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminQuotationBuilderService.save(req, adminUser.getEmail());
        });

        assertTrue(ex.getMessage().contains("Rate for Files unit must be a valid, non-negative value"));
    }

    @Test
    void testPdfGenerationUsesSavedItemRateAndAmount() {
        BigDecimal editedRate = new BigDecimal("999.00");
        BigDecimal qty = new BigDecimal("4");

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(),
                division.getId(),
                item.getId(),
                filesMaterial.getId(),
                filesOption.getId(),
                null,
                "PDF Render Item",
                "PDF Spec",
                List.of(),
                MeasurementType.FILES,
                qty,
                null,
                editedRate
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Top Floor", "301", "Studio", List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "PDF Client", "9876543214", "pdfclient@example.com", "Tech Park"
        );

        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "PDF req", "PDF notes", List.of(roomReq)
        );

        Quotation quotation = adminQuotationBuilderService.save(req, adminUser.getEmail());

        // Generate PDF
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "Generated PDF must have valid bytes");

        // Verify quotation financial consistency with saved rate & amount
        // 999 * 4 = 3996.00
        assertEquals(new BigDecimal("3996.00"), quotation.getSubtotal());
        assertEquals(new BigDecimal("3996.00"), quotation.getGrandTotal());
    }

    @Test
    void testQuotationBuilderTemplatesContainRateEditingForFiles() {
        ExtendedModelMap model = new ExtendedModelMap();
        adminQuotationBuilderController.builder(null, branch.getId(), model);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var servletApp = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var webExchange = servletApp.buildExchange(request, response);
        WebContext context = new WebContext(webExchange, request.getLocale(), model.asMap());

        String adminHtml = templateEngine.process("admin/quotation-builder", context);
        assertNotNull(adminHtml);
        assertTrue(adminHtml.contains("dimRate-"), "Admin quotation builder must include dimRate- input for files");
        assertTrue(adminHtml.contains("RATE (&#8377;)"), "Admin quotation builder must have RATE field");

        ExtendedModelMap empModel = new ExtendedModelMap();
        employeeQuotationController.builder(null, null, employeeAuth, empModel);
        WebContext empContext = new WebContext(webExchange, request.getLocale(), empModel.asMap());

        String empHtml = templateEngine.process("employee/quotation-builder", empContext);
        assertNotNull(empHtml);
        assertTrue(empHtml.contains("dimRate-"), "Employee quotation builder must include dimRate- input for files");
        assertTrue(empHtml.contains("RATE (&#8377;)"), "Employee quotation builder must have RATE field");
    }
}
