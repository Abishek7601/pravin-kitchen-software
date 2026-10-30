package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class EmployeePortalResponsiveRenderingTest {

    @Autowired
    private ITemplateEngine templateEngine;

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

    private User employee;
    private Branch branch;
    private Customer customer;
    private Quotation quotation;

    @BeforeEach
    void setUp() {
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Chennai Main");
            b.setBranchAddress("Chennai");
            b.setActive(true);
            return branchRepository.save(b);
        });

        employee = userRepository.findByEmail("test.employee.resp@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Pravin Test Employee");
            u.setEmail("test.employee.resp@pravin.com");
            u.setPassword("secret123");
            u.setRole(Role.EMPLOYEE);
            u.setBranch(branch);
            u.setActive(true);
            return userRepository.save(u);
        });

        customer = customerRepository.findByPhone("9876543210").orElseGet(() -> {
            Customer c = new Customer();
            c.setName("Ramesh Kumar");
            c.setPhone("9876543210");
            c.setEmail("ramesh.kumar.long.business.email@example.com");
            c.setBranch(branch);
            c.setActive(true);
            return customerRepository.save(c);
        });

        quotation = new Quotation();
        quotation.setQuotationNumber("QT-TEST-001");
        quotation.setCustomer(customer);
        quotation.setEmployee(employee);
        quotation.setBranch(branch);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setValidUntil(LocalDate.now().plusDays(365));
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setSubtotal(new BigDecimal("150000.00"));
        quotation.setGrandTotal(new BigDecimal("177000.00"));
        quotation = quotationRepository.save(quotation);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(),
                employee.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String renderTemplate(String templateName, MockHttpServletRequest request, MockHttpServletResponse response) {
        var application = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var exchange = application.buildExchange(request, response);
        var webContext = new WebContext(exchange);
        return templateEngine.process(templateName, webContext);
    }

    private void setupMockRequest(MockHttpServletRequest request) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_VERIFIED", true);
        request.setSession(session);
        request.setAttribute("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token"));
        request.setAttribute("employee", employee);
    }

    @Test
    @DisplayName("Verify Employee Dashboard renders with compact cards and responsive classes")
    void testEmployeeDashboardRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupMockRequest(request);

        request.setAttribute("oneYearQuotations", 12L);
        request.setAttribute("last60DaysQuotations", 5L);
        request.setAttribute("pendingQuotations", 3L);
        request.setAttribute("workingQuotations", 1L);
        request.setAttribute("sentQuotations", 8L);
        request.setAttribute("recentQuotations", List.of(quotation));

        String html = renderTemplate("employee/dashboard", request, response);
        assertThat(html).isNotEmpty();
        assertThat(html).contains("EMPLOYEE PORTAL");
        assertThat(html).contains("Employee Dashboard");
        assertThat(html).contains("Total One Year Quotations");
        assertThat(html).contains("Pending Quotations");
        assertThat(html).contains("Quick Actions");
        assertThat(html).contains("Recent Quotations");
        assertThat(html).contains("stats-grid");

        saveHtml("employee_dashboard.html", html);
    }

    @Test
    @DisplayName("Verify Employee Quotations page renders with table-card and filter tabs")
    void testEmployeeQuotationsRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupMockRequest(request);

        request.setAttribute("quotations", List.of(quotation));
        request.setAttribute("totalCount", 1L);
        request.setAttribute("pendingCount", 1L);
        request.setAttribute("workingCount", 0L);
        request.setAttribute("sentCount", 0L);
        request.setAttribute("pageTitle", "My Quotations");
        request.setAttribute("pageSubtitle", "View and manage your customer quotations.");

        String html = renderTemplate("employee/quotations", request, response);
        assertThat(html).isNotEmpty();
        assertThat(html).contains("My Quotations");
        assertThat(html).contains("+ Create Quotation");
        assertThat(html).contains("filter-bar");
        assertThat(html).contains("filter-tab");
        assertThat(html).contains("table-card");
        assertThat(html).contains("employeeQuotationTable");

        saveHtml("employee_quotations.html", html);
    }

    @Test
    @DisplayName("Verify Employee Customers page renders with table-wrapper and customer cards")
    void testEmployeeCustomersRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupMockRequest(request);

        request.setAttribute("customers", List.of(customer));

        String html = renderTemplate("employee/customers", request, response);
        assertThat(html).isNotEmpty();
        assertThat(html).contains("Customers");
        assertThat(html).contains("CUSTOMER LIST");
        assertThat(html).contains("table-wrapper");
        assertThat(html).contains("employeeCustomerTable");
        assertThat(html).contains("Ramesh Kumar");

        saveHtml("employee_customers.html", html);
    }

    @Test
    @DisplayName("Verify Employee Create Quotation page renders with responsive forms")
    void testEmployeeQuotationNewRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupMockRequest(request);

        request.setAttribute("nextQuotationNumber", "QT-TEST-NEW-01");
        request.setAttribute("quotationDate", LocalDate.now());

        String html = renderTemplate("employee/quotation-new", request, response);
        assertThat(html).isNotEmpty();
        assertThat(html).contains("Create Quotation");
        assertThat(html).contains("quotation-number-card");
        assertThat(html).contains("form-card");
        assertThat(html).contains("customerName");

        saveHtml("employee_quotation_new.html", html);
    }

    @Test
    @DisplayName("Verify Employee Quotation Builder page renders with room & quote summary")
    void testEmployeeQuotationBuilderRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupMockRequest(request);

        request.setAttribute("quotation", quotation);
        request.setAttribute("customer", customer);
        request.setAttribute("rooms", Collections.emptyList());
        request.setAttribute("roomItems", Collections.emptyMap());
        request.setAttribute("branches", List.of(branch));
        request.setAttribute("customers", List.of(customer));
        request.setAttribute("workCategories", workCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc());
        request.setAttribute("pricingModes", PricingMode.values());
        request.setAttribute("today", LocalDate.now());
        request.setAttribute("editQuotationId", quotation.getId().toString());
        request.setAttribute("selectedBranchId", branch.getId());
        request.setAttribute("employeeBranchId", branch.getId());

        String html = renderTemplate("employee/quotation-builder", request, response);
        assertThat(html).isNotEmpty();
        assertThat(html).contains("QUOTATION BUILDER");
        assertThat(html).contains("Create Quotation");

        saveHtml("employee_quotation_builder.html", html);
    }

    @Test
    @DisplayName("Verify Employee Quotation View page renders with details and print preview")
    void testEmployeeQuotationViewRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        setupMockRequest(request);

        request.setAttribute("quotation", quotation);
        request.setAttribute("customer", customer);
        request.setAttribute("rooms", Collections.emptyList());
        request.setAttribute("history", Collections.emptyList());

        String html = renderTemplate("employee/quotation-view", request, response);
        assertThat(html).isNotEmpty();
        assertThat(html).contains("Quotation");

        saveHtml("employee_quotation_view.html", html);
    }

    private void saveHtml(String filename, String content) throws Exception {
        File dir = new File("target/rendered_test_html");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
