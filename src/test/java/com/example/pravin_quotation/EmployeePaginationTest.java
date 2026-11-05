package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeCustomerController;
import com.example.pravin_quotation.controller.EmployeeDashboardController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeePaginationTest {

    @Autowired
    private EmployeeQuotationController quotationController;

    @Autowired
    private EmployeeCustomerController customerController;

    @Autowired
    private EmployeeDashboardController dashboardController;

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

    private User createEmployee(String prefix, Branch branch) {
        long ts = System.nanoTime();
        User emp = new User();
        emp.setName(prefix + " Emp");
        emp.setEmail(prefix.toLowerCase() + "_" + ts + "@pravin.com");
        emp.setPassword("secret");
        emp.setRole(Role.EMPLOYEE);
        emp.setBranch(branch);
        return userRepository.save(emp);
    }

    private Branch createBranch(String name) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setBranchAddress("Test Address");
        return branchRepository.save(branch);
    }

    private Customer createCustomer(String name, String phone, Branch branch) {
        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setBranch(branch);
        c.setActive(true);
        return customerRepository.save(c);
    }

    private Quotation createQuotation(String num, User employee, Branch branch, Customer customer, LocalDate date, QuotationStatus status, BigDecimal amount) {
        Quotation q = new Quotation();
        q.setQuotationNumber(num);
        q.setEmployee(employee);
        q.setBranch(branch);
        q.setCustomer(customer);
        q.setQuotationDate(date);
        q.setStatus(status);
        q.setGrandTotal(amount);
        q.setPricingMode(PricingMode.PREMIUM);
        return quotationRepository.save(q);
    }

    @Test
    void testEmployeeQuotationsPaginationTenRecordsPerPage() {
        Branch branch = createBranch("PagBranch1");
        User emp = createEmployee("QTest", branch);
        Customer cust = createCustomer("Cust1", "9000000001", branch);
        Authentication auth = new UsernamePasswordAuthenticationToken(emp.getEmail(), "secret");

        // Create 25 quotations
        for (int i = 1; i <= 25; i++) {
            createQuotation("QT-PAG-" + String.format("%03d", i), emp, branch, cust,
                    LocalDate.now().minusDays(i), QuotationStatus.PENDING, BigDecimal.valueOf(1000 + i));
        }

        // --- Page 1 ---
        ExtendedModelMap model1 = new ExtendedModelMap();
        String view1 = quotationController.quotations(null, null, null, 1, null, null, auth, model1);
        assertEquals("employee/quotations", view1);

        @SuppressWarnings("unchecked")
        List<Quotation> list1 = (List<Quotation>) model1.get("quotations");
        assertNotNull(list1);
        assertEquals(10, list1.size(), "Page 1 must contain exactly 10 records");
        assertEquals(1, model1.get("currentPage"));
        assertEquals(3, model1.get("totalPages"));
        assertEquals(25L, model1.get("totalElements"));
        assertEquals(1L, model1.get("pageStart"));
        assertEquals(10L, model1.get("pageEnd"));
        assertEquals(false, model1.get("hasPrevious"));
        assertEquals(true, model1.get("hasNext"));

        // --- Page 2 ---
        ExtendedModelMap model2 = new ExtendedModelMap();
        quotationController.quotations(null, null, null, 2, null, null, auth, model2);
        @SuppressWarnings("unchecked")
        List<Quotation> list2 = (List<Quotation>) model2.get("quotations");
        assertNotNull(list2);
        assertEquals(10, list2.size(), "Page 2 must contain exactly 10 records");
        assertEquals(2, model2.get("currentPage"));
        assertEquals(11L, model2.get("pageStart"));
        assertEquals(20L, model2.get("pageEnd"));
        assertEquals(true, model2.get("hasPrevious"));
        assertEquals(true, model2.get("hasNext"));

        // --- Page 3 ---
        ExtendedModelMap model3 = new ExtendedModelMap();
        quotationController.quotations(null, null, null, 3, null, null, auth, model3);
        @SuppressWarnings("unchecked")
        List<Quotation> list3 = (List<Quotation>) model3.get("quotations");
        assertNotNull(list3);
        assertEquals(5, list3.size(), "Page 3 must contain remaining 5 records");
        assertEquals(3, model3.get("currentPage"));
        assertEquals(21L, model3.get("pageStart"));
        assertEquals(25L, model3.get("pageEnd"));
        assertEquals(true, model3.get("hasPrevious"));
        assertEquals(false, model3.get("hasNext"));
    }

    @Test
    void testEmployeeQuotationsFilterAndSearchPagination() {
        Branch branch = createBranch("FilterBranch");
        User emp = createEmployee("FilterEmp", branch);
        Customer custA = createCustomer("Rajesh Kumar", "9111111111", branch);
        Customer custB = createCustomer("Suresh Raina", "9222222222", branch);
        Authentication auth = new UsernamePasswordAuthenticationToken(emp.getEmail(), "secret");

        // 12 PENDING for custA
        for (int i = 1; i <= 12; i++) {
            createQuotation("QT-FIL-P" + i, emp, branch, custA, LocalDate.now().minusDays(i), QuotationStatus.PENDING, BigDecimal.valueOf(5000));
        }
        // 8 SENT for custB
        for (int i = 1; i <= 8; i++) {
            createQuotation("QT-FIL-S" + i, emp, branch, custB, LocalDate.now().minusDays(i), QuotationStatus.SENT, BigDecimal.valueOf(8000));
        }

        // Test Filter by status PENDING
        ExtendedModelMap pendingModel = new ExtendedModelMap();
        quotationController.quotations(null, "PENDING", null, 1, null, null, auth, pendingModel);
        @SuppressWarnings("unchecked")
        List<Quotation> pendingList = (List<Quotation>) pendingModel.get("quotations");
        assertEquals(10, pendingList.size());
        assertEquals(12L, pendingModel.get("totalElements"));
        assertEquals(2, pendingModel.get("totalPages"));
        assertTrue(pendingList.stream().allMatch(q -> q.getStatus() == QuotationStatus.PENDING));

        // Test Search by customer name
        ExtendedModelMap searchModel = new ExtendedModelMap();
        quotationController.quotations(null, null, null, 1, "Rajesh", null, auth, searchModel);
        @SuppressWarnings("unchecked")
        List<Quotation> searchList = (List<Quotation>) searchModel.get("quotations");
        assertEquals(10, searchList.size());
        assertEquals(12L, searchModel.get("totalElements"));
        assertTrue(searchList.stream().allMatch(q -> q.getCustomer().getName().contains("Rajesh")));
    }

    @Test
    void testEmployeeQuotationsAccessControlScopingPreserved() {
        Branch branch = createBranch("ScopeBranch");
        User empA = createEmployee("EmpA", branch);
        User empB = createEmployee("EmpB", branch);
        Customer cust = createCustomer("CustScope", "9333333333", branch);

        createQuotation("QT-SCOP-A1", empA, branch, cust, LocalDate.now(), QuotationStatus.PENDING, BigDecimal.valueOf(1000));
        createQuotation("QT-SCOP-B1", empB, branch, cust, LocalDate.now(), QuotationStatus.PENDING, BigDecimal.valueOf(2000));

        Authentication authA = new UsernamePasswordAuthenticationToken(empA.getEmail(), "secret");
        ExtendedModelMap modelA = new ExtendedModelMap();
        quotationController.quotations(null, null, null, 1, null, null, authA, modelA);

        @SuppressWarnings("unchecked")
        List<Quotation> listA = (List<Quotation>) modelA.get("quotations");
        assertEquals(1, listA.size());
        assertEquals("QT-SCOP-A1", listA.get(0).getQuotationNumber());
        assertEquals(1L, modelA.get("totalElements"));
    }

    @Test
    void testEmployeeCustomerPaginationAndBranchScoping() {
        Branch branchA = createBranch("BranchAlpha");
        Branch branchB = createBranch("BranchBeta");

        User empA = createEmployee("CustEmpA", branchA);
        Authentication authA = new UsernamePasswordAuthenticationToken(empA.getEmail(), "secret");

        // 23 customers in Branch A
        for (int i = 1; i <= 23; i++) {
            createCustomer("AlphaCust " + String.format("%02d", i), "98000000" + String.format("%02d", i), branchA);
        }
        // 5 customers in Branch B
        for (int i = 1; i <= 5; i++) {
            createCustomer("BetaCust " + i, "990000000" + i, branchB);
        }

        // --- Page 1 ---
        ExtendedModelMap model1 = new ExtendedModelMap();
        String view1 = customerController.customers(1, null, authA, model1);
        assertEquals("employee/customers", view1);

        @SuppressWarnings("unchecked")
        List<Customer> list1 = (List<Customer>) model1.get("customers");
        assertNotNull(list1);
        assertEquals(10, list1.size(), "Page 1 must contain exactly 10 customers");
        assertEquals(23L, model1.get("totalRecords"));
        assertEquals(3, model1.get("totalPages"));
        assertEquals(1, model1.get("currentPage"));
        assertEquals(1L, model1.get("pageStart"));
        assertEquals(10L, model1.get("pageEnd"));
        assertEquals(false, model1.get("hasPrevious"));
        assertEquals(true, model1.get("hasNext"));
        assertTrue(list1.stream().allMatch(c -> c.getBranch().getId().equals(branchA.getId())),
                "Must only contain customers from Branch A");

        // --- Page 3 ---
        ExtendedModelMap model3 = new ExtendedModelMap();
        customerController.customers(3, null, authA, model3);
        @SuppressWarnings("unchecked")
        List<Customer> list3 = (List<Customer>) model3.get("customers");
        assertEquals(3, list3.size(), "Page 3 must contain remaining 3 customers");
        assertEquals(21L, model3.get("pageStart"));
        assertEquals(23L, model3.get("pageEnd"));
        assertEquals(true, model3.get("hasPrevious"));
        assertEquals(false, model3.get("hasNext"));

        // Ensure totalCustomers attribute is NOT leaked
        assertFalse(model1.containsAttribute("totalCustomers"));
    }

    @Test
    void testEmployeeDashboardRecentQuotationsPagination() {
        Branch branch = createBranch("DashBranch");
        User emp = createEmployee("DashEmp", branch);
        Customer cust = createCustomer("DashCust", "9777777777", branch);
        Authentication auth = new UsernamePasswordAuthenticationToken(emp.getEmail(), "secret");

        // 14 quotations
        for (int i = 1; i <= 14; i++) {
            createQuotation("QT-DASH-" + String.format("%02d", i), emp, branch, cust,
                    LocalDate.now().minusDays(i), QuotationStatus.PENDING, BigDecimal.valueOf(3000));
        }

        ExtendedModelMap model1 = new ExtendedModelMap();
        String view1 = dashboardController.dashboard(1, auth, model1);
        assertEquals("employee/dashboard", view1);

        @SuppressWarnings("unchecked")
        List<Quotation> recent1 = (List<Quotation>) model1.get("recentQuotations");
        assertNotNull(recent1);
        assertEquals(10, recent1.size(), "Dashboard recent quotations must paginate 10 per page");
        assertEquals(14L, model1.get("totalRecords"));
        assertEquals(2, model1.get("totalPages"));
        assertEquals(1, model1.get("currentPage"));
        assertEquals(1L, model1.get("pageStart"));
        assertEquals(10L, model1.get("pageEnd"));

        ExtendedModelMap model2 = new ExtendedModelMap();
        dashboardController.dashboard(2, auth, model2);
        @SuppressWarnings("unchecked")
        List<Quotation> recent2 = (List<Quotation>) model2.get("recentQuotations");
        assertNotNull(recent2);
        assertEquals(4, recent2.size(), "Dashboard page 2 must have remaining 4 records");
        assertEquals(11L, model2.get("pageStart"));
        assertEquals(14L, model2.get("pageEnd"));
    }

    @Test
    void testTemplateRenderingPaginationUi() {
        Branch branch = createBranch("UiBranch");
        User emp = createEmployee("UiEmp", branch);
        Customer cust = createCustomer("UiCust", "9666666666", branch);
        Authentication auth = new UsernamePasswordAuthenticationToken(emp.getEmail(), "secret");

        for (int i = 1; i <= 25; i++) {
            createQuotation("QT-UI-" + String.format("%02d", i), emp, branch, cust,
                    LocalDate.now().minusDays(i), QuotationStatus.PENDING, BigDecimal.valueOf(2000));
        }

        ExtendedModelMap model = new ExtendedModelMap();
        quotationController.quotations(null, null, null, 1, null, null, auth, model);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        var application = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var exchange = application.buildExchange(request, response);
        WebContext context = new WebContext(exchange);
        model.forEach(context::setVariable);

        String html = templateEngine.process("employee/quotations", context);

        assertTrue(html.contains("pagination-footer"), "Must contain pagination footer");
        assertTrue(html.contains("Showing"), "Must contain Showing text");
        assertTrue(html.contains("Previous"), "Must contain Previous button");
        assertTrue(html.contains("Next"), "Must contain Next button");
        assertTrue(html.contains(">1</span>") && html.contains(">10</span>"), "Must contain range 1 to 10");
        assertTrue(html.contains("25"), "Must contain total records 25");
    }
}

