package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.DistrictPricingController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.PricingService;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationItemService;
import com.example.pravin_quotation.service.QuotationRoomService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DistrictPricingRemovalTest {

    @Autowired
    private DistrictPricingController districtPricingController;

    @Autowired
    private QuotationCalculationService quotationCalculationService;

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationRoomService quotationRoomService;

    @Autowired
    private QuotationItemService quotationItemService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private DistrictPricingRepository districtPricingRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

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
    private EntityManager entityManager;

    private Branch branch;
    private User employee;
    private Customer customer;
    private MaterialOption materialOption;
    private Pricing standardPricing;

    @BeforeEach
    void setup() {
        branch = new Branch();
        branch.setName("Chennai Branch");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        employee = new User();
        employee.setName("Test Employee");
        employee.setEmail("test_emp_dp@pravin.com");
        employee.setPassword("secret");
        employee.setRole(Role.EMPLOYEE);
        employee.setBranch(branch);
        employee.setActive(true);
        employee = userRepository.save(employee);

        customer = new Customer();
        customer.setName("John Customer");
        customer.setPhone("9876543210");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);

        WorkCategory category = new WorkCategory();
        category.setName("Living Room");
        category.setActive(true);
        category = workCategoryRepository.save(category);

        Division division = new Division();
        division.setName("TV Unit");
        division.setWorkCategory(category);
        division.setActive(true);
        division = divisionRepository.save(division);

        Item item = new Item();
        item.setName("Modern TV Console");
        item.setDivision(division);
        item.setActive(true);
        item = itemRepository.save(item);

        Material material = new Material();
        material.setName("Marine Plywood");
        material.setItem(item);
        material.setActive(true);
        material = materialRepository.save(material);

        materialOption = new MaterialOption();
        materialOption.setName("18mm BWP");
        materialOption.setMaterial(material);
        materialOption.setActive(true);
        materialOption = materialOptionRepository.save(materialOption);

        // Configure standard pricing: rate = 1250.00
        standardPricing = new Pricing();
        standardPricing.setMaterialOption(materialOption);
        standardPricing.setPricingMode(PricingMode.MIDDLE);
        standardPricing.setRate(new BigDecimal("1250.00"));
        standardPricing.setUnit("SQFT");
        standardPricing.setActive(true);
        standardPricing = pricingRepository.save(standardPricing);
    }

    @Test
    void testDistrictPricingControllerRedirectsToAdminPricing() {
        assertEquals("redirect:/admin/pricing", districtPricingController.redirectToPricing());
    }

    @Test
    void testResolveRateUsesStandardPricingDirectly() {
        // Resolve rate via Admin Quotation Builder Service without needing district pricing
        BigDecimal rateWithBranch = adminQuotationBuilderService.resolveRate(
                branch.getId(), materialOption.getId(), PricingMode.MIDDLE);
        assertEquals(new BigDecimal("1250.00"), rateWithBranch);

        // Even with branchId = null, standard pricing resolves directly
        BigDecimal rateWithoutBranch = adminQuotationBuilderService.resolveRate(
                null, materialOption.getId(), PricingMode.MIDDLE);
        assertEquals(new BigDecimal("1250.00"), rateWithoutBranch);

        // Verify via controller endpoint with and without branchId
        ResponseEntity<?> resp = adminQuotationBuilderController.rate(
                branch.getId(), materialOption.getId(), PricingMode.MIDDLE);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertEquals(new BigDecimal("1250.00"), body.get("rate"));

        ResponseEntity<?> respNoBranch = adminQuotationBuilderController.rate(
                null, materialOption.getId(), PricingMode.MIDDLE);
        assertTrue(respNoBranch.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testEmployeeQuotationRateResolutionWithoutDistrictPricing() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                employee.getEmail(), "secret", Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

        Map<String, Object> response = employeeQuotationController.builderPrice(
                0L, materialOption.getId(), PricingMode.MIDDLE, auth);

        assertNotNull(response);
        assertEquals(new BigDecimal("1250.00"), response.get("rate"));
    }

    @Test
    void testQuotationCalculationResolvesStandardPricingDirectly() {
        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setEmployee(employee);
        quotation.setBranch(branch);
        quotation.setQuotationNumber("QT-STDP-001");
        quotation.setQuotationDate(LocalDate.now());
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation = quotationRepository.save(quotation);

        // resolveItemRate should succeed using standard pricing directly without needing any district pricing
        BigDecimal resolvedRate = quotationCalculationService.resolveItemRate(
                quotation.getId(), materialOption.getId(), PricingMode.MIDDLE);
        assertEquals(new BigDecimal("1250.00"), resolvedRate);
    }

    @Test
    void testFullQuotationCalculationWithAccessoriesTravelChargesAndGst() {
        // Create quotation with branch, travel charges, accessories, discount and GST
        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setEmployee(employee);
        quotation.setBranch(branch);
        quotation.setQuotationNumber("QT-CALC-TEST-001");
        quotation.setQuotationDate(LocalDate.now());
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setTravelCharge(new BigDecimal("500.00"));
        quotation.setAccessoriesAmount(new BigDecimal("1000.00"));
        quotation.setOtherCharges(new BigDecimal("200.00"));
        quotation.setDiscountAmount(new BigDecimal("1420.00"));
        quotation.setGstPercentage(new BigDecimal("18.00")); // 18%
        quotation = quotationRepository.save(quotation);

        QuotationRoom room = quotationRoomService.create(quotation, "Ground Floor", "Living", "TV Unit Area");

        BigDecimal sqft = new BigDecimal("10.00");
        BigDecimal rate = new BigDecimal("1250.00");
        BigDecimal amount = new BigDecimal("12500.00");

        quotationItemService.create(
                room,
                workCategoryRepository.findAll().get(0),
                divisionRepository.findAll().get(0),
                materialOption.getMaterial().getItem(),
                materialOption.getMaterial(),
                materialOption,
                "TV Unit Console",
                new BigDecimal("48"),
                new BigDecimal("30"),
                new BigDecimal("12"),
                sqft,
                rate,
                amount,
                amount,
                "Formula 1",
                "18mm BWP"
        );

        // Calculate quotation
        Quotation calculated = quotationCalculationService.calculateQuotation(quotation.getId());

        // Room subtotal: 12500.00
        // Accessories: 1000.00
        // Travel Charge: 500.00
        // Other Charges: 200.00
        // Subtotal = 12500.00
        // Taxable = 12500.00 + 1000.00 + 500.00 + 200.00 - 1420.00 = 12780.00
        // GST 18% of 12780.00 = 2300.40
        // Grand Total = 12780.00 + 2300.40 = 15080.40
        assertNotNull(calculated);
        assertEquals(new BigDecimal("12500.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("1420.00"), calculated.getDiscountAmount());
        assertEquals(new BigDecimal("12780.00"), calculated.getTaxableAmount());
        assertEquals(new BigDecimal("2300.40"), calculated.getGstAmount());
        assertEquals(new BigDecimal("15080.40"), calculated.getGrandTotal());
    }

    @Test
    void testLegacyDistrictPricingCleanupOnPricingDelete() {
        // Simulate an existing legacy row in district_pricing table pointing to standardPricing
        DistrictPricing legacyRow = new DistrictPricing();
        legacyRow.setBranch(branch);
        legacyRow.setPricing(standardPricing);
        legacyRow.setRate(new BigDecimal("1300.00"));
        legacyRow.setActive(true);
        districtPricingRepository.save(legacyRow);
        entityManager.flush();
        entityManager.clear();

        assertTrue(districtPricingRepository.existsByPricingId(standardPricing.getId()));

        // Now delete the standard pricing via PricingService
        pricingService.deletePricing(standardPricing.getId());
        entityManager.flush();

        // Both the legacy district pricing row and standard pricing row are safely removed
        assertFalse(pricingRepository.existsById(standardPricing.getId()));
        assertFalse(districtPricingRepository.existsByPricingId(standardPricing.getId()));
    }
}
