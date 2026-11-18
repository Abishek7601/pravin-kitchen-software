package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GradeSwitchingMeasurementRecalculationTest {

    private AdminQuotationBuilderService builderService;
    private QuotationItemSizeService quotationItemSizeService;
    private DivisionRepository divisionRepository;
    private WorkCategoryRepository workCategoryRepository;
    private MaterialRepository materialRepository;
    private MaterialOptionRepository materialOptionRepository;
    private MaterialSpecificationRepository materialSpecificationRepository;
    private PricingRepository pricingRepository;
    private DistrictPricingRepository districtPricingRepository;
    private QuotationRepository quotationRepository;
    private CustomerRepository customerRepository;
    private BranchRepository branchRepository;
    private UserRepository userRepository;
    private ItemRepository itemRepository;
    private QuotationRoomRepository quotationRoomRepository;
    private QuotationItemRepository quotationItemRepository;
    private QuotationItemSizeRepository quotationItemSizeRepository;
    private QuotationCalculationService quotationCalculationService;
    private QuotationRoomService quotationRoomService;
    private QuotationItemService quotationItemService;

    private Branch branchChennai;
    private WorkCategory modularKitchenCat;
    private Division wallCabinetDiv;
    private Division wallpaperDiv;
    private Division accessoriesDiv;

    private Material premiumWallCabinetMat;
    private Material middleWallCabinetMat;
    private Material economyWallCabinetMat;

    private Material premiumWallpaperMat;
    private Material middleWallpaperMat;

    private Material premiumBasketMat;
    private Material middleBasketMat;

    private final Map<Long, Division> divisionStore = new HashMap<>();
    private final Map<Long, Material> materialStore = new HashMap<>();
    private final Map<Long, WorkCategory> categoryStore = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> T createMock(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) return false;
                    if (returnType.equals(int.class)) return 0;
                    if (returnType.equals(long.class)) return 0L;
                    if (returnType.equals(double.class)) return 0.0;
                    if (returnType.equals(Optional.class)) return Optional.empty();
                    if (returnType.equals(List.class)) return Collections.emptyList();
                    return null;
                }
        );
    }

    @BeforeEach
    public void setup() {
        divisionStore.clear();
        materialStore.clear();
        categoryStore.clear();

        branchChennai = new Branch();
        branchChennai.setId(1L);
        branchChennai.setName("Chennai");

        modularKitchenCat = new WorkCategory();
        modularKitchenCat.setId(10L);
        modularKitchenCat.setName("Modular Kitchen");
        categoryStore.put(10L, modularKitchenCat);

        wallCabinetDiv = new Division();
        wallCabinetDiv.setId(20L);
        wallCabinetDiv.setName("Wall Cabinet");
        wallCabinetDiv.setWorkCategory(modularKitchenCat);
        divisionStore.put(20L, wallCabinetDiv);

        wallpaperDiv = new Division();
        wallpaperDiv.setId(21L);
        wallpaperDiv.setName("Wallpaper");
        divisionStore.put(21L, wallpaperDiv);

        accessoriesDiv = new Division();
        accessoriesDiv.setId(22L);
        accessoriesDiv.setName("Accessories");
        divisionStore.put(22L, accessoriesDiv);

        // Materials for Wall Cabinet
        premiumWallCabinetMat = new Material();
        premiumWallCabinetMat.setId(100L);
        premiumWallCabinetMat.setName("Wall Cabinet Premium");
        premiumWallCabinetMat.setDivision(wallCabinetDiv);
        premiumWallCabinetMat.setFormula("(L + W) * H");
        premiumWallCabinetMat.setUnit("Sq.ft");
        premiumWallCabinetMat.setMeasurementType(MeasurementType.SQFT);
        premiumWallCabinetMat.setPricingGrade(PricingMode.PREMIUM);
        premiumWallCabinetMat.setRate(new BigDecimal("1200.00"));
        premiumWallCabinetMat.setActive(true);
        materialStore.put(100L, premiumWallCabinetMat);

        middleWallCabinetMat = new Material();
        middleWallCabinetMat.setId(101L);
        middleWallCabinetMat.setName("Wall Cabinet Middle");
        middleWallCabinetMat.setDivision(wallCabinetDiv);
        middleWallCabinetMat.setFormula("L * W");
        middleWallCabinetMat.setUnit("Sq.ft");
        middleWallCabinetMat.setMeasurementType(MeasurementType.SQFT);
        middleWallCabinetMat.setPricingGrade(PricingMode.MIDDLE);
        middleWallCabinetMat.setRate(new BigDecimal("950.00"));
        middleWallCabinetMat.setActive(true);
        materialStore.put(101L, middleWallCabinetMat);

        economyWallCabinetMat = new Material();
        economyWallCabinetMat.setId(102L);
        economyWallCabinetMat.setName("Wall Cabinet Economy");
        economyWallCabinetMat.setDivision(wallCabinetDiv);
        economyWallCabinetMat.setFormula("L * H");
        economyWallCabinetMat.setUnit("Sq.ft");
        economyWallCabinetMat.setMeasurementType(MeasurementType.SQFT);
        economyWallCabinetMat.setPricingGrade(PricingMode.ECONOMY);
        economyWallCabinetMat.setRate(new BigDecimal("700.00"));
        economyWallCabinetMat.setActive(true);
        materialStore.put(102L, economyWallCabinetMat);

        // Materials for Wallpaper (Roll)
        premiumWallpaperMat = new Material();
        premiumWallpaperMat.setId(200L);
        premiumWallpaperMat.setName("Premium Wallpaper");
        premiumWallpaperMat.setDivision(wallpaperDiv);
        premiumWallpaperMat.setFormula("L * H");
        premiumWallpaperMat.setUnit("Roll");
        premiumWallpaperMat.setMeasurementType(MeasurementType.ROLL);
        premiumWallpaperMat.setCoveragePerRoll(new BigDecimal("50.00"));
        premiumWallpaperMat.setPricingGrade(PricingMode.PREMIUM);
        premiumWallpaperMat.setRate(new BigDecimal("3500.00"));
        premiumWallpaperMat.setActive(true);
        materialStore.put(200L, premiumWallpaperMat);

        middleWallpaperMat = new Material();
        middleWallpaperMat.setId(201L);
        middleWallpaperMat.setName("Middle Wallpaper");
        middleWallpaperMat.setDivision(wallpaperDiv);
        middleWallpaperMat.setFormula("L * H");
        middleWallpaperMat.setUnit("Roll");
        middleWallpaperMat.setMeasurementType(MeasurementType.ROLL);
        middleWallpaperMat.setCoveragePerRoll(new BigDecimal("40.00"));
        middleWallpaperMat.setPricingGrade(PricingMode.MIDDLE);
        middleWallpaperMat.setRate(new BigDecimal("2500.00"));
        middleWallpaperMat.setActive(true);
        materialStore.put(201L, middleWallpaperMat);

        // Materials for Accessories (Quantity)
        premiumBasketMat = new Material();
        premiumBasketMat.setId(300L);
        premiumBasketMat.setName("Plain Basket Premium");
        premiumBasketMat.setDivision(accessoriesDiv);
        premiumBasketMat.setUnit("Quantity");
        premiumBasketMat.setMeasurementType(MeasurementType.QUANTITY);
        premiumBasketMat.setPricingGrade(PricingMode.PREMIUM);
        premiumBasketMat.setRate(new BigDecimal("6000.00"));
        premiumBasketMat.setActive(true);
        materialStore.put(300L, premiumBasketMat);

        middleBasketMat = new Material();
        middleBasketMat.setId(301L);
        middleBasketMat.setName("Plain Basket Middle");
        middleBasketMat.setDivision(accessoriesDiv);
        middleBasketMat.setUnit("Quantity");
        middleBasketMat.setMeasurementType(MeasurementType.QUANTITY);
        middleBasketMat.setPricingGrade(PricingMode.MIDDLE);
        middleBasketMat.setRate(new BigDecimal("4500.00"));
        middleBasketMat.setActive(true);
        materialStore.put(301L, middleBasketMat);

        // Repositories
        divisionRepository = (DivisionRepository) Proxy.newProxyInstance(
                DivisionRepository.class.getClassLoader(),
                new Class<?>[]{DivisionRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.ofNullable(divisionStore.get((Long) args[0]));
                    }
                    return null;
                }
        );

        materialRepository = (MaterialRepository) Proxy.newProxyInstance(
                MaterialRepository.class.getClassLoader(),
                new Class<?>[]{MaterialRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        return Optional.ofNullable(materialStore.get((Long) args[0]));
                    }
                    if ("findActiveByDivisionIdAndPricingGrade".equals(name) || "findAllByDivisionIdAndPricingGrade".equals(name)) {
                        Long divId = (Long) args[0];
                        PricingMode grade = (PricingMode) args[1];
                        return materialStore.values().stream()
                                .filter(m -> m.getDivision() != null && Objects.equals(m.getDivision().getId(), divId)
                                        && m.getPricingGrade() == grade && Boolean.TRUE.equals(m.getActive()))
                                .toList();
                    }
                    if ("findActiveByDivisionId".equals(name) || "findByDivisionId".equals(name)) {
                        Long divId = (Long) args[0];
                        return materialStore.values().stream()
                                .filter(m -> m.getDivision() != null && Objects.equals(m.getDivision().getId(), divId)
                                        && Boolean.TRUE.equals(m.getActive()))
                                .toList();
                    }
                    return Collections.emptyList();
                }
        );

        workCategoryRepository = (WorkCategoryRepository) Proxy.newProxyInstance(
                WorkCategoryRepository.class.getClassLoader(),
                new Class<?>[]{WorkCategoryRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.ofNullable(categoryStore.get((Long) args[0]));
                    }
                    return null;
                }
        );

        pricingRepository = createMock(PricingRepository.class);
        materialOptionRepository = createMock(MaterialOptionRepository.class);
        materialSpecificationRepository = createMock(MaterialSpecificationRepository.class);
        districtPricingRepository = createMock(DistrictPricingRepository.class);
        customerRepository = createMock(CustomerRepository.class);
        itemRepository = createMock(ItemRepository.class);
        quotationRepository = createMock(QuotationRepository.class);
        quotationRoomRepository = createMock(QuotationRoomRepository.class);
        quotationItemRepository = createMock(QuotationItemRepository.class);
        quotationItemSizeRepository = createMock(QuotationItemSizeRepository.class);

        branchRepository = (BranchRepository) Proxy.newProxyInstance(
                BranchRepository.class.getClassLoader(),
                new Class<?>[]{BranchRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(branchChennai);
                    }
                    return Optional.empty();
                }
        );

        userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> Optional.empty()
        );

        quotationItemSizeService = new QuotationItemSizeService(quotationItemSizeRepository);
        quotationCalculationService = new QuotationCalculationService(
                quotationRepository,
                quotationRoomRepository,
                quotationItemRepository,
                pricingRepository,
                null,
                quotationItemSizeService
        );
        QuotationWorkflowRepository quotationWorkflowRepository = createMock(QuotationWorkflowRepository.class);
        QuotationWorkflowService quotationWorkflowService = new QuotationWorkflowService(quotationWorkflowRepository);
        quotationRoomService = new QuotationRoomService(quotationRoomRepository, quotationWorkflowService, quotationRepository);
        quotationItemService = new QuotationItemService(quotationItemRepository, quotationWorkflowService);

        builderService = new AdminQuotationBuilderService(
                quotationRepository,
                customerRepository,
                userRepository,
                branchRepository,
                workCategoryRepository,
                divisionRepository,
                itemRepository,
                materialRepository,
                materialOptionRepository,
                materialSpecificationRepository,
                pricingRepository,
                districtPricingRepository,
                quotationRoomService,
                quotationItemService,
                quotationItemSizeService,
                quotationCalculationService
        );
    }

    @Test
    @DisplayName("User switches from Premium to Middle Grade: Wall Cabinet measurements (120x60x90) recalculated with Middle rate & formula")
    public void testSwitchFromPremiumToMiddleGradeRecalculation() {
        // Step 1: User created item in Premium with Length: 120, Width: 60, Height: 90
        SizeRequest size = new SizeRequest(new BigDecimal("120.0"), new BigDecimal("60.0"), new BigDecimal("90.0"), null);
        ItemRequest premiumItem = new ItemRequest(
                10L, // category Modular Kitchen
                20L, // division Wall Cabinet
                null,
                100L, // Premium material ID
                null,
                null,
                "Wall Cabinet Premium",
                null,
                List.of(size),
                MeasurementType.SQFT,
                BigDecimal.ONE,
                null,
                new BigDecimal("1200.00"), // Premium rate
                null,
                PricingMode.PREMIUM
        );

        RoomRequest room = new RoomRequest(
                "Ground Floor",
                "1",
                "Modular Kitchen Room",
                List.of(premiumItem)
        );

        // Step 2: Switch to Middle Grade
        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, List.of(room));

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals(PricingMode.MIDDLE, response.targetGrade());
        assertEquals(1, response.rooms().size());

        SwitchGradeRoomResponse updatedRoom = response.rooms().get(0);
        assertEquals("Ground Floor", updatedRoom.floor());
        assertEquals("1", updatedRoom.room());
        assertEquals(1, updatedRoom.items().size());

        SwitchGradeItemResponse updatedItem = updatedRoom.items().get(0);
        // Verify Middle material counterpart was matched
        assertEquals(101L, updatedItem.materialId(), "Counterpart Middle material ID must be 101");
        assertEquals("Wall Cabinet Middle".toUpperCase(), updatedItem.materialName().toUpperCase());
        assertEquals(new BigDecimal("950.00"), updatedItem.rate(), "Middle rate must be 950.00");
        assertEquals("L * W", updatedItem.formula(), "Middle formula must be L * W");
        assertEquals(PricingMode.MIDDLE, updatedItem.pricingMode());
        assertFalse(updatedItem.rateUnavailable());

        // Verify measurements are preserved: Length 120, Width 60, Height 90
        assertEquals(1, updatedItem.sizes().size());
        SizeRequest updatedSize = updatedItem.sizes().get(0);
        assertEquals(new BigDecimal("120.0"), updatedSize.length());
        assertEquals(new BigDecimal("60.0"), updatedSize.width());
        assertEquals(new BigDecimal("90.0"), updatedSize.height());

        // Middle formula is L * W / 144 = 120 * 60 / 144 = 50.00 Sq.ft
        BigDecimal expectedSqft = new BigDecimal("50.00");
        assertEquals(expectedSqft, updatedItem.calculatedSqft());

        // Middle line amount = 50.00 Sq.ft * 950.00 = 47,500.00
        BigDecimal expectedAmount = new BigDecimal("47500.00");
        assertEquals(expectedAmount, updatedItem.lineAmount());

        // Subtotals check
        assertEquals(expectedAmount, response.gradeSubtotals().get("MIDDLE"));
        assertEquals(BigDecimal.ZERO, response.gradeSubtotals().get("PREMIUM"));
        assertEquals(expectedAmount, response.grandTotal());
    }

    @Test
    @DisplayName("Switch to Economy Grade: uses Economy counterpart, rate (700.00), and formula (L * H)")
    public void testSwitchToEconomyGradeRecalculation() {
        SizeRequest size = new SizeRequest(new BigDecimal("120.0"), new BigDecimal("60.0"), new BigDecimal("90.0"), null);
        ItemRequest middleItem = new ItemRequest(
                10L,
                20L,
                null,
                101L,
                null,
                null,
                "Wall Cabinet Middle",
                null,
                List.of(size),
                MeasurementType.SQFT,
                BigDecimal.ONE,
                null,
                new BigDecimal("950.00"),
                null,
                PricingMode.MIDDLE
        );

        RoomRequest room = new RoomRequest("First Floor", "2", "Kitchen", List.of(middleItem));

        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, List.of(room));

        assertNotNull(response);
        SwitchGradeItemResponse updatedItem = response.rooms().get(0).items().get(0);
        assertEquals(102L, updatedItem.materialId());
        assertEquals("Wall Cabinet Economy".toUpperCase(), updatedItem.materialName().toUpperCase());
        assertEquals(new BigDecimal("700.00"), updatedItem.rate());
        assertEquals("L * H", updatedItem.formula());

        // Economy formula is L * H / 144 = 120 * 90 / 144 = 75.00 Sq.ft
        assertEquals(new BigDecimal("75.00"), updatedItem.calculatedSqft());

        // Amount = 75.00 * 700.00 = 52,500.00
        assertEquals(new BigDecimal("52500.00"), updatedItem.lineAmount());
        assertEquals(new BigDecimal("52500.00"), response.gradeSubtotals().get("ECONOMY"));
    }

    @Test
    @DisplayName("Roll-based Material Grade Switching: Recalculates formula sqft, ceiling roll count and roll rate")
    public void testRollBasedMaterialGradeSwitching() {
        // Premium Wallpaper with Length: 240, Height: 120 (Formula L * H)
        SizeRequest size = new SizeRequest(new BigDecimal("240.0"), BigDecimal.ZERO, new BigDecimal("120.0"), null);
        ItemRequest premiumRollItem = new ItemRequest(
                null,
                21L, // Wallpaper
                null,
                200L, // Premium Wallpaper
                null,
                null,
                "Premium Wallpaper",
                null,
                List.of(size),
                MeasurementType.ROLL,
                BigDecimal.ONE,
                null,
                new BigDecimal("3500.00"),
                new BigDecimal("50.00"),
                PricingMode.PREMIUM
        );

        RoomRequest room = new RoomRequest("Ground Floor", "1", "Living Room", List.of(premiumRollItem));

        // Switch to Middle Grade (Middle Wallpaper coverage 40 sqft, rate 2500)
        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, List.of(room));

        assertNotNull(response);
        SwitchGradeItemResponse updatedItem = response.rooms().get(0).items().get(0);
        assertEquals(201L, updatedItem.materialId());
        assertEquals(new BigDecimal("2500.00"), updatedItem.rate());
        assertEquals(new BigDecimal("40.00"), updatedItem.coveragePerRoll());

        // Sq.ft = 240 * 120 / 144 = 200.00 Sq.ft
        assertEquals(new BigDecimal("200.00"), updatedItem.calculatedSqft());

        // Rolls = 200 / 40 = 5 rolls
        assertEquals(new BigDecimal("5"), updatedItem.quantity());

        // Amount = 5 * 2500 = 12,500.00
        assertEquals(new BigDecimal("12500.00"), updatedItem.lineAmount());
    }

    @Test
    @DisplayName("Quantity-based Material Grade Switching: Preserves entered quantity and applies target grade rate")
    public void testQuantityBasedMaterialGradeSwitching() {
        ItemRequest premiumBasket = new ItemRequest(
                null,
                22L, // Accessories
                null,
                300L, // Premium Basket
                null,
                null,
                "Plain Basket Premium",
                null,
                Collections.emptyList(),
                MeasurementType.QUANTITY,
                new BigDecimal("3"), // Quantity = 3
                null,
                new BigDecimal("6000.00"),
                null,
                PricingMode.PREMIUM
        );

        RoomRequest room = new RoomRequest("Ground Floor", "1", "Kitchen", List.of(premiumBasket));

        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, List.of(room));

        assertNotNull(response);
        SwitchGradeItemResponse updatedItem = response.rooms().get(0).items().get(0);
        assertEquals(301L, updatedItem.materialId());
        assertEquals("Plain Basket Middle".toUpperCase(), updatedItem.materialName().toUpperCase());
        assertEquals(new BigDecimal("4500.00"), updatedItem.rate());
        assertEquals(new BigDecimal("3"), updatedItem.quantity());

        // Amount = 3 * 4500 = 13,500.00
        assertEquals(new BigDecimal("13500.00"), updatedItem.lineAmount());
    }

    @Test
    @DisplayName("Unavailable material in target grade: rate is null, amount is zero, rateUnavailable is true with clear notice")
    public void testUnavailableMaterialInTargetGrade() {
        // Wallpaper has NO Economy material in materialStore
        SizeRequest size = new SizeRequest(new BigDecimal("240.0"), BigDecimal.ZERO, new BigDecimal("120.0"), null);
        ItemRequest middleRollItem = new ItemRequest(
                null,
                21L, // Wallpaper
                null,
                201L,
                null,
                null,
                "Middle Wallpaper",
                null,
                List.of(size),
                MeasurementType.ROLL,
                new BigDecimal("5"),
                null,
                new BigDecimal("2500.00"),
                new BigDecimal("40.00"),
                PricingMode.MIDDLE
        );

        RoomRequest room = new RoomRequest("Ground Floor", "1", "Living Room", List.of(middleRollItem));

        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, List.of(room));

        assertNotNull(response);
        SwitchGradeItemResponse updatedItem = response.rooms().get(0).items().get(0);
        assertTrue(updatedItem.rateUnavailable(), "rateUnavailable must be true");
        assertNull(updatedItem.rate(), "Rate must be null");
        assertEquals(BigDecimal.ZERO, updatedItem.lineAmount(), "Line amount must be zero");
        assertNotNull(updatedItem.unavailableMessage(), "Notice must explain unavailability");
        assertTrue(updatedItem.unavailableMessage().contains("Economy Grade"));

        // Measurement data must remain intact so user does not lose entered values
        assertEquals(1, updatedItem.sizes().size());
        assertEquals(new BigDecimal("240.0"), updatedItem.sizes().get(0).length());
    }

    @Test
    @DisplayName("Controller parity: Admin and Employee controllers both expose switch-grade endpoint and respond identically")
    public void testControllerParity() {
        AdminQuotationBuilderController adminController = new AdminQuotationBuilderController(
                branchRepository,
                customerRepository,
                workCategoryRepository,
                divisionRepository,
                itemRepository,
                materialRepository,
                materialOptionRepository,
                materialSpecificationRepository,
                builderService,
                quotationRepository,
                quotationRoomRepository,
                quotationItemRepository,
                quotationItemSizeRepository,
                pricingRepository,
                null,
                quotationCalculationService
        );

        EmployeeQuotationController employeeController = new EmployeeQuotationController(
                quotationRepository,
                userRepository,
                customerRepository,
                branchRepository,
                divisionRepository,
                pricingRepository,
                workCategoryRepository,
                itemRepository,
                materialRepository,
                materialOptionRepository,
                materialSpecificationRepository,
                builderService,
                null,
                null,
                quotationRoomService,
                quotationItemService,
                quotationItemSizeService,
                quotationCalculationService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        SizeRequest size = new SizeRequest(new BigDecimal("120.0"), new BigDecimal("60.0"), new BigDecimal("90.0"), null);
        ItemRequest item = new ItemRequest(
                10L,
                20L,
                null,
                100L,
                null,
                null,
                "Wall Cabinet Premium",
                null,
                List.of(size),
                MeasurementType.SQFT,
                BigDecimal.ONE,
                null,
                new BigDecimal("1200.00"),
                null,
                PricingMode.PREMIUM
        );
        RoomRequest room = new RoomRequest("Ground Floor", "1", "Kitchen", List.of(item));
        SwitchGradeRequest req = new SwitchGradeRequest(branchChennai.getId(), PricingMode.MIDDLE, List.of(room));

        ResponseEntity<?> adminResp = adminController.switchGrade(req);
        assertEquals(200, adminResp.getStatusCode().value());
        assertTrue(adminResp.getBody() instanceof SwitchGradeResponse);
        SwitchGradeResponse adminData = (SwitchGradeResponse) adminResp.getBody();
        assertEquals(101L, adminData.rooms().get(0).items().get(0).materialId());

        ResponseEntity<?> empResp = employeeController.switchGrade(req, null);
        assertEquals(200, empResp.getStatusCode().value());
        assertTrue(empResp.getBody() instanceof SwitchGradeResponse);
        SwitchGradeResponse empData = (SwitchGradeResponse) empResp.getBody();
        assertEquals(101L, empData.rooms().get(0).items().get(0).materialId());
        assertEquals(adminData.grandTotal(), empData.grandTotal());
    }

    @Test
    @DisplayName("Full Grade Cycle: Premium -> Middle -> Economy -> Premium correctly updates materials, formulas, sqft, rates and subtotals at each step")
    public void testFullCycleSwitchingPremiumMiddleEconomyPremium() {
        // Step 1: Start with Premium Wall Cabinet (L: 120, W: 60, H: 90)
        // Formula: (L + W) * H / 144 = (120 + 60) * 90 / 144 = 180 * 90 / 144 = 112.50 Sq.ft
        // Rate: 1200.00 -> Amount: 112.50 * 1200 = 135,000.00
        SizeRequest initialSize = new SizeRequest(new BigDecimal("120.0"), new BigDecimal("60.0"), new BigDecimal("90.0"), null);
        ItemRequest item = new ItemRequest(
                10L, 20L, null, 100L, null, null,
                "Wall Cabinet Premium", null, List.of(initialSize),
                MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1200.00"), null, PricingMode.PREMIUM
        );
        RoomRequest room = new RoomRequest("Ground Floor", "1", "Kitchen", List.of(item));

        // Switch to Middle
        SwitchGradeResponse toMiddle = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, List.of(room));
        assertEquals(PricingMode.MIDDLE, toMiddle.targetGrade());
        SwitchGradeItemResponse middleItem = toMiddle.rooms().get(0).items().get(0);
        assertEquals(101L, middleItem.materialId());
        assertEquals("L * W", middleItem.formula());
        assertEquals(new BigDecimal("50.00"), middleItem.calculatedSqft()); // 120 * 60 / 144 = 50
        assertEquals(new BigDecimal("950.00"), middleItem.rate());
        assertEquals(new BigDecimal("47500.00"), middleItem.lineAmount());

        // Prepare room for next switch
        RoomRequest roomFromMiddle = new RoomRequest(
                "Ground Floor", "1", "Kitchen",
                List.of(new ItemRequest(
                        10L, 20L, null, middleItem.materialId(), null, null,
                        middleItem.materialName(), null, middleItem.sizes(),
                        MeasurementType.SQFT, BigDecimal.ONE, null, middleItem.rate(), null, PricingMode.MIDDLE
                ))
        );

        // Switch from Middle to Economy
        SwitchGradeResponse toEconomy = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, List.of(roomFromMiddle));
        assertEquals(PricingMode.ECONOMY, toEconomy.targetGrade());
        SwitchGradeItemResponse economyItem = toEconomy.rooms().get(0).items().get(0);
        assertEquals(102L, economyItem.materialId());
        assertEquals("L * H", economyItem.formula());
        assertEquals(new BigDecimal("75.00"), economyItem.calculatedSqft()); // 120 * 90 / 144 = 75
        assertEquals(new BigDecimal("700.00"), economyItem.rate());
        assertEquals(new BigDecimal("52500.00"), economyItem.lineAmount());

        // Prepare room for switch back to Premium
        RoomRequest roomFromEconomy = new RoomRequest(
                "Ground Floor", "1", "Kitchen",
                List.of(new ItemRequest(
                        10L, 20L, null, economyItem.materialId(), null, null,
                        economyItem.materialName(), null, economyItem.sizes(),
                        MeasurementType.SQFT, BigDecimal.ONE, null, economyItem.rate(), null, PricingMode.ECONOMY
                ))
        );

        // Switch from Economy back to Premium
        SwitchGradeResponse backToPremium = builderService.switchGrade(branchChennai.getId(), PricingMode.PREMIUM, List.of(roomFromEconomy));
        assertEquals(PricingMode.PREMIUM, backToPremium.targetGrade());
        SwitchGradeItemResponse backPremiumItem = backToPremium.rooms().get(0).items().get(0);
        assertEquals(100L, backPremiumItem.materialId());
        assertEquals("(L + W) * H", backPremiumItem.formula());
        assertEquals(new BigDecimal("112.50"), backPremiumItem.calculatedSqft());
        assertEquals(new BigDecimal("1200.00"), backPremiumItem.rate());
        assertEquals(new BigDecimal("135000.00"), backPremiumItem.lineAmount());
        assertEquals(new BigDecimal("135000.00"), backToPremium.gradeSubtotals().get("PREMIUM"));
        assertEquals(new BigDecimal("135000.00"), backToPremium.grandTotal());
    }

    @Test
    @DisplayName("Multi-Room quotation switching: preserves all floors, rooms, and recalculates mixed material types")
    public void testMultiRoomQuotationGradeSwitching() {
        // Room 1: Ground Floor - 1 (Kitchen): Wall Cabinet (Sqft)
        SizeRequest s1 = new SizeRequest(new BigDecimal("120.0"), new BigDecimal("60.0"), new BigDecimal("90.0"), null);
        ItemRequest item1 = new ItemRequest(10L, 20L, null, 100L, null, null, "Wall Cabinet Premium", null, List.of(s1), MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1200.00"), null, PricingMode.PREMIUM);
        RoomRequest room1 = new RoomRequest("Ground Floor", "1", "Kitchen", List.of(item1));

        // Room 2: First Floor - 1 (Living Room): Wallpaper (Roll)
        SizeRequest s2 = new SizeRequest(new BigDecimal("240.0"), BigDecimal.ZERO, new BigDecimal("120.0"), null);
        ItemRequest item2 = new ItemRequest(null, 21L, null, 200L, null, null, "Premium Wallpaper", null, List.of(s2), MeasurementType.ROLL, BigDecimal.ONE, null, new BigDecimal("3500.00"), new BigDecimal("50.00"), PricingMode.PREMIUM);
        RoomRequest room2 = new RoomRequest("First Floor", "1", "Living Room", List.of(item2));

        // Room 3: First Floor - 2 (Dining): Plain Basket (Quantity)
        ItemRequest item3 = new ItemRequest(null, 22L, null, 300L, null, null, "Plain Basket Premium", null, Collections.emptyList(), MeasurementType.QUANTITY, new BigDecimal("2"), null, new BigDecimal("6000.00"), null, PricingMode.PREMIUM);
        RoomRequest room3 = new RoomRequest("First Floor", "2", "Dining", List.of(item3));

        // Switch all rooms to Middle Grade
        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, List.of(room1, room2, room3));

        assertNotNull(response);
        assertEquals(3, response.rooms().size());

        // Room 1: Ground Floor - 1
        SwitchGradeRoomResponse r1 = response.rooms().get(0);
        assertEquals("Ground Floor", r1.floor());
        assertEquals("1", r1.room());
        assertEquals("Ground Floor||1", r1.key());
        SwitchGradeItemResponse i1 = r1.items().get(0);
        assertEquals(101L, i1.materialId());
        assertEquals(new BigDecimal("47500.00"), i1.lineAmount());

        // Room 2: First Floor - 1
        SwitchGradeRoomResponse r2 = response.rooms().get(1);
        assertEquals("First Floor", r2.floor());
        assertEquals("1", r2.room());
        assertEquals("First Floor||1", r2.key());
        SwitchGradeItemResponse i2 = r2.items().get(0);
        assertEquals(201L, i2.materialId());
        assertEquals(new BigDecimal("12500.00"), i2.lineAmount());

        // Room 3: First Floor - 2
        SwitchGradeRoomResponse r3 = response.rooms().get(2);
        assertEquals("First Floor", r3.floor());
        assertEquals("2", r3.room());
        assertEquals("First Floor||2", r3.key());
        SwitchGradeItemResponse i3 = r3.items().get(0);
        assertEquals(301L, i3.materialId());
        assertEquals(new BigDecimal("9000.00"), i3.lineAmount()); // 2 * 4500.00

        // Grand Total = 47500 + 12500 + 9000 = 69,000.00
        BigDecimal expectedGrand = new BigDecimal("69000.00");
        assertEquals(expectedGrand, response.gradeSubtotals().get("MIDDLE"));
        assertEquals(expectedGrand, response.grandTotal());
    }

    @Test
    @DisplayName("Grade switching across different material names: Automatically loads target-grade material for work type")
    public void testUnmatchedMaterialDoesNotSilentlyMapToArbitraryMaterial() {
        // Create a division with an Economy material named "Steel Grille Economy"
        Division doorsDiv = new Division();
        doorsDiv.setId(30L);
        doorsDiv.setName("Doors");
        divisionStore.put(30L, doorsDiv);

        Material steelGrille = new Material();
        steelGrille.setId(401L);
        steelGrille.setName("Steel Grille Economy");
        steelGrille.setDivision(doorsDiv);
        steelGrille.setPricingGrade(PricingMode.ECONOMY);
        steelGrille.setMeasurementType(MeasurementType.QUANTITY);
        steelGrille.setRate(new BigDecimal("400.00"));
        steelGrille.setSpecification("16 Gauge Mild Steel Grille");
        steelGrille.setActive(true);
        materialStore.put(401L, steelGrille);

        // Premium item is "Teak Wood Carved Door Premium"
        Material teakDoor = new Material();
        teakDoor.setId(400L);
        teakDoor.setName("Teak Wood Carved Door Premium");
        teakDoor.setDivision(doorsDiv);
        teakDoor.setPricingGrade(PricingMode.PREMIUM);
        teakDoor.setMeasurementType(MeasurementType.QUANTITY);
        teakDoor.setRate(new BigDecimal("8000.00"));
        teakDoor.setSpecification("First Quality Burma Teak");
        teakDoor.setActive(true);
        materialStore.put(400L, teakDoor);

        ItemRequest item = new ItemRequest(null, 30L, null, 400L, null, null, "Teak Wood Carved Door Premium", "First Quality Burma Teak", Collections.emptyList(), MeasurementType.QUANTITY, new BigDecimal("2"), null, new BigDecimal("8000.00"), null, PricingMode.PREMIUM);
        RoomRequest room = new RoomRequest("Ground Floor", "1", "Main Entry", List.of(item));

        // Switch to Economy: "Steel Grille Economy" has a different name but is the configured material for Doors in Economy
        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, List.of(room));

        assertNotNull(response);
        SwitchGradeItemResponse updatedItem = response.rooms().get(0).items().get(0);
        assertFalse(updatedItem.rateUnavailable(), "Configured Economy material for work type must be loaded");
        assertEquals(401L, updatedItem.materialId(), "Must map to Steel Grille Economy");
        assertEquals("STEEL GRILLE ECONOMY", updatedItem.materialName().toUpperCase());
        assertEquals("16 Gauge Mild Steel Grille", updatedItem.specification());
        assertEquals(new BigDecimal("400.00"), updatedItem.rate());
        assertEquals(new BigDecimal("2"), updatedItem.quantity(), "Quantity must be transferred");
        assertEquals(new BigDecimal("800.00"), updatedItem.lineAmount(), "Amount must be recalculated with Economy rate");

        // When NO material is configured for that division in target grade
        Division emptyDiv = new Division();
        emptyDiv.setId(31L);
        emptyDiv.setName("Sunshade");
        divisionStore.put(31L, emptyDiv);

        ItemRequest emptyItem = new ItemRequest(null, 31L, null, 999L, null, null, "Custom Awning", null, Collections.emptyList(), MeasurementType.QUANTITY, BigDecimal.ONE, null, new BigDecimal("1000.00"), null, PricingMode.PREMIUM);
        RoomRequest emptyRoom = new RoomRequest("Ground Floor", "1", "Balcony", List.of(emptyItem));
        SwitchGradeResponse noMatResp = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, List.of(emptyRoom));
        SwitchGradeItemResponse noMatItem = noMatResp.rooms().get(0).items().get(0);
        assertTrue(noMatItem.rateUnavailable());
        assertNull(noMatItem.rate());
        assertEquals(BigDecimal.ZERO, noMatItem.lineAmount());
    }

    @Test
    @DisplayName("Files/Image based material grade switching: Preserves quantity, updates rate and image url")
    public void testFilesMaterialGradeSwitching() {
        Division artDiv = new Division();
        artDiv.setId(40L);
        artDiv.setName("Wall Art");
        divisionStore.put(40L, artDiv);

        Material premiumArt = new Material();
        premiumArt.setId(500L);
        premiumArt.setName("Acrylic Wall Art Premium");
        premiumArt.setDivision(artDiv);
        premiumArt.setUnit("Files");
        premiumArt.setMeasurementType(MeasurementType.FILES);
        premiumArt.setImageUrl("/images/art-premium.jpg");
        premiumArt.setPricingGrade(PricingMode.PREMIUM);
        premiumArt.setRate(new BigDecimal("15000.00"));
        premiumArt.setActive(true);
        materialStore.put(500L, premiumArt);

        Material middleArt = new Material();
        middleArt.setId(501L);
        middleArt.setName("Acrylic Wall Art Middle");
        middleArt.setDivision(artDiv);
        middleArt.setUnit("Files");
        middleArt.setMeasurementType(MeasurementType.FILES);
        middleArt.setImageUrl("/images/art-middle.jpg");
        middleArt.setPricingGrade(PricingMode.MIDDLE);
        middleArt.setRate(new BigDecimal("10000.00"));
        middleArt.setActive(true);
        materialStore.put(501L, middleArt);

        ItemRequest item = new ItemRequest(null, 40L, null, 500L, null, null, "Acrylic Wall Art Premium", null, Collections.emptyList(), MeasurementType.FILES, new BigDecimal("2"), "/images/custom-client.jpg", new BigDecimal("15000.00"), null, PricingMode.PREMIUM);
        RoomRequest room = new RoomRequest("First Floor", "1", "Hall", List.of(item));

        SwitchGradeResponse response = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, List.of(room));

        assertNotNull(response);
        SwitchGradeItemResponse updatedItem = response.rooms().get(0).items().get(0);
        assertEquals(501L, updatedItem.materialId());
        assertEquals("Acrylic Wall Art Middle".toUpperCase(), updatedItem.materialName().toUpperCase());
        assertEquals(new BigDecimal("10000.00"), updatedItem.rate());
        assertEquals(new BigDecimal("2"), updatedItem.quantity());
        assertEquals(new BigDecimal("20000.00"), updatedItem.lineAmount()); // 2 * 10000
    }

    @Test
    @DisplayName("Repeated Grade Switching: Box Type and Partition Type across Premium, Middle and Economy in all sequences")
    public void testRepeatedGradeSwitchingWithMixedSubtypesAndMissingEconomyMaterial() {
        Division baseCabinetDiv = new Division();
        baseCabinetDiv.setId(50L);
        baseCabinetDiv.setName("Base Cabinet");
        divisionStore.put(50L, baseCabinetDiv);

        // Premium materials
        Material matBoxPrem = new Material();
        matBoxPrem.setId(601L);
        matBoxPrem.setName("Marine Ply Lamination Box Type Premium");
        matBoxPrem.setDivision(baseCabinetDiv);
        matBoxPrem.setPricingGrade(PricingMode.PREMIUM);
        matBoxPrem.setRate(new BigDecimal("1400.00"));
        matBoxPrem.setFormula("(L + W) × H");
        matBoxPrem.setSpecification("Hai Hello Bye");
        matBoxPrem.setActive(true);
        materialStore.put(601L, matBoxPrem);

        Material matPartPrem = new Material();
        matPartPrem.setId(602L);
        matPartPrem.setName("Marine Ply Lamination Partition Type Premium");
        matPartPrem.setDivision(baseCabinetDiv);
        matPartPrem.setPricingGrade(PricingMode.PREMIUM);
        matPartPrem.setRate(new BigDecimal("1200.00"));
        matPartPrem.setFormula("(L + W) × H");
        matPartPrem.setSpecification("16 Mm Thickness");
        matPartPrem.setActive(true);
        materialStore.put(602L, matPartPrem);

        // Middle materials
        Material matBoxMid = new Material();
        matBoxMid.setId(603L);
        matBoxMid.setName("Marine Ply Lamination Box Type Middle");
        matBoxMid.setDivision(baseCabinetDiv);
        matBoxMid.setPricingGrade(PricingMode.MIDDLE);
        matBoxMid.setRate(new BigDecimal("1200.00"));
        matBoxMid.setFormula("(L + W) × H");
        matBoxMid.setSpecification("Heloo Bye");
        matBoxMid.setActive(true);
        materialStore.put(603L, matBoxMid);

        Material matPartMid = new Material();
        matPartMid.setId(604L);
        matPartMid.setName("Marine Ply Lamination Partition Type Middle");
        matPartMid.setDivision(baseCabinetDiv);
        matPartMid.setPricingGrade(PricingMode.MIDDLE);
        matPartMid.setRate(new BigDecimal("1000.00"));
        matPartMid.setFormula("(L + W) × H");
        matPartMid.setSpecification("17mm Thickness");
        matPartMid.setActive(true);
        materialStore.put(604L, matPartMid);

        // Economy material: ONLY Box Type exists! No Partition Type in Economy!
        Material matBoxEco = new Material();
        matBoxEco.setId(605L);
        matBoxEco.setName("Engineering Wood Box Type Economy");
        matBoxEco.setDivision(baseCabinetDiv);
        matBoxEco.setPricingGrade(PricingMode.ECONOMY);
        matBoxEco.setRate(new BigDecimal("1000.00"));
        matBoxEco.setFormula("(L + W) × H");
        matBoxEco.setSpecification("Bye");
        matBoxEco.setActive(true);
        materialStore.put(605L, matBoxEco);

        // Initial Quote in Premium with 2 items:
        // Item 1: Box Type (L: 44, W: 22, H: 12)
        // Item 2: Partition Type (L: 34, W: 22, H: 12)
        SizeRequest size1 = new SizeRequest(new BigDecimal("44"), new BigDecimal("22"), new BigDecimal("12"), null);
        SizeRequest size2 = new SizeRequest(new BigDecimal("34"), new BigDecimal("22"), new BigDecimal("12"), null);

        ItemRequest item1 = new ItemRequest(10L, 50L, null, 601L, "Marine Ply Lamination Box Type Premium", null, null, "", "Hai Hello Bye", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1400.00"), null, PricingMode.PREMIUM, 601L, PricingMode.PREMIUM, "Marine Ply Lamination Box Type Premium", "Hai Hello Bye");
        ItemRequest item2 = new ItemRequest(10L, 50L, null, 602L, "Marine Ply Lamination Partition Type Premium", null, null, "", "16 Mm Thickness", List.of(size2), MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1200.00"), null, PricingMode.PREMIUM, 602L, PricingMode.PREMIUM, "Marine Ply Lamination Partition Type Premium", "16 Mm Thickness");

        List<RoomRequest> currentRooms = List.of(new RoomRequest("Ground Floor", "1", "Kitchen", List.of(item1, item2)));

        // Helper to convert response back into RoomRequest for chained switching simulation
        java.util.function.Function<SwitchGradeResponse, List<RoomRequest>> toRoomRequests = resp -> {
            List<RoomRequest> rList = new ArrayList<>();
            for (SwitchGradeRoomResponse r : resp.rooms()) {
                List<ItemRequest> itList = new ArrayList<>();
                for (SwitchGradeItemResponse it : r.items()) {
                    itList.add(new ItemRequest(
                            it.workCategoryId(), it.divisionId(), it.itemId(), it.materialId(), it.materialName(),
                            it.materialOptionId(), it.materialSpecificationId(), it.itemDescription(), it.specification(),
                            it.sizes(), MeasurementType.valueOf(it.measurementType()), it.quantity(), it.imageUrl(),
                            it.rate(), it.coveragePerRoll(), it.pricingMode(),
                            it.originalMaterialId(), it.originalPricingMode(), it.originalMaterialName(), it.originalSpecification()
                    ));
                }
                rList.add(new RoomRequest(r.floor(), r.room(), r.workDescription(), itList));
            }
            return rList;
        };

        // =====================================================================
        // Sequence 1: Premium -> Middle -> Economy -> Middle -> Premium
        // =====================================================================

        // 1. Switch to MIDDLE
        SwitchGradeResponse respMiddle1 = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, currentRooms);
        assertEquals(PricingMode.MIDDLE, respMiddle1.targetGrade());
        assertEquals(2, respMiddle1.rooms().get(0).items().size());
        SwitchGradeItemResponse mItem1 = respMiddle1.rooms().get(0).items().get(0);
        SwitchGradeItemResponse mItem2 = respMiddle1.rooms().get(0).items().get(1);

        assertEquals(603L, mItem1.materialId(), "Item 1 must map to Middle Box Type");
        assertEquals("Heloo Bye", mItem1.specification(), "Item 1 must load Middle Box Type specification");
        assertEquals(new BigDecimal("1200.00"), mItem1.rate());
        assertEquals(601L, mItem1.originalMaterialId(), "Item 1 must preserve originalMaterialId");

        assertEquals(604L, mItem2.materialId(), "Item 2 must map to Middle Partition Type");
        assertEquals("17mm Thickness", mItem2.specification(), "Item 2 must load Middle Partition Type specification");
        assertEquals(new BigDecimal("1000.00"), mItem2.rate());
        assertEquals(602L, mItem2.originalMaterialId(), "Item 2 must preserve originalMaterialId");

        // 2. Switch from Middle to ECONOMY
        SwitchGradeResponse respEco1 = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, toRoomRequests.apply(respMiddle1));
        assertEquals(PricingMode.ECONOMY, respEco1.targetGrade());
        SwitchGradeItemResponse eItem1 = respEco1.rooms().get(0).items().get(0);
        SwitchGradeItemResponse eItem2 = respEco1.rooms().get(0).items().get(1);

        assertEquals(605L, eItem1.materialId(), "Item 1 must map to Economy Box Type");
        assertEquals("Bye", eItem1.specification(), "Item 1 must load Economy Box Type specification");
        assertEquals(new BigDecimal("1000.00"), eItem1.rate());
        assertFalse(eItem1.rateUnavailable());

        // CRITICAL CHECK: In Economy, Item 2 (Partition Type) has NO material!
        // It must NOT map to Box Type! It must be marked unavailable!
        assertNull(eItem2.materialId(), "Item 2 has no Partition Type in Economy; materialId must be null");
        assertTrue(eItem2.rateUnavailable(), "Item 2 must be marked rateUnavailable in Economy");
        assertNull(eItem2.rate(), "Item 2 rate must be null in Economy");
        assertEquals(BigDecimal.ZERO, eItem2.lineAmount(), "Item 2 line amount must be zero");
        assertTrue(eItem2.materialName().contains("Material Unavailable"), "Material name must indicate unavailable");
        assertEquals("", eItem2.specification(), "Item 2 specification must not display another grade's spec");
        assertEquals(602L, eItem2.originalMaterialId(), "Item 2 must STILL remember originalMaterialId 602");

        // 3. Switch back to MIDDLE from Economy!
        SwitchGradeResponse respMiddle2 = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, toRoomRequests.apply(respEco1));
        SwitchGradeItemResponse m2Item1 = respMiddle2.rooms().get(0).items().get(0);
        SwitchGradeItemResponse m2Item2 = respMiddle2.rooms().get(0).items().get(1);

        assertEquals(603L, m2Item1.materialId(), "Item 1 must restore to Middle Box Type");
        assertEquals("Heloo Bye", m2Item1.specification());
        assertEquals(new BigDecimal("1200.00"), m2Item1.rate());

        // CRITICAL: Item 2 must restore to Middle Partition Type, NOT Middle Box Type!
        assertEquals(604L, m2Item2.materialId(), "Item 2 must RESTORE to Middle Partition Type, NOT Box Type!");
        assertEquals("17mm Thickness", m2Item2.specification(), "Item 2 must RESTORE Middle Partition Type specification!");
        assertEquals(new BigDecimal("1000.00"), m2Item2.rate());
        assertFalse(m2Item2.rateUnavailable());

        // 4. Switch back to PREMIUM from Middle!
        SwitchGradeResponse respPrem2 = builderService.switchGrade(branchChennai.getId(), PricingMode.PREMIUM, toRoomRequests.apply(respMiddle2));
        SwitchGradeItemResponse p2Item1 = respPrem2.rooms().get(0).items().get(0);
        SwitchGradeItemResponse p2Item2 = respPrem2.rooms().get(0).items().get(1);

        assertEquals(601L, p2Item1.materialId(), "Item 1 must restore to Premium Box Type");
        assertEquals("Hai Hello Bye", p2Item1.specification());
        assertEquals(new BigDecimal("1400.00"), p2Item1.rate());

        assertEquals(602L, p2Item2.materialId(), "Item 2 must restore to Premium Partition Type");
        assertEquals("16 Mm Thickness", p2Item2.specification());
        assertEquals(new BigDecimal("1200.00"), p2Item2.rate());

        // =====================================================================
        // Sequence 2: Premium -> Economy -> Premium (direct hop)
        // =====================================================================
        SwitchGradeResponse respDirectEco = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, currentRooms);
        SwitchGradeItemResponse de1 = respDirectEco.rooms().get(0).items().get(0);
        SwitchGradeItemResponse de2 = respDirectEco.rooms().get(0).items().get(1);
        assertEquals(605L, de1.materialId());
        assertTrue(de2.rateUnavailable());

        SwitchGradeResponse respBackPrem = builderService.switchGrade(branchChennai.getId(), PricingMode.PREMIUM, toRoomRequests.apply(respDirectEco));
        SwitchGradeItemResponse bp1 = respBackPrem.rooms().get(0).items().get(0);
        SwitchGradeItemResponse bp2 = respBackPrem.rooms().get(0).items().get(1);
        assertEquals(601L, bp1.materialId());
        assertEquals(602L, bp2.materialId());
        assertEquals("16 Mm Thickness", bp2.specification());

        // =====================================================================
        // Sequence 3: 10 repeated switches in a loop (idempotency verification)
        // =====================================================================
        List<RoomRequest> loopRooms = currentRooms;
        PricingMode[] sequence = {PricingMode.MIDDLE, PricingMode.ECONOMY, PricingMode.PREMIUM, PricingMode.MIDDLE, PricingMode.PREMIUM, PricingMode.ECONOMY, PricingMode.MIDDLE};
        for (PricingMode target : sequence) {
            SwitchGradeResponse stepResp = builderService.switchGrade(branchChennai.getId(), target, loopRooms);
            assertEquals(target, stepResp.targetGrade());
            SwitchGradeItemResponse itA = stepResp.rooms().get(0).items().get(0);
            SwitchGradeItemResponse itB = stepResp.rooms().get(0).items().get(1);

            if (target == PricingMode.PREMIUM) {
                assertEquals(601L, itA.materialId());
                assertEquals(602L, itB.materialId());
                assertEquals("Hai Hello Bye", itA.specification());
                assertEquals("16 Mm Thickness", itB.specification());
            } else if (target == PricingMode.MIDDLE) {
                assertEquals(603L, itA.materialId());
                assertEquals(604L, itB.materialId());
                assertEquals("Heloo Bye", itA.specification());
                assertEquals("17mm Thickness", itB.specification());
            } else if (target == PricingMode.ECONOMY) {
                assertEquals(605L, itA.materialId());
                assertNull(itB.materialId());
                assertTrue(itB.rateUnavailable());
                assertEquals("Bye", itA.specification());
                assertEquals("", itB.specification());
            }
            loopRooms = toRoomRequests.apply(stepResp);
        }
    }

    @Test
    @DisplayName("Position-Based Mapping: Premium (3 mats) -> Economy (2 mats) -> Middle (3 mats) -> Premium (3 mats)")
    public void testPositionBasedMapping_ThreeMaterialsToTwoMaterials_MissingPositionShowsUnavailableWithoutDuplication() {
        // Division: Modular Kitchen / Wall Panel
        Division testDiv = new Division();
        testDiv.setId(700L);
        testDiv.setName("Wall Panel");
        divisionStore.put(700L, testDiv);

        // Premium: 3 materials (Pos 0, 1, 2)
        Material prem1 = new Material(); prem1.setId(701L); prem1.setName("Marine Ply Box Type Premium"); prem1.setDivision(testDiv); prem1.setPricingGrade(PricingMode.PREMIUM); prem1.setDisplayOrder(1); prem1.setFormula("L * W"); prem1.setUnit("Sq.ft"); prem1.setMeasurementType(MeasurementType.SQFT); prem1.setRate(new BigDecimal("1500.00")); prem1.setActive(true);
        Material prem2 = new Material(); prem2.setId(702L); prem2.setName("Marine Ply Partition Type Premium"); prem2.setDivision(testDiv); prem2.setPricingGrade(PricingMode.PREMIUM); prem2.setDisplayOrder(2); prem2.setFormula("L * W"); prem2.setUnit("Sq.ft"); prem2.setMeasurementType(MeasurementType.SQFT); prem2.setRate(new BigDecimal("1200.00")); prem2.setActive(true);
        Material prem3 = new Material(); prem3.setId(703L); prem3.setName("Premium Decorative Panel"); prem3.setDivision(testDiv); prem3.setPricingGrade(PricingMode.PREMIUM); prem3.setDisplayOrder(3); prem3.setFormula("L * W"); prem3.setUnit("Sq.ft"); prem3.setMeasurementType(MeasurementType.SQFT); prem3.setRate(new BigDecimal("1800.00")); prem3.setActive(true);
        materialStore.put(701L, prem1); materialStore.put(702L, prem2); materialStore.put(703L, prem3);

        // Economy: 2 materials (Pos 0, 1) - Position 2 does NOT exist!
        Material eco1 = new Material(); eco1.setId(711L); eco1.setName("Engineering Wood Box Type Economy"); eco1.setDivision(testDiv); eco1.setPricingGrade(PricingMode.ECONOMY); eco1.setDisplayOrder(1); eco1.setFormula("L * W"); eco1.setUnit("Sq.ft"); eco1.setMeasurementType(MeasurementType.SQFT); eco1.setRate(new BigDecimal("900.00")); eco1.setActive(true);
        Material eco2 = new Material(); eco2.setId(712L); eco2.setName("Economy Partition Board"); eco2.setDivision(testDiv); eco2.setPricingGrade(PricingMode.ECONOMY); eco2.setDisplayOrder(2); eco2.setFormula("L * W"); eco2.setUnit("Sq.ft"); eco2.setMeasurementType(MeasurementType.SQFT); eco2.setRate(new BigDecimal("750.00")); eco2.setActive(true);
        materialStore.put(711L, eco1); materialStore.put(712L, eco2);

        // Middle: 3 materials (Pos 0, 1, 2)
        Material mid1 = new Material(); mid1.setId(721L); mid1.setName("Marine Ply Box Type Middle"); mid1.setDivision(testDiv); mid1.setPricingGrade(PricingMode.MIDDLE); mid1.setDisplayOrder(1); mid1.setFormula("L * W"); mid1.setUnit("Sq.ft"); mid1.setMeasurementType(MeasurementType.SQFT); mid1.setRate(new BigDecimal("1300.00")); mid1.setActive(true);
        Material mid2 = new Material(); mid2.setId(722L); mid2.setName("Marine Ply Partition Type Middle"); mid2.setDivision(testDiv); mid2.setPricingGrade(PricingMode.MIDDLE); mid2.setDisplayOrder(2); mid2.setFormula("L * W"); mid2.setUnit("Sq.ft"); mid2.setMeasurementType(MeasurementType.SQFT); mid2.setRate(new BigDecimal("1050.00")); mid2.setActive(true);
        Material mid3 = new Material(); mid3.setId(723L); mid3.setName("Middle Decorative Panel"); mid3.setDivision(testDiv); mid3.setPricingGrade(PricingMode.MIDDLE); mid3.setDisplayOrder(3); mid3.setFormula("L * W"); mid3.setUnit("Sq.ft"); mid3.setMeasurementType(MeasurementType.SQFT); mid3.setRate(new BigDecimal("1550.00")); mid3.setActive(true);
        materialStore.put(721L, mid1); materialStore.put(722L, mid2); materialStore.put(723L, mid3);

        // Initial Quote in Premium with 3 items (Pos 0, 1, 2)
        List<SizeRequest> sizes1 = List.of(new SizeRequest(new BigDecimal("40.00"), new BigDecimal("20.00"), BigDecimal.ZERO, null));
        List<SizeRequest> sizes2 = List.of(new SizeRequest(new BigDecimal("30.00"), new BigDecimal("20.00"), BigDecimal.ZERO, null));
        List<SizeRequest> sizes3 = List.of(new SizeRequest(new BigDecimal("50.00"), new BigDecimal("20.00"), BigDecimal.ZERO, null));

        ItemRequest item1 = new ItemRequest(10L, 700L, null, 701L, "Marine Ply Box Type Premium", null, null, "", "", sizes1, MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1500.00"), null, PricingMode.PREMIUM, 701L, PricingMode.PREMIUM, "Marine Ply Box Type Premium", "", 0);
        ItemRequest item2 = new ItemRequest(10L, 700L, null, 702L, "Marine Ply Partition Type Premium", null, null, "", "", sizes2, MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1200.00"), null, PricingMode.PREMIUM, 702L, PricingMode.PREMIUM, "Marine Ply Partition Type Premium", "", 1);
        ItemRequest item3 = new ItemRequest(10L, 700L, null, 703L, "Premium Decorative Panel", null, null, "", "", sizes3, MeasurementType.SQFT, BigDecimal.ONE, null, new BigDecimal("1800.00"), null, PricingMode.PREMIUM, 703L, PricingMode.PREMIUM, "Premium Decorative Panel", "", 2);

        RoomRequest room = new RoomRequest("Ground Floor", "1", "", List.of(item1, item2, item3));

        java.util.function.Function<SwitchGradeResponse, List<RoomRequest>> toRoomRequests = (resp) -> {
            List<RoomRequest> rooms = new ArrayList<>();
            for (SwitchGradeRoomResponse r : resp.rooms()) {
                List<ItemRequest> items = new ArrayList<>();
                for (SwitchGradeItemResponse it : r.items()) {
                    items.add(new ItemRequest(
                            it.workCategoryId(), it.divisionId(), it.itemId(), it.materialId(), it.materialName(),
                            it.materialOptionId(), it.materialSpecificationId(), it.itemDescription(), it.specification(),
                            it.sizes(), MeasurementType.valueOf(it.measurementType()), it.quantity(), it.imageUrl(),
                            it.rate(), it.coveragePerRoll(), it.pricingMode(),
                            it.originalMaterialId(), it.originalPricingMode(), it.originalMaterialName(), it.originalSpecification(),
                            it.originalPosition()
                    ));
                }
                rooms.add(new RoomRequest(r.floor(), r.room(), r.workDescription(), items));
            }
            return rooms;
        };

        // 1. Switch Premium -> Economy
        SwitchGradeResponse ecoResp = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, List.of(room));
        assertEquals(PricingMode.ECONOMY, ecoResp.targetGrade());
        List<SwitchGradeItemResponse> ecoItems = ecoResp.rooms().get(0).items();
        assertEquals(3, ecoItems.size());

        // Pos 0 maps to Engineering Wood Box Type Economy
        assertEquals(711L, ecoItems.get(0).materialId());
        assertTrue(ecoItems.get(0).materialName().equalsIgnoreCase("Engineering Wood Box Type Economy"));
        assertFalse(ecoItems.get(0).rateUnavailable());
        assertEquals(Integer.valueOf(0), ecoItems.get(0).originalPosition());

        // Pos 1 maps to Economy Partition Board
        assertEquals(712L, ecoItems.get(1).materialId());
        assertTrue(ecoItems.get(1).materialName().equalsIgnoreCase("Economy Partition Board"));
        assertFalse(ecoItems.get(1).rateUnavailable());
        assertEquals(Integer.valueOf(1), ecoItems.get(1).originalPosition());

        // Pos 2 is MISSING in Economy: MUST show Material Unavailable (Economy Grade) with 0 amount, NOT duplicated!
        assertNull(ecoItems.get(2).materialId(), "Missing position must have null materialId");
        assertTrue(ecoItems.get(2).rateUnavailable(), "Missing position must be rateUnavailable");
        assertEquals(BigDecimal.ZERO, ecoItems.get(2).lineAmount(), "Unavailable item amount must be 0");
        assertTrue(ecoItems.get(2).materialName().contains("Material Unavailable"), "Name must indicate unavailable");
        assertEquals(Integer.valueOf(2), ecoItems.get(2).originalPosition(), "Original position 2 must be preserved");
        assertEquals(new BigDecimal("50.00"), ecoItems.get(2).sizes().get(0).length(), "Measurements must be preserved intact");

        // 2. Switch Economy -> Middle
        SwitchGradeResponse midResp = builderService.switchGrade(branchChennai.getId(), PricingMode.MIDDLE, toRoomRequests.apply(ecoResp));
        assertEquals(PricingMode.MIDDLE, midResp.targetGrade());
        List<SwitchGradeItemResponse> midItems = midResp.rooms().get(0).items();
        assertEquals(3, midItems.size());

        // Pos 0 maps to Marine Ply Box Type Middle
        assertEquals(721L, midItems.get(0).materialId());
        assertTrue(midItems.get(0).materialName().equalsIgnoreCase("Marine Ply Box Type Middle"));
        assertFalse(midItems.get(0).rateUnavailable());

        // Pos 1 maps to Marine Ply Partition Type Middle
        assertEquals(722L, midItems.get(1).materialId());
        assertTrue(midItems.get(1).materialName().equalsIgnoreCase("Marine Ply Partition Type Middle"));
        assertFalse(midItems.get(1).rateUnavailable());

        // Pos 2 RESTORES Middle Decorative Panel!
        assertEquals(723L, midItems.get(2).materialId(), "Position 2 must restore Middle Decorative Panel!");
        assertTrue(midItems.get(2).materialName().equalsIgnoreCase("Middle Decorative Panel"));
        assertFalse(midItems.get(2).rateUnavailable());
        assertTrue(midItems.get(2).lineAmount().compareTo(BigDecimal.ZERO) > 0, "Line amount must be recalculated with Middle rate");

        // 3. Switch Middle -> Premium
        SwitchGradeResponse premResp = builderService.switchGrade(branchChennai.getId(), PricingMode.PREMIUM, toRoomRequests.apply(midResp));
        assertEquals(PricingMode.PREMIUM, premResp.targetGrade());
        List<SwitchGradeItemResponse> premItems = premResp.rooms().get(0).items();
        assertEquals(3, premItems.size());
        assertEquals(701L, premItems.get(0).materialId());
        assertEquals(702L, premItems.get(1).materialId());
        assertEquals(703L, premItems.get(2).materialId());

        // 4. Switch Premium -> Economy again (repeated cycle)
        SwitchGradeResponse ecoResp2 = builderService.switchGrade(branchChennai.getId(), PricingMode.ECONOMY, toRoomRequests.apply(premResp));
        List<SwitchGradeItemResponse> ecoItems2 = ecoResp2.rooms().get(0).items();
        assertEquals(711L, ecoItems2.get(0).materialId());
        assertEquals(712L, ecoItems2.get(1).materialId());
        assertNull(ecoItems2.get(2).materialId());
        assertTrue(ecoItems2.get(2).rateUnavailable());
        assertEquals(BigDecimal.ZERO, ecoItems2.get(2).lineAmount());
    }
}
