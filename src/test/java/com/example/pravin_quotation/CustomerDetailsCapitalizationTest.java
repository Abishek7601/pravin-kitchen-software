package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.util.TitleCaseUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CustomerDetailsCapitalizationTest {

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

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

    @Autowired
    private DivisionRepository divisionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialOptionRepository materialOptionRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private com.example.pravin_quotation.repository.DistrictPricingRepository districtPricingRepository;

    @Autowired
    private ITemplateEngine templateEngine;

    private User adminUser;
    private User employeeUser;
    private Branch branch;
    private Authentication adminAuth;
    private Authentication employeeAuth;
    private WorkCategory workCategory;
    private Division division;
    private Item item;
    private Material material;
    private MaterialOption materialOption;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branch = new Branch();
        branch.setName("Kanyakumari District " + ts);
        branch.setBranchAddress("Main Road, Kanyakumari");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        adminUser = new User();
        adminUser.setName("Admin User " + ts);
        adminUser.setEmail("admin_" + ts + "@example.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Employee User " + ts);
        employeeUser.setEmail("employee_" + ts + "@example.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branch);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

        workCategory = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory cat = new WorkCategory();
            cat.setName("Kitchen");
            cat.setActive(true);
            return workCategoryRepository.save(cat);
        });

        division = divisionRepository.findAll().stream().findFirst().orElseGet(() -> {
            Division div = new Division();
            div.setName("Base Units");
            div.setWorkCategory(workCategory);
            div.setActive(true);
            return divisionRepository.save(div);
        });

        item = itemRepository.findAll().stream().findFirst().orElseGet(() -> {
            Item it = new Item();
            it.setName("Cabinet");
            it.setDivision(division);
            it.setActive(true);
            return itemRepository.save(it);
        });

        material = materialRepository.findAll().stream().findFirst().orElseGet(() -> {
            Material m = new Material();
            m.setName("Marine Ply");
            m.setItem(item);
            m.setActive(true);
            return materialRepository.save(m);
        });

        materialOption = materialOptionRepository.findAll().stream().findFirst().orElseGet(() -> {
            MaterialOption opt = new MaterialOption();
            opt.setName("Grade A 710");
            opt.setMaterial(material);
            opt.setActive(true);
            return materialOptionRepository.save(opt);
        });

        Pricing p = pricingRepository.findByMaterialOptionIdAndPricingMode(materialOption.getId(), PricingMode.MIDDLE)
                .orElseGet(() -> {
                    Pricing newP = new Pricing();
                    newP.setMaterialOption(materialOption);
                    newP.setPricingMode(PricingMode.MIDDLE);
                    newP.setRate(new BigDecimal("350.00"));
                    newP.setFormula("(L + W) * H");
                    newP.setUnit("SQFT");
                    newP.setActive(true);
                    return pricingRepository.save(newP);
                });

        com.example.pravin_quotation.model.DistrictPricing dp = districtPricingRepository.findByBranchIdAndPricingId(branch.getId(), p.getId()).orElse(null);
        if (dp == null) {
            dp = new com.example.pravin_quotation.model.DistrictPricing();
            dp.setBranch(branch);
            dp.setPricing(p);
            dp.setRate(new BigDecimal("350.00"));
            dp.setActive(true);
            districtPricingRepository.save(dp);
        }
    }

    private AdminQuotationBuilderService.BuilderRequest createBuilderRequest(
            String rawName, String rawPhone, String rawEmail, String rawAddress) {

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("24"), new BigDecimal("36"), new BigDecimal("30"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                workCategory.getId(), division.getId(), item.getId(), material.getId(), materialOption.getId(),
                "Item 1", "Specification 1", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Kitchen Area", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                rawName, rawPhone, rawEmail, rawAddress
        );

        return new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE,
                "Customer requirements", "Notes", List.of(roomReq)
        );
    }

    @Test
    void testAdminQuotationCreationCapitalizesCustomerDetails() {
        long ts = System.currentTimeMillis();
        // Lowercase customer details
        String rawName = "abishek kumar";
        String rawPhone = "98" + (ts % 100000000L);
        String rawEmail = "abishek.kumar@example.com";
        String rawAddress = "12, cross street, kanyakumari";

        AdminQuotationBuilderService.BuilderRequest request =
                createBuilderRequest(rawName, rawPhone, rawEmail, rawAddress);

        Quotation quotation = adminQuotationBuilderService.save(request, adminUser.getEmail());

        assertNotNull(quotation.getId());
        Customer savedCustomer = quotation.getCustomer();
        assertNotNull(savedCustomer);

        // Name and address must be capitalized in title case
        assertEquals("Abishek Kumar", savedCustomer.getName());
        assertEquals("12, Cross Street, Kanyakumari", savedCustomer.getAddress());

        // Email must remain lowercase
        assertEquals("abishek.kumar@example.com", savedCustomer.getEmail());

        // Phone number must not be altered
        assertEquals(rawPhone, savedCustomer.getPhone());
    }

    @Test
    void testEmployeeQuotationCreationCapitalizesCustomerDetails() {
        long ts = System.currentTimeMillis() + 10;
        // Lowercase customer details
        String rawName = "pravin kitchens";
        String rawPhone = "97" + (ts % 100000000L);
        String rawEmail = "contact@pravinkitchens.com";
        String rawAddress = "kanyakumari";

        AdminQuotationBuilderService.BuilderRequest request =
                createBuilderRequest(rawName, rawPhone, rawEmail, rawAddress);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = employeeQuotationController.saveBuilderQuotation(request, employeeAuth, redirectAttributes);

        assertTrue(result.startsWith("redirect:/employee/quotations/view/"));
        Long qId = Long.parseLong(result.substring("redirect:/employee/quotations/view/".length()));

        Quotation saved = quotationRepository.findById(qId).orElseThrow();
        Customer savedCustomer = saved.getCustomer();

        // Name and address must be capitalized
        assertEquals("Pravin Kitchens", savedCustomer.getName());
        assertEquals("Kanyakumari", savedCustomer.getAddress());

        // Email must remain lowercase
        assertEquals("contact@pravinkitchens.com", savedCustomer.getEmail());

        // Phone number preserved
        assertEquals(rawPhone, savedCustomer.getPhone());
    }

    @Test
    void testPreserveInternalCasingAndSpecialFormatting() {
        long ts = System.currentTimeMillis() + 20;
        String rawName = "McDonald o'neil";
        String rawPhone = "96" + (ts % 100000000L);
        String rawEmail = "mcdonald@example.com";
        String rawAddress = "Pravin KITCHENS, 2nd floor, anna nagar";

        AdminQuotationBuilderService.BuilderRequest request =
                createBuilderRequest(rawName, rawPhone, rawEmail, rawAddress);

        Quotation quotation = adminQuotationBuilderService.save(request, adminUser.getEmail());
        Customer customer = quotation.getCustomer();

        // "McDonald" must preserve internal 'D', "o'neil" capitalized to "O'neil"
        assertEquals("McDonald O'neil", customer.getName());

        // "Pravin KITCHENS" preserves uppercase, "2nd" preserved, "floor" and "anna nagar" capitalized
        assertEquals("Pravin KITCHENS, 2nd Floor, Anna Nagar", customer.getAddress());
        assertEquals("mcdonald@example.com", customer.getEmail());
    }

    @Test
    void testAdminQuotationBuilderHtmlContainsCapitalizationScriptAndClasses() throws IOException {
        File file = new File("src/main/resources/templates/admin/quotation-builder.html");
        String content = Files.readString(file.toPath());

        assertTrue(content.contains("title-capitalization.js"), "Must include title-capitalization.js");
        assertTrue(content.contains("attachTitleCapitalization"), "Must define or use attachTitleCapitalization");
        assertTrue(content.contains("toTitleCase"), "Must define or use toTitleCase");
        assertTrue(content.contains("id=\"customerName\""), "Must contain customerName input");
        assertTrue(content.contains("id=\"customerAddress\""), "Must contain customerAddress textarea");
    }

    @Test
    void testEmployeeQuotationBuilderHtmlContainsCapitalizationScriptAndClasses() throws IOException {
        File file = new File("src/main/resources/templates/employee/quotation-builder.html");
        String content = Files.readString(file.toPath());

        assertTrue(content.contains("title-capitalization.js"), "Must include title-capitalization.js");
        assertTrue(content.contains("attachTitleCapitalization"), "Must define or use attachTitleCapitalization");
        assertTrue(content.contains("toTitleCase"), "Must define or use toTitleCase");
        assertTrue(content.contains("id=\"customerName\""), "Must contain customerName input");
        assertTrue(content.contains("id=\"customerAddress\""), "Must contain customerAddress textarea");
    }

    @Test
    void testEmployeeQuotationNewHtmlContainsCapitalizationScriptAndClasses() throws IOException {
        File file = new File("src/main/resources/templates/employee/quotation-new.html");
        String content = Files.readString(file.toPath());

        assertTrue(content.contains("title-capitalization.js"), "Must include title-capitalization.js");
        assertTrue(content.contains("attachTitleCapitalization"), "Must define or use attachTitleCapitalization");
        assertTrue(content.contains("id=\"customerName\""), "Must contain customerName input");
        assertTrue(content.contains("id=\"address\""), "Must contain address textarea");
    }

    @Test
    void testStaticTitleCapitalizationJsExistsAndValid() throws IOException {
        File file = new File("src/main/resources/static/js/title-capitalization.js");
        assertTrue(file.exists(), "title-capitalization.js must exist");
        String content = Files.readString(file.toPath());

        assertTrue(content.contains("function toTitleCase"), "Must define toTitleCase function");
        assertTrue(content.contains("function attachTitleCapitalization"), "Must define attachTitleCapitalization");
        assertTrue(content.contains("customerName"), "Must target customerName");
        assertTrue(content.contains("customerAddress"), "Must target customerAddress");
        assertTrue(content.contains("customerEmail"), "Must handle customerEmail without title capitalization");
    }
}
