package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminController;
import com.example.pravin_quotation.controller.EmployeeDashboardController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.AdminDashboardService;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DashboardCardsLayoutTest {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private AdminController adminController;

    @Autowired
    private EmployeeDashboardService employeeDashboardService;

    @Autowired
    private EmployeeDashboardController employeeDashboardController;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ITemplateEngine templateEngine;

    @Test
    void testAdminDashboardMetricsAndTemplateRendering() {
        long ts = System.currentTimeMillis();

        Branch branch1 = new Branch();
        branch1.setName("Branch-Alpha-" + ts);
        branch1.setBranchAddress("Address 1");
        branch1 = branchRepository.save(branch1);

        Branch branch2 = new Branch();
        branch2.setName("Branch-Beta-" + ts);
        branch2.setBranchAddress("Address 2");
        branch2 = branchRepository.save(branch2);

        Customer cust1 = new Customer();
        cust1.setName("Customer-One-" + ts);
        cust1.setPhone("9876543201");
        cust1.setBranch(branch1);
        cust1 = customerRepository.save(cust1);

        Customer cust2 = new Customer();
        cust2.setName("Customer-Two-" + ts);
        cust2.setPhone("9876543202");
        cust2.setBranch(branch2);
        cust2 = customerRepository.save(cust2);

        User emp1 = new User();
        emp1.setName("Staff A");
        emp1.setEmail("staffA_" + ts + "@pravin.com");
        emp1.setPassword("pass");
        emp1.setRole(Role.EMPLOYEE);
        emp1.setBranch(branch1);
        emp1 = userRepository.save(emp1);

        LocalDate today = LocalDate.now();

        // Quotations setup:
        // Q1: 10 days ago (within 60 days, within 1 year), GrandTotal = 10,000, PENDING
        createQuotation("Q-ADM-1-" + ts, emp1, branch1, cust1, today.minusDays(10), QuotationStatus.PENDING, new BigDecimal("10000.00"));

        // Q2: 30 days ago (within 60 days, within 1 year), GrandTotal = 25,000, WORKING
        createQuotation("Q-ADM-2-" + ts, emp1, branch1, cust1, today.minusDays(30), QuotationStatus.WORKING, new BigDecimal("25000.00"));

        // Q3: 50 days ago (within 60 days, within 1 year), GrandTotal = 40,000, SENT
        createQuotation("Q-ADM-3-" + ts, emp1, branch1, cust2, today.minusDays(50), QuotationStatus.SENT, new BigDecimal("40000.00"));

        // Q4: 100 days ago (outside 60 days, within 1 year), GrandTotal = 60,000, SENT
        createQuotation("Q-ADM-4-" + ts, emp1, branch2, cust2, today.minusDays(100), QuotationStatus.SENT, new BigDecimal("60000.00"));

        // Q5: 400 days ago (outside 1 year), GrandTotal = 50,000, SENT
        createQuotation("Q-ADM-5-" + ts, emp1, branch2, cust2, today.minusDays(400), QuotationStatus.SENT, new BigDecimal("50000.00"));

        // 1. Verify AdminDashboardService methods
        long totalEmployees = adminDashboardService.getTotalEmployees();
        long totalBranches = adminDashboardService.getTotalBranches();
        long oneYearQuotations = adminDashboardService.getOneYearQuotationCount();
        long sixtyDaysQuotations = adminDashboardService.getLast60DaysQuotationCount();
        BigDecimal sixtyDaysQuotedValue = adminDashboardService.getLast60DaysQuotedValue();
        long oneYearCustomers = adminDashboardService.getOneYearCustomerCount();
        long pendingQuotations = adminDashboardService.getPendingQuotationCount();
        long workingQuotations = adminDashboardService.getWorkingQuotationCount();
        long sentQuotations = adminDashboardService.getSentQuotationCount();
        BigDecimal totalPortfolioValue = adminDashboardService.getTotalPortfolioValue();

        assertTrue(totalEmployees >= 1);
        assertTrue(totalBranches >= 2);
        assertTrue(oneYearQuotations >= 4);
        assertTrue(sixtyDaysQuotations >= 3);
        assertTrue(sixtyDaysQuotedValue.compareTo(new BigDecimal("75000.00")) >= 0);
        assertTrue(oneYearCustomers >= 2);
        assertTrue(pendingQuotations >= 1);
        assertTrue(workingQuotations >= 1);
        assertTrue(sentQuotations >= 3);
        assertTrue(totalPortfolioValue.compareTo(new BigDecimal("185000.00")) >= 0);

        // 2. Verify AdminController.dashboard populates all 10 model attributes
        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminController.dashboard(model);
        assertEquals("admin/dashboard", view);

        assertEquals(totalEmployees, model.get("totalEmployees"));
        assertEquals(totalBranches, model.get("totalBranches"));
        assertEquals(oneYearQuotations, model.get("oneYearQuotationCount"));
        assertEquals(sixtyDaysQuotations, model.get("last60DaysQuotationCount"));
        assertEquals(sixtyDaysQuotedValue, model.get("last60DaysQuotedValue"));
        assertEquals(oneYearCustomers, model.get("oneYearCustomerCount"));
        assertEquals(pendingQuotations, model.get("pendingQuotationCount"));
        assertEquals(workingQuotations, model.get("workingQuotationCount"));
        assertEquals(sentQuotations, model.get("sentQuotationCount"));
        assertEquals(totalPortfolioValue, model.get("totalPortfolioValue"));

        // 3. Render admin/dashboard HTML and verify the 10 cards and their exact sequence
        String html = renderTemplate("admin/dashboard", model);

        // All 10 card labels are present
        int idxTotalEmployees = html.indexOf("Total Employees");
        int idxTotalBranches = html.indexOf("Total Branches");
        int idxOneYearQuotations = html.indexOf("One Year Quotations");
        int idx60DaysQuotations = html.indexOf("60 Days Quotations");
        int idx60DaysQuotedValue = html.indexOf("60 Days Quoted Value");
        int idxOneYearCustomers = html.indexOf("One Year Customers");
        int idxQuotationsPending = html.indexOf("Quotations Pending");
        int idxQuotationsWorking = html.indexOf("Quotations Working");
        int idxQuotationsSent = html.indexOf("Quotations Sent");
        int idxTotalPortfolioValue = html.indexOf("Total Portfolio Value");

        assertTrue(idxTotalEmployees != -1, "Must contain 'Total Employees'");
        assertTrue(idxTotalBranches != -1, "Must contain 'Total Branches'");
        assertTrue(idxOneYearQuotations != -1, "Must contain 'One Year Quotations'");
        assertTrue(idx60DaysQuotations != -1, "Must contain '60 Days Quotations'");
        assertTrue(idx60DaysQuotedValue != -1, "Must contain '60 Days Quoted Value'");
        assertTrue(idxOneYearCustomers != -1, "Must contain 'One Year Customers'");
        assertTrue(idxQuotationsPending != -1, "Must contain 'Quotations Pending'");
        assertTrue(idxQuotationsWorking != -1, "Must contain 'Quotations Working'");
        assertTrue(idxQuotationsSent != -1, "Must contain 'Quotations Sent'");
        assertTrue(idxTotalPortfolioValue != -1, "Must contain 'Total Portfolio Value'");

        // Verify exact layout ordering matching the hand-drawn layout:
        // Row 1: Total Employees -> Total Branches -> One Year Quotations -> 60 Days Quotations
        assertTrue(idxTotalEmployees < idxTotalBranches, "Card 1 (Total Employees) must precede Card 2 (Total Branches)");
        assertTrue(idxTotalBranches < idxOneYearQuotations, "Card 2 (Total Branches) must precede Card 3 (One Year Quotations)");
        assertTrue(idxOneYearQuotations < idx60DaysQuotations, "Card 3 (One Year Quotations) must precede Card 4 (60 Days Quotations)");

        // Row 2: 60 Days Quoted Value -> One Year Customers -> Quotations Pending -> Quotations Working
        assertTrue(idx60DaysQuotations < idx60DaysQuotedValue, "Card 4 must precede Card 5 (60 Days Quoted Value)");
        assertTrue(idx60DaysQuotedValue < idxOneYearCustomers, "Card 5 must precede Card 6 (One Year Customers)");
        assertTrue(idxOneYearCustomers < idxQuotationsPending, "Card 6 must precede Card 7 (Quotations Pending)");
        assertTrue(idxQuotationsPending < idxQuotationsWorking, "Card 7 must precede Card 8 (Quotations Working)");

        // Row 3: Quotations Sent -> Total Portfolio Value
        assertTrue(idxQuotationsWorking < idxQuotationsSent, "Card 8 must precede Card 9 (Quotations Sent)");
        assertTrue(idxQuotationsSent < idxTotalPortfolioValue, "Card 9 must precede Card 10 (Total Portfolio Value)");

        // Verify navigation behavior
        assertTrue(html.contains("/admin/employees"));
        assertTrue(html.contains("/admin/branches"));
        assertTrue(html.contains("/admin/quotations?period=year") || html.contains("/admin/quotations"));
        assertTrue(html.contains("/admin/customers?period=1year") || html.contains("/admin/customers"));
        assertTrue(html.contains("/admin/quotations/status/PENDING"));
        assertTrue(html.contains("/admin/quotations/status/WORKING"));
        assertTrue(html.contains("/admin/quotations/status/SENT"));
        assertTrue(html.contains("/admin/portfolio"));
    }

    @Test
    void testEmployeeDashboardGreetingCardRemovedAndCardSequence() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Branch-Emp-" + ts);
        branch.setBranchAddress("Address Emp");
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer-Emp-" + ts);
        customer.setPhone("9876543209");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        User employee = new User();
        employee.setName("Anish Kumar");
        employee.setEmail("anish_" + ts + "@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee = userRepository.save(employee);

        LocalDate today = LocalDate.now();

        createQuotation("Q-ANISH-1-" + ts, employee, branch, customer, today.minusDays(5), QuotationStatus.PENDING, new BigDecimal("15000.00"));
        createQuotation("Q-ANISH-2-" + ts, employee, branch, customer, today.minusDays(15), QuotationStatus.WORKING, new BigDecimal("35000.00"));
        createQuotation("Q-ANISH-3-" + ts, employee, branch, customer, today.minusDays(45), QuotationStatus.SENT, new BigDecimal("55000.00"));

        Authentication auth = new UsernamePasswordAuthenticationToken(employee.getEmail(), "secret");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = employeeDashboardController.dashboard(auth, model);
        assertEquals("employee/dashboard", view);

        // Verify model attributes
        assertEquals(3L, model.get("last60DaysQuotations"));
        assertEquals(3L, model.get("oneYearQuotations"));
        assertEquals(1L, model.get("pendingQuotations"));
        assertEquals(1L, model.get("workingQuotations"));
        assertEquals(1L, model.get("sentQuotations"));

        // Render template
        String html = renderTemplate("employee/dashboard", model);

        // 1. Verify "Hello, Anish" greeting card is completely removed
        assertFalse(html.contains("welcome-card"), "The welcome-card section must be completely removed");
        assertFalse(html.contains("Welcome back"), "The 'Welcome back' greeting text must be completely removed");
        assertFalse(html.contains("Hello,"), "The 'Hello, <name>' greeting must be completely removed");

        // But employee details in the header profile must still be present
        assertTrue(html.contains("Anish Kumar"), "Top bar employee profile must still display employee name");
        assertTrue(html.contains("anish_" + ts + "@pravin.com"), "Top bar employee profile must display employee email");

        // 2. Verify all 5 cards in the exact requested order:
        // 1. Total One Year Quotations
        // 2. 60 Days Quotations
        // 3. Pending Quotations
        // 4. Working Quotations
        // 5. Sent Quotations
        int idxOneYear = html.indexOf("Total One Year Quotations");
        int idx60Days = html.indexOf("60 Days Quotations");
        int idxPending = html.indexOf("Pending Quotations");
        int idxWorking = html.indexOf("Working Quotations");
        int idxSent = html.indexOf("Sent Quotations");

        assertTrue(idxOneYear != -1, "Must contain 'Total One Year Quotations'");
        assertTrue(idx60Days != -1, "Must contain '60 Days Quotations'");
        assertTrue(idxPending != -1, "Must contain 'Pending Quotations'");
        assertTrue(idxWorking != -1, "Must contain 'Working Quotations'");
        assertTrue(idxSent != -1, "Must contain 'Sent Quotations'");

        assertTrue(idxOneYear < idx60Days, "Card 1 (Total One Year Quotations) must precede Card 2 (60 Days Quotations)");
        assertTrue(idx60Days < idxPending, "Card 2 (60 Days Quotations) must precede Card 3 (Pending Quotations)");
        assertTrue(idxPending < idxWorking, "Card 3 (Pending Quotations) must precede Card 4 (Working Quotations)");
        assertTrue(idxWorking < idxSent, "Card 4 (Working Quotations) must precede Card 5 (Sent Quotations)");
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

    private Quotation createQuotation(String number, User emp, Branch branch, Customer cust, LocalDate date, QuotationStatus status, BigDecimal amount) {
        Quotation q = new Quotation();
        q.setQuotationNumber(number);
        q.setEmployee(emp);
        q.setBranch(branch);
        q.setCustomer(cust);
        q.setQuotationDate(date);
        q.setStatus(status);
        q.setPricingMode(PricingMode.ECONOMY);
        q.setGrandTotal(amount);
        q.setSubtotal(amount);
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        return quotationRepository.save(q);
    }
}
