package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeDashboardController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.EmployeeDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeDashboardSummaryCardsTest {

    @Autowired
    private EmployeeDashboardService employeeDashboardService;

    @Autowired
    private EmployeeDashboardController employeeDashboardController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ITemplateEngine templateEngine;

    @Test
    void testEmployeeDashboardServiceCountsAndScoping() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Branch-" + ts);
        branch.setBranchAddress("Test Address");
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer-" + ts);
        customer.setPhone("9876543210");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        // Employee A
        User empA = new User();
        empA.setName("Employee Alpha");
        empA.setEmail("alpha_" + ts + "@pravin.com");
        empA.setPassword("secret");
        empA.setRole(Role.EMPLOYEE);
        empA.setBranch(branch);
        empA = userRepository.save(empA);

        // Employee B
        User empB = new User();
        empB.setName("Employee Beta");
        empB.setEmail("beta_" + ts + "@pravin.com");
        empB.setPassword("secret");
        empB.setRole(Role.EMPLOYEE);
        empB.setBranch(branch);
        empB = userRepository.save(empB);

        LocalDate today = LocalDate.now();

        // Quotations for Employee A:
        createQuotation("Q-A1-" + ts, empA, branch, customer, today.minusDays(10), QuotationStatus.PENDING);
        createQuotation("Q-A2-" + ts, empA, branch, customer, today.minusDays(40), QuotationStatus.WORKING);
        createQuotation("Q-A3-" + ts, empA, branch, customer, today.minusDays(59), QuotationStatus.SENT);
        createQuotation("Q-A4-" + ts, empA, branch, customer, today.minusDays(120), QuotationStatus.SENT);
        createQuotation("Q-A5-" + ts, empA, branch, customer, today.minusDays(400), QuotationStatus.PENDING);

        // Quotations for Employee B:
        createQuotation("Q-B1-" + ts, empB, branch, customer, today.minusDays(5), QuotationStatus.SENT);
        createQuotation("Q-B2-" + ts, empB, branch, customer, today.minusDays(20), QuotationStatus.PENDING);

        // Verify Employee A counts
        assertEquals(3, employeeDashboardService.getLast60DaysQuotationCount(empA.getId()));
        assertEquals(4, employeeDashboardService.getOneYearQuotationCount(empA.getId()));
        assertEquals(2, employeeDashboardService.getPendingQuotationCount(empA.getId()));
        assertEquals(1, employeeDashboardService.getWorkingQuotationCount(empA.getId()));
        assertEquals(2, employeeDashboardService.getSentQuotationCount(empA.getId()));

        // Verify Employee B counts are strictly isolated:
        assertEquals(2, employeeDashboardService.getLast60DaysQuotationCount(empB.getId()));
        assertEquals(2, employeeDashboardService.getOneYearQuotationCount(empB.getId()));
        assertEquals(1, employeeDashboardService.getPendingQuotationCount(empB.getId()));
        assertEquals(0, employeeDashboardService.getWorkingQuotationCount(empB.getId()));
        assertEquals(1, employeeDashboardService.getSentQuotationCount(empB.getId()));

        // Employee with 0 quotations
        User empC = new User();
        empC.setName("Employee Zero");
        empC.setEmail("zero_" + ts + "@pravin.com");
        empC.setPassword("secret");
        empC.setRole(Role.EMPLOYEE);
        empC.setBranch(branch);
        empC = userRepository.save(empC);

        assertEquals(0, employeeDashboardService.getLast60DaysQuotationCount(empC.getId()));
        assertEquals(0, employeeDashboardService.getOneYearQuotationCount(empC.getId()));
        assertEquals(0, employeeDashboardService.getPendingQuotationCount(empC.getId()));
        assertEquals(0, employeeDashboardService.getWorkingQuotationCount(empC.getId()));
        assertEquals(0, employeeDashboardService.getSentQuotationCount(empC.getId()));
    }

    @Test
    void testEmployeeDashboardControllerAndRenderingIntegration() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Madurai Branch");
        branch.setBranchAddress("200 West St");
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer MDU");
        customer.setPhone("9876500000");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        User employee = new User();
        employee.setName("Suresh Raina");
        employee.setEmail("dashboard_ctrl_" + ts + "@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee = userRepository.save(employee);

        LocalDate today = LocalDate.now();

        createQuotation("Q-MDU-1-" + ts, employee, branch, customer, today.minusDays(5), QuotationStatus.PENDING);
        createQuotation("Q-MDU-2-" + ts, employee, branch, customer, today.minusDays(15), QuotationStatus.WORKING);
        createQuotation("Q-MDU-3-" + ts, employee, branch, customer, today.minusDays(25), QuotationStatus.SENT);

        Authentication auth = new UsernamePasswordAuthenticationToken(employee.getEmail(), "secret");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = employeeDashboardController.dashboard(auth, model);
        assertEquals("employee/dashboard", viewName);

        // Verify model attributes for 5 summary cards
        assertEquals(3L, model.get("last60DaysQuotations"));
        assertEquals(3L, model.get("oneYearQuotations"));
        assertEquals(1L, model.get("pendingQuotations"));
        assertEquals(1L, model.get("workingQuotations"));
        assertEquals(1L, model.get("sentQuotations"));

        // Render template with the model attributes
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var application = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var exchange = application.buildExchange(request, response);

        WebContext context = new WebContext(exchange);
        model.forEach(context::setVariable);

        String html = templateEngine.process("employee/dashboard", context);

        // 1. All five cards rendered with correct labels
        assertTrue(html.contains("60 Days Quotations"), "Must contain '60 Days Quotations' card");
        assertTrue(html.contains("Total One Year Quotations"), "Must contain 'Total One Year Quotations' card");
        assertTrue(html.contains("Pending Quotations"), "Must contain 'Pending Quotations' card");
        assertTrue(html.contains("Working Quotations"), "Must contain 'Working Quotations' card");
        assertTrue(html.contains("Sent Quotations"), "Must contain 'Sent Quotations' card");

        // 2. Card navigation links contain the filtered status parameters
        assertTrue(html.contains("status=PENDING"), "Pending card must link to status=PENDING");
        assertTrue(html.contains("status=WORKING"), "Working card must link to status=WORKING");
        assertTrue(html.contains("status=SENT"), "Sent card must link to status=SENT");

        // 3. Total Quoted Value card is removed
        assertFalse(html.contains("Total Quoted Value"), "Total Quoted Value card must NOT be present");

        // 4. Secondary Stats section is removed
        assertFalse(html.contains("secondary-stats"), "secondary-stats section must NOT be present");

        // 5. Quick Actions section: View Quotations and Customers cards removed, Create Quotation highlighted
        int quickActionsIdx = html.indexOf("class=\"quick-actions\"");
        assertTrue(quickActionsIdx != -1, "Quick actions container must exist");
        int quickActionsEndIdx = html.indexOf("</section>", quickActionsIdx);
        String quickActionsSection = html.substring(quickActionsIdx, quickActionsEndIdx);

        // Ensure View Quotations and Customers are removed from Quick Actions
        assertFalse(quickActionsSection.contains("View Quotations"), "Quick Actions must NOT contain 'View Quotations'");
        assertFalse(quickActionsSection.contains("Customers"), "Quick Actions must NOT contain 'Customers'");

        // Ensure Create Quotation is present and highlighted
        assertTrue(quickActionsSection.contains("Create Quotation"), "Quick Actions must contain 'Create Quotation'");
        assertTrue(quickActionsSection.contains("primary-action"), "Create Quotation must have 'primary-action' class");
        assertTrue(quickActionsSection.contains("action-cta"), "Create Quotation must have 'action-cta' CTA styling");
        assertTrue(quickActionsSection.contains("/employee/quotations/new"), "Create Quotation must link to /employee/quotations/new");

        // Verify that sidebar and navigation still have Quotations and Customers
        assertTrue(html.contains("/employee/quotations"), "Dashboard must still have quotations link in sidebar / recent list");
        assertTrue(html.contains("/employee/customers"), "Dashboard must still have customers link in sidebar");

        // Verify that Create Quotation destination route loads correctly
        ExtendedModelMap builderModel = new ExtendedModelMap();
        String builderResult = employeeQuotationController.builder(auth, builderModel);
        assertEquals("employee/quotation-builder", builderResult);
        assertNotNull(builderModel.get("quotation"));
        assertNotNull(builderModel.get("branches"));
        assertNotNull(builderModel.get("workCategories"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEmployeeQuotationStatusFilteringAndNavigation() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Coimbatore Branch");
        branch.setBranchAddress("Avinashi Rd");
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer CBE");
        customer.setPhone("9876511111");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        User emp1 = new User();
        emp1.setName("Employee One");
        emp1.setEmail("emp1_" + ts + "@pravin.com");
        emp1.setPassword("secret");
        emp1.setRole(Role.EMPLOYEE);
        emp1.setBranch(branch);
        emp1 = userRepository.save(emp1);

        User emp2 = new User();
        emp2.setName("Employee Two");
        emp2.setEmail("emp2_" + ts + "@pravin.com");
        emp2.setPassword("secret");
        emp2.setRole(Role.EMPLOYEE);
        emp2.setBranch(branch);
        emp2 = userRepository.save(emp2);

        LocalDate today = LocalDate.now();

        // Create quotations for emp1:
        // 2 PENDING, 1 WORKING, 3 SENT
        Quotation p1 = createQuotation("Q-EMP1-P1-" + ts, emp1, branch, customer, today.minusDays(2), QuotationStatus.PENDING);
        Quotation p2 = createQuotation("Q-EMP1-P2-" + ts, emp1, branch, customer, today.minusDays(3), QuotationStatus.PENDING);
        Quotation w1 = createQuotation("Q-EMP1-W1-" + ts, emp1, branch, customer, today.minusDays(4), QuotationStatus.WORKING);
        Quotation s1 = createQuotation("Q-EMP1-S1-" + ts, emp1, branch, customer, today.minusDays(5), QuotationStatus.SENT);
        Quotation s2 = createQuotation("Q-EMP1-S2-" + ts, emp1, branch, customer, today.minusDays(6), QuotationStatus.SENT);
        Quotation s3 = createQuotation("Q-EMP1-S3-" + ts, emp1, branch, customer, today.minusDays(7), QuotationStatus.SENT);

        // Create quotations for emp2:
        // Should NEVER appear in emp1's list
        Quotation otherP = createQuotation("Q-EMP2-P-" + ts, emp2, branch, customer, today.minusDays(2), QuotationStatus.PENDING);
        Quotation otherW = createQuotation("Q-EMP2-W-" + ts, emp2, branch, customer, today.minusDays(3), QuotationStatus.WORKING);
        Quotation otherS = createQuotation("Q-EMP2-S-" + ts, emp2, branch, customer, today.minusDays(4), QuotationStatus.SENT);

        Authentication auth = new UsernamePasswordAuthenticationToken(emp1.getEmail(), "secret");

        final Long emp1Id = emp1.getId();

        // -------------------------------------------------------------
        // 1. FILTER BY PENDING
        // -------------------------------------------------------------
        ExtendedModelMap pendingModel = new ExtendedModelMap();
        String pendingView = employeeQuotationController.quotations(null, "PENDING", null, auth, pendingModel);
        assertEquals("employee/quotations", pendingView);
        assertEquals("Pending Quotations", pendingModel.get("pageTitle"));
        assertEquals("PENDING", pendingModel.get("selectedStatus"));

        List<Quotation> pendingList = (List<Quotation>) pendingModel.get("quotations");
        assertNotNull(pendingList);
        assertEquals(2, pendingList.size(), "Must contain exactly 2 pending quotations for emp1");
        assertTrue(pendingList.stream().allMatch(q -> q.getStatus() == QuotationStatus.PENDING), "All returned quotations must have PENDING status");
        assertTrue(pendingList.stream().allMatch(q -> q.getEmployee().getId().equals(emp1Id)), "Must belong exclusively to emp1");
        assertTrue(pendingList.stream().anyMatch(q -> q.getQuotationNumber().equals(p1.getQuotationNumber())));
        assertTrue(pendingList.stream().anyMatch(q -> q.getQuotationNumber().equals(p2.getQuotationNumber())));
        assertFalse(pendingList.stream().anyMatch(q -> q.getQuotationNumber().equals(w1.getQuotationNumber())), "Must exclude WORKING");
        assertFalse(pendingList.stream().anyMatch(q -> q.getQuotationNumber().equals(s1.getQuotationNumber())), "Must exclude SENT");
        assertFalse(pendingList.stream().anyMatch(q -> q.getQuotationNumber().equals(otherP.getQuotationNumber())), "Must exclude emp2 quotations");

        // Render pending template
        String pendingHtml = renderTemplate("employee/quotations", pendingModel);
        assertTrue(pendingHtml.contains("Pending Quotations"));
        assertTrue(pendingHtml.contains(p1.getQuotationNumber()));
        assertTrue(pendingHtml.contains(p2.getQuotationNumber()));
        assertFalse(pendingHtml.contains(w1.getQuotationNumber()));
        assertFalse(pendingHtml.contains(s1.getQuotationNumber()));
        assertFalse(pendingHtml.contains("Back to Dashboard"));
        assertFalse(pendingHtml.contains("← Back"));

        // -------------------------------------------------------------
        // 2. FILTER BY WORKING
        // -------------------------------------------------------------
        ExtendedModelMap workingModel = new ExtendedModelMap();
        String workingView = employeeQuotationController.quotations(null, "WORKING", null, auth, workingModel);
        assertEquals("employee/quotations", workingView);
        assertEquals("Working Quotations", workingModel.get("pageTitle"));
        assertEquals("WORKING", workingModel.get("selectedStatus"));

        List<Quotation> workingList = (List<Quotation>) workingModel.get("quotations");
        assertNotNull(workingList);
        assertEquals(1, workingList.size(), "Must contain exactly 1 working quotation for emp1");
        assertTrue(workingList.stream().allMatch(q -> q.getStatus() == QuotationStatus.WORKING), "All returned quotations must have WORKING status");
        assertTrue(workingList.stream().allMatch(q -> q.getEmployee().getId().equals(emp1Id)), "Must belong exclusively to emp1");
        assertEquals(w1.getQuotationNumber(), workingList.get(0).getQuotationNumber());
        assertFalse(workingList.stream().anyMatch(q -> q.getQuotationNumber().equals(p1.getQuotationNumber())), "Must exclude PENDING");
        assertFalse(workingList.stream().anyMatch(q -> q.getQuotationNumber().equals(s1.getQuotationNumber())), "Must exclude SENT");
        assertFalse(workingList.stream().anyMatch(q -> q.getQuotationNumber().equals(otherW.getQuotationNumber())), "Must exclude emp2 quotations");

        // Render working template
        String workingHtml = renderTemplate("employee/quotations", workingModel);
        assertTrue(workingHtml.contains("Working Quotations"));
        assertTrue(workingHtml.contains(w1.getQuotationNumber()));
        assertFalse(workingHtml.contains(p1.getQuotationNumber()));
        assertFalse(workingHtml.contains(s1.getQuotationNumber()));
        assertFalse(workingHtml.contains("Back to Dashboard"));
        assertFalse(workingHtml.contains("← Back"));

        // -------------------------------------------------------------
        // 3. FILTER BY SENT
        // -------------------------------------------------------------
        ExtendedModelMap sentModel = new ExtendedModelMap();
        String sentView = employeeQuotationController.quotations(null, "SENT", null, auth, sentModel);
        assertEquals("employee/quotations", sentView);
        assertEquals("Sent Quotations", sentModel.get("pageTitle"));
        assertEquals("SENT", sentModel.get("selectedStatus"));

        List<Quotation> sentList = (List<Quotation>) sentModel.get("quotations");
        assertNotNull(sentList);
        assertEquals(3, sentList.size(), "Must contain exactly 3 sent quotations for emp1");
        assertTrue(sentList.stream().allMatch(q -> q.getStatus() == QuotationStatus.SENT), "All returned quotations must have SENT status");
        assertTrue(sentList.stream().allMatch(q -> q.getEmployee().getId().equals(emp1Id)), "Must belong exclusively to emp1");
        assertTrue(sentList.stream().anyMatch(q -> q.getQuotationNumber().equals(s1.getQuotationNumber())));
        assertTrue(sentList.stream().anyMatch(q -> q.getQuotationNumber().equals(s2.getQuotationNumber())));
        assertTrue(sentList.stream().anyMatch(q -> q.getQuotationNumber().equals(s3.getQuotationNumber())));
        assertFalse(sentList.stream().anyMatch(q -> q.getQuotationNumber().equals(p1.getQuotationNumber())), "Must exclude PENDING");
        assertFalse(sentList.stream().anyMatch(q -> q.getQuotationNumber().equals(w1.getQuotationNumber())), "Must exclude WORKING");
        assertFalse(sentList.stream().anyMatch(q -> q.getQuotationNumber().equals(otherS.getQuotationNumber())), "Must exclude emp2 quotations");

        // Render sent template
        String sentHtml = renderTemplate("employee/quotations", sentModel);
        assertTrue(sentHtml.contains("Sent Quotations"));
        assertTrue(sentHtml.contains(s1.getQuotationNumber()));
        assertFalse(sentHtml.contains(p1.getQuotationNumber()));
        assertFalse(sentHtml.contains(w1.getQuotationNumber()));
        assertFalse(sentHtml.contains("Back to Dashboard"));
        assertFalse(sentHtml.contains("← Back"));

        // -------------------------------------------------------------
        // 4. TEST CASE-INSENSITIVITY & PATH VARIABLE ("working" / "pending")
        // -------------------------------------------------------------
        ExtendedModelMap lowerModel = new ExtendedModelMap();
        employeeQuotationController.quotations("working", null, null, auth, lowerModel);
        assertEquals("Working Quotations", lowerModel.get("pageTitle"));
        assertEquals(1, ((List<?>) lowerModel.get("quotations")).size());

        // -------------------------------------------------------------
        // 5. TEST EMPTY STATE
        // -------------------------------------------------------------
        // emp2 has 0 WORKING quotations
        Authentication auth2 = new UsernamePasswordAuthenticationToken(emp2.getEmail(), "secret");
        emp2.setRole(Role.EMPLOYEE);
        // Delete emp2's working quotation if any
        quotationRepository.delete(otherW);

        ExtendedModelMap emptyModel = new ExtendedModelMap();
        employeeQuotationController.quotations(null, "WORKING", null, auth2, emptyModel);
        List<Quotation> emptyList = (List<Quotation>) emptyModel.get("quotations");
        assertTrue(emptyList.isEmpty(), "Emp2 working list must be empty");
        assertEquals("No working quotations", emptyModel.get("emptyMessageTitle"));

        String emptyHtml = renderTemplate("employee/quotations", emptyModel);
        assertTrue(emptyHtml.contains("No working quotations"));
        assertFalse(emptyHtml.contains("Back to Dashboard"));
        assertFalse(emptyHtml.contains("← Back"));
    }
    @Test
    void testEmployeeDashboardQuickActions() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Trichy Branch");
        branch.setBranchAddress("Main Road");
        branch = branchRepository.save(branch);

        User employee = new User();
        employee.setName("Karthik Raja");
        employee.setEmail("quick_actions_" + ts + "@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee = userRepository.save(employee);

        Authentication auth = new UsernamePasswordAuthenticationToken(employee.getEmail(), "secret");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = employeeDashboardController.dashboard(auth, model);
        assertEquals("employee/dashboard", viewName);

        String html = renderTemplate("employee/dashboard", model);

        // 1. Verify Quick Actions container
        int startIdx = html.indexOf("<div class=\"quick-actions\">");
        assertTrue(startIdx != -1, "Must contain <div class=\"quick-actions\">");
        int endIdx = html.indexOf("</section>", startIdx);
        assertTrue(endIdx != -1, "Must find closing section for Quick Actions");
        String sectionHtml = html.substring(startIdx, endIdx);

        // 2. View Quotations card is removed from Quick Actions
        assertFalse(sectionHtml.contains("View Quotations"), "Quick actions must NOT have 'View Quotations'");

        // 3. Customers card is removed from Quick Actions
        assertFalse(sectionHtml.contains("Customers"), "Quick actions must NOT have 'Customers'");

        // 4. Create Quotation card is present and highlighted as primary action
        assertTrue(sectionHtml.contains("Create Quotation"), "Quick actions must have 'Create Quotation'");
        assertTrue(sectionHtml.contains("primary-action"), "Quick action card must have 'primary-action' class");
        assertTrue(sectionHtml.contains("action-icon"), "Quick action card must have 'action-icon' class");
        assertTrue(sectionHtml.contains("action-badge"), "Quick action card must have 'action-badge' tag");
        assertTrue(sectionHtml.contains("action-cta"), "Quick action card must have 'action-cta' CTA styling");
        assertTrue(sectionHtml.contains("/employee/quotations/new"), "Quick action card must link to /employee/quotations/new");

        // 5. Verify the quotation builder route opens properly
        ExtendedModelMap builderModel = new ExtendedModelMap();
        String result = employeeQuotationController.builder(auth, builderModel);
        assertEquals("employee/quotation-builder", result, "Builder route must return quotation builder view");
        assertNotNull(builderModel.get("quotation"), "Model must contain quotation");
    }

    @Test
    void testNoBackOrBackToDashboardOnEmployeeDashboardQuotationsCustomers() {
        long ts = System.currentTimeMillis();
        Branch branch = new Branch();
        branch.setName("Madurai Branch " + ts);
        branch = branchRepository.save(branch);

        User employee = new User();
        employee.setName("Vimal Raj");
        employee.setEmail("vimal_" + ts + "@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee = userRepository.save(employee);

        Authentication auth = new UsernamePasswordAuthenticationToken(employee.getEmail(), "secret");

        // 1. Employee Dashboard
        ExtendedModelMap dashModel = new ExtendedModelMap();
        employeeDashboardController.dashboard(auth, dashModel);
        String dashHtml = renderTemplate("employee/dashboard", dashModel);
        assertFalse(dashHtml.contains("Back to Dashboard"), "Employee Dashboard must not have Back to Dashboard");
        assertFalse(dashHtml.contains("← Back"), "Employee Dashboard must not have Back button");
        assertTrue(dashHtml.contains("Employee Dashboard"));
        assertTrue(dashHtml.contains("Dashboard")); // sidebar
        assertTrue(dashHtml.contains("Quotations")); // sidebar
        assertTrue(dashHtml.contains("Customers")); // sidebar

        // 2. Employee Quotations
        ExtendedModelMap quotModel = new ExtendedModelMap();
        employeeQuotationController.quotations(null, null, null, auth, quotModel);
        String quotHtml = renderTemplate("employee/quotations", quotModel);
        assertFalse(quotHtml.contains("Back to Dashboard"), "Employee Quotations must not have Back to Dashboard");
        assertFalse(quotHtml.contains("← Back"), "Employee Quotations must not have Back button");
        assertTrue(quotHtml.contains("My Quotations"));
        assertTrue(quotHtml.contains("+ Create Quotation"));
        assertTrue(quotHtml.contains("Dashboard")); // sidebar

        // 3. Employee Customers
        ExtendedModelMap custModel = new ExtendedModelMap();
        custModel.addAttribute("employee", employee);
        custModel.addAttribute("customers", List.of());
        String custHtml = renderTemplate("employee/customers", custModel);
        assertFalse(custHtml.contains("Back to Dashboard"), "Employee Customers must not have Back to Dashboard");
        assertFalse(custHtml.contains("← Back"), "Employee Customers must not have Back button");
        assertFalse(custHtml.contains("page-actions"), "Empty page-actions div should be removed from Customers");
        assertTrue(custHtml.contains("Customers"));
        assertTrue(custHtml.contains("Dashboard")); // sidebar

        // 4. Verify other employee page (e.g., quotation-new) STILL preserves Back buttons
        ExtendedModelMap newQuotModel = new ExtendedModelMap();
        newQuotModel.addAttribute("employee", employee);
        newQuotModel.addAttribute("customer", new Customer());
        newQuotModel.addAttribute("pricingModes", List.of(PricingMode.values()));
        String newQuotHtml = renderTemplate("employee/quotation-new", newQuotModel);
        assertTrue(newQuotHtml.contains("Back to Dashboard"), "Other employee page (quotation-new) must preserve Back to Dashboard");
        assertTrue(newQuotHtml.contains("← Back"), "Other employee page (quotation-new) must preserve Back button");
    }

    private String renderTemplate(String templateName, ExtendedModelMap model) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        org.springframework.security.web.csrf.DefaultCsrfToken csrfToken =
                new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");
        request.setAttribute("_csrf", csrfToken);
        var application = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var exchange = application.buildExchange(request, response);

        WebContext context = new WebContext(exchange);
        context.setVariable("_csrf", csrfToken);
        model.forEach(context::setVariable);
        return templateEngine.process(templateName, context);
    }

    private Quotation createQuotation(String number, User emp, Branch branch, Customer cust, LocalDate date, QuotationStatus status) {
        Quotation q = new Quotation();
        q.setQuotationNumber(number);
        q.setEmployee(emp);
        q.setBranch(branch);
        q.setCustomer(cust);
        q.setQuotationDate(date);
        q.setStatus(status);
        q.setPricingMode(PricingMode.ECONOMY);
        q.setGrandTotal(BigDecimal.valueOf(50000.00));
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        return quotationRepository.save(q);
    }
}
