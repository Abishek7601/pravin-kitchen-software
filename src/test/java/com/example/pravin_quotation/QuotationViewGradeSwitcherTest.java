package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.controller.QuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.AdminQuotationBuilderService.ResolvedMaterialRate;
import com.example.pravin_quotation.service.QuotationItemSizeService;
import com.example.pravin_quotation.service.QuotationViewGradeService;
import com.example.pravin_quotation.service.QuotationViewGradeService.GradeViewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationViewGradeSwitcherTest {

    private QuotationRepository quotationRepository;
    private QuotationRoomRepository quotationRoomRepository;
    private QuotationItemRepository quotationItemRepository;
    private QuotationItemSizeRepository quotationItemSizeRepository;
    private QuotationItemSizeService quotationItemSizeService;
    private AdminQuotationBuilderService adminQuotationBuilderService;
    private PricingRepository pricingRepository;

    private QuotationViewGradeService gradeService;

    private Branch branch;
    private Quotation quotation;
    private QuotationRoom room;
    private WorkCategory category;
    private Division baseCabinetDiv;

    private Material premiumBoxMat;
    private Material middleBoxMat;
    private Material economyBoxMat;

    private Material premiumPartitionMat;
    private Material middlePartitionMat;
    // Note: No economyPartitionMat (simulating item unavailable in Economy!)

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
    void setUp() {
        quotationRepository = createMock(QuotationRepository.class);
        quotationRoomRepository = createMock(QuotationRoomRepository.class);
        quotationItemRepository = createMock(QuotationItemRepository.class);
        quotationItemSizeRepository = createMock(QuotationItemSizeRepository.class);
        quotationItemSizeService = new QuotationItemSizeService(quotationItemSizeRepository);
        pricingRepository = createMock(PricingRepository.class);

        branch = new Branch();
        branch.setId(101L);
        branch.setName("Madurai");

        category = new WorkCategory();
        category.setId(10L);
        category.setName("Modular Kitchen");

        baseCabinetDiv = new Division();
        baseCabinetDiv.setId(20L);
        baseCabinetDiv.setName("Base Cabinet");
        baseCabinetDiv.setWorkCategory(category);

        // Position 0 materials
        premiumBoxMat = new Material();
        premiumBoxMat.setId(1L);
        premiumBoxMat.setName("Marine Ply Lamination Box Type Premium");
        premiumBoxMat.setPricingGrade(PricingMode.PREMIUM);
        premiumBoxMat.setDivision(baseCabinetDiv);
        premiumBoxMat.setActive(true);

        middleBoxMat = new Material();
        middleBoxMat.setId(2L);
        middleBoxMat.setName("Marine Ply Lamination Box Type Middle");
        middleBoxMat.setPricingGrade(PricingMode.MIDDLE);
        middleBoxMat.setDivision(baseCabinetDiv);
        middleBoxMat.setActive(true);

        economyBoxMat = new Material();
        economyBoxMat.setId(3L);
        economyBoxMat.setName("Engineering Wood Box Type Economy");
        economyBoxMat.setPricingGrade(PricingMode.ECONOMY);
        economyBoxMat.setDivision(baseCabinetDiv);
        economyBoxMat.setActive(true);

        // Position 1 materials
        premiumPartitionMat = new Material();
        premiumPartitionMat.setId(4L);
        premiumPartitionMat.setName("Marine Ply Partition Type Premium");
        premiumPartitionMat.setPricingGrade(PricingMode.PREMIUM);
        premiumPartitionMat.setDivision(baseCabinetDiv);
        premiumPartitionMat.setActive(true);

        middlePartitionMat = new Material();
        middlePartitionMat.setId(5L);
        middlePartitionMat.setName("Marine Ply Partition Type Middle");
        middlePartitionMat.setPricingGrade(PricingMode.MIDDLE);
        middlePartitionMat.setDivision(baseCabinetDiv);
        middlePartitionMat.setActive(true);

        // Quotation
        quotation = new Quotation();
        quotation.setId(12373L);
        quotation.setQuotationNumber("QT-00053");
        quotation.setBranch(branch);
        quotation.setPricingMode(PricingMode.PREMIUM);
        quotation.setSelectedGrade(PricingMode.PREMIUM);
        quotation.setShowSqftSize(true);
        quotation.setStatus(QuotationStatus.PENDING);

        room = new QuotationRoom();
        room.setId(501L);
        room.setQuotation(quotation);
        room.setFloor("Ground Floor");
        room.setRoom("Room 1");
        room.setActive(true);

        // Item 1: Position 0
        QuotationItem item1 = new QuotationItem();
        item1.setId(1001L);
        item1.setQuotationRoom(room);
        item1.setWorkCategory(category);
        item1.setDivision(baseCabinetDiv);
        item1.setMaterial(premiumBoxMat);
        item1.setSpecification("Hai Hello Bye");
        item1.setMeasurementType(MeasurementType.SQFT);
        item1.setFormula("(L + W) × H");
        item1.setLengthValue(BigDecimal.valueOf(44));
        item1.setWidthValue(BigDecimal.valueOf(22));
        item1.setHeightValue(BigDecimal.valueOf(12));
        item1.setCalculatedSqft(BigDecimal.valueOf(5.50));
        item1.setRate(BigDecimal.valueOf(1400.00));
        item1.setAmount(BigDecimal.valueOf(7700.00));
        item1.setActive(true);

        // Item 2: Position 1
        QuotationItem item2 = new QuotationItem();
        item2.setId(1002L);
        item2.setQuotationRoom(room);
        item2.setWorkCategory(category);
        item2.setDivision(baseCabinetDiv);
        item2.setMaterial(premiumPartitionMat);
        item2.setSpecification("16 Mm Thickness");
        item2.setMeasurementType(MeasurementType.SQFT);
        item2.setFormula("(L + W) × H");
        item2.setLengthValue(BigDecimal.valueOf(34));
        item2.setWidthValue(BigDecimal.valueOf(22));
        item2.setHeightValue(BigDecimal.valueOf(12));
        item2.setCalculatedSqft(BigDecimal.valueOf(4.67));
        item2.setRate(BigDecimal.valueOf(1200.00));
        item2.setAmount(BigDecimal.valueOf(5604.00));
        item2.setActive(true);

        room.setSubtotal(BigDecimal.valueOf(13304.00));
        quotation.setSubtotal(BigDecimal.valueOf(13304.00));
        quotation.setGrandTotal(BigDecimal.valueOf(13304.00));

        // Proxy implementations
        quotationRepository = (QuotationRepository) Proxy.newProxyInstance(
                QuotationRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(quotation);
                    }
                    return null;
                }
        );

        quotationRoomRepository = (QuotationRoomRepository) Proxy.newProxyInstance(
                QuotationRoomRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRoomRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("findByQuotationId")) {
                        return List.of(room);
                    }
                    return Collections.emptyList();
                }
        );

        quotationItemRepository = (QuotationItemRepository) Proxy.newProxyInstance(
                QuotationItemRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("findByQuotationRoomId")) {
                        return List.of(item1, item2);
                    }
                    return Collections.emptyList();
                }
        );

        adminQuotationBuilderService = new AdminQuotationBuilderService(
                quotationRepository,
                createMock(CustomerRepository.class),
                createMock(UserRepository.class),
                createMock(BranchRepository.class),
                createMock(WorkCategoryRepository.class),
                createMock(DivisionRepository.class),
                createMock(ItemRepository.class),
                createMock(MaterialRepository.class),
                createMock(MaterialOptionRepository.class),
                createMock(MaterialSpecificationRepository.class),
                pricingRepository,
                createMock(DistrictPricingRepository.class),
                null,
                null,
                null,
                null
        ) {
            @Override
            public List<Material> getActiveMaterialsForDivisionAndGrade(Long divisionId, PricingMode targetGrade, Long branchId) {
                if (targetGrade == PricingMode.PREMIUM) return List.of(premiumBoxMat, premiumPartitionMat);
                if (targetGrade == PricingMode.MIDDLE) return List.of(middleBoxMat, middlePartitionMat);
                if (targetGrade == PricingMode.ECONOMY) return List.of(economyBoxMat);
                return Collections.emptyList();
            }

            @Override
            public ResolvedMaterialRate resolveMaterialAndRate(Long branchId, Long divisionId, Long materialId, Long materialOptionId, PricingMode targetGrade, Integer position) {
                if (targetGrade == PricingMode.PREMIUM) {
                    if (position != null && position == 0) {
                        return new ResolvedMaterialRate(premiumBoxMat.getId(), null, premiumBoxMat.getName(), BigDecimal.valueOf(1400.00), "(L + W) × H", "SQ.FT", MeasurementType.SQFT, null, null, true, null, "Hai Hello Bye", null);
                    } else if (position != null && position == 1) {
                        return new ResolvedMaterialRate(premiumPartitionMat.getId(), null, premiumPartitionMat.getName(), BigDecimal.valueOf(1200.00), "(L + W) × H", "SQ.FT", MeasurementType.SQFT, null, null, true, null, "16 Mm Thickness", null);
                    }
                } else if (targetGrade == PricingMode.MIDDLE) {
                    if (position != null && position == 0) {
                        return new ResolvedMaterialRate(middleBoxMat.getId(), null, middleBoxMat.getName(), BigDecimal.valueOf(1200.00), "(L + W) × H", "SQ.FT", MeasurementType.SQFT, null, null, true, null, "Heloo Bye", null);
                    } else if (position != null && position == 1) {
                        return new ResolvedMaterialRate(middlePartitionMat.getId(), null, middlePartitionMat.getName(), BigDecimal.valueOf(1000.00), "(L + W) × H", "SQ.FT", MeasurementType.SQFT, null, null, true, null, "17mm Thickness", null);
                    }
                } else if (targetGrade == PricingMode.ECONOMY) {
                    if (position != null && position == 0) {
                        return new ResolvedMaterialRate(economyBoxMat.getId(), null, economyBoxMat.getName(), BigDecimal.valueOf(1000.00), "(L + W) × H", "SQ.FT", MeasurementType.SQFT, null, null, true, null, "Bye", null);
                    } else {
                        return ResolvedMaterialRate.unavailable("No material available in Economy Grade for BASE CABINET (PARTITION).");
                    }
                }
                return ResolvedMaterialRate.unavailable("No material available");
            }
        };

        gradeService = new QuotationViewGradeService(
                quotationRepository,
                quotationRoomRepository,
                quotationItemRepository,
                quotationItemSizeService,
                adminQuotationBuilderService,
                pricingRepository
        );
    }

    @Test
    @DisplayName("Verify Premium Grade view data returns correct materials, rates, and payment schedule")
    void testPremiumGradeViewData() {
        GradeViewResponse resp = gradeService.getGradeViewData(12373L, PricingMode.PREMIUM);

        assertTrue(resp.success());
        assertEquals("PREMIUM", resp.activeGrade());
        assertEquals("Premium", resp.activeGradeLabel());
        assertTrue(resp.gradeBadgeClass().contains("grade-badge-premium"));
        assertTrue(resp.quotedProjectValue().contains("13,304"));

        assertNotNull(resp.paymentTerms());
        assertTrue(resp.paymentTerms().token10().contains("1,330.40"));
        assertTrue(resp.paymentTerms().advance50().contains("6,652.00"));
        assertTrue(resp.paymentTerms().part35().contains("4,656.40"));
        assertTrue(resp.paymentTerms().final5().contains("665.20"));

        assertEquals(1, resp.rooms().size());
        assertEquals(2, resp.rooms().get(0).items().size());

        var it1 = resp.rooms().get(0).items().get(0);
        assertTrue(it1.materialAvailable());
        assertTrue(it1.materialName().toUpperCase().contains("MARINE PLY LAMINATION BOX TYPE PREMIUM"));
        assertTrue(it1.amount().contains("7,700.00"));

        var it2 = resp.rooms().get(0).items().get(1);
        assertTrue(it2.materialAvailable());
        assertTrue(it2.materialName().toUpperCase().contains("MARINE PLY PARTITION TYPE PREMIUM"));
        assertTrue(it2.amount().contains("5,604.00"));
    }

    @Test
    @DisplayName("Verify Middle Grade view data switches materials by position, recalculates rates and amounts")
    void testMiddleGradeViewData() {
        GradeViewResponse resp = gradeService.getGradeViewData(12373L, PricingMode.MIDDLE);

        assertTrue(resp.success());
        assertEquals("MIDDLE", resp.activeGrade());
        assertEquals("Middle / Standard", resp.activeGradeLabel());
        assertTrue(resp.gradeBadgeClass().contains("grade-badge-middle"));

        assertEquals(1, resp.rooms().size());
        assertEquals(2, resp.rooms().get(0).items().size());

        var it1 = resp.rooms().get(0).items().get(0);
        assertTrue(it1.materialAvailable());
        assertTrue(it1.materialName().toUpperCase().contains("MARINE PLY LAMINATION BOX TYPE MIDDLE"));
        assertEquals("Heloo Bye", it1.specification());
        assertTrue(it1.unitRate().contains("1,200.00"));
        assertTrue(it1.amount().contains("6,600.00")); // 5.50 * 1200

        var it2 = resp.rooms().get(0).items().get(1);
        assertTrue(it2.materialAvailable());
        assertTrue(it2.materialName().toUpperCase().contains("MARINE PLY PARTITION TYPE MIDDLE"));
        assertEquals("17mm Thickness", it2.specification());
        assertTrue(it2.unitRate().contains("1,000.00"));
        assertTrue(it2.amount().contains("4,670.00")); // 4.67 * 1000

        // Subtotal = 6,600 + 4,670 = 11,270.00
        assertTrue(resp.quotedProjectValue().contains("11,270"));
        assertTrue(resp.paymentTerms().token10().contains("1,127.00"));
        assertTrue(resp.paymentTerms().advance50().contains("5,635.00"));
        assertTrue(resp.paymentTerms().part35().contains("3,944.50"));
        assertTrue(resp.paymentTerms().final5().contains("563.50"));
    }

    @Test
    @DisplayName("Verify Economy Grade view data handles unavailable materials gracefully without fallback")
    void testEconomyGradeViewDataWithUnavailableMaterial() {
        GradeViewResponse resp = gradeService.getGradeViewData(12373L, PricingMode.ECONOMY);

        assertTrue(resp.success());
        assertEquals("ECONOMY", resp.activeGrade());
        assertEquals("Economy", resp.activeGradeLabel());
        assertTrue(resp.gradeBadgeClass().contains("grade-badge-economy"));

        assertEquals(1, resp.rooms().size());
        assertEquals(2, resp.rooms().get(0).items().size());

        // Item 1 (Position 0): Available in Economy
        var it1 = resp.rooms().get(0).items().get(0);
        assertTrue(it1.materialAvailable());
        assertTrue(it1.materialName().toUpperCase().contains("ENGINEERING WOOD BOX TYPE ECONOMY"));
        assertEquals("Bye", it1.specification());
        assertTrue(it1.unitRate().contains("1,000.00"));
        assertTrue(it1.amount().contains("5,500.00")); // 5.50 * 1000

        // Item 2 (Position 1): Unavailable in Economy
        var it2 = resp.rooms().get(0).items().get(1);
        assertFalse(it2.materialAvailable(), "Position 1 must be marked unavailable in Economy");
        assertEquals("Material Unavailable (Economy Grade)", it2.materialName());
        assertNotNull(it2.availabilityMessage());
        assertTrue(it2.availabilityMessage().contains("No material available in Economy Grade"));
        assertEquals("₹ 0.00", it2.unitRate());
        assertEquals("₹ 0.00", it2.amount());

        // Subtotal = 5,500.00
        assertTrue(resp.quotedProjectValue().contains("5,500"));
        assertTrue(resp.paymentTerms().token10().contains("550.00"));
    }

    @Test
    @DisplayName("Verify non-destructive switching cycle: Premium -> Middle -> Economy -> Premium preserves exact data")
    void testNonDestructiveSwitchingCycle() {
        // Step 1: Premium
        GradeViewResponse p1 = gradeService.getGradeViewData(12373L, PricingMode.PREMIUM);
        // Step 2: Middle
        GradeViewResponse m = gradeService.getGradeViewData(12373L, PricingMode.MIDDLE);
        // Step 3: Economy
        GradeViewResponse e = gradeService.getGradeViewData(12373L, PricingMode.ECONOMY);
        // Step 4: Back to Premium
        GradeViewResponse p2 = gradeService.getGradeViewData(12373L, PricingMode.PREMIUM);

        assertEquals(p1.quotedProjectValue(), p2.quotedProjectValue());
        assertEquals(p1.paymentTerms().token10(), p2.paymentTerms().token10());
        assertEquals(p1.rooms().get(0).items().get(0).materialName(), p2.rooms().get(0).items().get(0).materialName());
        assertEquals(p1.rooms().get(0).items().get(0).amount(), p2.rooms().get(0).items().get(0).amount());
        assertEquals(p1.rooms().get(0).items().get(1).materialName(), p2.rooms().get(0).items().get(1).materialName());
        assertEquals(p1.rooms().get(0).items().get(1).amount(), p2.rooms().get(0).items().get(1).amount());

        // Verify underlying quotation was NOT mutated
        assertEquals(BigDecimal.valueOf(13304.00), quotation.getGrandTotal());
        assertEquals(PricingMode.PREMIUM, quotation.getPricingMode());
    }

    @Test
    @DisplayName("Verify Admin quotation-view.html has all required Grade Switcher UI elements and JS functions")
    void testAdminViewTemplateHasGradeSwitcher() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));

        assertTrue(html.contains("id=\"quotedProjectValueVal\""), "Must have #quotedProjectValueVal for dynamic amount");
        assertTrue(html.contains("id=\"quotedProjectValueBadge\""), "Must have #quotedProjectValueBadge for grade badge");
        assertTrue(html.contains("id=\"viewGradeSwitcherContainer\""), "Must have #viewGradeSwitcherContainer");
        assertTrue(html.contains("name=\"viewGradeSwitch\""), "Must have radio inputs with name='viewGradeSwitch'");
        assertTrue(html.contains("value=\"PREMIUM\"") && html.contains("value=\"MIDDLE\"") && html.contains("value=\"ECONOMY\""),
                "Must provide all 3 grades");
        assertTrue(html.contains("id=\"paymentTermToken\""), "Must have #paymentTermToken");
        assertTrue(html.contains("id=\"paymentTermAdvance\""), "Must have #paymentTermAdvance");
        assertTrue(html.contains("id=\"paymentTermPart\""), "Must have #paymentTermPart");
        assertTrue(html.contains("id=\"paymentTermFinal\""), "Must have #paymentTermFinal");
        assertTrue(html.contains("id=\"breakdownTableBody\""), "Must have #breakdownTableBody for table rendering");
        assertTrue(html.contains("id=\"breakdownTableFoot\""), "Must have #breakdownTableFoot for table footer rendering");
        assertTrue(html.contains("id=\"workCountBadge\""), "Must have #workCountBadge");
        assertTrue(html.contains("function switchViewGrade("), "Must contain switchViewGrade function");
        assertTrue(html.contains("function renderGradeViewData("), "Must contain renderGradeViewData function");
        assertTrue(html.contains("/admin/quotations/view-grade-data/"), "Must fetch from admin view-grade-data endpoint");
    }

    @Test
    @DisplayName("Verify Employee quotation-workspace.html has all required Grade Switcher UI elements and JS functions")
    void testEmployeeWorkspaceTemplateHasGradeSwitcher() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));

        assertTrue(html.contains("id=\"quotedProjectValueVal\""), "Must have #quotedProjectValueVal for dynamic amount");
        assertTrue(html.contains("id=\"quotedProjectValueBadge\""), "Must have #quotedProjectValueBadge for grade badge");
        assertTrue(html.contains("id=\"viewGradeSwitcherContainer\""), "Must have #viewGradeSwitcherContainer");
        assertTrue(html.contains("name=\"viewGradeSwitch\""), "Must have radio inputs with name='viewGradeSwitch'");
        assertTrue(html.contains("id=\"paymentTermToken\""), "Must have #paymentTermToken");
        assertTrue(html.contains("id=\"paymentTermAdvance\""), "Must have #paymentTermAdvance");
        assertTrue(html.contains("id=\"paymentTermPart\""), "Must have #paymentTermPart");
        assertTrue(html.contains("id=\"paymentTermFinal\""), "Must have #paymentTermFinal");
        assertTrue(html.contains("id=\"breakdownTableBody\""), "Must have #breakdownTableBody for table rendering");
        assertTrue(html.contains("id=\"breakdownTableFoot\""), "Must have #breakdownTableFoot for table footer rendering");
        assertTrue(html.contains("id=\"workCountBadge\""), "Must have #workCountBadge");
        assertTrue(html.contains("function switchViewGrade("), "Must contain switchViewGrade function");
        assertTrue(html.contains("function renderGradeViewData("), "Must contain renderGradeViewData function");
        assertTrue(html.contains("/employee/quotations/view-grade-data/"), "Must fetch from employee view-grade-data endpoint");
    }

    @Test
    @DisplayName("Verify CSS defines Quote Summary Grade Switcher styles in quotation-view.css")
    void testCssDefinesGradeSwitcherStyles() throws IOException {
        String css = Files.readString(Path.of("src/main/resources/static/css/quotation-view.css"));

        assertTrue(css.contains(".summary-grade-switch-container"), "Must define .summary-grade-switch-container");
        assertTrue(css.contains(".summary-grade-label"), "Must define .summary-grade-label");
        assertTrue(css.contains(".summary-segmented-control"), "Must define .summary-segmented-control");
        assertTrue(css.contains(".segmented-pill"), "Must define .segmented-pill");
    }

    @Test
    @DisplayName("Verify IndianCurrencyFormatterTest critical strings remain preserved in templates")
    void testIndianCurrencyFormatterExactStringsPreserved() throws IOException {
        String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/quotation-view.html"));
        String empHtml = Files.readString(Path.of("src/main/resources/templates/employee/quotation-workspace.html"));

        assertTrue(adminHtml.contains("@inr.format(quotation.grandTotal != null ?"));
        assertTrue(adminHtml.contains("@inr.format(quotation.travelCharge)"));
        assertTrue(adminHtml.contains("@inr.format(quotation.gstAmount)"));

        assertTrue(empHtml.contains("@inr.format(quotation.grandTotal != null ?"));
        assertTrue(empHtml.contains("@inr.format(quotation.travelCharge)"));
        assertTrue(empHtml.contains("@inr.format(quotation.gstAmount)"));
    }
}
