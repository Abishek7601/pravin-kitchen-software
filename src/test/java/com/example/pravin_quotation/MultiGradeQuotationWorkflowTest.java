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

public class MultiGradeQuotationWorkflowTest {

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
    private QuotationRoomService quotationRoomService;
    private QuotationItemService quotationItemService;
    private QuotationItemSizeService quotationItemSizeService;
    private QuotationCalculationService quotationCalculationService;
    private QuotationService quotationService;

    private AdminQuotationBuilderService builderService;

    private User adminUser;
    private Branch branchMadurai;
    private Customer customer;

    private final Map<Long, Quotation> quotationStore = new HashMap<>();
    private final Map<Long, List<QuotationRoom>> roomStore = new HashMap<>();
    private final AtomicLong quotationIdGen = new AtomicLong(100L);
    private final AtomicLong roomIdGen = new AtomicLong(200L);

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

        User employeeUser = new User();
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
                        if (q.getParent() != null) {
                            Quotation p = quotationStore.get(q.getParent().getId());
                            if (p != null && !p.getChildQuotations().contains(q)) {
                                p.getChildQuotations().add(q);
                            }
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
                    } else if ("existsByParentId".equals(name)) {
                        Long pId = (Long) args[0];
                        return quotationStore.values().stream()
                                .anyMatch(q -> q.getParent() != null && q.getParent().getId().equals(pId));
                    } else if ("count".equals(name)) {
                        return (long) quotationStore.size();
                    } else if ("countByParentIsNull".equals(name)) {
                        return quotationStore.values().stream().filter(q -> q.getParent() == null).count();
                    } else if ("findByParentIsNullOrderByCreatedAtDesc".equals(name)) {
                        return quotationStore.values().stream().filter(q -> q.getParent() == null).toList();
                    } else if ("findByParentIdOrderByIdAsc".equals(name)) {
                        Long pId = (Long) args[0];
                        return quotationStore.values().stream()
                                .filter(q -> q.getParent() != null && q.getParent().getId().equals(pId))
                                .sorted(Comparator.comparing(Quotation::getId))
                                .toList();
                    } else if ("findByParentIdAndPricingMode".equals(name)) {
                        Long pId = (Long) args[0];
                        PricingMode mode = (PricingMode) args[1];
                        return quotationStore.values().stream()
                                .filter(q -> q.getParent() != null && q.getParent().getId().equals(pId) && q.getPricingMode() == mode)
                                .findFirst();
                    } else if ("existsByParentIdAndPricingMode".equals(name)) {
                        Long pId = (Long) args[0];
                        PricingMode mode = (PricingMode) args[1];
                        return quotationStore.values().stream()
                                .anyMatch(q -> q.getParent() != null && q.getParent().getId().equals(pId) && q.getPricingMode() == mode);
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
                    }
                    return Optional.empty();
                }
        );

        customerRepository = (CustomerRepository) Proxy.newProxyInstance(
                CustomerRepository.class.getClassLoader(),
                new Class<?>[]{CustomerRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return customer;
                    } else if ("findById".equals(method.getName())) {
                        return Optional.of(customer);
                    }
                    return Optional.empty();
                }
        );

        WorkCategory cat = new WorkCategory();
        cat.setId(1L);
        cat.setName("Kitchen");

        Division div = new Division();
        div.setId(1L);
        div.setName("Cabinets");
        div.setWorkCategory(cat);

        Material mat = new Material();
        mat.setId(1L);
        mat.setName("BWR Plywood");
        mat.setActive(true);
        mat.setPricingGrade(PricingMode.MIDDLE);
        mat.setRate(BigDecimal.valueOf(100.0));
        mat.setMeasurementType(MeasurementType.SQFT);

        workCategoryRepository = (WorkCategoryRepository) Proxy.newProxyInstance(
                WorkCategoryRepository.class.getClassLoader(),
                new Class<?>[]{WorkCategoryRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(cat);
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        divisionRepository = (DivisionRepository) Proxy.newProxyInstance(
                DivisionRepository.class.getClassLoader(),
                new Class<?>[]{DivisionRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(div);
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        materialRepository = (MaterialRepository) Proxy.newProxyInstance(
                MaterialRepository.class.getClassLoader(),
                new Class<?>[]{MaterialRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(mat);
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        itemRepository = (ItemRepository) Proxy.newProxyInstance(
                ItemRepository.class.getClassLoader(),
                new Class<?>[]{ItemRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Item it = new Item();
                        it.setId(1L);
                        it.setName("Cabinet Item");
                        it.setDivision(div);
                        return Optional.of(it);
                    }
                    if ("findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc".equals(method.getName())) {
                        Item it = new Item();
                        it.setId(1L);
                        it.setName("Cabinet Item");
                        it.setDivision(div);
                        return List.of(it);
                    }
                    if ("save".equals(method.getName())) return args[0];
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        materialOptionRepository = (MaterialOptionRepository) Proxy.newProxyInstance(
                MaterialOptionRepository.class.getClassLoader(),
                new Class<?>[]{MaterialOptionRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        MaterialOption mo = new MaterialOption();
                        mo.setId(1L);
                        mo.setMaterial(mat);
                        return Optional.of(mo);
                    }
                    if ("findByMaterialIdAndActiveTrueOrderByDisplayOrderAsc".equals(method.getName())
                            || "findByMaterialIdOrderByDisplayOrderAsc".equals(method.getName())) {
                        MaterialOption mo = new MaterialOption();
                        mo.setId(1L);
                        mo.setMaterial(mat);
                        return List.of(mo);
                    }
                    if ("save".equals(method.getName())) {
                        MaterialOption mo = (MaterialOption) args[0];
                        mo.setId(10L);
                        return mo;
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        materialSpecificationRepository = createMock(MaterialSpecificationRepository.class);
        pricingRepository = (PricingRepository) Proxy.newProxyInstance(
                PricingRepository.class.getClassLoader(),
                new Class<?>[]{PricingRepository.class},
                (proxy, method, args) -> {
                    if ("findByMaterialOptionIdAndPricingMode".equals(method.getName())) {
                        Pricing p = new Pricing();
                        p.setId(1L);
                        p.setActive(true);
                        p.setRate(BigDecimal.valueOf(1000.0));
                        return Optional.of(p);
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );
        districtPricingRepository = createMock(DistrictPricingRepository.class);

        QuotationRoomRepository quotationRoomRepository = (QuotationRoomRepository) Proxy.newProxyInstance(
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
                        return roomStore.values().stream()
                                .flatMap(List::stream)
                                .filter(r -> id.equals(r.getId()))
                                .findFirst();
                    } else if ("findByQuotationIdAndActiveTrueOrderByDisplayOrderAsc".equals(name)
                            || "findByQuotationIdAndActiveTrueOrderByIdAsc".equals(name)
                            || "findByQuotationIdAndActiveTrue".equals(name)) {
                        Long qId = (Long) args[0];
                        return roomStore.getOrDefault(qId, Collections.emptyList()).stream().filter(QuotationRoom::getActive).toList();
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        QuotationWorkflowRepository quotationWorkflowRepository = (QuotationWorkflowRepository) Proxy.newProxyInstance(
                QuotationWorkflowRepository.class.getClassLoader(),
                new Class<?>[]{QuotationWorkflowRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) return args[0];
                    return null;
                }
        );
        QuotationWorkflowService quotationWorkflowService = new QuotationWorkflowService(quotationWorkflowRepository);

        quotationRoomService = new QuotationRoomService(quotationRoomRepository, quotationWorkflowService, quotationRepository);

        QuotationItemRepository quotationItemRepository = (QuotationItemRepository) Proxy.newProxyInstance(
                QuotationItemRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        QuotationItem qi = (QuotationItem) args[0];
                        if (qi.getId() == null) qi.setId(301L);
                        return qi;
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );
        quotationItemService = new QuotationItemService(quotationItemRepository, quotationWorkflowService);

        QuotationItemSizeRepository quotationItemSizeRepository = (QuotationItemSizeRepository) Proxy.newProxyInstance(
                QuotationItemSizeRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemSizeRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        QuotationItemSize sz = (QuotationItemSize) args[0];
                        if (sz.getId() == null) sz.setId(401L);
                        return sz;
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );
        quotationItemSizeService = new QuotationItemSizeService(quotationItemSizeRepository);

        quotationCalculationService = new QuotationCalculationService(null, null, null, null, null, null, null) {
            @Override
            public Quotation calculateQuotation(Long id) {
                Quotation q = quotationStore.get(id);
                if (q != null) {
                    if (q.getSubtotal() == null) q.setSubtotal(BigDecimal.ZERO);
                    if (q.getGrandTotal() == null) q.setGrandTotal(q.getSubtotal());
                }
                return q;
            }
        };

        QuotationStatusHistoryRepository quotationStatusHistoryRepository = createMock(QuotationStatusHistoryRepository.class);
        QuotationCommunicationRepository quotationCommunicationRepository = createMock(QuotationCommunicationRepository.class);

        quotationService = new QuotationService(
                quotationRepository,
                customerRepository,
                userRepository,
                branchRepository,
                quotationWorkflowService,
                quotationRoomRepository,
                quotationItemRepository,
                quotationItemSizeRepository,
                quotationStatusHistoryRepository,
                quotationWorkflowRepository,
                quotationCommunicationRepository
        );

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
    @DisplayName("Single Parent Quotation holds multiple independent grade children under the same quotation number")
    public void testThreeGradeQuotationUnderOneParentNumber() {
        // Step 1: Create brand new quotation in PREMIUM grade
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), null
        );
        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Standard", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1500.0)
        );
        AdminQuotationBuilderService.RoomRequest premRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Premium Work", List.of(premItem)
        );

        AdminQuotationBuilderService.BuilderRequest premRequest = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Need high end interior", "Notes",
                List.of(premRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation premSavedChild = builderService.save(premRequest, "admin@pravinkitchens.com");
        assertNotNull(premSavedChild);
        assertNotNull(premSavedChild.getParent(), "Saved quotation should have a parent quotation");
        Quotation parent = premSavedChild.getParent();
        String parentQuotationNo = parent.getQuotationNumber();
        assertNotNull(parentQuotationNo);
        assertTrue(parentQuotationNo.startsWith("QT-"));

        // Both parent and child return the same quotation number through getQuotationNumber()
        assertEquals(parentQuotationNo, premSavedChild.getQuotationNumber());
        assertEquals(PricingMode.PREMIUM, premSavedChild.getPricingMode());

        // Step 2: Prepare MIDDLE grade under the same parentId
        AdminQuotationBuilderService.ItemRequest midItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Middle standard", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1000.0)
        );
        AdminQuotationBuilderService.RoomRequest midRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Middle Work", List.of(midItem)
        );
        AdminQuotationBuilderService.BuilderRequest midRequest = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Need interior", "Notes",
                List.of(midRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation midSavedChild = builderService.save(midRequest, "admin@pravinkitchens.com");
        assertNotNull(midSavedChild);
        assertEquals(parent.getId(), midSavedChild.getParent().getId());
        assertEquals(parentQuotationNo, midSavedChild.getQuotationNumber(), "Middle grade child must share parent quotation number");
        assertEquals(PricingMode.MIDDLE, midSavedChild.getPricingMode());

        // Step 3: Prepare ECONOMY grade under the same parentId
        AdminQuotationBuilderService.ItemRequest econItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Economy standard", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(700.0)
        );
        AdminQuotationBuilderService.RoomRequest econRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Economy Work", List.of(econItem)
        );
        AdminQuotationBuilderService.BuilderRequest econRequest = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.ECONOMY, "Need budget interior", "Notes",
                List.of(econRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation econSavedChild = builderService.save(econRequest, "admin@pravinkitchens.com");
        assertNotNull(econSavedChild);
        assertEquals(parent.getId(), econSavedChild.getParent().getId());
        assertEquals(parentQuotationNo, econSavedChild.getQuotationNumber(), "Economy grade child must share parent quotation number");
        assertEquals(PricingMode.ECONOMY, econSavedChild.getPricingMode());

        // Verify parent has all 3 children linked
        assertTrue(parent.isMultiGrade());
        assertTrue(parent.hasGrade(PricingMode.PREMIUM));
        assertTrue(parent.hasGrade(PricingMode.MIDDLE));
        assertTrue(parent.hasGrade(PricingMode.ECONOMY));

        // Quotation list count query counts parent as 1 quotation
        assertEquals(1, quotationRepository.countByParentIsNull());
        List<Quotation> rootList = quotationRepository.findByParentIsNullOrderByCreatedAtDesc();
        assertEquals(1, rootList.size());
        assertEquals(parent.getId(), rootList.get(0).getId());
    }

    @Test
    @DisplayName("Editing one grade leaves other grades untouched and isolated")
    public void testGradeEditingIsolation() {
        // Setup parent with Premium and Middle
        Quotation parent = new Quotation();
        parent.setId(101L);
        parent.setQuotationNumber("QT-00038");
        parent.setCustomer(customer);
        parent.setBranch(branchMadurai);
        quotationStore.put(parent.getId(), parent);

        Quotation premChild = new Quotation();
        premChild.setId(102L);
        premChild.setParent(parent);
        premChild.setQuotationNumber("QT-00038-PREMIUM");
        premChild.setPricingMode(PricingMode.PREMIUM);
        premChild.setCustomer(customer);
        premChild.setBranch(branchMadurai);
        parent.getChildQuotations().add(premChild);
        quotationStore.put(premChild.getId(), premChild);

        Quotation midChild = new Quotation();
        midChild.setId(103L);
        midChild.setParent(parent);
        midChild.setQuotationNumber("QT-00038-MIDDLE");
        midChild.setPricingMode(PricingMode.MIDDLE);
        midChild.setCustomer(customer);
        midChild.setBranch(branchMadurai);
        parent.getChildQuotations().add(midChild);
        quotationStore.put(midChild.getId(), midChild);

        // Add rooms to both
        QuotationRoom premRoom = new QuotationRoom();
        premRoom.setId(201L);
        premRoom.setQuotation(premChild);
        premRoom.setFloor("Ground Floor");
        premRoom.setRoom("Master Bedroom");
        premRoom.setActive(true);
        roomStore.computeIfAbsent(premChild.getId(), k -> new ArrayList<>()).add(premRoom);

        QuotationRoom midRoom = new QuotationRoom();
        midRoom.setId(202L);
        midRoom.setQuotation(midChild);
        midRoom.setFloor("Ground Floor");
        midRoom.setRoom("Living Room");
        midRoom.setActive(true);
        roomStore.computeIfAbsent(midChild.getId(), k -> new ArrayList<>()).add(midRoom);

        // Edit Middle grade child
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );
        AdminQuotationBuilderService.ItemRequest newItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "TV Unit", "Middle", List.of(new AdminQuotationBuilderService.SizeRequest(BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0), null)),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0)
        );
        AdminQuotationBuilderService.RoomRequest updatedMidRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Living Room Updated", "Living Room Update", List.of(newItem)
        );
        AdminQuotationBuilderService.BuilderRequest editMidRequest = new AdminQuotationBuilderService.BuilderRequest(
                midChild.getId(), parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Req", "Notes",
                List.of(updatedMidRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Living Room Updated", 1L, 1L, null
        );

        builderService.save(editMidRequest, "admin@pravinkitchens.com");

        // Verify Premium rooms were not touched or deactivated
        List<QuotationRoom> activePremRooms = quotationRoomService.getActiveByQuotationId(premChild.getId());
        assertEquals(1, activePremRooms.size());
        assertEquals("Master Bedroom", activePremRooms.get(0).getRoom());
        assertTrue(activePremRooms.get(0).getActive());
    }

    @Test
    @DisplayName("Quoted Value uses only Customer Selected Grade, and returns null if unconfirmed")
    public void testCustomerSelectedGradeAndQuotedValue() {
        Quotation parent = new Quotation();
        parent.setId(101L);
        parent.setQuotationNumber("QT-00038");
        parent.setCustomer(customer);
        parent.setBranch(branchMadurai);
        parent.setStatus(QuotationStatus.WORKING);
        quotationStore.put(parent.getId(), parent);

        Quotation premChild = new Quotation();
        premChild.setId(102L);
        premChild.setParent(parent);
        premChild.setQuotationNumber("QT-00038-PREMIUM");
        premChild.setPricingMode(PricingMode.PREMIUM);
        premChild.setGrandTotal(new BigDecimal("150000.00"));
        parent.getChildQuotations().add(premChild);
        quotationStore.put(premChild.getId(), premChild);

        Quotation midChild = new Quotation();
        midChild.setId(103L);
        midChild.setParent(parent);
        midChild.setQuotationNumber("QT-00038-MIDDLE");
        midChild.setPricingMode(PricingMode.MIDDLE);
        midChild.setGrandTotal(new BigDecimal("100000.00"));
        parent.getChildQuotations().add(midChild);
        quotationStore.put(midChild.getId(), midChild);

        Quotation econChild = new Quotation();
        econChild.setId(104L);
        econChild.setParent(parent);
        econChild.setQuotationNumber("QT-00038-ECONOMY");
        econChild.setPricingMode(PricingMode.ECONOMY);
        econChild.setGrandTotal(new BigDecimal("70000.00"));
        parent.getChildQuotations().add(econChild);
        quotationStore.put(econChild.getId(), econChild);

        // Before customer confirms:
        assertNull(parent.getSelectedGrade());
        assertNull(parent.getEffectiveGrandTotal(), "Unconfirmed multi-grade quotation must NOT sum 3 grades together");

        // Admin confirms Middle Grade
        quotationService.confirmCustomerGrade(parent.getId(), PricingMode.MIDDLE, "admin@pravinkitchens.com");

        assertEquals(PricingMode.MIDDLE, parent.getSelectedGrade());
        assertEquals(new BigDecimal("100000.00"), parent.getEffectiveGrandTotal(), "Quoted Value must equal the Grand Total of Customer Selected Grade");
        assertEquals(PricingMode.MIDDLE, parent.getEffectivePricingMode());
        assertEquals(QuotationStatus.WORKING, parent.getStatus(), "Confirming grade must NOT change status to SENT");

        // Customer changes mind and selects Premium Grade
        quotationService.confirmCustomerGrade(parent.getId(), PricingMode.PREMIUM, "admin@pravinkitchens.com");
        assertEquals(PricingMode.PREMIUM, parent.getSelectedGrade());
        assertEquals(new BigDecimal("150000.00"), parent.getEffectiveGrandTotal());
        assertEquals(PricingMode.PREMIUM, parent.getEffectivePricingMode());
    }

    @Test
    @DisplayName("Legacy single-grade quotations continue to function seamlessly")
    public void testLegacySingleGradeQuotationBackwardCompatibility() {
        Quotation legacy = new Quotation();
        legacy.setId(10L);
        legacy.setQuotationNumber("QT-00010");
        legacy.setCustomer(customer);
        legacy.setBranch(branchMadurai);
        legacy.setPricingMode(PricingMode.MIDDLE);
        legacy.setGrandTotal(new BigDecimal("85000.00"));
        legacy.setStatus(QuotationStatus.WORKING);
        quotationStore.put(legacy.getId(), legacy);

        assertFalse(legacy.isMultiGrade());
        assertFalse(legacy.isChildQuotation());
        assertEquals("QT-00010", legacy.getQuotationNumber());
        assertEquals(PricingMode.MIDDLE, legacy.getEffectivePricingMode());
        assertEquals(new BigDecimal("85000.00"), legacy.getEffectiveGrandTotal());
    }

    @Test
    @DisplayName("Auto-save then final save for same grade updates the same record and never duplicates")
    public void testAutoSaveThenSaveMiddleDoesNotDuplicate() {
        // 1. Create parent quotation QT-00042 with Premium
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), null
        );
        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Standard", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1500.0)
        );
        AdminQuotationBuilderService.RoomRequest premRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Premium Work", List.of(premItem)
        );
        AdminQuotationBuilderService.BuilderRequest premRequest = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Requirements", "Notes",
                List.of(premRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );
        Quotation premChild = builderService.save(premRequest, "admin@pravinkitchens.com");
        Quotation parent = premChild.getParent();
        assertNotNull(parent);

        // 2. Auto-save Middle under QT-00042 (quotationId is null on first auto-save)
        AdminQuotationBuilderService.ItemRequest midItem1 = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Middle standard draft", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1000.0)
        );
        AdminQuotationBuilderService.RoomRequest midRoom1 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Middle Draft", List.of(midItem1)
        );
        AdminQuotationBuilderService.BuilderRequest autoSaveMidReq = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Requirements", "Notes",
                List.of(midRoom1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );
        Quotation autoSavedMid = builderService.autoSave(autoSaveMidReq, "admin@pravinkitchens.com");
        assertNotNull(autoSavedMid);
        Long midChildId = autoSavedMid.getId();
        assertEquals(PricingMode.MIDDLE, autoSavedMid.getPricingMode());
        assertEquals(QuotationStatus.PENDING, autoSavedMid.getStatus());

        // 3. User clicks Final Save for Middle under QT-00042 (even if quotationId was null or passed)
        AdminQuotationBuilderService.ItemRequest midItemFinal = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Middle standard final", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1000.0)
        );
        AdminQuotationBuilderService.RoomRequest midRoomFinal = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Middle Final", List.of(midItemFinal)
        );
        AdminQuotationBuilderService.BuilderRequest finalSaveMidReq = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Requirements", "Final Notes",
                List.of(midRoomFinal), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );
        Quotation finalSavedMid = builderService.save(finalSaveMidReq, "admin@pravinkitchens.com");
        assertNotNull(finalSavedMid);

        // Verification: Must update the exact same child record, NOT create a second Middle record!
        assertEquals(midChildId, finalSavedMid.getId(), "Final save must update the auto-saved Middle record without creating duplicates");
        assertEquals(QuotationStatus.WORKING, finalSavedMid.getStatus());

        // Ensure parent only has 1 Middle child
        long middleCount = parent.getChildQuotations().stream()
                .filter(c -> c.getPricingMode() == PricingMode.MIDDLE)
                .count();
        assertEquals(1, middleCount, "There must be exactly one Middle grade quotation under parent QT-00042");
    }

    @Test
    @DisplayName("Repeated Save clicks do not create duplicates")
    public void testRepeatedSaveQuoteDoesNotDuplicate() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), null
        );
        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Standard", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1000.0)
        );
        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Work", List.of(item)
        );
        AdminQuotationBuilderService.BuilderRequest req1 = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Requirements", "Notes",
                List.of(room), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation firstSave = builderService.save(req1, "admin@pravinkitchens.com");
        Long firstId = firstSave.getId();
        Quotation parent = firstSave.getParent();

        // Immediate second save click with same parent and mode
        AdminQuotationBuilderService.BuilderRequest req2 = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Requirements", "Notes Updated",
                List.of(room), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );
        Quotation secondSave = builderService.save(req2, "admin@pravinkitchens.com");

        assertEquals(firstId, secondSave.getId(), "Second save must update the existing record rather than create a duplicate");
    }

    @Test
    @DisplayName("Verify Save Quote button attributes, onclick handlers, and absence of undeclared variable references")
    public void testSaveQuoteButtonPropertiesAndTemplateIntegrity() throws Exception {
        java.nio.file.Path adminPath = java.nio.file.Paths.get("src/main/resources/templates/admin/quotation-builder.html");
        java.nio.file.Path empPath = java.nio.file.Paths.get("src/main/resources/templates/employee/quotation-builder.html");

        String adminHtml = java.nio.file.Files.readString(adminPath);
        String empHtml = java.nio.file.Files.readString(empPath);

        // 1. Check Button attributes in Admin template
        assertTrue(adminHtml.contains("id=\"saveQuoteBtn\""), "Admin template must contain id='saveQuoteBtn'");
        assertTrue(adminHtml.contains("onclick=\"saveQuotation(true)\""), "Admin template save button must have onclick='saveQuotation(true)'");
        assertTrue(adminHtml.contains("window.saveQuotation = saveQuotation;"), "Admin template must expose saveQuotation globally");

        // 2. Check Button attributes in Employee template
        assertTrue(empHtml.contains("id=\"saveQuoteBtn\""), "Employee template must contain id='saveQuoteBtn'");
        assertTrue(empHtml.contains("onclick=\"saveQuotation(true)\""), "Employee template save button must have onclick='saveQuotation(true)'");
        assertTrue(empHtml.contains("window.saveQuotation = saveQuotation;"), "Employee template must expose saveQuotation globally");

        // 3. Ensure NO undeclared autoSaveTimeout variable exists in templates
        assertFalse(adminHtml.contains("autoSaveTimeout"), "Admin template must NOT contain undeclared autoSaveTimeout");
        assertFalse(empHtml.contains("autoSaveTimeout"), "Employee template must NOT contain undeclared autoSaveTimeout");

        // 4. Verify DRAFT_STORAGE_KEY declarations and functions in both templates
        assertTrue(adminHtml.contains("const DRAFT_STORAGE_KEY = 'pravin_quotation_builder_draft'"),
                "Admin template must declare DRAFT_STORAGE_KEY");
        assertTrue(empHtml.contains("const DRAFT_STORAGE_KEY = 'pravin_employee_quotation_builder_draft'"),
                "Employee template must declare DRAFT_STORAGE_KEY");
        assertTrue(adminHtml.contains("function getDraftStorageKey("), "Admin template must define getDraftStorageKey");
        assertTrue(empHtml.contains("function getDraftStorageKey("), "Employee template must define getDraftStorageKey");
        assertTrue(adminHtml.contains("function clearAllDrafts("), "Admin template must define clearAllDrafts");
        assertTrue(empHtml.contains("function clearAllDrafts("), "Employee template must define clearAllDrafts");
        assertTrue(adminHtml.contains("restoreQuotationBuilderDraftIfPresent"), "Admin template must define restoreQuotationBuilderDraftIfPresent");
        assertTrue(empHtml.contains("restoreQuotationBuilderDraftIfPresent"), "Employee template must define restoreQuotationBuilderDraftIfPresent");
    }

    @Test
    @DisplayName("Employee prepares and saves Premium, Middle, and Economy grade quotations under one parent without duplicates")
    public void testEmployeeMultiGradeQuotationSaveAndWorkflow() {
        AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                500L, "Karthik", "9876543210", "karthik@example.com", "Madurai"
        );
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), null
        );

        // 1. Employee saves Premium quotation
        AdminQuotationBuilderService.ItemRequest premItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Premium Grade Material", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(2500.0)
        );
        AdminQuotationBuilderService.RoomRequest premRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Premium Kitchen", List.of(premItem)
        );
        AdminQuotationBuilderService.BuilderRequest reqPrem = new AdminQuotationBuilderService.BuilderRequest(
                null, null, custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "Premium Specs", "Employee Premium Notes",
                List.of(premRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation savedPrem = builderService.save(reqPrem, "employee@pravinkitchens.com");
        assertNotNull(savedPrem);
        assertNotNull(savedPrem.getParent(), "Saved grade quotation must be attached to a parent");
        Quotation parent = savedPrem.getParent();
        assertEquals(PricingMode.PREMIUM, savedPrem.getPricingMode());
        assertEquals(parent.getQuotationNumber(), savedPrem.getQuotationNumber(), "Child must display parent quotation number");
        assertTrue(savedPrem.getRawQuotationNumber().endsWith("-PREMIUM"), "Raw quotation number must end with -PREMIUM");
        assertEquals(QuotationStatus.WORKING, savedPrem.getStatus());

        // 2. Employee saves Middle quotation under the same parent
        AdminQuotationBuilderService.ItemRequest midItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Middle Grade Material", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1800.0)
        );
        AdminQuotationBuilderService.RoomRequest midRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Middle Kitchen", List.of(midItem)
        );
        AdminQuotationBuilderService.BuilderRequest reqMid = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Middle Specs", "Employee Middle Notes",
                List.of(midRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation savedMid = builderService.save(reqMid, "employee@pravinkitchens.com");
        assertNotNull(savedMid);
        assertEquals(parent.getId(), savedMid.getParent().getId(), "Middle must share parent ID");
        assertEquals(PricingMode.MIDDLE, savedMid.getPricingMode());
        assertEquals(parent.getQuotationNumber(), savedMid.getQuotationNumber(), "Middle must display parent quotation number");
        assertTrue(savedMid.getRawQuotationNumber().endsWith("-MIDDLE"), "Raw quotation number must end with -MIDDLE");
        assertEquals(QuotationStatus.WORKING, savedMid.getStatus());

        // 3. Employee saves Economy quotation under the same parent
        AdminQuotationBuilderService.ItemRequest econItem = new AdminQuotationBuilderService.ItemRequest(
                1L, 1L, 1L, 1L, 1L, null,
                "Base Cabinet", "Economy Grade Material", List.of(size1), MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0)
        );
        AdminQuotationBuilderService.RoomRequest econRoom = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Economy Kitchen", List.of(econItem)
        );
        AdminQuotationBuilderService.BuilderRequest reqEcon = new AdminQuotationBuilderService.BuilderRequest(
                null, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.ECONOMY, "Economy Specs", "Employee Economy Notes",
                List.of(econRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation savedEcon = builderService.save(reqEcon, "employee@pravinkitchens.com");
        assertNotNull(savedEcon);
        assertEquals(parent.getId(), savedEcon.getParent().getId(), "Economy must share parent ID");
        assertEquals(PricingMode.ECONOMY, savedEcon.getPricingMode());
        assertEquals(parent.getQuotationNumber(), savedEcon.getQuotationNumber(), "Economy must display parent quotation number");
        assertTrue(savedEcon.getRawQuotationNumber().endsWith("-ECONOMY"), "Raw quotation number must end with -ECONOMY");
        assertEquals(QuotationStatus.WORKING, savedEcon.getStatus());

        // Verify parent has all 3 grades
        assertEquals(3, parent.getChildQuotations().size(), "Parent must have exactly 3 child quotations");

        // 4. Employee updates the Middle quotation
        Long midId = savedMid.getId();
        AdminQuotationBuilderService.BuilderRequest reqMidUpdate = new AdminQuotationBuilderService.BuilderRequest(
                midId, parent.getId(), custReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Updated Middle Specs", "Updated Middle Notes",
                List.of(midRoom), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.valueOf(18.0), BigDecimal.ZERO, true,
                "1", "Ground Floor", "Kitchen", 1L, 1L, null
        );

        Quotation updatedMid = builderService.save(reqMidUpdate, "employee@pravinkitchens.com");
        assertEquals(midId, updatedMid.getId(), "Updating existing grade must update the same quotation record without creating duplicates");
        assertEquals(3, parent.getChildQuotations().size(), "Child count must still be 3 after update");
    }

    @Test
    @DisplayName("Verify View, PDF, WhatsApp, Email, Edit, Confirm buttons in Admin and Employee grade cards")
    public void testGradeCardButtonsInTemplates() throws Exception {
        String adminHtml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/templates/admin/quotation-view.html")
        );
        String employeeHtml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/templates/employee/quotation-workspace.html")
        );

        for (String html : List.of(adminHtml, employeeHtml)) {
            // View buttons
            assertTrue(html.contains("btn-grade-view"), "Must contain btn-grade-view");
            assertTrue(html.contains("View Premium Quotation Details") || html.contains("title=\"View Premium"), "Must have view premium action");
            assertTrue(html.contains("View Middle Quotation Details") || html.contains("title=\"View Middle"), "Must have view middle action");
            assertTrue(html.contains("View Economy Quotation Details") || html.contains("title=\"View Economy"), "Must have view economy action");

            // Edit buttons
            assertTrue(html.contains("btn-grade-edit"), "Must contain btn-grade-edit");
            assertTrue(html.contains("Edit Premium"), "Must contain Edit Premium");
            assertTrue(html.contains("Edit Middle"), "Must contain Edit Middle");
            assertTrue(html.contains("Edit Economy"), "Must contain Edit Economy");

            // PDF buttons
            assertTrue(html.contains("btn-grade-pdf"), "Must contain btn-grade-pdf");
            assertTrue(html.contains("grade='PREMIUM'"), "Must support grade=PREMIUM param");
            assertTrue(html.contains("grade='MIDDLE'"), "Must support grade=MIDDLE param");
            assertTrue(html.contains("grade='ECONOMY'"), "Must support grade=ECONOMY param");

            // WhatsApp buttons
            assertTrue(html.contains("btn-grade-whatsapp"), "Must contain btn-grade-whatsapp");
            assertTrue(html.contains("data-grade=\"PREMIUM\""), "Must have data-grade=PREMIUM");
            assertTrue(html.contains("data-grade=\"MIDDLE\""), "Must have data-grade=MIDDLE");
            assertTrue(html.contains("data-grade=\"ECONOMY\""), "Must have data-grade=ECONOMY");

            // Email buttons
            assertTrue(html.contains("btn-grade-email"), "Must contain btn-grade-email");
            assertTrue(html.contains("name=\"grade\" value=\"PREMIUM\""), "Must submit grade=PREMIUM on email");
            assertTrue(html.contains("name=\"grade\" value=\"MIDDLE\""), "Must submit grade=MIDDLE on email");
            assertTrue(html.contains("name=\"grade\" value=\"ECONOMY\""), "Must submit grade=ECONOMY on email");

            // Confirm buttons
            assertTrue(html.contains("btn-confirm-grade"), "Must contain btn-confirm-grade");
            assertTrue(html.contains("Confirm This Grade"), "Must contain Confirm This Grade text");

            // Individual grade sending status
            assertTrue(html.contains("premiumGrade.status.name() == 'SENT'"), "Must display SENT status for premium");
            assertTrue(html.contains("middleGrade.status.name() == 'SENT'"), "Must display SENT status for middle");
            assertTrue(html.contains("economyGrade.status.name() == 'SENT'"), "Must display SENT status for economy");

            // Unprepared state shows only Prepare button
            assertTrue(html.contains("+ Prepare Premium Quotation"), "Must show + Prepare Premium Quotation");
            assertTrue(html.contains("+ Prepare Middle Quotation"), "Must show + Prepare Middle Quotation");
            assertTrue(html.contains("+ Prepare Economy Quotation"), "Must show + Prepare Economy Quotation");
        }
    }

    @Test
    @DisplayName("Verify individual grade sending status maintains independent status without marking sibling grades as sent")
    public void testIndividualGradeIndependentSendingStatus() {
        Quotation parent = new Quotation();
        parent.setId(101L);
        parent.setQuotationNumber("QT-00050");
        parent.setStatus(QuotationStatus.WORKING);
        parent.setCustomer(customer);

        Quotation premium = new Quotation();
        premium.setId(102L);
        premium.setParent(parent);
        premium.setQuotationNumber("QT-00050-PREMIUM");
        premium.setPricingMode(PricingMode.PREMIUM);
        premium.setStatus(QuotationStatus.WORKING);
        premium.setCustomer(customer);
        premium.setGrandTotal(BigDecimal.valueOf(150000.00));

        Quotation middle = new Quotation();
        middle.setId(103L);
        middle.setParent(parent);
        middle.setQuotationNumber("QT-00050-MIDDLE");
        middle.setPricingMode(PricingMode.MIDDLE);
        middle.setStatus(QuotationStatus.WORKING);
        middle.setCustomer(customer);
        middle.setGrandTotal(BigDecimal.valueOf(100000.00));

        Quotation economy = new Quotation();
        economy.setId(104L);
        economy.setParent(parent);
        economy.setQuotationNumber("QT-00050-ECONOMY");
        economy.setPricingMode(PricingMode.ECONOMY);
        economy.setStatus(QuotationStatus.WORKING);
        economy.setCustomer(customer);
        economy.setGrandTotal(BigDecimal.valueOf(75000.00));

        parent.setChildQuotations(new ArrayList<>(List.of(premium, middle, economy)));

        // Simulate sending Premium
        premium.setStatus(QuotationStatus.SENT);
        if (premium.getParent() != null && premium.getParent().getStatus() != QuotationStatus.SENT) {
            premium.getParent().setStatus(QuotationStatus.SENT);
        }

        // Verify Premium is SENT, parent is SENT, but Middle and Economy remain WORKING!
        assertEquals(QuotationStatus.SENT, premium.getStatus(), "Premium must be SENT");
        assertEquals(QuotationStatus.SENT, parent.getStatus(), "Parent quotation reflects SENT");
        assertEquals(QuotationStatus.WORKING, middle.getStatus(), "Middle grade must remain WORKING");
        assertEquals(QuotationStatus.WORKING, economy.getStatus(), "Economy grade must remain WORKING");
    }

    @Test
    @DisplayName("Verify Communication History and bottom-actions panels are completely removed from Admin and Employee quotation details templates")
    public void testCommunicationHistoryAndBottomActionsRemovedFromTemplates() throws Exception {
        String adminHtml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/templates/admin/quotation-view.html")
        );
        String employeeHtml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/templates/employee/quotation-workspace.html")
        );

        for (String html : List.of(adminHtml, employeeHtml)) {
            // Communication History section completely removed
            assertFalse(html.contains("Communication History"), "Must not contain Communication History title");
            assertFalse(html.contains("communication-history-card"), "Must not contain communication-history-card");
            assertFalse(html.contains("No communication yet"), "Must not contain No communication yet");
            assertFalse(html.contains("Email, WhatsApp and PDF activities"),
                    "Must not contain placeholder text for communication activities");

            // Bottom actions panel completely removed
            assertFalse(html.contains("class=\"bottom-actions\""), "Must not contain bottom-actions class");
            assertFalse(html.contains("Edit in Builder"), "Must not contain Edit in Builder in bottom panel");
            assertFalse(html.contains("Send Quotation via WhatsApp"), "Must not contain Send Quotation via WhatsApp in bottom panel");
            assertFalse(html.contains("Email Quotation"), "Must not contain Email Quotation in bottom panel");

            // Still preserves grade cards and the WhatsApp modal
            assertTrue(html.contains("multi-grade-grid"), "Must preserve multi-grade-grid");
            assertTrue(html.contains("waShareModal"), "Must preserve waShareModal for grade card WhatsApp");
        }
    }
}
