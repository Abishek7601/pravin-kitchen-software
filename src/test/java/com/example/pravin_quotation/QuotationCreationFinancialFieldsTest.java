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
import org.springframework.http.ResponseEntity;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationCreationFinancialFieldsTest {

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private AdminQuotationBuilderController adminBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private UserRepository userRepository;

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
    private QuotationPdfService quotationPdfService;

    @Autowired
    private ITemplateEngine templateEngine;

    private User admin;
    private User employee;
    private Branch branch;
    private Authentication adminAuth;
    private Authentication employeeAuth;
    private WorkCategory category;
    private Division division;
    private Item item;
    private Material material;
    private MaterialOption option;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branch = new Branch();
        branch.setName("Financial Test Branch " + ts);
        branch.setBranchAddress("123 Main St");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        admin = new User();
        admin.setName("Admin " + ts);
        admin.setEmail("admin_" + ts + "@test.com");
        admin.setPassword("secret");
        admin.setRole(Role.SUPER_ADMIN);
        admin.setActive(true);
        admin = userRepository.save(admin);

        employee = new User();
        employee.setName("Employee " + ts);
        employee.setEmail("emp_" + ts + "@test.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);
        employee = userRepository.save(employee);

        adminAuth = new UsernamePasswordAuthenticationToken(admin.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        employeeAuth = new UsernamePasswordAuthenticationToken(employee.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

        category = new WorkCategory();
        category.setName("Kitchen " + ts);
        category.setActive(true);
        category.setDisplayOrder(1);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("Base Unit " + ts);
        division.setWorkCategory(category);
        division.setActive(true);
        division.setDisplayOrder(1);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Base Item " + ts);
        item.setDivision(division);
        item.setActive(true);
        item.setDisplayOrder(1);
        item = itemRepository.save(item);

        material = new Material();
        material.setName("Plywood " + ts);
        material.setItem(item);
        material.setActive(true);
        material.setDisplayOrder(1);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("710 BWP " + ts);
        option.setMaterial(material);
        option.setActive(true);
        option = materialOptionRepository.save(option);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(option);
        pricing.setPricingMode(PricingMode.MIDDLE);
        pricing.setRate(new BigDecimal("400.00"));
        pricing.setFormula("(L + W) * H");
        pricing.setUnit("SQFT");
        pricing.setActive(true);
        pricingRepository.save(pricing);
    }

    @Test
    void testSaveAndCalculateFinancialFieldsDuringCreation() {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("24"), new BigDecimal("36"), new BigDecimal("30"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                "Base Cabinet", "Marine Grade", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Main Kitchen", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                "Rajesh Kumar", "9876543210", "rajesh@example.com", "No 10 Green Avenue"
        );

        BigDecimal accessories = new BigDecimal("5000.00");
        BigDecimal travel = new BigDecimal("1500.00");
        BigDecimal other = new BigDecimal("800.00");
        BigDecimal discount = new BigDecimal("2000.00");
        BigDecimal gstPct = new BigDecimal("18.00");
        BigDecimal offerPct = new BigDecimal("5.00");

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null,
                customerReq,
                branch.getId(),
                LocalDate.now(),
                PricingMode.MIDDLE,
                "Need soft close hinges",
                "Priority delivery",
                List.of(roomReq),
                accessories,
                travel,
                other,
                discount,
                gstPct,
                offerPct
        );

        Quotation saved = builderService.save(request, admin.getEmail());

        assertNotNull(saved.getId());
        assertEquals("QT-", saved.getQuotationNumber().substring(0, 3));
        assertEquals(0, accessories.compareTo(saved.getAccessoriesAmount()));
        assertEquals(0, travel.compareTo(saved.getTravelCharge()));
        assertEquals(0, other.compareTo(saved.getOtherCharges()));
        assertEquals(0, discount.compareTo(saved.getDiscountAmount()));
        assertEquals(0, gstPct.compareTo(saved.getGstPercentage()));
        assertEquals(0, offerPct.compareTo(saved.getOfferPercentage()));

        // Verification of calculations:
        // Subtotal > 0
        BigDecimal subtotal = saved.getSubtotal();
        assertTrue(subtotal.compareTo(BigDecimal.ZERO) > 0);

        // Step 2 & 3: Amount After Discount = Subtotal + Acc + Travel + Other - Disc
        BigDecimal expectedAmountBeforeDiscount = subtotal.add(accessories).add(travel).add(other);
        BigDecimal expectedAmountAfterDiscount = expectedAmountBeforeDiscount.subtract(discount);
        assertEquals(0, expectedAmountAfterDiscount.setScale(2, RoundingMode.HALF_UP).compareTo(saved.getTotalAmount()));

        // Step 4: Offer Amount = Amount After Discount * 5 / 100
        BigDecimal expectedOfferAmount = expectedAmountAfterDiscount.multiply(offerPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        assertEquals(0, expectedOfferAmount.compareTo(saved.getOfferAmount()));

        // Taxable Amount (Amount After Offer) = Amount After Discount - Offer Amount
        BigDecimal expectedTaxable = expectedAmountAfterDiscount.subtract(expectedOfferAmount);
        assertEquals(0, expectedTaxable.setScale(2, RoundingMode.HALF_UP).compareTo(saved.getTaxableAmount()));

        // Step 5: GST Amount = Amount After Offer * 18 / 100
        BigDecimal expectedGst = expectedTaxable.multiply(gstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        assertEquals(0, expectedGst.compareTo(saved.getGstAmount()));

        // Step 6: Grand Total = Amount After Offer + GST Amount
        BigDecimal expectedGrandTotal = expectedTaxable.add(expectedGst);
        assertEquals(0, expectedGrandTotal.setScale(2, RoundingMode.HALF_UP).compareTo(saved.getGrandTotal()));

        // Check Admin loadQuotation endpoint
        ResponseEntity<?> adminLoadResp = adminBuilderController.loadQuotation(saved.getId());
        assertEquals(200, adminLoadResp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> adminData = (Map<String, Object>) adminLoadResp.getBody();
        assertNotNull(adminData);
        assertEquals(accessories, adminData.get("accessoriesAmount"));
        assertEquals(travel, adminData.get("travelCharge"));
        assertEquals(other, adminData.get("otherCharges"));
        assertEquals(discount, adminData.get("discountAmount"));
        assertEquals(gstPct, adminData.get("gstPercentage"));
        assertEquals(offerPct, adminData.get("offerPercentage"));

        // Check Employee loadQuotation endpoint
        saved.setEmployee(employee);
        quotationRepository.save(saved);
        ResponseEntity<?> empLoadResp = employeeQuotationController.builderLoadQuotation(saved.getId(), employeeAuth);
        assertEquals(200, empLoadResp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> empData = (Map<String, Object>) empLoadResp.getBody();
        assertNotNull(empData);
        assertEquals(accessories, empData.get("accessoriesAmount"));
        assertEquals(travel, empData.get("travelCharge"));
        assertEquals(other, empData.get("otherCharges"));
        assertEquals(discount, empData.get("discountAmount"));
        assertEquals(gstPct, empData.get("gstPercentage"));
        assertEquals(offerPct, empData.get("offerPercentage"));

        // Check PDF generation executes successfully
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(saved.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testTemplateContainsFinancialSectionElements() {
        ExtendedModelMap model = new ExtendedModelMap();
        employeeQuotationController.builder(null, null, employeeAuth, model);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var servletApp = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var webExchange = servletApp.buildExchange(request, response);
        WebContext context = new WebContext(webExchange, request.getLocale(), model.asMap());

        String renderedHtml = templateEngine.process("employee/quotation-builder", context);
        assertNotNull(renderedHtml);

        // Financial fields present (Accessories removed from UI)
        assertTrue(renderedHtml.contains("id=\"financialCard\""));
        assertFalse(renderedHtml.contains("id=\"accessoriesAmount\""), "Accessories input must be removed from quotation creation");
        assertFalse(renderedHtml.contains("id=\"displayAccessories\""), "Accessories display row must be removed from quotation creation");
        assertFalse(renderedHtml.contains("id=\"printPdfBtn\""), "Print / Save PDF button must be removed from quotation creation");
        assertTrue(renderedHtml.contains("id=\"travelCharge\""));
        assertTrue(renderedHtml.contains("id=\"otherCharges\""));
        assertTrue(renderedHtml.contains("id=\"discountAmount\""));
        assertTrue(renderedHtml.contains("id=\"gstPercentage\""));
        assertTrue(renderedHtml.contains("id=\"offerPercentage\""));

        // Financial summary display elements present
        assertTrue(renderedHtml.contains("id=\"displaySubtotal\""));
        assertTrue(renderedHtml.contains("id=\"displayTravel\""));
        assertTrue(renderedHtml.contains("id=\"displayOther\""));
        assertTrue(renderedHtml.contains("id=\"displayDiscount\""));
        assertTrue(renderedHtml.contains("id=\"displayGstAmount\""));
        assertTrue(renderedHtml.contains("id=\"displayTotalAmount\""));
        assertTrue(renderedHtml.contains("id=\"displayOfferAmount\""));
        assertTrue(renderedHtml.contains("id=\"displayGrandTotal\""));

        // Must still satisfy the legacy test assertion trap
        assertFalse(renderedHtml.contains("Quote Summary"));
        assertFalse(renderedHtml.contains("Final Details"));

        // Also verify admin template
        String adminHtml = templateEngine.process("admin/quotation-builder", context);
        assertNotNull(adminHtml);
        assertFalse(adminHtml.contains("id=\"accessoriesAmount\""), "Accessories input must be removed from admin quotation creation");
        assertFalse(adminHtml.contains("id=\"displayAccessories\""), "Accessories display row must be removed from admin quotation creation");
        assertFalse(adminHtml.contains("id=\"printPdfBtn\""), "Print / Save PDF button must be removed from admin quotation creation");
        assertTrue(adminHtml.contains("id=\"travelCharge\""));
        assertTrue(adminHtml.contains("id=\"saveQuoteBtn\""));
        assertTrue(adminHtml.contains("id=\"clearQuoteBtn\""));
    }
}
