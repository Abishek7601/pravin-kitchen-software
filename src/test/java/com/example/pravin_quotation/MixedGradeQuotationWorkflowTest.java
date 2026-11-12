package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class MixedGradeQuotationWorkflowTest {

    private QuotationRepository quotationRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private BranchRepository branchRepository;
    private WorkCategoryRepository workCategoryRepository;
    private DivisionRepository divisionRepository;
    private ItemRepository itemRepository;
    private MaterialRepository materialRepository;
    private MaterialOptionRepository materialOptionRepository;
    private MaterialSpecificationRepository materialSpecificationRepository;
    private PricingRepository pricingRepository;
    private DistrictPricingRepository districtPricingRepository;
    private QuotationRoomRepository quotationRoomRepository;
    private QuotationItemRepository quotationItemRepository;
    private QuotationItemSizeRepository quotationItemSizeRepository;
    private QuotationRoomService quotationRoomService;
    private QuotationItemService quotationItemService;
    private QuotationItemSizeService quotationItemSizeService;
    private QuotationCalculationService quotationCalculationService;
    private NumberToWordsService numberToWordsService;

    private AdminQuotationBuilderService builderService;

    private User adminUser;
    private User employeeUser;
    private Branch branchMadurai;
    private Customer customer;

    private final Map<Long, Quotation> quotationStore = new HashMap<>();
    private final Map<Long, List<QuotationRoom>> roomStore = new HashMap<>();
    private final Map<Long, List<QuotationItem>> itemStore = new HashMap<>();
    private final Map<Long, List<QuotationItemSize>> sizeStore = new HashMap<>();
    private final Map<Long, Customer> customerStore = new HashMap<>();
    private final AtomicLong quotationIdGen = new AtomicLong(100L);
    private final AtomicLong customerIdGen = new AtomicLong(500L);
    private final AtomicLong roomIdGen = new AtomicLong(200L);
    private final AtomicLong itemIdGen = new AtomicLong(300L);
    private final AtomicLong sizeIdGen = new AtomicLong(400L);

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
        quotationStore.clear();
        roomStore.clear();
        itemStore.clear();
        sizeStore.clear();

        branchMadurai = new Branch();
        branchMadurai.setId(1L);
        branchMadurai.setName("Madurai");
        branchMadurai.setActive(true);

        adminUser = new User();
        adminUser.setId(10L);
        adminUser.setEmail("admin@pravinkitchens.com");
        adminUser.setName("Super Admin");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);

        employeeUser = new User();
        employeeUser.setId(20L);
        employeeUser.setEmail("employee@pravinkitchens.com");
        employeeUser.setName("Employee User");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branchMadurai);
        employeeUser.setActive(true);

        customer = new Customer();
        customer.setId(500L);
        customer.setName("Karthik");
        customer.setPhone("9876543210");
        customer.setEmail("karthik@example.com");
        customer.setBranch(branchMadurai);

        quotationRepository = (QuotationRepository) Proxy.newProxyInstance(
                QuotationRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Quotation q = (Quotation) args[0];
                        if (q.getId() == null) {
                            q.setId(quotationIdGen.incrementAndGet());
                        }
                        quotationStore.put(q.getId(), q);
                        return q;
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(quotationStore.get(id));
                    } else if ("findByQuotationNumber".equals(name)) {
                        String num = (String) args[0];
                        return quotationStore.values().stream()
                                .filter(q -> num.equals(q.getRawQuotationNumber()) || num.equals(q.getQuotationNumber()))
                                .findFirst();
                    } else if ("existsByQuotationNumber".equals(name)) {
                        String num = (String) args[0];
                        return quotationStore.values().stream()
                                .anyMatch(q -> num.equals(q.getRawQuotationNumber()) || num.equals(q.getQuotationNumber()));
                    } else if ("count".equals(name)) {
                        return (long) quotationStore.size();
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findByEmail".equals(method.getName())) {
                        String email = (String) args[0];
                        if ("admin@pravinkitchens.com".equals(email)) return Optional.of(adminUser);
                        if ("employee@pravinkitchens.com".equals(email)) return Optional.of(employeeUser);
                    }
                    return Optional.empty();
                }
        );

        branchRepository = (BranchRepository) Proxy.newProxyInstance(
                BranchRepository.class.getClassLoader(),
                new Class<?>[]{BranchRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Long id = (Long) args[0];
                        if (Long.valueOf(1L).equals(id)) return Optional.of(branchMadurai);
                    } else if ("findAll".equals(method.getName())) {
                        return List.of(branchMadurai);
                    }
                    return Optional.empty();
                }
        );

        customerStore.clear();
        customerStore.put(customer.getId(), customer);

        customerRepository = (CustomerRepository) Proxy.newProxyInstance(
                CustomerRepository.class.getClassLoader(),
                new Class<?>[]{CustomerRepository.class},
                (proxy, method, args) -> {
                    String mName = method.getName();
                    if ("save".equals(mName)) {
                        Customer c = (Customer) args[0];
                        if (c.getId() == null) {
                            c.setId(customerIdGen.incrementAndGet());
                        }
                        customerStore.put(c.getId(), c);
                        return c;
                    }
                    if ("findById".equals(mName)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(customerStore.get(id));
                    }
                    if ("findByPhone".equals(mName)) {
                        String phone = (String) args[0];
                        return customerStore.values().stream().filter(c -> phone.equals(c.getPhone())).findFirst();
                    }
                    if ("findByEmail".equals(mName)) {
                        String email = (String) args[0];
                        return customerStore.values().stream().filter(c -> email.equalsIgnoreCase(c.getEmail())).findFirst();
                    }
                    return Optional.empty();
                }
        );

        WorkCategory kitchenCat = new WorkCategory();
        kitchenCat.setId(1L);
        kitchenCat.setName("Modular Kitchen");

        Division cabinetDiv = new Division();
        cabinetDiv.setId(1L);
        cabinetDiv.setName("Base Cabinet");
        cabinetDiv.setWorkCategory(kitchenCat);

        Material premMat = new Material();
        premMat.setId(1L);
        premMat.setName("Marine Plywood (Premium)");
        premMat.setActive(true);
        premMat.setPricingGrade(PricingMode.PREMIUM);
        premMat.setRate(BigDecimal.valueOf(1800.0));
        premMat.setMeasurementType(MeasurementType.SQFT);

        Material midMat = new Material();
        midMat.setId(2L);
        midMat.setName("BWR Plywood (Middle)");
        midMat.setActive(true);
        midMat.setPricingGrade(PricingMode.MIDDLE);
        midMat.setRate(BigDecimal.valueOf(1200.0));
        midMat.setMeasurementType(MeasurementType.SQFT);

        Material econMat = new Material();
        econMat.setId(3L);
        econMat.setName("Commercial Plywood (Economy)");
        econMat.setActive(true);
        econMat.setPricingGrade(PricingMode.ECONOMY);
        econMat.setRate(BigDecimal.valueOf(800.0));
        econMat.setMeasurementType(MeasurementType.SQFT);

        workCategoryRepository = (WorkCategoryRepository) Proxy.newProxyInstance(
                WorkCategoryRepository.class.getClassLoader(),
                new Class<?>[]{WorkCategoryRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(kitchenCat);
                    return Optional.empty();
                }
        );

        divisionRepository = (DivisionRepository) Proxy.newProxyInstance(
                DivisionRepository.class.getClassLoader(),
                new Class<?>[]{DivisionRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(cabinetDiv);
                    return Optional.empty();
                }
        );

        materialRepository = (MaterialRepository) Proxy.newProxyInstance(
                MaterialRepository.class.getClassLoader(),
                new Class<?>[]{MaterialRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Long id = (Long) args[0];
                        if (Long.valueOf(1L).equals(id)) return Optional.of(premMat);
                        if (Long.valueOf(2L).equals(id)) return Optional.of(midMat);
                        if (Long.valueOf(3L).equals(id)) return Optional.of(econMat);
                    }
                    return Optional.empty();
                }
        );

        itemRepository = (ItemRepository) Proxy.newProxyInstance(
                ItemRepository.class.getClassLoader(),
                new Class<?>[]{ItemRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Item it = new Item();
                        it.setId(1L);
                        it.setName("Base Unit");
                        it.setDivision(cabinetDiv);
                        return Optional.of(it);
                    }
                    return Optional.empty();
                }
        );

        materialOptionRepository = (MaterialOptionRepository) Proxy.newProxyInstance(
                MaterialOptionRepository.class.getClassLoader(),
                new Class<?>[]{MaterialOptionRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Long id = (Long) args[0];
                        MaterialOption mo = new MaterialOption();
                        mo.setId(id);
                        if (Long.valueOf(1L).equals(id)) mo.setMaterial(premMat);
                        else if (Long.valueOf(2L).equals(id)) mo.setMaterial(midMat);
                        else mo.setMaterial(econMat);
                        return Optional.of(mo);
                    }
                    return Optional.empty();
                }
        );

        materialSpecificationRepository = createMock(MaterialSpecificationRepository.class);

        pricingRepository = (PricingRepository) Proxy.newProxyInstance(
                PricingRepository.class.getClassLoader(),
                new Class<?>[]{PricingRepository.class},
                (proxy, method, args) -> {
                    if ("findByMaterialOptionIdAndPricingMode".equals(method.getName())) {
                        Long optionId = (Long) args[0];
                        PricingMode mode = (PricingMode) args[1];
                        Pricing p = new Pricing();
                        p.setId(optionId);
                        p.setActive(true);
                        p.setPricingMode(mode);
                        if (mode == PricingMode.PREMIUM) p.setRate(BigDecimal.valueOf(1800.0));
                        else if (mode == PricingMode.MIDDLE) p.setRate(BigDecimal.valueOf(1200.0));
                        else p.setRate(BigDecimal.valueOf(800.0));
                        return Optional.of(p);
                    }
                    return Optional.empty();
                }
        );

        districtPricingRepository = createMock(DistrictPricingRepository.class);

        quotationRoomRepository = (QuotationRoomRepository) Proxy.newProxyInstance(
                QuotationRoomRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRoomRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        QuotationRoom r = (QuotationRoom) args[0];
                        if (r.getId() == null) r.setId(roomIdGen.incrementAndGet());
                        List<QuotationRoom> list = roomStore.computeIfAbsent(r.getQuotation().getId(), k -> new ArrayList<>());
                        if (!list.contains(r)) {
                            list.add(r);
                        }
                        return r;
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return roomStore.values().stream().flatMap(List::stream).filter(r -> id.equals(r.getId())).findFirst();
                    } else if ("findByQuotationIdAndActiveTrueOrderByDisplayOrderAsc".equals(name)
                            || "findByQuotationIdAndActiveTrueOrderByIdAsc".equals(name)
                            || "findByQuotationIdAndActiveTrue".equals(name)
                            || "findByQuotationId".equals(name)) {
                        Long qId = (Long) args[0];
                        return roomStore.getOrDefault(qId, Collections.emptyList()).stream().filter(QuotationRoom::getActive).toList();
                    }
                    return Collections.emptyList();
                }
        );

        QuotationWorkflowRepository quotationWorkflowRepository = (QuotationWorkflowRepository) Proxy.newProxyInstance(
                QuotationWorkflowRepository.class.getClassLoader(),
                new Class<?>[]{QuotationWorkflowRepository.class},
                (proxy, method, args) -> args[0]
        );
        QuotationWorkflowService quotationWorkflowService = new QuotationWorkflowService(quotationWorkflowRepository);
        quotationRoomService = new QuotationRoomService(quotationRoomRepository, quotationWorkflowService, quotationRepository);

        quotationItemRepository = (QuotationItemRepository) Proxy.newProxyInstance(
                QuotationItemRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        QuotationItem qi = (QuotationItem) args[0];
                        if (qi.getId() == null) qi.setId(itemIdGen.incrementAndGet());
                        List<QuotationItem> list = itemStore.computeIfAbsent(qi.getQuotationRoom().getId(), k -> new ArrayList<>());
                        if (!list.contains(qi)) list.add(qi);
                        return qi;
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        for (List<QuotationItem> list : itemStore.values()) {
                            for (QuotationItem qi : list) {
                                if (id.equals(qi.getId())) return Optional.of(qi);
                            }
                        }
                        return Optional.empty();
                    } else if ("findByQuotationRoomIdAndActiveTrueOrderByDisplayOrderAsc".equals(name)
                            || "findByQuotationRoomIdAndActiveTrueOrderByIdAsc".equals(name)
                            || "findByQuotationRoomIdAndActiveTrue".equals(name)
                            || "findByQuotationRoomId".equals(name)) {
                        Long rId = (Long) args[0];
                        return itemStore.getOrDefault(rId, Collections.emptyList()).stream().filter(QuotationItem::getActive).toList();
                    }
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return Collections.emptyList();
                }
        );
        quotationItemService = new QuotationItemService(quotationItemRepository, quotationWorkflowService);

        quotationItemSizeRepository = (QuotationItemSizeRepository) Proxy.newProxyInstance(
                QuotationItemSizeRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemSizeRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        QuotationItemSize sz = (QuotationItemSize) args[0];
                        if (sz.getId() == null) sz.setId(sizeIdGen.incrementAndGet());
                        List<QuotationItemSize> list = sizeStore.computeIfAbsent(sz.getQuotationItem().getId(), k -> new ArrayList<>());
                        if (!list.contains(sz)) list.add(sz);
                        return sz;
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        for (List<QuotationItemSize> list : sizeStore.values()) {
                            for (QuotationItemSize sz : list) {
                                if (id.equals(sz.getId())) return Optional.of(sz);
                            }
                        }
                        return Optional.empty();
                    } else if ("findByQuotationItemIdAndActiveTrue".equals(name)
                            || "findByQuotationItemIdAndActiveTrueOrderByIdAsc".equals(name)
                            || "findByQuotationItemIdAndActiveTrueOrderByDisplayOrderAsc".equals(name)
                            || "findByQuotationItemId".equals(name)) {
                        Long itId = (Long) args[0];
                        return sizeStore.getOrDefault(itId, Collections.emptyList()).stream().filter(QuotationItemSize::getActive).toList();
                    }
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return Collections.emptyList();
                }
        );
        quotationItemSizeService = new QuotationItemSizeService(quotationItemSizeRepository);

        quotationCalculationService = new QuotationCalculationService(
                quotationRepository,
                quotationRoomRepository,
                quotationItemRepository,
                pricingRepository,
                districtPricingRepository,
                null,
                quotationItemSizeService
        ) {
            @Override
            public Quotation calculateQuotation(Long id) {
                Quotation q = quotationStore.get(id);
                if (q == null) return null;
                List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationIdAndActiveTrueOrderByIdAsc(id);
                BigDecimal subtotal = BigDecimal.ZERO;
                for (QuotationRoom r : rooms) {
                    List<QuotationItem> items = quotationItemRepository.findByQuotationRoomIdAndActiveTrueOrderByIdAsc(r.getId());
                    BigDecimal roomSubtotal = BigDecimal.ZERO;
                    for (QuotationItem item : items) {
                        BigDecimal itemRate = item.getRate() != null ? item.getRate() : BigDecimal.ZERO;
                        BigDecimal sqft = BigDecimal.ZERO;
                        List<QuotationItemSize> sizes = quotationItemSizeRepository.findByQuotationItemIdAndActiveTrueOrderByDisplayOrderAsc(item.getId());
                        for (QuotationItemSize sz : sizes) {
                            sqft = sqft.add(sz.getCalculatedSqft() != null ? sz.getCalculatedSqft() : BigDecimal.ONE);
                        }
                        if (sqft.compareTo(BigDecimal.ZERO) == 0) sqft = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                        BigDecimal total = itemRate.multiply(sqft);
                        item.setAmount(total);
                        item.setOfferPrice(total);
                        roomSubtotal = roomSubtotal.add(total);
                    }
                    r.setSubtotal(roomSubtotal);
                    r.setOfferPrice(roomSubtotal);
                    subtotal = subtotal.add(roomSubtotal);
                }
                q.setSubtotal(subtotal);
                BigDecimal discount = q.getDiscountAmount() != null ? q.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal other = q.getOtherCharges() != null ? q.getOtherCharges() : BigDecimal.ZERO;
                BigDecimal taxable = subtotal.subtract(discount).add(other);
                if (taxable.compareTo(BigDecimal.ZERO) < 0) taxable = BigDecimal.ZERO;
                q.setTaxableAmount(taxable);
                BigDecimal gstPct = q.getGstPercentage() != null ? q.getGstPercentage() : BigDecimal.ZERO;
                BigDecimal gstAmt = taxable.multiply(gstPct).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                q.setGstAmount(gstAmt);
                q.setGrandTotal(taxable.add(gstAmt));
                return q;
            }
        };

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

        numberToWordsService = new NumberToWordsService();
    }

    @Test
    @DisplayName("Single quotation holds mixed-grade items under ONE single quotation number without parent/child split")
    public void testMixedGradeItemsUnderSingleQuotationNumber() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(6.0), BigDecimal.valueOf(6.0), BigDecimal.valueOf(12.0), null
        );

        // 1. Premium Grade item (Rate: 1800)
        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Unit Premium", "Marine Ply", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1800.0), null, PricingMode.PREMIUM
        );

        // 2. Middle Grade item (Rate: 1200)
        AdminQuotationBuilderService.ItemRequest midItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 2L, 2L, null,
                "Base Unit Middle", "BWR Ply", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0), null, PricingMode.MIDDLE
        );

        // 3. Economy Grade item (Rate: 800)
        AdminQuotationBuilderService.ItemRequest econItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 3L, 3L, null,
                "Base Unit Economy", "Commercial Ply", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(800.0), null, PricingMode.ECONOMY
        );

        AdminQuotationBuilderService.RoomRequest mixedRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Mixed Grade Kitchen", List.of(premItem, midItem, econItem)
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Need mixed grade design", "Single mixed quote",
                List.of(mixedRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation saved = builderService.save(builderReq, "admin@pravinkitchens.com");
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertNotNull(saved.getQuotationNumber());
        assertTrue(saved.getQuotationNumber().startsWith("QT-"));

        // Criterion 1: Exactly ONE quotation record, no parent and no child quotations created
        assertNull(saved.getParent(), "Mixed-grade quotation must not have a parent quotation");
        assertTrue(saved.getChildQuotations() == null || saved.getChildQuotations().isEmpty(),
                "Mixed-grade quotation must not have separate child quotations");
        assertFalse(saved.isMultiGrade(), "Single mixed-grade quotation is not multiGrade child structure");

        // Verify rooms and items
        List<QuotationRoom> activeRooms = quotationRoomService.getActiveByQuotationId(saved.getId());
        assertEquals(1, activeRooms.size());

        List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(activeRooms.get(0).getId());
        assertEquals(3, items.size(), "All 3 mixed-grade items must belong to the same quotation room");

        // Verify grade preservation on each item
        QuotationItem item1 = items.get(0);
        QuotationItem item2 = items.get(1);
        QuotationItem item3 = items.get(2);

        assertEquals(PricingMode.PREMIUM, item1.getPricingMode());
        assertEquals(PricingMode.MIDDLE, item2.getPricingMode());
        assertEquals(PricingMode.ECONOMY, item3.getPricingMode());

        assertEquals(0, BigDecimal.valueOf(1800.0).compareTo(item1.getRate()));
        assertEquals(0, BigDecimal.valueOf(1200.0).compareTo(item2.getRate()));
        assertEquals(0, BigDecimal.valueOf(800.0).compareTo(item3.getRate()));

        // Criterion 5: Grade-wise subtotals calculation
        Map<PricingMode, BigDecimal> subtotals = quotationCalculationService.calculateGradeSubtotals(saved.getId());
        assertEquals(0, BigDecimal.valueOf(1800.0).compareTo(subtotals.get(PricingMode.PREMIUM)), "Premium subtotal must be 1800");
        assertEquals(0, BigDecimal.valueOf(1200.0).compareTo(subtotals.get(PricingMode.MIDDLE)), "Middle subtotal must be 1200");
        assertEquals(0, BigDecimal.valueOf(800.0).compareTo(subtotals.get(PricingMode.ECONOMY)), "Economy subtotal must be 800");

        // Combined subtotal = 1800 + 1200 + 800 = 3800
        assertEquals(0, BigDecimal.valueOf(3800.0).compareTo(saved.getSubtotal()), "Combined subtotal must be 3800");

        // GST at 18% on 3800 = 684.00; Grand Total = 4484.00
        assertEquals(0, new BigDecimal("684.00").compareTo(saved.getGstAmount()));
        assertEquals(0, new BigDecimal("4484.00").compareTo(saved.getGrandTotal()));

        // Criterion 6: Payment Terms Schedule based on combined Grand Total (4484.00)
        BigDecimal grandTotal = saved.getGrandTotal();
        BigDecimal token10 = grandTotal.multiply(new BigDecimal("0.10"));
        BigDecimal advance50 = grandTotal.multiply(new BigDecimal("0.50"));
        BigDecimal part35 = grandTotal.multiply(new BigDecimal("0.35"));
        BigDecimal final5 = grandTotal.multiply(new BigDecimal("0.05"));

        assertEquals(0, new BigDecimal("448.400").compareTo(token10));
        assertEquals(0, new BigDecimal("2242.000").compareTo(advance50));
        assertEquals(0, new BigDecimal("1569.400").compareTo(part35));
        assertEquals(0, new BigDecimal("224.200").compareTo(final5));
    }

    @Test
    @DisplayName("Employee Portal saves and recalculates mixed-grade quotation identically to Admin")
    public void testEmployeePortalMixedGradeQuotationWorkflow() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(6.0), BigDecimal.valueOf(6.0), BigDecimal.valueOf(12.0), null
        );

        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Premium Item", "Spec 1", List.of(size), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1800.0), null, PricingMode.PREMIUM
        );
        AdminQuotationBuilderService.ItemRequest midItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 2L, 2L, null,
                "Middle Item", "Spec 2", List.of(size), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0), null, PricingMode.MIDDLE
        );

        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "1st Floor", "Bedroom", "Employee Bedroom", List.of(premItem, midItem)
        );

        AdminQuotationBuilderService.BuilderRequest builderReq = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Employee Mixed Quote", "Notes",
                List.of(room), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "1st Floor", "Bedroom", 1L, 1L, null
        );

        // Employee saves
        Quotation empSaved = builderService.save(builderReq, "employee@pravinkitchens.com");
        assertNotNull(empSaved);
        assertEquals(Role.EMPLOYEE, empSaved.getEmployee().getRole());
        assertNull(empSaved.getParent());

        Map<PricingMode, BigDecimal> subtotals = quotationCalculationService.calculateGradeSubtotals(empSaved.getId());
        assertEquals(0, BigDecimal.valueOf(1800.0).compareTo(subtotals.get(PricingMode.PREMIUM)));
        assertEquals(0, BigDecimal.valueOf(1200.0).compareTo(subtotals.get(PricingMode.MIDDLE)));
        assertEquals(0, BigDecimal.ZERO.compareTo(subtotals.get(PricingMode.ECONOMY)));
    }

    @Test
    @DisplayName("Auto-save saves PENDING and subsequent save updates same record to WORKING with all mixed items preserved")
    public void testAutoSaveAndFinalSaveLifecycle() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(6.0), BigDecimal.valueOf(6.0), BigDecimal.valueOf(12.0), null
        );

        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Premium Draft", "Marine", List.of(size), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1800.0), null, PricingMode.PREMIUM
        );

        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living", "Draft Work", List.of(premItem)
        );

        AdminQuotationBuilderService.BuilderRequest autoSaveReq = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Requirements", "Auto-save Draft",
                List.of(room), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Living", 1L, 1L, null
        );

        // 1. Auto-save
        Quotation autoSaved = builderService.autoSave(autoSaveReq, "admin@pravinkitchens.com");
        assertNotNull(autoSaved);
        assertEquals(QuotationStatus.PENDING, autoSaved.getStatus());
        Long quotationId = autoSaved.getId();

        // 2. Add Middle item and click Final Save
        AdminQuotationBuilderService.ItemRequest midItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 2L, 2L, null,
                "Middle Additional", "BWR", List.of(size), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0), null, PricingMode.MIDDLE
        );

        AdminQuotationBuilderService.RoomRequest updatedRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living", "Draft Work", List.of(premItem, midItem)
        );

        AdminQuotationBuilderService.BuilderRequest finalSaveReq = new AdminQuotationBuilderService.BuilderRequest(
                quotationId, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Requirements", "Final Save",
                List.of(updatedRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Living", 1L, 1L, null
        );

        Quotation finalSaved = builderService.save(finalSaveReq, "admin@pravinkitchens.com");
        assertEquals(quotationId, finalSaved.getId(), "Final save must update the exact same record");
        assertEquals(QuotationStatus.WORKING, finalSaved.getStatus());

        List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(quotationId);
        List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(rooms.get(0).getId());
        assertEquals(2, items.size());
        assertEquals(PricingMode.PREMIUM, items.get(0).getPricingMode());
        assertEquals(PricingMode.MIDDLE, items.get(1).getPricingMode());
    }

    @Test
    @DisplayName("PDF service generates valid PDF bytes containing grade breakdown for mixed-grade quotation")
    public void testMixedGradePdfGeneration() {
        Quotation q = new Quotation();
        q.setId(555L);
        q.setQuotationNumber("QT-00055");
        q.setCustomer(customer);
        q.setBranch(branchMadurai);
        q.setEmployee(adminUser);
        q.setQuotationDate(LocalDate.now());
        q.setValidUntil(LocalDate.now().plusDays(30));
        q.setSubtotal(new BigDecimal("3000.00"));
        q.setTaxableAmount(new BigDecimal("3000.00"));
        q.setGstPercentage(new BigDecimal("18.00"));
        q.setGstAmount(new BigDecimal("540.00"));
        q.setGrandTotal(new BigDecimal("3540.00"));
        q.setStatus(QuotationStatus.WORKING);
        quotationStore.put(q.getId(), q);

        QuotationRoom room = new QuotationRoom();
        room.setId(666L);
        room.setQuotation(q);
        room.setFloor("Ground Floor");
        room.setRoom("Kitchen");
        room.setActive(true);
        roomStore.put(q.getId(), List.of(room));

        QuotationItem item1 = new QuotationItem();
        item1.setId(777L);
        item1.setQuotationRoom(room);
        item1.setItemDescription("Cabinet 1");
        item1.setPricingMode(PricingMode.PREMIUM);
        item1.setRate(new BigDecimal("1800.00"));
        item1.setAmount(new BigDecimal("1800.00"));
        item1.setOfferPrice(new BigDecimal("1800.00"));
        item1.setActive(true);

        QuotationItem item2 = new QuotationItem();
        item2.setId(778L);
        item2.setQuotationRoom(room);
        item2.setItemDescription("Cabinet 2");
        item2.setPricingMode(PricingMode.MIDDLE);
        item2.setRate(new BigDecimal("1200.00"));
        item2.setAmount(new BigDecimal("1200.00"));
        item2.setOfferPrice(new BigDecimal("1200.00"));
        item2.setActive(true);

        itemStore.put(room.getId(), List.of(item1, item2));

        QuotationPdfService pdfService = new QuotationPdfService(
                quotationRepository,
                quotationRoomRepository,
                quotationItemRepository,
                numberToWordsService,
                branchRepository,
                pricingRepository,
                quotationCalculationService
        );

        byte[] pdfBytes = pdfService.generateQuotationPdf(q.getId());
        assertNotNull(pdfBytes, "Generated PDF bytes must not be null");
        assertTrue(pdfBytes.length > 1000, "PDF size should be substantial and valid");
        // PDF magic header %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        // Verify PDF quotation type dynamic determination
        String resolvedType = pdfService.resolveQuotationType(q);
        assertEquals("Premium + Middle", resolvedType, "Quotation type should be 'Premium + Middle' for items with Premium and Middle grades");

        // Add Economy item
        QuotationItem item3 = new QuotationItem();
        item3.setId(779L);
        item3.setQuotationRoom(room);
        item3.setItemDescription("Cabinet 3");
        item3.setPricingMode(PricingMode.ECONOMY);
        item3.setActive(true);
        itemStore.put(room.getId(), List.of(item1, item2, item3));

        assertEquals("Premium + Middle + Economy", pdfService.resolveQuotationType(q),
                "Quotation type should maintain Premium -> Middle -> Economy order joined by ' + '");

        // Only Middle and Economy
        itemStore.put(room.getId(), List.of(item2, item3));
        assertEquals("Middle + Economy", pdfService.resolveQuotationType(q),
                "Quotation type should be 'Middle + Economy' when only those grades are present");

        // Single Grade
        itemStore.put(room.getId(), List.of(item1));
        assertEquals("Premium", pdfService.resolveQuotationType(q),
                "Quotation type should be 'Premium' when only Premium items exist");
    }

    @Test
    @DisplayName("Verify templates have action buttons moved to bottom and material names display grade in brackets")
    public void testTemplatesBottomActionButtonsAndMaterialGradeInBrackets() throws Exception {
        String adminHtml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/admin/quotation-view.html"));
        String empHtml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        String fragHtml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/fragments/quotation-breakdown-table.html"));

        // 1. Both admin and employee details should have bottom action bar
        assertTrue(adminHtml.contains("quotation-bottom-actions-bar"), "Admin details must contain quotation-bottom-actions-bar");
        assertTrue(empHtml.contains("quotation-bottom-actions-bar"), "Employee details must contain quotation-bottom-actions-bar");

        // 2. Both admin and employee details must NOT have Edit/Download/WhatsApp/Email buttons in the top quotation-summary-card
        int adminSummaryStart = adminHtml.indexOf("class=\"detail-card quotation-summary-card\"");
        int adminSummaryEnd = adminHtml.indexOf("</section>", adminSummaryStart);
        String adminTopCard = adminHtml.substring(adminSummaryStart, adminSummaryEnd);
        assertFalse(adminTopCard.contains("btn-grade-edit"), "Top summary card in admin must not contain edit quote button");
        assertFalse(adminTopCard.contains("btn-grade-pdf"), "Top summary card in admin must not contain download pdf button");
        assertFalse(adminTopCard.contains("btn-grade-whatsapp"), "Top summary card in admin must not contain share whatsapp button");
        assertFalse(adminTopCard.contains("btn-grade-email"), "Top summary card in admin must not contain send email button");

        int empSummaryStart = empHtml.indexOf("class=\"detail-card quotation-summary-card\"");
        int empSummaryEnd = empHtml.indexOf("</section>", empSummaryStart);
        String empTopCard = empHtml.substring(empSummaryStart, empSummaryEnd);
        assertFalse(empTopCard.contains("btn-grade-edit"), "Top summary card in employee must not contain edit quote button");
        assertFalse(empTopCard.contains("btn-grade-pdf"), "Top summary card in employee must not contain download pdf button");
        assertFalse(empTopCard.contains("btn-grade-whatsapp"), "Top summary card in employee must not contain share whatsapp button");
        assertFalse(empTopCard.contains("btn-grade-email"), "Top summary card in employee must not contain send email button");

        // 3. Material column displays grade inside brackets beside the material name
        assertTrue(adminHtml.contains("item.effectivePricingMode == T(com.example.pravin_quotation.model.PricingMode).PREMIUM ? 'Premium'"),
                "Admin view must format material name with bracketed grade");
        assertTrue(empHtml.contains("item.effectivePricingMode == T(com.example.pravin_quotation.model.PricingMode).PREMIUM ? 'Premium'"),
                "Employee view must format material name with bracketed grade");
        assertTrue(fragHtml.contains("item.effectivePricingMode == T(com.example.pravin_quotation.model.PricingMode).PREMIUM ? 'Premium'"),
                "Shared breakdown table fragment must format material name with bracketed grade");
    }

    @Test
    @DisplayName("Verify Project Information pricingModeDisplay produces all 7 combinations in Premium -> Middle -> Economy order")
    public void testPricingModeDisplayAllSevenCombinations() {
        QuotationCalculationService calcService = new QuotationCalculationService(
                quotationRepository, quotationRoomRepository, quotationItemRepository,
                pricingRepository, districtPricingRepository, null, quotationItemSizeService
        );

        // 1. Premium only
        assertEquals("Premium", calcService.formatPricingModeDisplay(Set.of(PricingMode.PREMIUM), PricingMode.MIDDLE));

        // 2. Middle only
        assertEquals("Middle / Standard", calcService.formatPricingModeDisplay(Set.of(PricingMode.MIDDLE), PricingMode.MIDDLE));

        // 3. Economy only
        assertEquals("Economy", calcService.formatPricingModeDisplay(Set.of(PricingMode.ECONOMY), PricingMode.MIDDLE));

        // 4. Premium + Middle
        assertEquals("Premium + Middle", calcService.formatPricingModeDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), PricingMode.MIDDLE));

        // 5. Premium + Economy
        assertEquals("Premium + Economy", calcService.formatPricingModeDisplay(Set.of(PricingMode.PREMIUM, PricingMode.ECONOMY), PricingMode.MIDDLE));

        // 6. Middle + Economy
        assertEquals("Middle + Economy", calcService.formatPricingModeDisplay(Set.of(PricingMode.MIDDLE, PricingMode.ECONOMY), PricingMode.MIDDLE));

        // 7. All three: Premium + Middle + Economy
        assertEquals("Premium + Middle + Economy", calcService.formatPricingModeDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE, PricingMode.ECONOMY), PricingMode.MIDDLE));

        // Fallback when empty
        assertEquals("Middle / Standard", calcService.formatPricingModeDisplay(Collections.emptySet(), PricingMode.MIDDLE));
        assertEquals("Premium", calcService.formatPricingModeDisplay(Collections.emptySet(), PricingMode.PREMIUM));
        assertEquals("Economy", calcService.formatPricingModeDisplay(Collections.emptySet(), PricingMode.ECONOMY));
    }

    @Test
    @DisplayName("Edit quote updates same quotation ID, deactivates old items, adds new items, and recalculates grade subtotals")
    public void testEditQuotationPreservesSameQuotationAndUpdatesGradeSubtotals() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(6.0), BigDecimal.valueOf(6.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Unit Premium", "Marine Ply", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1800.0), null, PricingMode.PREMIUM
        );
        AdminQuotationBuilderService.RoomRequest room1 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen 1", List.of(premItem)
        );
        AdminQuotationBuilderService.BuilderRequest initialReq = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Requirements", "Notes",
                List.of(room1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation created = builderService.save(initialReq, "admin@pravinkitchens.com");
        Long originalId = created.getId();
        String originalQuoteNo = created.getQuotationNumber();

        // Now Edit quote: same quotationId, add Economy item as well
        AdminQuotationBuilderService.ItemRequest econItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 3L, 3L, null,
                "Wall Unit Economy", "Commercial Ply", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(800.0), null, PricingMode.ECONOMY
        );
        AdminQuotationBuilderService.RoomRequest updatedRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen 1 Updated", List.of(premItem, econItem)
        );
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                originalId, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Requirements Updated", "Notes Updated",
                List.of(updatedRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation edited = builderService.save(editReq, "admin@pravinkitchens.com");
        assertEquals(originalId, edited.getId(), "Edited quote must retain the exact same quotation ID");
        assertEquals(originalQuoteNo, edited.getQuotationNumber(), "Edited quote must retain the exact same quotation number");

        // Grade subtotals should have Premium: 1800, Middle: 0, Economy: 800
        Map<PricingMode, BigDecimal> subtotals = quotationCalculationService.calculateGradeSubtotals(edited.getId());
        assertEquals(0, BigDecimal.valueOf(1800.0).compareTo(subtotals.get(PricingMode.PREMIUM)));
        assertEquals(0, BigDecimal.ZERO.compareTo(subtotals.get(PricingMode.MIDDLE)));
        assertEquals(0, BigDecimal.valueOf(800.0).compareTo(subtotals.get(PricingMode.ECONOMY)));
        // Combined subtotal = 2600.00
        assertEquals(0, BigDecimal.valueOf(2600.0).compareTo(edited.getSubtotal()));
    }

    @Test
    @DisplayName("Verify Quotation List Pricing column formats strictly Premium -> Middle -> Economy uppercase labels")
    public void testPricingColumnDisplayLabels() {
        QuotationCalculationService calcService = new QuotationCalculationService(
                quotationRepository, quotationRoomRepository, quotationItemRepository,
                pricingRepository, districtPricingRepository, null, quotationItemSizeService
        );

        // 1. Premium only
        assertEquals("PREMIUM", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM), null));

        // 2. Middle only
        assertEquals("MIDDLE", calcService.formatPricingColumnDisplay(Set.of(PricingMode.MIDDLE), null));

        // 3. Economy only
        assertEquals("ECONOMY", calcService.formatPricingColumnDisplay(Set.of(PricingMode.ECONOMY), null));

        // 4. Premium + Middle
        assertEquals("PREMIUM + MIDDLE", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), null));

        // 5. Premium + Economy
        assertEquals("PREMIUM + ECONOMY", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.ECONOMY), null));

        // 6. Middle + Economy
        assertEquals("MIDDLE + ECONOMY", calcService.formatPricingColumnDisplay(Set.of(PricingMode.MIDDLE, PricingMode.ECONOMY), null));

        // 7. All three: Premium + Middle + Economy
        assertEquals("PREMIUM + MIDDLE + ECONOMY", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE, PricingMode.ECONOMY), null));

        // Not Prepared when empty
        assertEquals("Not Prepared", calcService.formatPricingColumnDisplay(Collections.emptySet(), null));
        assertEquals("Not Prepared", calcService.formatPricingColumnDisplay(null, null));

        // Confirmed grade indicators
        assertEquals("✓ PREMIUM", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM), PricingMode.PREMIUM));
        assertEquals("✓ MIDDLE", calcService.formatPricingColumnDisplay(Set.of(PricingMode.MIDDLE), PricingMode.MIDDLE));
        assertEquals("✓ ECONOMY", calcService.formatPricingColumnDisplay(Set.of(PricingMode.ECONOMY), PricingMode.ECONOMY));
        assertEquals("✓ PREMIUM + MIDDLE", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), PricingMode.PREMIUM));
        assertEquals("PREMIUM + ✓ MIDDLE", calcService.formatPricingColumnDisplay(Set.of(PricingMode.PREMIUM, PricingMode.MIDDLE), PricingMode.MIDDLE));
    }

    @Test
    @DisplayName("Verify Customer Details edit persistence in database without creating duplicates")
    public void testCustomerDetailsEditPersistence() {
        AdminQuotationBuilderService.CustomerRequest custReq1 = new AdminQuotationBuilderService.CustomerRequest(
                null, "Abishek", "9123456780", "abishek@example.com", "Madurai"
        );
        AdminQuotationBuilderService.SizeRequest sz = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(6.0), BigDecimal.valueOf(6.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest item1 = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Cabinet", "Ply", List.of(sz), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1000.0), null, PricingMode.PREMIUM
        );
        AdminQuotationBuilderService.RoomRequest room1 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen 1", List.of(item1)
        );
        AdminQuotationBuilderService.BuilderRequest req1 = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq1, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Initial Requirements", "Initial Notes",
                List.of(room1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation q1 = builderService.save(req1, "admin@pravinkitchens.com");
        assertNotNull(q1.getCustomer());
        Long savedCustId = q1.getCustomer().getId();
        assertEquals("Abishek", q1.getCustomer().getName());

        // Now Edit quote: same quote ID, existing customer ID, modified details
        AdminQuotationBuilderService.CustomerRequest custEdit = new AdminQuotationBuilderService.CustomerRequest(
                savedCustId, "Abishek S R", "9123456780", "abishek.sr@example.com", "Madurai South"
        );
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                q1.getId(), null, custEdit, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Updated Requirements", "Updated Notes",
                List.of(room1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation qEdited = builderService.save(editReq, "admin@pravinkitchens.com");
        assertEquals(q1.getId(), qEdited.getId(), "Must retain same quotation ID");
        assertEquals(savedCustId, qEdited.getCustomer().getId(), "Must retain and update same customer entity");
        assertEquals("Abishek S R", qEdited.getCustomer().getName(), "Customer name must be updated");
        assertEquals("abishek.sr@example.com", qEdited.getCustomer().getEmail(), "Customer email must be updated");
        assertEquals("Madurai South", qEdited.getCustomer().getAddress(), "Customer address must be updated");
        assertEquals("Updated Requirements", qEdited.getCustomerRequirements(), "Requirements must update");
        assertEquals("Updated Notes", qEdited.getNotes(), "Notes must update");
    }

    @Test
    @DisplayName("Verify bottom actions bar layout CSS and AJAX email submit handler in templates")
    public void testBottomActionsBarLayoutAndAsyncEmail() throws Exception {
        String css = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/static/css/quotation-view.css"));
        assertTrue(css.contains(".quotation-bottom-actions-bar"), "CSS must contain .quotation-bottom-actions-bar");
        assertTrue(css.contains("display: flex !important;"), "Bar must be flex row");
        assertTrue(css.contains(".quotation-bottom-actions-bar .btn-grade-email"), "Must define email button in bar");
        assertTrue(css.contains("width: auto !important;"), "Email button must have width auto to align horizontally");
        assertTrue(css.contains("@keyframes agy-spin"), "CSS must contain spinner animation");

        String adminView = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/admin/quotation-view.html"));
        assertTrue(adminView.contains("class=\"quotation-bottom-actions-bar\""), "Admin view must contain quotation-bottom-actions-bar");
        assertFalse(adminView.contains("quotation-bottom-actions-bar\" th:if="), "Admin view action bar must not be restricted by th:if");
        assertTrue(adminView.contains("title=\"Edit in Quotation Builder\">✏ Edit Quote</a>"), "Admin view must contain Edit Quote button");
        assertTrue(adminView.contains("title=\"Download Quotation PDF\">↓ Download PDF</a>"), "Admin view must contain Download PDF button");
        assertTrue(adminView.contains("title=\"Share Quotation via WhatsApp\">💬 Share WhatsApp</button>"), "Admin view must contain Share WhatsApp button");
        assertTrue(adminView.contains("title=\"Email PDF to Customer\">✉ Send Email</button>"), "Admin view must contain Send Email button");
        assertTrue(adminView.contains("handleAsyncEmailSubmit(event, this)"), "Admin view must use handleAsyncEmailSubmit");
        assertTrue(adminView.contains("showToastNotification"), "Admin view must include toast notifications");
        assertTrue(adminView.contains("Sending Email..."), "Admin view must show sending email loader");

        String empWork = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/employee/quotation-workspace.html"));
        assertTrue(empWork.contains("class=\"quotation-bottom-actions-bar\""), "Employee workspace must contain quotation-bottom-actions-bar");
        assertFalse(empWork.contains("quotation-bottom-actions-bar\" th:if="), "Employee workspace action bar must not be restricted by th:if");
        assertTrue(empWork.contains("title=\"Edit in Quotation Builder\">✏ Edit Quote</a>"), "Employee workspace must contain Edit Quote button");
        assertTrue(empWork.contains("title=\"Download Quotation PDF\">↓ Download PDF</a>"), "Employee workspace must contain Download PDF button");
        assertTrue(empWork.contains("title=\"Share Quotation via WhatsApp\">💬 Share WhatsApp</button>"), "Employee workspace must contain Share WhatsApp button");
        assertTrue(empWork.contains("title=\"Email PDF to Customer\">✉ Send Email</button>"), "Employee workspace must contain Send Email button");
        assertTrue(empWork.contains("handleAsyncEmailSubmit(event, this)"), "Employee workspace must use handleAsyncEmailSubmit");
        assertTrue(empWork.contains("showToastNotification"), "Employee workspace must include toast notifications");

        String empQuotes = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/employee/quotations.html"));
        assertTrue(empQuotes.contains("<th>Pricing</th>"), "Employee quotations table must have Pricing column");
        assertTrue(empQuotes.contains("quotation.pricingModeDisplay"), "Employee quotations table must render pricingModeDisplay");

        String adminQuotes = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/templates/admin/quotations.html"));
        assertTrue(adminQuotes.contains("quotation.pricingModeDisplay"), "Admin quotations table must render pricingModeDisplay");
    }
}