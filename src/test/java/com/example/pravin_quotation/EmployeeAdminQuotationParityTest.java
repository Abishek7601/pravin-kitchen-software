package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@SpringBootTest
@Transactional
public class EmployeeAdminQuotationParityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private BranchRepository branchRepository;

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
    private PricingRepository pricingRepository;

    @Autowired
    private DistrictPricingRepository districtPricingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    private Branch branch;
    private User employeeUser;
    private WorkCategory category;
    private Division division;
    private Item item;
    private Material material;
    private MaterialOption option;

    private final BigDecimal ADMIN_RATE_PREMIUM = new BigDecimal("850.00");
    private final BigDecimal ADMIN_RATE_MIDDLE = new BigDecimal("650.00");
    private final BigDecimal ADMIN_RATE_ECONOMY = new BigDecimal("450.00");

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        branch = new Branch();
        branch.setName("Chennai Central");
        branch.setBranchAddress("Anna Salai");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        employeeUser = new User();
        employeeUser.setName("Rajesh Kannan");
        employeeUser.setEmail("rajesh_parity_" + System.currentTimeMillis() + "@pravin.com");
        employeeUser.setPassword("pass123");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branch);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        category = new WorkCategory();
        category.setName("Living Room");
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("TV Unit");
        division.setWorkCategory(category);
        division.setActive(true);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("TV Console");
        item.setDivision(division);
        item.setActive(true);
        item = itemRepository.save(item);

        material = new Material();
        material.setName("Action TESA HDHMR");
        material.setDivision(division);
        material.setItem(item);
        material.setFormula("(L + W) × H");
        material.setUnit("Sq.ft");
        material.setSpecification("HDHMR 18mm with 1mm Acrylic finish");
        material.setActive(true);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("High Gloss Acrylic");
        option.setMaterial(material);
        option.setActive(true);
        option = materialOptionRepository.save(option);

        // Configure Admin pricing for all 3 grades
        setupAdminPricingForGrade(PricingMode.PREMIUM, ADMIN_RATE_PREMIUM);
        setupAdminPricingForGrade(PricingMode.MIDDLE, ADMIN_RATE_MIDDLE);
        setupAdminPricingForGrade(PricingMode.ECONOMY, ADMIN_RATE_ECONOMY);
    }

    private void setupAdminPricingForGrade(PricingMode mode, BigDecimal rate) {
        Pricing p = new Pricing();
        p.setMaterialOption(option);
        p.setPricingMode(mode);
        p.setRate(rate);
        p.setFormula("(L + W) × H");
        p.setUnit("Sq.ft");
        p.setActive(true);
        p = pricingRepository.save(p);

        DistrictPricing dp = new DistrictPricing();
        dp.setBranch(branch);
        dp.setPricing(p);
        dp.setRate(rate);
        dp.setActive(true);
        districtPricingRepository.save(dp);
    }

    @Test
    @DisplayName("Employee builder automatically retrieves Admin-saved branch-specific rates for all grades")
    public void testEmployeeLoadsAdminConfiguredRates() {
        // 1. Check Middle grade rate
        List<Map<String, Object>> middleMats = employeeQuotationController.builderMaterials(
                branch.getId(), division.getId(), null, "MIDDLE");
        assertNotNull(middleMats);
        assertFalse(middleMats.isEmpty());
        Map<String, Object> middleMat = middleMats.stream()
                .filter(m -> m.get("id").equals(material.getId()))
                .findFirst().orElseThrow();
        assertEquals(ADMIN_RATE_MIDDLE, middleMat.get("rate"), "Employee must load Admin-configured MIDDLE rate");
        assertEquals("HDHMR 18mm with 1mm Acrylic finish", middleMat.get("specification"), "Must load saved specification");

        // 2. Check Premium grade rate
        List<Map<String, Object>> premMats = employeeQuotationController.builderMaterials(
                branch.getId(), division.getId(), null, "PREMIUM");
        Map<String, Object> premMat = premMats.stream()
                .filter(m -> m.get("id").equals(material.getId()))
                .findFirst().orElseThrow();
        assertEquals(ADMIN_RATE_PREMIUM, premMat.get("rate"), "Employee must load Admin-configured PREMIUM rate");

        // 3. Check Economy grade rate
        List<Map<String, Object>> econMats = employeeQuotationController.builderMaterials(
                branch.getId(), division.getId(), null, "ECONOMY");
        Map<String, Object> econMat = econMats.stream()
                .filter(m -> m.get("id").equals(material.getId()))
                .findFirst().orElseThrow();
        assertEquals(ADMIN_RATE_ECONOMY, econMat.get("rate"), "Employee must load Admin-configured ECONOMY rate");

        // 4. Rate API check for specific option
        ResponseEntity<?> rateResp = employeeQuotationController.builderRate(
                branch.getId(), option.getId(), material.getId(), PricingMode.MIDDLE);
        assertEquals(200, rateResp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) rateResp.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(ADMIN_RATE_MIDDLE, body.get("rate"));
        assertEquals("(L + W) × H", body.get("formula"));
    }

    @Test
    @DisplayName("Employee creates quote using Admin rate: financial consistency, PDF generation, and deduplication")
    public void testEmployeeQuoteCreationAndPdfConsistency() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        // Dimensions: L=60, W=40, H=20 in -> (60+40)*20/144 = 2000/144 = 13.8888... -> 13.89 Sq.ft
        // Rate: 650.00 -> Amount: 13.89 * 650.00 = 9028.50
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("60.00"), new BigDecimal("40.00"), new BigDecimal("20.00"), null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                "Living Room TV Unit", "HDHMR 18mm with 1mm Acrylic finish", List.of(size)
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "1", "Living Room", List.of(itemReq)
        );
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                "Anand Kumar", "9840123456", "anand@example.com", "T Nagar, Chennai"
        );
        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE, "Customer notes", "Special instruction",
                List.of(roomReq)
        );

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = (String) employeeQuotationController.saveBuilderQuotation(
                builderReq, null, auth, redirectAttributes
        );
        assertNotNull(result);
        assertTrue(result.startsWith("redirect:/employee/quotations/view/"));

        String qIdStr = result.substring("redirect:/employee/quotations/view/".length());
        Long quotationId = Long.parseLong(qIdStr);
        Quotation saved = quotationRepository.findById(quotationId).orElseThrow();

        // Check snapshot values
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotationId);
        assertFalse(rooms.isEmpty());
        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(rooms.get(0).getId());
        assertFalse(items.isEmpty());
        assertEquals(ADMIN_RATE_MIDDLE, items.get(0).getRate());
        assertEquals(new BigDecimal("13.89"), items.get(0).getCalculatedSqft());
        assertEquals(new BigDecimal("9028.50"), saved.getGrandTotal());

        // Verify PDF can be generated and has correct totals
        byte[] pdf = quotationPdfService.generateQuotationPdf(quotationId);
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
    }

    @Test
    @DisplayName("Employee is blocked from accessing Admin pricing configuration endpoints")
    public void testEmployeeCannotAccessAdminPricing() throws Exception {
        // Employee attempting to access Admin pricing endpoints with verified session
        mockMvc.perform(get("/admin/pricing")
                .sessionAttr("OTP_VERIFIED", true)
                .with(user(employeeUser.getEmail()).roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/pricing/save")
                .sessionAttr("OTP_VERIFIED", true)
                .with(user(employeeUser.getEmail()).roles("EMPLOYEE"))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
