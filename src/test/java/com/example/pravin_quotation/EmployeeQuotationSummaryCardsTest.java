package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeQuotationSummaryCardsTest {

    @Autowired
    private ITemplateEngine templateEngine;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testEmployeeQuotationSummaryCardsCountCalculation() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Branch-Emp-Test-" + ts);
        branch.setBranchAddress("100 Main St");
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer " + ts);
        customer.setPhone("9876543210");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        // Employee A
        User empA = new User();
        empA.setName("Employee Alpha");
        empA.setEmail("emp_alpha_" + ts + "@test.com");
        empA.setPassword("password");
        empA.setRole(Role.EMPLOYEE);
        empA.setBranch(branch);
        empA.setActive(true);
        empA = userRepository.save(empA);

        // Employee B
        User empB = new User();
        empB.setName("Employee Beta");
        empB.setEmail("emp_beta_" + ts + "@test.com");
        empB.setPassword("password");
        empB.setRole(Role.EMPLOYEE);
        empB.setBranch(branch);
        empB.setActive(true);
        empB = userRepository.save(empB);

        // For Employee A: 2 SENT, 3 PENDING, 1 WORKING
        createQuotation("QT-A-SENT-1-" + ts, branch, customer, empA, QuotationStatus.SENT);
        createQuotation("QT-A-SENT-2-" + ts, branch, customer, empA, QuotationStatus.SENT);
        createQuotation("QT-A-PEND-1-" + ts, branch, customer, empA, QuotationStatus.PENDING);
        createQuotation("QT-A-PEND-2-" + ts, branch, customer, empA, QuotationStatus.PENDING);
        createQuotation("QT-A-PEND-3-" + ts, branch, customer, empA, QuotationStatus.PENDING);
        createQuotation("QT-A-WORK-1-" + ts, branch, customer, empA, QuotationStatus.WORKING);

        // For Employee B: 1 SENT, 0 PENDING, 4 WORKING
        createQuotation("QT-B-SENT-1-" + ts, branch, customer, empB, QuotationStatus.SENT);
        createQuotation("QT-B-WORK-1-" + ts, branch, customer, empB, QuotationStatus.WORKING);
        createQuotation("QT-B-WORK-2-" + ts, branch, customer, empB, QuotationStatus.WORKING);
        createQuotation("QT-B-WORK-3-" + ts, branch, customer, empB, QuotationStatus.WORKING);
        createQuotation("QT-B-WORK-4-" + ts, branch, customer, empB, QuotationStatus.WORKING);

        // 1. Verify counts for Employee A
        long empASent = quotationRepository.countByEmployeeIdAndStatus(empA.getId(), QuotationStatus.SENT);
        long empAPending = quotationRepository.countByEmployeeIdAndStatus(empA.getId(), QuotationStatus.PENDING);
        long empAWorking = quotationRepository.countByEmployeeIdAndStatus(empA.getId(), QuotationStatus.WORKING);

        assertEquals(2, empASent, "Employee A Sent count must be 2");
        assertEquals(3, empAPending, "Employee A Pending count must be 3");
        assertEquals(1, empAWorking, "Employee A Working count must be 1");

        // 2. Verify counts for Employee B (Pending must be 0)
        long empBSent = quotationRepository.countByEmployeeIdAndStatus(empB.getId(), QuotationStatus.SENT);
        long empBPending = quotationRepository.countByEmployeeIdAndStatus(empB.getId(), QuotationStatus.PENDING);
        long empBWorking = quotationRepository.countByEmployeeIdAndStatus(empB.getId(), QuotationStatus.WORKING);

        assertEquals(1, empBSent, "Employee B Sent count must be 1");
        assertEquals(0, empBPending, "Employee B Pending count must be 0 when no quotations are pending");
        assertEquals(4, empBWorking, "Employee B Working count must be 4");

        // 3. Verify counts are strictly employee-specific
        assertNotEquals(empASent, empBSent);
        assertNotEquals(empAPending, empBPending);
        assertNotEquals(empAWorking, empBWorking);
    }

    @Test
    void testStatusChangeUpdatesCountsDynamically() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Branch-Dynamic-" + ts);
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer Dynamic " + ts);
        customer.setPhone("9876543211");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        User emp = new User();
        emp.setName("Employee Dynamic");
        emp.setEmail("emp_dyn_" + ts + "@test.com");
        emp.setPassword("password");
        emp.setRole(Role.EMPLOYEE);
        emp = userRepository.save(emp);

        Quotation q = createQuotation("QT-DYN-" + ts, branch, customer, emp, QuotationStatus.PENDING);

        // Initially: 1 Pending, 0 Working, 0 Sent
        assertEquals(1, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.PENDING));
        assertEquals(0, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.WORKING));
        assertEquals(0, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.SENT));

        // Change to WORKING
        q.setStatus(QuotationStatus.WORKING);
        quotationRepository.save(q);

        assertEquals(0, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.PENDING));
        assertEquals(1, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.WORKING));
        assertEquals(0, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.SENT));

        // Change to SENT
        q.setStatus(QuotationStatus.SENT);
        quotationRepository.save(q);

        assertEquals(0, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.PENDING));
        assertEquals(0, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.WORKING));
        assertEquals(1, quotationRepository.countByEmployeeIdAndStatus(emp.getId(), QuotationStatus.SENT));
    }

    @Test
    void testStatusFilteredQuotationsQuery() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Branch-Filter-" + ts);
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Customer Filter " + ts);
        customer.setPhone("9876543212");
        customer = customerRepository.save(customer);

        User emp = new User();
        emp.setName("Employee Filter");
        emp.setEmail("emp_filter_" + ts + "@test.com");
        emp.setPassword("password");
        emp.setRole(Role.EMPLOYEE);
        emp = userRepository.save(emp);

        createQuotation("QT-F-SENT-" + ts, branch, customer, emp, QuotationStatus.SENT);
        createQuotation("QT-F-PEND-" + ts, branch, customer, emp, QuotationStatus.PENDING);
        createQuotation("QT-F-WORK-" + ts, branch, customer, emp, QuotationStatus.WORKING);

        List<Quotation> sentList = quotationRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(emp.getId(), QuotationStatus.SENT);
        assertEquals(1, sentList.size());
        assertEquals(QuotationStatus.SENT, sentList.get(0).getStatus());

        List<Quotation> pendingList = quotationRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(emp.getId(), QuotationStatus.PENDING);
        assertEquals(1, pendingList.size());
        assertEquals(QuotationStatus.PENDING, pendingList.get(0).getStatus());

        List<Quotation> workingList = quotationRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(emp.getId(), QuotationStatus.WORKING);
        assertEquals(1, workingList.size());
        assertEquals(QuotationStatus.WORKING, workingList.get(0).getStatus());
    }

    @Test
    void testEmployeeDetailsTemplateRendersSummaryCards() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Chennai Central");
        branch = branchRepository.save(branch);

        User emp = new User();
        emp.setName("Pravin Kumar");
        emp.setEmail("pravin_" + ts + "@example.com");
        emp.setPassword("password");
        emp.setRole(Role.EMPLOYEE);
        emp.setBranch(branch);
        emp.setActive(true);
        emp = userRepository.save(emp);

        Customer customer = new Customer();
        customer.setName("Karthik Raja");
        customer.setPhone("9876543215");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        Quotation qSent = createQuotation("QT-VIEW-SENT-" + ts, branch, customer, emp, QuotationStatus.SENT);
        Quotation qPending = createQuotation("QT-VIEW-PEND-" + ts, branch, customer, emp, QuotationStatus.PENDING);
        Quotation qWorking = createQuotation("QT-VIEW-WORK-" + ts, branch, customer, emp, QuotationStatus.WORKING);

        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");

        WebContext context = createWebContext();
        context.setVariable("employee", emp);
        context.setVariable("quotations", List.of(qSent, qPending, qWorking));
        context.setVariable("totalQuotations", 3L);
        context.setVariable("last60Days", 3L);
        context.setVariable("quotedValue", new BigDecimal("35400.00"));
        context.setVariable("sentCount", 1L);
        context.setVariable("pendingCount", 1L);
        context.setVariable("workingCount", 1L);
        context.setVariable("approvedCount", 1L);
        context.setVariable("selectedStatus", null);
        context.setVariable("_csrf", csrfToken);

        String html = templateEngine.process("admin/employee-details", context);
        assertNotNull(html);

        // 1. Employee Quotation Summary Cards (Display-only, dynamic counts visible)
        assertTrue(html.contains("Sent Quotations"), "HTML should display Sent Quotations card");
        assertTrue(html.contains("Pending Quotations"), "HTML should display Pending Quotations card");
        assertTrue(html.contains("Working Quotations"), "HTML should display Working Quotations card");

        // Verify card CSS classes
        assertTrue(html.contains("quotation-status-grid"), "HTML should contain quotation-status-grid section");
        assertTrue(html.contains("sent-card"), "HTML should contain sent-card class");
        assertTrue(html.contains("pending-card"), "HTML should contain pending-card class");
        assertTrue(html.contains("working-card"), "HTML should contain working-card class");
        assertTrue(html.contains("sent-icon"), "HTML should contain sent-icon class");
        assertTrue(html.contains("pending-icon"), "HTML should contain pending-icon class");
        assertTrue(html.contains("working-icon"), "HTML should contain working-icon class");

        // Verify summary cards are display-only (div, not anchor links, no clickable class, no card-arrow)
        assertFalse(html.contains("<a class=\"summary-card sent-card\"")
                || html.contains("<a class=\"summary-card pending-card\"")
                || html.contains("<a class=\"summary-card working-card\""),
                "Quotation summary cards must not be <a> links");
        assertTrue(html.contains("<div class=\"summary-card sent-card\">"), "Sent Quotations card must be a div");
        assertTrue(html.contains("<div class=\"summary-card pending-card\">"), "Pending Quotations card must be a div");
        assertTrue(html.contains("<div class=\"summary-card working-card\">"), "Working Quotations card must be a div");
        assertFalse(html.contains("card-arrow"), "Summary cards must not contain card-arrow indicating clickability");
        assertFalse(html.contains("summary-card clickable"), "Summary cards must not have clickable class");


        // 2. Card alignment (both rows have matching 3-card structure and classes)
        assertTrue(html.contains("summary-grid"), "HTML should contain summary-grid section");
        assertTrue(html.contains("total-card"), "HTML should contain total-card class");
        assertTrue(html.contains("recent-card"), "HTML should contain recent-card class");
        assertTrue(html.contains("value-card"), "HTML should contain value-card class");
        assertTrue(html.contains("Total Quotations"));
        assertTrue(html.contains("Last 60 Days"));
        assertTrue(html.contains("Quoted Value"));

        // 3. Remove Duplicate Edit Employee Button (exactly one Edit Employee button)
        int editCount = 0;
        int idx = 0;
        while ((idx = html.indexOf("Edit Employee", idx)) != -1) {
            editCount++;
            idx += "Edit Employee".length();
        }
        assertEquals(1, editCount, "There must be exactly one Edit Employee button on the page");
        assertTrue(html.contains("class=\"edit-button\""), "Remaining Edit Employee button must be in header actions");
        assertFalse(html.contains("action-edit"), "Duplicate action-edit button must be removed from account actions");

        // 4. Quotation Activity Filter pills remain functional
        assertTrue(html.contains("status-filter-pills"), "HTML should contain status-filter-pills");
        assertTrue(html.contains("filter-pill"), "HTML should contain filter-pill");
        assertTrue(html.contains("/admin/employees/view/" + emp.getId() + "?status=SENT"));
        assertTrue(html.contains("/admin/employees/view/" + emp.getId() + "?status=PENDING"));
        assertTrue(html.contains("/admin/employees/view/" + emp.getId() + "?status=WORKING"));

    }

    private WebContext createWebContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication app = JakartaServletWebApplication.buildApplication(request.getServletContext());
        IWebExchange exchange = app.buildExchange(request, response);
        return new WebContext(exchange);
    }

    private Quotation createQuotation(String qNum, Branch branch, Customer cust, User emp, QuotationStatus status) {
        Quotation q = new Quotation();
        q.setQuotationNumber(qNum);
        q.setBranch(branch);
        q.setCustomer(cust);
        q.setEmployee(emp);
        q.setQuotationDate(LocalDate.now());
        q.setPricingMode(PricingMode.MIDDLE);
        q.setStatus(status);
        q.setSubtotal(new BigDecimal("10000.00"));
        q.setGrandTotal(new BigDecimal("11800.00"));
        return quotationRepository.save(q);
    }
}
