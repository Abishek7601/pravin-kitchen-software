package com.example.pravin_quotation;

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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeQuotationWorkspaceActionButtonsTest {

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
    private ITemplateEngine templateEngine;

    @Test
    void testTopbarActionsRemovedFromEmployeeQuotationWorkspace() {
        long ts = System.currentTimeMillis();

        Branch branch = new Branch();
        branch.setName("Branch-" + ts);
        branch.setBranchAddress("Test Address");
        branch = branchRepository.save(branch);

        Customer customer = new Customer();
        customer.setName("Cust-" + ts);
        customer.setPhone("9998887771");
        customer.setBranch(branch);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        User employee = new User();
        employee.setName("Emp-" + ts);
        employee.setEmail("emp_" + ts + "@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee = userRepository.save(employee);

        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-ACT-" + ts);
        quotation.setEmployee(employee);
        quotation.setBranch(branch);
        quotation.setCustomer(customer);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setStatus(QuotationStatus.WORKING);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setGrandTotal(BigDecimal.valueOf(15000));
        quotation = quotationRepository.save(quotation);

        Authentication auth = new UsernamePasswordAuthenticationToken(employee.getEmail(), "secret");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = employeeQuotationController.viewQuotation(quotation.getId(), auth, model);
        assertEquals("employee/quotation-workspace", viewName);

        // Render template with WebContext
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        org.springframework.security.web.csrf.DefaultCsrfToken csrf =
                new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token");
        request.setAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName(), csrf);
        request.setAttribute("_csrf", csrf);

        var application = JakartaServletWebApplication.buildApplication(request.getServletContext());
        var exchange = application.buildExchange(request, response);
        WebContext context = new WebContext(exchange);
        model.forEach(context::setVariable);
        context.setVariable("_csrf", csrf);

        String html = templateEngine.process("employee/quotation-workspace", context);

        // 1. Topbar exists and contains page-heading and employee-profile
        assertTrue(html.contains("<header class=\"topbar\">"));
        assertTrue(html.contains("QUOTATION WORKSPACE"));
        assertTrue(html.contains("employee-profile"));

        // Extract the <header class="topbar"> section
        int topbarStart = html.indexOf("<header class=\"topbar\">");
        int topbarEnd = html.indexOf("</header>", topbarStart);
        assertTrue(topbarStart != -1 && topbarEnd != -1, "Topbar must exist");
        String topbarHtml = html.substring(topbarStart, topbarEnd);

        // 2. The 3 action buttons and nav-header-actions container must be completely removed from topbar
        assertFalse(topbarHtml.contains("Edit in Builder"), "Topbar must NOT contain 'Edit in Builder'");
        assertFalse(topbarHtml.contains("Preview / PDF"), "Topbar must NOT contain 'Preview / PDF'");
        assertFalse(topbarHtml.contains("Print"), "Topbar must NOT contain 'Print'");
        assertFalse(topbarHtml.contains("nav-header-actions"), "Topbar must NOT contain 'nav-header-actions' container");

        // 3. Page information and quotation number remain properly rendered
        assertTrue(topbarHtml.contains("QT-ACT-" + ts), "Topbar must display quotation number");
        assertTrue(html.contains("Cust-" + ts), "Customer details must be displayed");
        assertTrue(html.contains("0.00") || html.contains("15,000"), "Quotation amount must be displayed");
    }
}

