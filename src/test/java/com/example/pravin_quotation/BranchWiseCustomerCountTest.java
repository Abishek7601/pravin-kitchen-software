package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminController;
import com.example.pravin_quotation.controller.CustomerController;
import com.example.pravin_quotation.dto.BranchCustomerCountDto;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.UserRepository;
import com.example.pravin_quotation.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class BranchWiseCustomerCountTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private CustomerController customerController;

    @Autowired
    private AdminController adminController;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    private Branch chennaiBranch;
    private Branch coimbatoreBranch;
    private Branch emptyBranch;
    private User adminUser;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long nano = System.nanoTime() % 1000000;

        // Create isolated test branches
        chennaiBranch = new Branch();
        chennaiBranch.setName("Chennai_" + nano);
        chennaiBranch.setActive(true);
        chennaiBranch = branchRepository.save(chennaiBranch);

        coimbatoreBranch = new Branch();
        coimbatoreBranch.setName("Coimbatore_" + nano);
        coimbatoreBranch.setActive(true);
        coimbatoreBranch = branchRepository.save(coimbatoreBranch);

        emptyBranch = new Branch();
        emptyBranch.setName("Madurai_" + nano);
        emptyBranch.setActive(true);
        emptyBranch = branchRepository.save(emptyBranch);

        // Lookup or create Admin User
        adminUser = userRepository.findByEmail("superadmin@pravin.com").orElseGet(() -> {
            User u = new User();
            u.setName("Super Admin");
            u.setEmail("superadmin@pravin.com");
            u.setPassword("password");
            u.setRole(Role.SUPER_ADMIN);
            u.setActive(true);
            return userRepository.save(u);
        });

        // Create Chennai Customers
        Customer c1 = new Customer();
        c1.setName("Anand Kumar " + nano);
        c1.setEmail("anand" + nano + "@chennai.com");
        c1.setPhone("98" + String.format("%08d", (nano * 13) % 100000000));
        c1.setAddress("Anna Nagar, Chennai");
        c1.setBranch(chennaiBranch);
        c1.setActive(true);
        c1 = customerRepository.save(c1);

        Customer c2 = new Customer();
        c2.setName("Balaji S " + nano);
        c2.setEmail("balaji" + nano + "@chennai.com");
        c2.setPhone("97" + String.format("%08d", (nano * 17) % 100000000));
        c2.setAddress("T Nagar, Chennai");
        c2.setBranch(chennaiBranch);
        c2.setActive(true);
        c2 = customerRepository.save(c2);

        // Multiple quotations for Anand Kumar (Chennai customer)
        for (int i = 0; i < 3; i++) {
            Quotation q = new Quotation();
            q.setQuotationNumber("QT-CHN-" + nano + "-" + i);
            q.setCustomer(c1);
            q.setBranch(chennaiBranch);
            q.setEmployee(adminUser);
            q.setPricingMode(PricingMode.MIDDLE);
            q.setQuotationDate(LocalDate.now());
            q.setStatus(QuotationStatus.WORKING);
            q.setGrandTotal(new BigDecimal("120000"));
            quotationRepository.save(q);
        }

        // Create Coimbatore Customer
        Customer c3 = new Customer();
        c3.setName("Chandran K " + nano);
        c3.setEmail("chandran" + nano + "@coimbatore.com");
        c3.setPhone("96" + String.format("%08d", (nano * 19) % 100000000));
        c3.setAddress("RS Puram, Coimbatore");
        c3.setBranch(coimbatoreBranch);
        c3.setActive(true);
        customerRepository.save(c3);
    }

    @Test
    @WithMockUser(username = "superadmin@pravin.com", roles = {"SUPER_ADMIN"})
    public void testClickChennaiCustomerCountReturnsOnlyChennaiCustomers() throws Exception {
        // Query Chennai by branchId query parameter
        mockMvc.perform(get("/admin/customers")
                        .param("branchId", chennaiBranch.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/customers"))
                .andExpect(model().attribute("selectedBranch", notNullValue()))
                .andExpect(model().attribute("branchCustomerCount", 2))
                .andExpect(content().string(containsString(chennaiBranch.getName() + " Branch Customers")))
                .andExpect(content().string(containsString("Anand Kumar")))
                .andExpect(content().string(containsString("Balaji S")))
                // Ensure Coimbatore customer NEVER appears in Chennai list
                .andExpect(content().string(not(containsString("Chandran K"))))
                .andExpect(content().string(containsString("Back to Customer Statistics")));

        // Also test path variable route: /admin/customers/branch/{id}
        mockMvc.perform(get("/admin/customers/branch/" + chennaiBranch.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/customers"))
                .andExpect(model().attribute("selectedBranch", notNullValue()))
                .andExpect(content().string(containsString(chennaiBranch.getName() + " Branch Customers")));
    }

    @Test
    @WithMockUser(username = "superadmin@pravin.com", roles = {"SUPER_ADMIN"})
    public void testClickCoimbatoreCustomerCountReturnsOnlyCoimbatoreCustomers() throws Exception {
        // Query Coimbatore
        mockMvc.perform(get("/admin/customers")
                        .param("branchId", coimbatoreBranch.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/customers"))
                .andExpect(model().attribute("selectedBranch", notNullValue()))
                .andExpect(model().attribute("branchCustomerCount", 1))
                .andExpect(content().string(containsString(coimbatoreBranch.getName() + " Branch Customers")))
                .andExpect(content().string(containsString("Chandran K")))
                // Ensure Chennai customers NEVER appear in Coimbatore list
                .andExpect(content().string(not(containsString("Anand Kumar"))))
                .andExpect(content().string(not(containsString("Balaji S"))));
    }

    @Test
    @WithMockUser(username = "superadmin@pravin.com", roles = {"SUPER_ADMIN"})
    public void testEmptyBranchDisplaysSuitableEmptyState() throws Exception {
        // Query Madurai (0 customers)
        mockMvc.perform(get("/admin/customers")
                        .param("branchId", emptyBranch.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/customers"))
                .andExpect(model().attribute("branchCustomerCount", 0))
                .andExpect(content().string(containsString(emptyBranch.getName() + " Branch Customers")))
                .andExpect(content().string(containsString("No customers found for " + emptyBranch.getName() + " branch")))
                .andExpect(content().string(containsString("There are currently no customer records associated with the")))
                .andExpect(content().string(containsString("View All Customers")));
    }

    @Test
    public void testDistinctCustomerCountingMatchesListAndIgnoresMultipleQuotations() {
        ExtendedModelMap model = new ExtendedModelMap();
        customerController.customers(null, chennaiBranch.getId(), model);

        @SuppressWarnings("unchecked")
        List<Customer> chennaiCustomers = (List<Customer>) model.get("customers");
        assertNotNull(chennaiCustomers);

        // Even though Anand Kumar has 3 quotations, there are only 2 distinct customers for Chennai
        assertEquals(2, chennaiCustomers.size());
        assertEquals(2, model.get("branchCustomerCount"));

        // Verify all customers belong strictly to Chennai
        for (Customer c : chennaiCustomers) {
            assertEquals(chennaiBranch.getId(), c.getBranch().getId());
        }

        // Branch-wise DTO count also matches 2
        List<BranchCustomerCountDto> branchCounts = customerService.getBranchWiseCustomerCounts();
        BranchCustomerCountDto chennaiDto = branchCounts.stream()
                .filter(b -> b.branchId().equals(chennaiBranch.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(2L, chennaiDto.customerCount());
    }

    @Test
    @WithMockUser(username = "superadmin@pravin.com", roles = {"SUPER_ADMIN"})
    public void testAdminDashboardPreservesExistingFunctionalityAndIncludesClickableBranchCounts() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        String view = adminController.dashboard(model);
        assertEquals("admin/dashboard", view);

        // Verify all original dashboard cards are preserved
        assertTrue(model.containsAttribute("totalEmployees"));
        assertTrue(model.containsAttribute("totalBranches"));
        assertTrue(model.containsAttribute("oneYearQuotationCount"));
        assertTrue(model.containsAttribute("last60DaysQuotationCount"));
        assertTrue(model.containsAttribute("last60DaysQuotedValue"));
        assertTrue(model.containsAttribute("oneYearCustomerCount"));
        assertTrue(model.containsAttribute("pendingQuotationCount"));
        assertTrue(model.containsAttribute("workingQuotationCount"));
        assertTrue(model.containsAttribute("sentQuotationCount"));
        assertTrue(model.containsAttribute("totalPortfolioValue"));

        // Verify branch customer counts are included
        assertTrue(model.containsAttribute("branchCustomerCounts"));

        // Verify dashboard HTML no longer contains Customer Statistics or Master Data
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("CUSTOMER STATISTICS"))))
                .andExpect(content().string(not(containsString("Branch-Wise Customer Count"))))
                .andExpect(content().string(not(containsString("MASTER DATA"))))
                .andExpect(content().string(not(containsString("Manage Quotation Data"))));
    }

    @Test
    @WithMockUser(username = "emp@pravin.com", roles = {"EMPLOYEE"})
    public void testEmployeeCannotAccessAdminBranchCustomerFiltering() throws Exception {
        // Security check: Employee role is prevented from accessing /admin/** endpoints (redirected to OTP or forbidden)
        mockMvc.perform(get("/admin/customers")
                        .param("branchId", chennaiBranch.getId().toString()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 403 || status == 302, "Expected 403 Forbidden or 302 Redirect but was " + status);
                });
    }
}
