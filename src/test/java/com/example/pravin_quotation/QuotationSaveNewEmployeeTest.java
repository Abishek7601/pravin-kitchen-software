package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class QuotationSaveNewEmployeeTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCompareAllEmployeeFields() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM users WHERE role = 'EMPLOYEE'");
        assertFalse(rows.isEmpty(), "There should be employee records");
        for (Map<String, Object> row : rows) {
            assertNotNull(row.get("id"));
            assertNotNull(row.get("name"));
            assertNotNull(row.get("email"));
            assertNotNull(row.get("branch_id"), "Employee must have branch_id");
            assertEquals(true, row.get("active"), "Employee must be active");
        }
    }

    @Test
    void testMockMvcBuilderPageRendering() throws Exception {
        String html = mockMvc.perform(get("/employee/quotations/builder")
                .with(user("anish@gmail.com").roles("EMPLOYEE"))
                .sessionAttr("OTP_VERIFIED", true))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 1. Verify quoteNumberPreview is present and not commented out
        assertTrue(html.contains("id=\"quoteNumberPreview\""), "HTML must contain id='quoteNumberPreview'");
        assertFalse(html.contains("<!--        <span class=\"quote-number-preview\""), "quoteNumberPreview must not be commented out");

        // 2. Verify error banners exist
        assertTrue(html.contains("id=\"clientErrorBanner\""), "HTML must contain clientErrorBanner");
        assertTrue(html.contains("th:if=\"${errorMessage}\"") || html.contains("class=\"alert alert-danger\""), "HTML must have error alert element");

        // 3. Verify branch selection element
        assertTrue(html.contains("id=\"branchId\""), "HTML must contain id='branchId'");
        assertTrue(html.contains("data-employee-branch=\"4\""), "Anish (branch 4) should have data-employee-branch='4'");
    }

    @Test
    void testSaveQuotationForExistingEmployee() {
        User existingEmployee = userRepository.findByEmail("anish@gmail.com").orElseThrow();
        assertEquals(Role.EMPLOYEE, existingEmployee.getRole());
        assertNotNull(existingEmployee.getBranch());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                existingEmployee.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        WorkCategory cat = workCategoryRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActive())).findFirst().orElseThrow();
        Division div = divisionRepository.findAll().stream().filter(d -> Boolean.TRUE.equals(d.getActive())).findFirst().orElseThrow();
        Item item = itemRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActive())).findFirst().orElseThrow();
        Material mat = materialRepository.findAll().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).findFirst().orElseThrow();
        MaterialOption opt = materialOptionRepository.findAll().stream().filter(o -> Boolean.TRUE.equals(o.getActive())).findFirst().orElseThrow();

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("24"), new BigDecimal("36"), new BigDecimal("30"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), item.getId(), mat.getId(), opt.getId(),
                "Unit Existing", "Spec Existing", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Living Room", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Existing Employee Client", "9844001122", "existclient@test.com", "Kanyakumari"
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, existingEmployee.getBranch().getId(), LocalDate.now(), PricingMode.MIDDLE, "Requirements", "Notes",
                List.of(roomReq)
        );

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = employeeQuotationController.saveBuilderQuotation(builderReq, auth, redirectAttributes);

        assertTrue(result.startsWith("redirect:/employee/quotations/view/"), "Existing employee should redirect to view page");
        String quotationIdStr = result.substring("redirect:/employee/quotations/view/".length());
        Long quotationId = Long.parseLong(quotationIdStr);

        Quotation saved = quotationRepository.findById(quotationId).orElseThrow();
        assertEquals(existingEmployee.getId(), saved.getEmployee().getId(), "Quotation employee must match");
        assertEquals(existingEmployee.getBranch().getId(), saved.getBranch().getId(), "Quotation branch must match");
        assertNotNull(saved.getCustomer());
    }

    @Test
    void testSaveQuotationForNewlyCreatedEmployee() {
        Branch branch = branchRepository.findAll().stream().filter(b -> Boolean.TRUE.equals(b.getActive())).findFirst().orElseThrow();

        long ts = System.currentTimeMillis();
        String newEmpEmail = "brand_new_emp_" + ts + "@pravin.com";

        // 1. Create employee via EmployeeService.createEmployee
        User newEmployee = employeeService.createEmployee(
                "Brand New Employee",
                newEmpEmail,
                "Password@123",
                branch.getId()
        );

        assertNotNull(newEmployee.getId());
        assertEquals(Role.EMPLOYEE, newEmployee.getRole());
        assertEquals(true, newEmployee.getActive());
        assertEquals(branch.getId(), newEmployee.getBranch().getId());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                newEmpEmail,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        // 2. Verify builder model contains effectiveBranchId
        ExtendedModelMap model = new ExtendedModelMap();
        String builderView = employeeQuotationController.builder(null, null, auth, model);
        assertEquals("employee/quotation-builder", builderView);
        assertEquals(branch.getId(), model.get("selectedBranchId"));
        assertEquals(branch.getId(), model.get("employeeBranchId"));

        // 3. Prepare items
        WorkCategory cat = workCategoryRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActive())).findFirst().orElseThrow();
        Division div = divisionRepository.findAll().stream().filter(d -> Boolean.TRUE.equals(d.getActive())).findFirst().orElseThrow();
        Item item = itemRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActive())).findFirst().orElseThrow();
        Material mat = materialRepository.findAll().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).findFirst().orElseThrow();
        MaterialOption opt = materialOptionRepository.findAll().stream().filter(o -> Boolean.TRUE.equals(o.getActive())).findFirst().orElseThrow();

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("24"), new BigDecimal("36"), new BigDecimal("30"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), item.getId(), mat.getId(), opt.getId(),
                "Unit New", "Spec New", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Kitchen", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Brand New Customer", "9977665544", "brandnewcust_" + ts + "@test.com", "City"
        );

        // Even if branchId is passed as null, controller defaults to new employee's branch!
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, null, LocalDate.now(), PricingMode.MIDDLE, "Requirements", "Notes",
                List.of(roomReq)
        );

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = employeeQuotationController.saveBuilderQuotation(builderReq, auth, redirectAttributes);

        assertTrue(result.startsWith("redirect:/employee/quotations/view/"), "New employee should redirect to view page");
        String quotationIdStr = result.substring("redirect:/employee/quotations/view/".length());
        Long quotationId = Long.parseLong(quotationIdStr);

        Quotation saved = quotationRepository.findById(quotationId).orElseThrow();
        assertEquals(newEmployee.getId(), saved.getEmployee().getId(), "Quotation employee must match newly created employee");
        assertEquals(branch.getId(), saved.getBranch().getId(), "Quotation branch must match new employee branch");
        assertNotNull(saved.getCustomer());
        assertEquals("Brand New Customer", saved.getCustomer().getName());
        assertEquals("9977665544", saved.getCustomer().getPhone());
    }

    @Test
    void testMockMvcSaveQuotationJsonAcceptAndErrorHandling() throws Exception {
        User klassy = userRepository.findByEmail("klassy@gmail.com").orElseThrow();
        WorkCategory cat = workCategoryRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActive())).findFirst().orElseThrow();
        Division div = divisionRepository.findAll().stream().filter(d -> Boolean.TRUE.equals(d.getActive())).findFirst().orElseThrow();
        Item item = itemRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActive())).findFirst().orElseThrow();
        Material mat = materialRepository.findAll().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).findFirst().orElseThrow();
        MaterialOption opt = materialOptionRepository.findAll().stream().filter(o -> Boolean.TRUE.equals(o.getActive())).findFirst().orElseThrow();

        // 1. Success case with Accept: application/json
        String jsonSuccess = String.format("""
            {
                "customer": {
                    "id": null,
                    "name": "Klassy Customer",
                    "phone": "9944332211",
                    "email": "klassy_cust@example.com",
                    "address": "Madurai"
                },
                "branchId": %d,
                "quotationDate": "2026-10-21",
                "pricingMode": "MIDDLE",
                "rooms": [
                    {
                        "floor": "Ground Floor",
                        "room": "1",
                        "workDescription": "Kitchen",
                        "items": [
                            {
                                "workCategoryId": %d,
                                "divisionId": %d,
                                "itemId": %d,
                                "materialId": %d,
                                "materialOptionId": %d,
                                "itemDescription": "Base cabinet",
                                "specification": "Standard",
                                "sizes": [
                                    {
                                        "length": 30,
                                        "width": 24,
                                        "height": 36,
                                        "offerPrice": null
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        """, klassy.getBranch().getId(), cat.getId(), div.getId(), item.getId(), mat.getId(), opt.getId());

        mockMvc.perform(post("/employee/quotations/builder/save")
                .with(user("klassy@gmail.com").roles("EMPLOYEE"))
                .with(csrf())
                .sessionAttr("OTP_VERIFIED", true)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSuccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectUrl").exists())
                .andExpect(jsonPath("$.quotationId").isNumber());

        // 2. Error case (attempting to create quotation for another branch)
        Long unauthorizedBranchId = klassy.getBranch().getId() == 1L ? 2L : 1L;
        String jsonBranchMismatch = String.format("""
            {
                "customer": {
                    "id": null,
                    "name": "Mismatch Customer",
                    "phone": "9944332299",
                    "email": "mismatch@example.com",
                    "address": "Other City"
                },
                "branchId": %d,
                "quotationDate": "2026-10-21",
                "pricingMode": "MIDDLE",
                "rooms": [
                    {
                        "floor": "Ground Floor",
                        "room": "1",
                        "items": [
                            {
                                "workCategoryId": %d,
                                "divisionId": %d,
                                "itemId": %d,
                                "materialId": %d,
                                "materialOptionId": %d,
                                "sizes": [{ "length": 30, "width": 24, "height": 36 }]
                            }
                        ]
                    }
                ]
            }
        """, unauthorizedBranchId, cat.getId(), div.getId(), item.getId(), mat.getId(), opt.getId());

        mockMvc.perform(post("/employee/quotations/builder/save")
                .with(user("klassy@gmail.com").roles("EMPLOYEE"))
                .with(csrf())
                .sessionAttr("OTP_VERIFIED", true)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBranchMismatch))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized: Cannot create quotations for another branch."));
    }

    @Test
    void testExistingCustomerEmailReuseInSameBranch() {
        User anish = userRepository.findByEmail("anish@gmail.com").orElseThrow();
        Branch branch = anish.getBranch();

        // Customer already exists with this phone / email in anish's branch
        Customer existing = customerRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branch.getId()).get(0);
        assertNotNull(existing.getEmail(), "Existing customer should have email for this test");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                anish.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        WorkCategory cat = workCategoryRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActive())).findFirst().orElseThrow();
        Division div = divisionRepository.findAll().stream().filter(d -> Boolean.TRUE.equals(d.getActive())).findFirst().orElseThrow();
        Item item = itemRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActive())).findFirst().orElseThrow();
        Material mat = materialRepository.findAll().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).findFirst().orElseThrow();
        MaterialOption opt = materialOptionRepository.findAll().stream().filter(o -> Boolean.TRUE.equals(o.getActive())).findFirst().orElseThrow();

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("25"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                cat.getId(), div.getId(), item.getId(), mat.getId(), opt.getId(),
                "Unit", "Spec", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Kitchen", List.of(itemReq)
        );
        // Providing existing customer's email and a different phone number
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                existing.getName(), "9112233445", existing.getEmail(), existing.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE, "Requirements", "Notes",
                List.of(roomReq)
        );

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = employeeQuotationController.saveBuilderQuotation(builderReq, auth, redirectAttributes);

        assertTrue(result.startsWith("redirect:/employee/quotations/view/"));
        String quotationIdStr = result.substring("redirect:/employee/quotations/view/".length());
        Quotation saved = quotationRepository.findById(Long.parseLong(quotationIdStr)).orElseThrow();
        assertEquals(existing.getId(), saved.getCustomer().getId(), "Should reuse existing customer by email in same branch");
    }
}
