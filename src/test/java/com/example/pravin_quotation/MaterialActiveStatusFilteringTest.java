package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.controller.WorkCategoryController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.AdminQuotationBuilderService.*;
import com.example.pravin_quotation.service.WorkCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MaterialActiveStatusFilteringTest {

    @Autowired
    private WorkCategoryService workCategoryService;

    @Autowired
    private WorkCategoryController workCategoryController;

    @Autowired
    private AdminQuotationBuilderController adminBuilderController;

    @Autowired
    private EmployeeQuotationController employeeBuilderController;

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

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

    private Branch branch;
    private WorkCategory category;
    private Division division;
    private Item item;
    private Material activeMaterial;
    private Material inactiveMaterial;
    private MaterialOption activeOption;
    private MaterialOption inactiveOption;
    private User adminUser;
    private User employeeUser;
    private Customer customer;
    private Authentication employeeAuth;

    @BeforeEach
    public void setup() {
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Test Branch " + System.currentTimeMillis());
            b.setBranchAddress("Address");
            b.setWhatsapp("9876543210");
            b.setEmail("branch@test.com");
            b.setActive(true);
            return branchRepository.save(b);
        });

        adminUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Admin User");
                    u.setEmail("admin_" + System.currentTimeMillis() + "@test.com");
                    u.setPassword("password");
                    u.setRole(Role.SUPER_ADMIN);
                    u.setActive(true);
                    return userRepository.save(u);
                });

        employeeUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Employee User");
                    u.setEmail("emp_" + System.currentTimeMillis() + "@test.com");
                    u.setPassword("password");
                    u.setRole(Role.EMPLOYEE);
                    u.setBranch(branch);
                    u.setActive(true);
                    return userRepository.save(u);
                });

        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        customer = new Customer();
        customer.setName("Test Customer " + System.currentTimeMillis());
        customer.setPhone("9988776655");
        customer.setAddress("Customer Address");
        customer.setBranch(branch);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        long ts = System.currentTimeMillis();
        category = new WorkCategory();
        category.setName("Category " + ts);
        category.setActive(true);
        category = workCategoryRepository.save(category);

        division = new Division();
        division.setName("Division " + ts);
        division.setWorkCategory(category);
        division.setActive(true);
        division.setDisplayOrder(1);
        division = divisionRepository.save(division);

        item = new Item();
        item.setName("Item " + ts);
        item.setDivision(division);
        item.setActive(true);
        item.setDisplayOrder(1);
        item = itemRepository.save(item);

        // 1. Create Active Material
        activeMaterial = new Material();
        activeMaterial.setName("ACTIVE MAT " + ts);
        activeMaterial.setItem(item);
        activeMaterial.setDivision(division);
        activeMaterial.setActive(true);
        activeMaterial.setPricingGrade(PricingMode.PREMIUM);
        activeMaterial.setRate(new BigDecimal("1000.00"));
        activeMaterial.setUnit("Sq.ft");
        activeMaterial.setFormula("L × W");
        activeMaterial.setDisplayOrder(1);
        activeMaterial = materialRepository.save(activeMaterial);

        activeOption = new MaterialOption();
        activeOption.setName("Active Opt " + ts);
        activeOption.setMaterial(activeMaterial);
        activeOption.setActive(true);
        activeOption = materialOptionRepository.save(activeOption);

        Pricing p1 = new Pricing();
        p1.setMaterialOption(activeOption);
        p1.setPricingMode(PricingMode.PREMIUM);
        p1.setRate(new BigDecimal("1000.00"));
        p1.setUnit("Sq.ft");
        p1.setActive(true);
        p1 = pricingRepository.save(p1);

        DistrictPricing dp1 = new DistrictPricing();
        dp1.setPricing(p1);
        dp1.setBranch(branch);
        dp1.setRate(new BigDecimal("1000.00"));
        dp1.setActive(true);
        districtPricingRepository.save(dp1);

        // 2. Create Inactive Material
        inactiveMaterial = new Material();
        inactiveMaterial.setName("INACTIVE MAT " + ts);
        inactiveMaterial.setItem(item);
        inactiveMaterial.setDivision(division);
        inactiveMaterial.setActive(false);
        inactiveMaterial.setPricingGrade(PricingMode.PREMIUM);
        inactiveMaterial.setRate(new BigDecimal("800.00"));
        inactiveMaterial.setUnit("Sq.ft");
        inactiveMaterial.setFormula("L × W");
        inactiveMaterial.setDisplayOrder(2);
        inactiveMaterial = materialRepository.save(inactiveMaterial);

        inactiveOption = new MaterialOption();
        inactiveOption.setName("Inactive Opt " + ts);
        inactiveOption.setMaterial(inactiveMaterial);
        inactiveOption.setActive(false);
        inactiveOption = materialOptionRepository.save(inactiveOption);

        Pricing p2 = new Pricing();
        p2.setMaterialOption(inactiveOption);
        p2.setPricingMode(PricingMode.PREMIUM);
        p2.setRate(new BigDecimal("800.00"));
        p2.setUnit("Sq.ft");
        p2.setActive(true);
        p2 = pricingRepository.save(p2);

        DistrictPricing dp2 = new DistrictPricing();
        dp2.setPricing(p2);
        dp2.setBranch(branch);
        dp2.setRate(new BigDecimal("800.00"));
        dp2.setActive(true);
        districtPricingRepository.save(dp2);
    }

    private CustomerRequest createCustomerRequest() {
        return new CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );
    }

    @Test
    @DisplayName("Admin Quotation Builder: Only active materials are returned when onlyAvailable=true")
    public void testAdminBuilderOnlyActiveMaterialsReturned() {
        List<Map<String, Object>> materials = adminBuilderController.materials(
                branch.getId(),
                division.getId(),
                null,
                "PREMIUM",
                true
        );

        assertNotNull(materials);
        assertTrue(materials.stream().anyMatch(m -> m.get("id").equals(activeMaterial.getId())));
        assertFalse(materials.stream().anyMatch(m -> m.get("id").equals(inactiveMaterial.getId())),
                "Inactive material must NOT appear in builder materials when onlyAvailable=true");
    }

    @Test
    @DisplayName("Employee Quotation Builder: Only active materials are returned when onlyAvailable=true")
    public void testEmployeeBuilderOnlyActiveMaterialsReturned() {
        List<Map<String, Object>> materials = employeeBuilderController.builderMaterials(
                branch.getId(),
                division.getId(),
                null,
                "PREMIUM",
                true,
                employeeAuth
        );

        assertNotNull(materials);
        assertTrue(materials.stream().anyMatch(m -> m.get("id").equals(activeMaterial.getId())));
        assertFalse(materials.stream().anyMatch(m -> m.get("id").equals(inactiveMaterial.getId())),
                "Inactive material must NOT appear in employee builder materials when onlyAvailable=true");
    }

    @Test
    @DisplayName("WorkCategoryService: toggleWorkTypeMaterial toggles active status in DB")
    public void testToggleWorkTypeMaterialService() {
        assertTrue(activeMaterial.getActive());

        // Toggle Active -> Inactive
        boolean newStatus = workCategoryService.toggleWorkTypeMaterial(activeMaterial.getId());
        assertFalse(newStatus);

        Material reloaded = materialRepository.findById(activeMaterial.getId()).orElseThrow();
        assertFalse(reloaded.getActive());

        // Also check MaterialOption synced
        List<MaterialOption> opts = materialOptionRepository.findByMaterialIdOrderByDisplayOrderAsc(activeMaterial.getId());
        assertFalse(opts.get(0).getActive());

        // Toggle Inactive -> Active
        boolean reToggledStatus = workCategoryService.toggleWorkTypeMaterial(activeMaterial.getId());
        assertTrue(reToggledStatus);

        Material reloadedAgain = materialRepository.findById(activeMaterial.getId()).orElseThrow();
        assertTrue(reloadedAgain.getActive());
    }

    @Test
    @DisplayName("WorkCategoryController: toggleWorkTypeMaterial API endpoint toggles status")
    public void testToggleWorkTypeMaterialControllerEndpoint() {
        ResponseEntity<?> resp = workCategoryController.toggleWorkTypeMaterial(
                activeMaterial.getId(),
                division.getId(),
                null
        );

        assertEquals(200, resp.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(false, body.get("active"));

        Material reloaded = materialRepository.findById(activeMaterial.getId()).orElseThrow();
        assertFalse(reloaded.getActive());
    }

    @Test
    @DisplayName("Quotation Builder Save: Attempting to save new quotation with inactive material throws IllegalArgumentException")
    public void testSaveNewQuotationWithInactiveMaterialThrows() {
        SizeRequest size = new SizeRequest(
                new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ZERO, null
        );
        ItemRequest itemReq = new ItemRequest(
                category.getId(), division.getId(), item.getId(), inactiveMaterial.getId(), inactiveOption.getId(),
                null, "Inactive mat quotation item", "", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null
        );
        RoomRequest roomReq = new RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        BuilderRequest request = new BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Inactive Test", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> builderService.save(request, adminUser.getEmail())
        );

        assertTrue(ex.getMessage().contains("inactive"),
                "Exception message should mention inactive: " + ex.getMessage());
    }

    @Test
    @DisplayName("Historical Quotation Safety: Deactivating material does not break existing quotation re-saving")
    public void testExistingQuotationPreservedWhenMaterialDeactivated() {
        // 1. Create a quotation with activeMaterial
        SizeRequest size = new SizeRequest(
                new BigDecimal("10"), new BigDecimal("10"), BigDecimal.ZERO, null
        );
        ItemRequest itemReq = new ItemRequest(
                category.getId(), division.getId(), item.getId(), activeMaterial.getId(), activeOption.getId(),
                null, "Active mat quotation item", "", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1000.00")
        );
        RoomRequest roomReq = new RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        BuilderRequest request = new BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Initial Project", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation savedQuotation = builderService.save(request, adminUser.getEmail());
        assertNotNull(savedQuotation);
        assertNotNull(savedQuotation.getId());

        // 2. Now Admin deactivates the material in Master Data
        workCategoryService.toggleWorkTypeMaterial(activeMaterial.getId());
        Material reloadedMat = materialRepository.findById(activeMaterial.getId()).orElseThrow();
        assertFalse(reloadedMat.getActive(), "Material should now be inactive");

        // 3. Re-save / update the existing quotation (e.g. updating remarks or customer requirements)
        BuilderRequest updateRequest = new BuilderRequest(
                savedQuotation.getId(), createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Updated Requirements", "Updated notes",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        // This must NOT throw because the item was already in the quotation
        Quotation updatedQuotation = assertDoesNotThrow(
                () -> builderService.save(updateRequest, adminUser.getEmail()),
                "Re-saving an existing quotation containing a deactivated material must succeed to preserve history"
        );

        assertEquals("Updated Requirements", updatedQuotation.getCustomerRequirements());
    }
}
