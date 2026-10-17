package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeQuotationViewLogoTest {

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    private String loadResource(String path) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        assertTrue(resource.exists(), "Resource must exist: " + path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Employee Quotation View template displays company logo correctly with proper asset")
    public void testEmployeeQuotationViewTemplateLogo() throws Exception {
        String template = loadResource("templates/employee/quotation-workspace.html");

        // Verify correct company logo asset is used
        assertTrue(template.contains("@{/images/company-logo.png}"),
                "Template must use the standard /images/company-logo.png asset");

        // Verify page-heading / page-label contains the logo
        assertTrue(template.contains("class=\"brand-logo-img\""),
                "Logo element must use brand-logo-img class");
        assertTrue(template.contains("object-fit: contain"),
                "Logo must have object-fit: contain to preserve aspect ratio");
        assertTrue(template.contains("alt=\"Pravin KITCHENS"),
                "Logo must have descriptive alt text matching brand");

        // Sidebar fragment inclusion preserved
        assertTrue(template.contains("fragments/employee-navigation :: sidebar"),
                "Sidebar fragment must be included");
    }

    @Test
    @DisplayName("Employee Quotation Workspace CSS correctly styles sidebar and page logos without distortion")
    public void testEmployeeQuotationWorkspaceCssLogo() throws Exception {
        String css = loadResource("static/css/employee-quotation-workspace.css");

        // Sidebar brand icon and logo image styles
        assertTrue(css.contains(".sidebar .brand-icon"),
                "employee-quotation-workspace.css must style .sidebar .brand-icon");
        assertTrue(css.contains(".sidebar .brand-icon .brand-logo-img"),
                "employee-quotation-workspace.css must specifically style .sidebar .brand-icon .brand-logo-img");
        assertTrue(css.contains("width: 60px"),
                "Sidebar brand icon must have 60px width");
        assertTrue(css.contains("height: 60px"),
                "Sidebar brand icon must have 60px height");
        assertTrue(css.contains("object-fit: contain"),
                "Sidebar logo must have object-fit: contain");
        assertTrue(css.contains("border: none"),
                "Sidebar logo must not have ugly inner border");

        // Page header / label logo scoped styles
        assertTrue(css.contains(".page-label .brand-logo-img") || css.contains(".page-heading .brand-logo-img"),
                "Page heading logo must be properly scoped");

        // Mobile responsive rules
        assertTrue(css.contains("@media (max-width: 768px)"),
                "Must include responsive rules for mobile");
    }

    @Test
    @DisplayName("Employee Quotation View endpoint loads successfully with quotation")
    public void testEmployeeQuotationViewEndpoint() {
        Quotation sampleQuotation = quotationRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(sampleQuotation, "At least one quotation should exist in test database");

        User owner = sampleQuotation.getEmployee();
        assertNotNull(owner, "Quotation must have an owner employee");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                owner.getEmail(),
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        Model model = new ConcurrentModel();
        String view = employeeQuotationController.viewQuotation(sampleQuotation.getId(), auth, model);

        assertEquals("employee/quotation-workspace", view,
                "Employee quotation view endpoint must return employee/quotation-workspace");
        assertTrue(model.containsAttribute("quotation"), "Model must contain quotation");
    }
}
