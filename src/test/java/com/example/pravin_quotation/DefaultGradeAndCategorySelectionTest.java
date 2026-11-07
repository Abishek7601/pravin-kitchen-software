package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DefaultGradeAndCategorySelectionTest {

    @Autowired
    private ITemplateEngine templateEngine;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private User employee;
    private Authentication auth;
    private Branch branch;

    @BeforeEach
    void setUp() {
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Test Branch");
            b.setActive(true);
            return branchRepository.save(b);
        });

        employee = userRepository.findByEmail("emp_grade_cat_test@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Test Employee");
            u.setEmail("emp_grade_cat_test@pravin.com");
            u.setPassword("encoded");
            u.setRole(Role.EMPLOYEE);
            u.setActive(true);
            u.setBranch(branch);
            return userRepository.save(u);
        });

        auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
    }

    private String renderTemplate(String templateName, Map<String, Object> modelMap) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var servletApp = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var webExchange = servletApp.buildExchange(request, response);
        WebContext context = new WebContext(webExchange, request.getLocale(), modelMap);
        return templateEngine.process(templateName, context);
    }

    @Test
    @DisplayName("1. Admin New Quotation: First grade in pricingModes is checked by default, not hardcoded Middle")
    void testAdminNewQuotationFirstGradeSelectedByDefault() {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminQuotationBuilderController.builder(null, null, null, null, null, null, model);
        assertEquals("admin/quotation-builder", view);

        @SuppressWarnings("unchecked")
        PricingMode[] pricingModes = (PricingMode[]) model.get("pricingModes");
        assertNotNull(pricingModes);
        assertTrue(pricingModes.length > 0);
        PricingMode expectedFirstGrade = pricingModes[0]; // PREMIUM

        String renderedHtml = renderTemplate("admin/quotation-builder", model.asMap());

        // Extract checked radio button for pricingMode
        Pattern checkedPattern = Pattern.compile("<input[^>]*name=\"pricingMode\"[^>]*value=\"([^\"]+)\"[^>]*checked[^>]*>");
        Matcher matcher = checkedPattern.matcher(renderedHtml);
        assertTrue(matcher.find(), "Must find a checked pricingMode radio button");
        assertEquals(expectedFirstGrade.name(), matcher.group(1),
                "First configured grade (" + expectedFirstGrade.name() + ") must be checked by default in Admin New Quotation");

        // Verify MIDDLE is NOT checked by default
        Pattern middleChecked = Pattern.compile("<input[^>]*name=\"pricingMode\"[^>]*value=\"MIDDLE\"[^>]*checked[^>]*>");
        assertFalse(middleChecked.matcher(renderedHtml).find(), "MIDDLE must not be checked by default when PREMIUM is first");
    }

    @Test
    @DisplayName("2. Employee New Quotation: First grade in pricingModes is checked by default and controller sets first grade")
    void testEmployeeNewQuotationFirstGradeSelectedByDefault() {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = employeeQuotationController.builder(null, null, auth, model);
        assertEquals("employee/quotation-builder", view);

        Quotation q = (Quotation) model.get("quotation");
        assertNotNull(q);
        assertEquals(PricingMode.values()[0], q.getPricingMode(),
                "EmployeeQuotationController must set first grade on new quotation model");

        @SuppressWarnings("unchecked")
        PricingMode[] pricingModes = (PricingMode[]) model.get("pricingModes");
        assertNotNull(pricingModes);
        PricingMode expectedFirstGrade = pricingModes[0];

        String renderedHtml = renderTemplate("employee/quotation-builder", model.asMap());

        Pattern checkedPattern = Pattern.compile("<input[^>]*name=\"pricingMode\"[^>]*value=\"([^\"]+)\"[^>]*checked[^>]*>");
        Matcher matcher = checkedPattern.matcher(renderedHtml);
        assertTrue(matcher.find(), "Must find a checked pricingMode radio button in Employee builder");
        assertEquals(expectedFirstGrade.name(), matcher.group(1),
                "First configured grade (" + expectedFirstGrade.name() + ") must be checked by default in Employee New Quotation");
    }

    @Test
    @DisplayName("3. Dynamic Grade Order: Changing grade order renders new first grade as checked")
    void testDynamicGradeOrderSelectsNewFirstGrade() {
        ExtendedModelMap model = new ExtendedModelMap();
        adminQuotationBuilderController.builder(null, null, null, null, null, null, model);

        // Override pricingModes order to: ECONOMY -> MIDDLE -> PREMIUM
        List<PricingMode> customOrder = List.of(PricingMode.ECONOMY, PricingMode.MIDDLE, PricingMode.PREMIUM);
        model.addAttribute("pricingModes", customOrder);

        String renderedHtml = renderTemplate("admin/quotation-builder", model.asMap());

        Pattern checkedPattern = Pattern.compile("<input[^>]*name=\"pricingMode\"[^>]*value=\"([^\"]+)\"[^>]*checked[^>]*>");
        Matcher matcher = checkedPattern.matcher(renderedHtml);
        assertTrue(matcher.find());
        assertEquals("ECONOMY", matcher.group(1),
                "When grade order is ECONOMY -> MIDDLE -> PREMIUM, ECONOMY must automatically be checked by default");
    }

    @Test
    @DisplayName("4. Target Grade Preservation: When target grade is specified, that grade is checked")
    void testTargetGradeIsPreservedWhenSpecified() {
        ExtendedModelMap model = new ExtendedModelMap();
        adminQuotationBuilderController.builder(null, null, null, 100L, "MIDDLE", null, model);

        String renderedHtml = renderTemplate("admin/quotation-builder", model.asMap());

        Pattern checkedPattern = Pattern.compile("<input[^>]*name=\"pricingMode\"[^>]*value=\"([^\"]+)\"[^>]*checked[^>]*>");
        Matcher matcher = checkedPattern.matcher(renderedHtml);
        assertTrue(matcher.find());
        assertEquals("MIDDLE", matcher.group(1),
                "When targetGrade is MIDDLE (e.g. multi-grade quotation), MIDDLE must be checked");
    }

    @Test
    @DisplayName("5. Category Selection Script: initInitialCategory selects first category, not hardcoded MODULAR KITCHEN")
    void testCategorySelectionScriptDoesNotHardcodeModularKitchen() throws Exception {
        String adminHtml = Files.readString(Paths.get("src/main/resources/templates/admin/quotation-builder.html"));
        String empHtml = Files.readString(Paths.get("src/main/resources/templates/employee/quotation-builder.html"));

        for (String content : List.of(adminHtml, empHtml)) {
            // Must not have hardcoded MODULAR KITCHEN in initInitialCategory
            assertFalse(content.contains("includes('MODULAR KITCHEN')"),
                    "Must not hardcode MODULAR KITCHEN search in initInitialCategory");
            assertFalse(content.contains("includes('KITCHEN')"),
                    "Must not hardcode KITCHEN search in initInitialCategory");

            // Must select the first category button
            assertTrue(content.contains("const target = catButtons[0];"),
                    "Must select the first category button catButtons[0]");
            assertTrue(content.contains("getFirstConfiguredGrade"),
                    "Must dynamically determine first configured grade via getFirstConfiguredGrade()");
        }
    }

    @Test
    @DisplayName("6. Existing / Pending Quotation Load: Preserves previously saved grade and category")
    void testExistingQuotationPreservesSavedGradeAndCategory() {
        Customer cust = new Customer();
        cust.setName("Saved Customer");
        cust.setPhone("9876543210");
        cust.setBranch(branch);
        cust.setActive(true);
        cust = customerRepository.save(cust);

        Quotation q = new Quotation();
        q.setQuotationNumber("QT-TEST-SAVED");
        q.setQuotationDate(LocalDate.now());
        q.setCustomer(cust);
        q.setBranch(branch);
        q.setEmployee(employee);
        q.setPricingMode(PricingMode.ECONOMY); // Specifically ECONOMY
        q.setStatus(QuotationStatus.PENDING);
        q.setLastActiveFloor("2nd Floor");
        q.setLastActiveRoom("Room 3");
        q.setLastActiveCategoryId(99L);
        q.setLastActiveDivisionId(101L);
        q.setSubtotal(BigDecimal.valueOf(5000));
        q.setGrandTotal(BigDecimal.valueOf(5000));
        q = quotationRepository.save(q);

        // Load quotation via admin controller load endpoint
        org.springframework.http.ResponseEntity<?> resp = adminQuotationBuilderController.loadQuotation(q.getId());
        assertNotNull(resp);
        @SuppressWarnings("unchecked")
        Map<String, Object> loaded = (Map<String, Object>) resp.getBody();
        assertNotNull(loaded);
        assertEquals("ECONOMY", loaded.get("pricingMode"), "Must preserve saved ECONOMY grade");
        assertEquals("2nd Floor", loaded.get("lastActiveFloor"), "Must preserve saved floor");
        assertEquals("Room 3", loaded.get("lastActiveRoom"), "Must preserve saved room");
        assertEquals(99L, loaded.get("lastActiveCategoryId"), "Must preserve saved category id");
        assertEquals(101L, loaded.get("lastActiveDivisionId"), "Must preserve saved division id");

        // Repeat for employee load endpoint
        org.springframework.http.ResponseEntity<?> empResp = employeeQuotationController.builderLoadQuotation(q.getId(), auth);
        assertNotNull(empResp);
        @SuppressWarnings("unchecked")
        Map<String, Object> empLoaded = (Map<String, Object>) empResp.getBody();
        assertNotNull(empLoaded);
        assertEquals("ECONOMY", empLoaded.get("pricingMode"), "Employee portal must preserve saved ECONOMY grade");
        assertEquals("2nd Floor", empLoaded.get("lastActiveFloor"), "Employee portal must preserve saved floor");
        assertEquals("Room 3", empLoaded.get("lastActiveRoom"), "Employee portal must preserve saved room");
        assertEquals(99L, empLoaded.get("lastActiveCategoryId"), "Employee portal must preserve saved category id");
    }

    @Test
    @DisplayName("7. Stale Draft Protection: Script protects new quotations from stale drafts")
    void testStaleDraftProtectionInTemplates() throws Exception {
        String adminHtml = Files.readString(Paths.get("src/main/resources/templates/admin/quotation-builder.html"));
        String empHtml = Files.readString(Paths.get("src/main/resources/templates/employee/quotation-builder.html"));

        for (String content : List.of(adminHtml, empHtml)) {
            // Verify isCompletelyNewQuotation check exists
            assertTrue(content.contains("const isCompletelyNewQuotation = !initialQuotationId && !initialParentId && !initialTargetGrade;"),
                    "Must define isCompletelyNewQuotation guard");

            // Verify restore draft guards against new quotation without saved material return
            assertTrue(content.contains("if (isCompletelyNewQuotation && !hasSavedReturn)"),
                    "restoreQuotationBuilderDraftIfPresent must guard against stale drafts on new quotations");
        }
    }
}
