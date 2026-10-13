package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DistrictQuotationActivityTest {

    @Autowired
    private ITemplateEngine templateEngine;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Test
    void testDistrictWiseQuotationCountsAndFiltering() {
        long ts = System.currentTimeMillis();
        // Create 2 branches/districts with unique names
        Branch branch1 = new Branch();
        branch1.setName("TestBranch1-" + ts);
        branch1.setBranchAddress("123 Cross Cut Road, Gandhipuram");
        branch1 = branchRepository.save(branch1);

        Branch branch2 = new Branch();
        branch2.setName("TestBranch2-" + ts);
        branch2.setBranchAddress("45 Anna Nagar");
        branch2 = branchRepository.save(branch2);

        Customer cust1 = new Customer();
        cust1.setName("Ramesh");
        cust1.setPhone("9876543211");
        cust1.setBranch(branch1);
        cust1 = customerRepository.save(cust1);

        Customer cust2 = new Customer();
        cust2.setName("Suresh");
        cust2.setPhone("9876543212");
        cust2.setBranch(branch2);
        cust2 = customerRepository.save(cust2);

        User emp = new User();
        emp.setName("Staff 1");
        emp.setEmail("staff1_district_test@test.com");
        emp.setPassword("password");
        emp.setRole(Role.EMPLOYEE);
        emp = userRepository.save(emp);

        // Branch 1: 1 SENT, 2 PENDING, 1 WORKING
        createQuotation("Q-CBE-001", branch1, cust1, emp, QuotationStatus.SENT);
        createQuotation("Q-CBE-002", branch1, cust1, emp, QuotationStatus.PENDING);
        createQuotation("Q-CBE-003", branch1, cust1, emp, QuotationStatus.PENDING);
        createQuotation("Q-CBE-004", branch1, cust1, emp, QuotationStatus.WORKING);

        // Branch 2: 2 SENT, 1 PENDING, 0 WORKING
        createQuotation("Q-MDU-001", branch2, cust2, emp, QuotationStatus.SENT);
        createQuotation("Q-MDU-002", branch2, cust2, emp, QuotationStatus.SENT);
        createQuotation("Q-MDU-003", branch2, cust2, emp, QuotationStatus.PENDING);

        // Test Branch 1 counts
        long cbeSent = quotationRepository.countByBranchIdAndStatus(branch1.getId(), QuotationStatus.SENT);
        long cbePending = quotationRepository.countByBranchIdAndStatus(branch1.getId(), QuotationStatus.PENDING);
        long cbeWorking = quotationRepository.countByBranchIdAndStatus(branch1.getId(), QuotationStatus.WORKING);

        assertEquals(1, cbeSent, "Branch 1 Sent count should be 1");
        assertEquals(2, cbePending, "Branch 1 Pending count should be 2");
        assertEquals(1, cbeWorking, "Branch 1 Working count should be 1");

        // Test Branch 2 counts
        long mduSent = quotationRepository.countByBranchIdAndStatus(branch2.getId(), QuotationStatus.SENT);
        long mduPending = quotationRepository.countByBranchIdAndStatus(branch2.getId(), QuotationStatus.PENDING);
        long mduWorking = quotationRepository.countByBranchIdAndStatus(branch2.getId(), QuotationStatus.WORKING);

        assertEquals(2, mduSent, "Branch 2 Sent count should be 2");
        assertEquals(1, mduPending, "Branch 2 Pending count should be 1");
        assertEquals(0, mduWorking, "Branch 2 Working count should be 0");

        final Long branch1Id = branch1.getId();
        final Long branch2Id = branch2.getId();

        // Test QuotationService getByBranchIdAndStatus filtering
        List<Quotation> cbePendingList = quotationService.getByBranchIdAndStatus(branch1Id, QuotationStatus.PENDING);
        assertEquals(2, cbePendingList.size());
        assertTrue(cbePendingList.stream().allMatch(q -> q.getBranch().getId().equals(branch1Id)));
        assertTrue(cbePendingList.stream().allMatch(q -> q.getStatus() == QuotationStatus.PENDING));

        List<Quotation> mduSentList = quotationService.getByBranchIdAndStatus(branch2Id, QuotationStatus.SENT);
        assertEquals(2, mduSentList.size());
        assertTrue(mduSentList.stream().allMatch(q -> q.getBranch().getId().equals(branch2Id)));
        assertTrue(mduSentList.stream().allMatch(q -> q.getStatus() == QuotationStatus.SENT));
    }

    @Test
    void testQuotationDeletionCascadesProperly() {
        Branch branch = new Branch();
        branch.setName("TestDelBranch-" + System.currentTimeMillis());
        branch = branchRepository.save(branch);

        Customer cust = new Customer();
        cust.setName("Anand");
        cust.setPhone("9876543213");
        cust.setBranch(branch);
        cust = customerRepository.save(cust);

        User emp = new User();
        emp.setName("Admin User");
        emp.setEmail("admin_test_del@test.com");
        emp.setPassword("password");
        emp.setRole(Role.SUPER_ADMIN);
        emp = userRepository.save(emp);

        Quotation q = createQuotation("Q-DEL-001", branch, cust, emp, QuotationStatus.WORKING);

        QuotationRoom room1 = new QuotationRoom();
        room1.setQuotation(q);
        room1.setRoom("Kitchen");
        room1.setFloor("Ground Floor");
        room1.setSubtotal(new BigDecimal("50000.00"));
        room1 = quotationRoomRepository.save(room1);

        Long qId = q.getId();
        Long roomId = room1.getId();

        // Perform deletion
        quotationService.delete(qId);

        // Verify quotation is deleted
        assertFalse(quotationRepository.findById(qId).isPresent());
        // Verify room is deleted
        assertFalse(quotationRoomRepository.findById(roomId).isPresent());

        // Verify master records are preserved
        assertTrue(branchRepository.findById(branch.getId()).isPresent(), "Branch master record must not be deleted");
        assertTrue(customerRepository.findById(cust.getId()).isPresent(), "Customer master record must not be deleted");
        assertTrue(userRepository.findById(emp.getId()).isPresent(), "User master record must not be deleted");
    }

    @Test
    void testDistrictQuotationPagesRenderWithoutThymeleafError() {
        Branch branch = new Branch();
        branch.setName("TestRenderBranch-" + System.currentTimeMillis());
        branch.setBranchAddress("789 Trichy Road");
        branch = branchRepository.save(branch);

        Customer cust = new Customer();
        cust.setName("Karthik");
        cust.setPhone("9876543219");
        cust.setBranch(branch);
        cust = customerRepository.save(cust);

        User emp = new User();
        emp.setName("Admin User");
        emp.setEmail("admin_render_test@test.com");
        emp.setPassword("password");
        emp.setRole(Role.SUPER_ADMIN);
        emp = userRepository.save(emp);

        Quotation qSent = createQuotation("Q-SENT-001", branch, cust, emp, QuotationStatus.SENT);
        Quotation qPending = createQuotation("Q-PEND-001", branch, cust, emp, QuotationStatus.PENDING);
        Quotation qWorking = createQuotation("Q-WORK-001", branch, cust, emp, QuotationStatus.WORKING);

        DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token");

        // 1. Test SENT quotations page
        WebContext sentContext = createWebContext();
        sentContext.setVariable("branch", branch);
        sentContext.setVariable("selectedStatus", QuotationStatus.SENT);
        sentContext.setVariable("quotations", List.of(qSent));
        sentContext.setVariable("pendingCount", 1L);
        sentContext.setVariable("workingCount", 1L);
        sentContext.setVariable("sentCount", 1L);
        sentContext.setVariable("_csrf", csrfToken);

        String sentHtml = templateEngine.process("admin/branch-quotations", sentContext);
        assertNotNull(sentHtml);
        assertTrue(sentHtml.contains("Q-SENT-001"));
        assertTrue(sentHtml.contains("data-quotation-number=\"Q-SENT-001\""));
        assertFalse(sentHtml.contains("th:onsubmit"));

        // 2. Test PENDING quotations page
        WebContext pendingContext = createWebContext();
        pendingContext.setVariable("branch", branch);
        pendingContext.setVariable("selectedStatus", QuotationStatus.PENDING);
        pendingContext.setVariable("quotations", List.of(qPending));
        pendingContext.setVariable("pendingCount", 1L);
        pendingContext.setVariable("workingCount", 1L);
        pendingContext.setVariable("sentCount", 1L);
        pendingContext.setVariable("_csrf", csrfToken);

        String pendingHtml = templateEngine.process("admin/branch-quotations", pendingContext);
        assertNotNull(pendingHtml);
        assertTrue(pendingHtml.contains("Q-PEND-001"));
        assertTrue(pendingHtml.contains("data-quotation-number=\"Q-PEND-001\""));

        // 3. Test WORKING quotations page
        WebContext workingContext = createWebContext();
        workingContext.setVariable("branch", branch);
        workingContext.setVariable("selectedStatus", QuotationStatus.WORKING);
        workingContext.setVariable("quotations", List.of(qWorking));
        workingContext.setVariable("pendingCount", 1L);
        workingContext.setVariable("workingCount", 1L);
        workingContext.setVariable("sentCount", 1L);
        workingContext.setVariable("_csrf", csrfToken);

        String workingHtml = templateEngine.process("admin/branch-quotations", workingContext);
        assertNotNull(workingHtml);
        assertTrue(workingHtml.contains("Q-WORK-001"));
        assertTrue(workingHtml.contains("data-quotation-number=\"Q-WORK-001\""));

        // 4. Test Branch Details template with quotations
        WebContext branchDetailsContext = createWebContext();
        branchDetailsContext.setVariable("branch", branch);
        branchDetailsContext.setVariable("employees", List.of(emp));
        branchDetailsContext.setVariable("customers", List.of(cust));
        branchDetailsContext.setVariable("quotations", List.of(qSent, qPending, qWorking));
        branchDetailsContext.setVariable("pendingCount", 1L);
        branchDetailsContext.setVariable("workingCount", 1L);
        branchDetailsContext.setVariable("sentCount", 1L);
        branchDetailsContext.setVariable("selectedStatus", null);
        branchDetailsContext.setVariable("_csrf", csrfToken);

        String branchDetailsHtml = templateEngine.process("admin/branch-details", branchDetailsContext);
        assertNotNull(branchDetailsHtml);
        assertTrue(branchDetailsHtml.contains("data-quotation-number=\"Q-SENT-001\""));

        // Verify Approved Business section is removed
        assertFalse(branchDetailsHtml.contains("APPROVED BUSINESS"), "Approved Business section should be removed");
        assertFalse(branchDetailsHtml.contains("id=\"approved\""), "id='approved' section should be removed");

        // Verify Customers section is removed
        assertFalse(branchDetailsHtml.contains("CUSTOMER ACTIVITY"), "Customers section should be removed");

        // Verify Quoted Value and Quotations sections exist and Quoted Value appears above Quotations
        assertTrue(branchDetailsHtml.contains("id=\"quotation-value\""), "Quoted Value section should exist");
        assertTrue(branchDetailsHtml.contains("id=\"quotations\""), "Quotations section should exist");

        int quotedValueIndex = branchDetailsHtml.indexOf("id=\"quotation-value\"");
        int quotationsIndex = branchDetailsHtml.indexOf("id=\"quotations\"");

        assertTrue(quotedValueIndex < quotationsIndex, "Quoted Value must appear above Quotations section");
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
