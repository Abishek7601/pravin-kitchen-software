package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationPdfService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RollMaterialCalculationTest {

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

    @Autowired
    private AdminQuotationBuilderController adminQuotationBuilderController;

    @Autowired
    private EmployeeQuotationController employeeQuotationController;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private QuotationItemSizeRepository quotationItemSizeRepository;

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

    private Branch branch;
    private User adminUser;
    private User employeeUser;
    private Authentication employeeAuth;
    private WorkCategory wallpaperCategory;
    private Division wallpaperDivision;
    private Item wallpaperWorkType;
    private Material rollMaterial;
    private MaterialOption rollOption;
    private Customer customer;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        branch = new Branch();
        branch.setName("Roll Branch " + ts);
        branch.setBranchAddress("Wallpaper Street");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        adminUser = new User();
        adminUser.setName("Admin " + ts);
        adminUser.setEmail("admin_roll_" + ts + "@test.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        employeeUser = new User();
        employeeUser.setName("Employee " + ts);
        employeeUser.setEmail("emp_roll_" + ts + "@test.com");
        employeeUser.setPassword("password");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branch);
        employeeUser.setActive(true);
        employeeUser = userRepository.save(employeeUser);

        employeeAuth = new UsernamePasswordAuthenticationToken(
                employeeUser.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );

        wallpaperCategory = new WorkCategory();
        wallpaperCategory.setName("WALLPAPER_" + ts);
        wallpaperCategory.setDisplayOrder(1);
        wallpaperCategory.setActive(true);
        wallpaperCategory = workCategoryRepository.save(wallpaperCategory);

        wallpaperDivision = new Division();
        wallpaperDivision.setWorkCategory(wallpaperCategory);
        wallpaperDivision.setName("WALLPAPER_DIV_" + ts);
        wallpaperDivision.setActive(true);
        wallpaperDivision = divisionRepository.save(wallpaperDivision);

        wallpaperWorkType = new Item();
        wallpaperWorkType.setDivision(wallpaperDivision);
        wallpaperWorkType.setName("WALLPAPER WORK TYPE " + ts);
        wallpaperWorkType.setActive(true);
        wallpaperWorkType = itemRepository.save(wallpaperWorkType);

        rollMaterial = new Material();
        rollMaterial.setDivision(wallpaperDivision);
        rollMaterial.setItem(wallpaperWorkType);
        rollMaterial.setName("ROYAL WALLPAPER " + ts);
        rollMaterial.setMeasurementType(MeasurementType.ROLL);
        rollMaterial.setFormula("L × H");
        rollMaterial.setCoveragePerRoll(new BigDecimal("40.00"));
        rollMaterial.setUnit("Roll");
        rollMaterial.setActive(true);
        rollMaterial = materialRepository.save(rollMaterial);

        rollOption = new MaterialOption();
        rollOption.setMaterial(rollMaterial);
        rollOption.setName("Standard Pattern");
        rollOption.setActive(true);
        rollOption = materialOptionRepository.save(rollOption);

        Pricing pricing = new Pricing();
        pricing.setMaterialOption(rollOption);
        pricing.setPricingMode(PricingMode.PREMIUM);
        pricing.setRate(new BigDecimal("500.00"));
        pricing.setUnit("Roll");
        pricing.setActive(true);
        pricing = pricingRepository.save(pricing);

        // Branch rate for Premium grade
        DistrictPricing dp = new DistrictPricing();
        dp.setBranch(branch);
        dp.setPricing(pricing);
        dp.setRate(new BigDecimal("500.00"));
        dp.setActive(true);
        districtPricingRepository.save(dp);

        customer = new Customer();
        customer.setName("Wallpaper Client");
        customer.setPhone("9876543210");
        customer.setBranch(branch);
        customer = customerRepository.save(customer);
    }

    @Test
    void testCeilingRollCalculationLogic() {
        BigDecimal coverage = new BigDecimal("40");

        // Sqft = 20 -> Ceil(20/40) = 1 Roll
        BigDecimal sqft20 = new BigDecimal("20");
        BigDecimal rolls20 = sqft20.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(1, rolls20.intValue());

        // Sqft = 40 -> Ceil(40/40) = 1 Roll
        BigDecimal sqft40 = new BigDecimal("40");
        BigDecimal rolls40 = sqft40.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(1, rolls40.intValue());

        // Sqft = 41 -> Ceil(41/40) = 2 Rolls
        BigDecimal sqft41 = new BigDecimal("41");
        BigDecimal rolls41 = sqft41.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(2, rolls41.intValue());

        // Sqft = 80 -> Ceil(80/40) = 2 Rolls
        BigDecimal sqft80 = new BigDecimal("80");
        BigDecimal rolls80 = sqft80.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(2, rolls80.intValue());

        // Sqft = 81 -> Ceil(81/40) = 3 Rolls
        BigDecimal sqft81 = new BigDecimal("81");
        BigDecimal rolls81 = sqft81.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(3, rolls81.intValue());

        // Sqft = 120 -> Ceil(120/40) = 3 Rolls
        BigDecimal sqft120 = new BigDecimal("120");
        BigDecimal rolls120 = sqft120.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(3, rolls120.intValue());

        // Sqft = 121 -> Ceil(121/40) = 4 Rolls
        BigDecimal sqft121 = new BigDecimal("121");
        BigDecimal rolls121 = sqft121.divide(coverage, 0, RoundingMode.CEILING);
        assertEquals(4, rolls121.intValue());
    }

    @Test
    void testAdminSaveRollItemQuotation() {
        // Dimensions: L = 120 in, H = 96 in (Formula: L × H)
        // Area = (120 * 96) / 144 = 11520 / 144 = 80 Sq.ft
        // Rolls required = Ceil(80 / 40) = 2 Rolls
        // Rate = ₹500 per roll -> Amount = 2 * 500 = ₹1,000

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), BigDecimal.ZERO, new BigDecimal("96"), null
        );

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                wallpaperCategory.getId(),
                wallpaperDivision.getId(),
                wallpaperWorkType.getId(),
                rollMaterial.getId(),
                rollOption.getId(),
                null,
                "Royal Wallpaper Wall 1",
                "Textured wallpaper",
                List.of(size1),
                MeasurementType.ROLL,
                null,
                null,
                new BigDecimal("500.00"),
                new BigDecimal("40.00")
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor",
                "Living Room",
                "Living room wallpapering",
                List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branch.getId(),
                LocalDate.now(),
                PricingMode.PREMIUM,
                "Customer specs",
                "Roll Quotation Notes",
                List.of(roomReq),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true
        );

        Quotation quotation = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());
        assertNotNull(quotation);
        assertNotNull(quotation.getId());

        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(quotation.getId());
        assertEquals(1, rooms.size());
        QuotationRoom qRoom = rooms.get(0);

        List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(qRoom.getId());
        assertEquals(1, items.size());

        QuotationItem qItem = items.get(0);
        assertEquals(MeasurementType.ROLL, qItem.getMeasurementType());
        assertTrue(qItem.isRoll());
        assertEquals("L × H", qItem.getFormula());
        assertEquals(new BigDecimal("40.00"), qItem.getCoveragePerRoll());

        // 80.00 Sq.ft
        assertEquals(0, new BigDecimal("80.00").compareTo(qItem.getCalculatedSqft()));
        // 2 Rolls
        assertEquals(0, new BigDecimal("2").compareTo(qItem.getQuantity()));
        // ₹500 Rate
        assertEquals(0, new BigDecimal("500.00").compareTo(qItem.getRate()));
        // ₹1000 Total Amount
        assertEquals(0, new BigDecimal("1000.00").compareTo(qItem.getAmount()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(qRoom.getSubtotal()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(quotation.getSubtotal()));

        // Check child sizes saved in repository
        List<QuotationItemSize> itemSizes = quotationItemSizeRepository.findByQuotationItemIdOrderByDisplayOrderAsc(qItem.getId());
        assertNotNull(itemSizes);
        assertEquals(1, itemSizes.size());
        assertEquals(0, new BigDecimal("120").compareTo(itemSizes.get(0).getLengthValue()));
        assertEquals(0, new BigDecimal("96").compareTo(itemSizes.get(0).getHeightValue()));
    }

    @Test
    void testBuilderLoadReturnsRollCoverageAndCalculations() {
        // Save quotation first
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), BigDecimal.ZERO, new BigDecimal("96"), null
        );

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                wallpaperCategory.getId(),
                wallpaperDivision.getId(),
                wallpaperWorkType.getId(),
                rollMaterial.getId(),
                rollOption.getId(),
                null,
                "Royal Wallpaper Wall 1",
                "Textured wallpaper",
                List.of(size1),
                MeasurementType.ROLL,
                null,
                null,
                new BigDecimal("500.00"),
                new BigDecimal("40.00")
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor",
                "Living Room",
                "Wallpapering",
                List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branch.getId(),
                LocalDate.now(),
                PricingMode.PREMIUM,
                "Customer specs",
                "Roll Load Test",
                List.of(roomReq),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true
        );

        Quotation quotation = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());

        // Test loading quotation via Admin controller
        ResponseEntity<?> loadResponse = adminQuotationBuilderController.loadQuotation(quotation.getId());
        assertTrue(loadResponse.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) loadResponse.getBody();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) body.get("rooms");
        assertEquals(1, rooms.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) rooms.get(0).get("items");
        assertEquals(1, items.size());

        Map<String, Object> loadedItem = items.get(0);
        assertEquals("ROLL", loadedItem.get("measurementType"));
        assertEquals("Roll", loadedItem.get("unit"));
        assertEquals("L × H", loadedItem.get("formula"));
        assertEquals(new BigDecimal("40.00"), loadedItem.get("coveragePerRoll"));
        assertEquals(new BigDecimal("80.00"), loadedItem.get("calculatedSqft"));
        assertEquals(new BigDecimal("2"), loadedItem.get("quantity"));
        assertEquals(new BigDecimal("500.00"), loadedItem.get("rate"));
        assertEquals(new BigDecimal("1000.00"), loadedItem.get("amount"));
    }

    @Test
    void testEmployeeMaterialsAndRateEndpointsReturnCoveragePerRoll() {
        // Test /materials endpoint
        List<Map<String, Object>> materials = employeeQuotationController.builderMaterials(
                branch.getId(), wallpaperDivision.getId(), wallpaperWorkType.getId(), "PREMIUM"
        );
        assertNotNull(materials);
        assertFalse(materials.isEmpty());

        Map<String, Object> matMap = materials.get(0);
        assertEquals("ROLL", matMap.get("measurementType"));
        assertEquals("Roll", matMap.get("unit"));
        assertEquals("L × H", matMap.get("formula"));
        assertEquals(new BigDecimal("40.00"), matMap.get("coveragePerRoll"));
        assertTrue((Boolean) matMap.get("available"));

        // Test /rate endpoint
        ResponseEntity<?> rateResponse = employeeQuotationController.builderRate(
                branch.getId(), wallpaperDivision.getId(), rollOption.getId(), rollMaterial.getId(), PricingMode.PREMIUM, employeeAuth
        );
        assertTrue(rateResponse.getStatusCode().is2xxSuccessful());
        @SuppressWarnings("unchecked")
        Map<String, Object> rateMap = (Map<String, Object>) rateResponse.getBody();
        assertNotNull(rateMap);
        assertEquals("ROLL", rateMap.get("measurementType"));
        assertEquals("Roll", rateMap.get("unit"));
        assertEquals(new BigDecimal("40.00"), rateMap.get("coveragePerRoll"));
        assertEquals(new BigDecimal("500.00"), rateMap.get("rate"));
        assertTrue((Boolean) rateMap.get("available"));
    }

    @Test
    void testPdfGenerationWithRollMaterial() throws Exception {
        // Save quotation with Roll item
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                new BigDecimal("120"), BigDecimal.ZERO, new BigDecimal("96"), null
        );

        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                wallpaperCategory.getId(),
                wallpaperDivision.getId(),
                wallpaperWorkType.getId(),
                rollMaterial.getId(),
                rollOption.getId(),
                null,
                "Royal Wallpaper Wall 1",
                "High quality roll",
                List.of(size1),
                MeasurementType.ROLL,
                null,
                null,
                new BigDecimal("500.00"),
                new BigDecimal("40.00")
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor",
                "Living Room",
                "Living room wallpapering",
                List.of(itemReq)
        );

        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress()
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null,
                custReq,
                branch.getId(),
                LocalDate.now(),
                PricingMode.PREMIUM,
                "Customer specs",
                "Roll Quotation Notes",
                List.of(roomReq),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true
        );

        Quotation quotation = adminQuotationBuilderService.save(builderReq, adminUser.getEmail());

        // Generate PDF
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
