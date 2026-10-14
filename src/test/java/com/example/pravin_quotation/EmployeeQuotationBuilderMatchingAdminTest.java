package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeQuotationBuilderMatchingAdminTest {

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

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
    private ITemplateEngine templateEngine;

    private User employeeA;
    private User employeeB;
    private Branch branch;
    private Authentication authA;
    private Authentication authB;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branch = new Branch();
        branch.setName("Branch-" + ts);
        branch.setBranchAddress("Building 123");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        employeeA = new User();
        employeeA.setName("Emp Alpha " + ts);
        employeeA.setEmail("alpha_" + ts + "@test.com");
        employeeA.setPassword("secret");
        employeeA.setRole(Role.EMPLOYEE);
        employeeA.setBranch(branch);
        employeeA.setActive(true);
        employeeA = userRepository.save(employeeA);

        employeeB = new User();
        employeeB.setName("Emp Beta " + ts);
        employeeB.setEmail("beta_" + ts + "@test.com");
        employeeB.setPassword("secret");
        employeeB.setRole(Role.EMPLOYEE);
        employeeB.setBranch(branch);
        employeeB.setActive(true);
        employeeB = userRepository.save(employeeB);

        authA = new UsernamePasswordAuthenticationToken(
                employeeA.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        authB = new UsernamePasswordAuthenticationToken(
                employeeB.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
    }

    @Test
    void testEmployeeQuotationBuilderPageRenderingAndModelAttributes() {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = employeeQuotationController.builder(null, null, authA, model);

        assertEquals("employee/quotation-builder", view);
        assertNotNull(model.get("employee"));
        assertNotNull(model.get("quotation"));
        assertNotNull(model.get("branches"));
        assertNotNull(model.get("workCategories"));
        assertNotNull(model.get("pricingModes"));
        assertNotNull(model.get("today"));
        assertEquals(branch.getId(), model.get("selectedBranchId"));

        // Render template through Thymeleaf to verify HTML and JS expressions compile
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var servletApp = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var webExchange = servletApp.buildExchange(request, response);
        WebContext context = new WebContext(webExchange, request.getLocale(), model.asMap());

        String renderedHtml = templateEngine.process("employee/quotation-builder", context);
        assertNotNull(renderedHtml);
        assertTrue(renderedHtml.contains("/employee/quotations"));
        assertTrue(renderedHtml.contains("quotations\\/builder") || renderedHtml.contains("quotations/builder"));
        assertTrue(renderedHtml.contains("Customer Details"));
        assertTrue(renderedHtml.contains("Pricing Mode"));
        assertTrue(renderedHtml.contains("Floor &amp; Room") || renderedHtml.contains("Floor & Room"));
        assertTrue(renderedHtml.contains("Which Room Are You Working On?"));
        assertTrue(renderedHtml.contains("Material &amp; Size") || renderedHtml.contains("Material & Size"));
        assertTrue(renderedHtml.contains("Quote Summary"));
        assertTrue(renderedHtml.contains("Final Details"));
        assertTrue(renderedHtml.contains("admin-quotation-builder.css"));
    }

    @Test
    void testEmployeeQuotationBuilderHierarchyEndpoints() {
        WorkCategory cat = new WorkCategory();
        cat.setName("TestCategory");
        cat.setActive(true);
        cat.setDisplayOrder(1);
        cat = workCategoryRepository.save(cat);

        Division div = new Division();
        div.setName("TestDivision");
        div.setWorkCategory(cat);
        div.setActive(true);
        div.setDisplayOrder(1);
        div = divisionRepository.save(div);

        Item item = new Item();
        item.setName("TestItem");
        item.setDivision(div);
        item.setActive(true);
        item.setDisplayOrder(1);
        item = itemRepository.save(item);

        Material mat = new Material();
        mat.setName("TestMaterial");
        mat.setItem(item);
        mat.setActive(true);
        mat.setDisplayOrder(1);
        mat = materialRepository.save(mat);

        MaterialOption opt = new MaterialOption();
        opt.setName("TestOption");
        opt.setMaterial(mat);
        opt.setActive(true);
        opt.setDisplayOrder(1);
        opt = materialOptionRepository.save(opt);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(opt);
        pricing.setPricingMode(PricingMode.MIDDLE);
        pricing.setRate(new BigDecimal("450.00"));
        pricing.setFormula("(L + W) * H");
        pricing.setUnit("SQFT");
        pricing.setActive(true);
        pricingRepository.save(pricing);

        // Verify divisions endpoint
        List<Map<String, Object>> divisions = employeeQuotationController.builderDivisions(cat.getId());
        assertFalse(divisions.isEmpty());
        assertEquals("TESTDIVISION", divisions.get(0).get("name"));

        // Verify items endpoint
        List<Map<String, Object>> items = employeeQuotationController.builderItems(div.getId());
        assertFalse(items.isEmpty());
        assertEquals("TESTITEM", items.get(0).get("name"));

        // Verify materials endpoint
        List<Map<String, Object>> materials = employeeQuotationController.builderMaterials(item.getId());
        assertFalse(materials.isEmpty());
        assertEquals("TESTMATERIAL", materials.get(0).get("name"));

        // Verify material options endpoint
        List<Map<String, Object>> options = employeeQuotationController.builderMaterialOptions(mat.getId());
        assertFalse(options.isEmpty());
        assertEquals("TESTOPTION", options.get(0).get("name"));

        // Verify rate endpoint
        ResponseEntity<?> rateResp = employeeQuotationController.builderRate(branch.getId(), opt.getId(), PricingMode.MIDDLE);
        assertEquals(200, rateResp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> rateMap = (Map<String, Object>) rateResp.getBody();
        assertNotNull(rateMap);
        assertEquals(new BigDecimal("450.00"), rateMap.get("rate"));
        assertEquals("(L + W) * H", rateMap.get("formula"));
        assertEquals("SQFT", rateMap.get("unit"));
    }

    @Test
    void testEmployeeQuotationBuilderSaveMultiRoomAndAssignsLoggedInEmployee() {
        WorkCategory cat = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory c = new WorkCategory();
            c.setName("Living");
            c.setActive(true);
            return workCategoryRepository.save(c);
        });

        Division div = divisionRepository.findAll().stream().findFirst().orElseGet(() -> {
            Division d = new Division();
            d.setName("Cabinetry");
            d.setWorkCategory(cat);
            d.setActive(true);
            return divisionRepository.save(d);
        });

        Item item = itemRepository.findAll().stream().findFirst().orElseGet(() -> {
            Item i = new Item();
            i.setName("Base Unit");
            i.setDivision(div);
            i.setActive(true);
            return itemRepository.save(i);
        });

        Material mat = materialRepository.findAll().stream().findFirst().orElseGet(() -> {
            Material m = new Material();
            m.setName("Plywood");
            m.setItem(item);
            m.setActive(true);
            return materialRepository.save(m);
        });

        MaterialOption opt = materialOptionRepository.findAll().stream().findFirst().orElseGet(() -> {
            MaterialOption o = new MaterialOption();
            o.setName("710 Marine");
            o.setMaterial(mat);
            o.setActive(true);
            return materialOptionRepository.save(o);
        });

        pricingRepository.findByMaterialOptionIdAndPricingMode(opt.getId(), PricingMode.MIDDLE)
                .orElseGet(() -> {
                    Pricing p = new Pricing();
                    p.setMaterialOption(opt);
                    p.setPricingMode(PricingMode.MIDDLE);
                    p.setRate(new BigDecimal("350.00"));
                    p.setFormula("(L + W) * H");
                    p.setUnit("SQFT");
                    p.setActive(true);
                    return pricingRepository.save(p);
                });

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("24"), new BigDecimal("36"), new BigDecimal("30"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq1 = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), item.getId(), mat.getId(), opt.getId(),
                "Unit 1", "Specification 1", List.of(size1)
        );
        AdminQuotationBuilderService.RoomRequest roomReq1 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Kitchen Area", List.of(itemReq1)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Employee Cust", "9876543211", "empcust@example.com", "City Center"
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE, "Customer requirements", "Internal notes",
                List.of(roomReq1)
        );

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = employeeQuotationController.saveBuilderQuotation(builderReq, authA, redirectAttributes);

        assertTrue(result.startsWith("redirect:/employee/quotations/view/"));

        String qIdStr = result.substring("redirect:/employee/quotations/view/".length());
        Long quotationId = Long.parseLong(qIdStr);

        Quotation saved = quotationRepository.findById(quotationId).orElseThrow();
        assertEquals(employeeA.getId(), saved.getEmployee().getId(), "Quotation must be assigned to logged-in employee");
        assertEquals("Employee Cust", saved.getCustomer().getName());
        assertEquals(PricingMode.MIDDLE, saved.getPricingMode());
        assertTrue(saved.getSubtotal().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(QuotationStatus.WORKING, saved.getStatus());
    }

    @Test
    void testEmployeeQuotationBuilderLoadAndOwnershipSecurity() {
        Customer customer = new Customer();
        customer.setName("Owner Customer");
        customer.setPhone("9876500000");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        Quotation qA = new Quotation();
        qA.setQuotationNumber("QT-OWN-001");
        qA.setCustomer(customer);
        qA.setEmployee(employeeA);
        qA.setBranch(branch);
        qA.setQuotationDate(LocalDate.now());
        qA.setPricingMode(PricingMode.MIDDLE);
        qA.setStatus(QuotationStatus.PENDING);
        final Quotation savedQA = quotationRepository.save(qA);

        // Employee A can load their own quotation
        ResponseEntity<?> loadResponseA = employeeQuotationController.builderLoadQuotation(savedQA.getId(), authA);
        assertEquals(200, loadResponseA.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) loadResponseA.getBody();
        assertNotNull(data);
        assertEquals("QT-OWN-001", data.get("quotationNumber"));

        // Employee B attempting to load Employee A's quotation throws IllegalStateException (ownership check)
        assertThrows(IllegalStateException.class, () -> {
            employeeQuotationController.builderLoadQuotation(savedQA.getId(), authB);
        });

        // Redirect routes
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String redirectResultOwner = employeeQuotationController.quotationBuilder(savedQA.getId(), authA, ra);
        assertEquals("redirect:/employee/quotations/builder?quotationId=" + savedQA.getId(), redirectResultOwner);

        // Unauthorized employee is safely redirected to list with error
        RedirectAttributesModelMap raOther = new RedirectAttributesModelMap();
        String redirectResultOther = employeeQuotationController.quotationBuilder(savedQA.getId(), authB, raOther);
        assertEquals("redirect:/employee/quotations", redirectResultOther);
        assertNotNull(raOther.getFlashAttributes().get("errorMessage"));
    }
}
