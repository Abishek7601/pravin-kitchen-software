package com.example.pravin_quotation;

import com.example.pravin_quotation.controller.AdminQuotationBuilderController;
import com.example.pravin_quotation.controller.EmployeeQuotationController;
import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationSaveAndResumeVerificationTest {

    private final Map<Long, Quotation> quotationStore = new HashMap<>();
    private final AtomicLong quotationIdGen = new AtomicLong(100L);
    private final List<QuotationRoom> roomStore = new ArrayList<>();
    private final AtomicLong roomIdGen = new AtomicLong(200L);
    private final List<QuotationItem> itemStore = new ArrayList<>();
    private final AtomicLong itemIdGen = new AtomicLong(300L);
    private final List<QuotationItemSize> sizeStore = new ArrayList<>();
    private final AtomicLong sizeIdGen = new AtomicLong(400L);

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
    private AdminQuotationBuilderService builderService;

    private User adminUser;
    private User employeeUser;
    private Branch branchKk;

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

        branchKk = new Branch();
        branchKk.setId(1L);
        branchKk.setName("Kanniyakumari");
        branchKk.setActive(true);

        adminUser = new User();
        adminUser.setId(10L);
        adminUser.setEmail("admin@pravinkitchens.com");
        adminUser.setName("Super Admin");
        adminUser.setRole(Role.SUPER_ADMIN);
        adminUser.setActive(true);

        employeeUser = new User();
        employeeUser.setId(20L);
        employeeUser.setEmail("employee@pravinkitchens.com");
        employeeUser.setName("John Employee");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branchKk);
        employeeUser.setActive(true);

        WorkCategory cat = new WorkCategory();
        cat.setId(1L);
        cat.setName("Modular Kitchen");
        cat.setActive(true);

        Division div = new Division();
        div.setId(2L);
        div.setName("Base Cabinet");
        div.setWorkCategory(cat);
        div.setActive(true);

        Material mat1 = new Material();
        mat1.setId(4L);
        mat1.setName("Marine Ply Lamination Box Type Premium");
        mat1.setActive(true);
        mat1.setPricingGrade(PricingMode.PREMIUM);
        mat1.setRate(BigDecimal.valueOf(1400.0));
        mat1.setMeasurementType(MeasurementType.SQFT);
        mat1.setDivision(div);

        Material mat2 = new Material();
        mat2.setId(5L);
        mat2.setName("Marine Ply Lamination Partition Type Premium");
        mat2.setActive(true);
        mat2.setPricingGrade(PricingMode.PREMIUM);
        mat2.setRate(BigDecimal.valueOf(1200.0));
        mat2.setMeasurementType(MeasurementType.SQFT);
        mat2.setDivision(div);

        quotationRepository = (QuotationRepository) Proxy.newProxyInstance(
                QuotationRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Quotation q = (Quotation) args[0];
                        if (q.getId() == null) q.setId(quotationIdGen.incrementAndGet());
                        quotationStore.put(q.getId(), q);
                        return q;
                    } else if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(quotationStore.get(id));
                    } else if ("count".equals(name)) {
                        return (long) quotationStore.size();
                    } else if ("existsByQuotationNumber".equals(name)) {
                        return false;
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
                        if (Long.valueOf(1L).equals(id)) return Optional.of(branchKk);
                    }
                    return Optional.empty();
                }
        );

        customerRepository = (CustomerRepository) Proxy.newProxyInstance(
                CustomerRepository.class.getClassLoader(),
                new Class<?>[]{CustomerRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        Customer c = (Customer) args[0];
                        if (c.getId() == null) c.setId(500L);
                        return c;
                    }
                    return null;
                }
        );

        workCategoryRepository = (WorkCategoryRepository) Proxy.newProxyInstance(
                WorkCategoryRepository.class.getClassLoader(),
                new Class<?>[]{WorkCategoryRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(cat);
                    return Optional.empty();
                }
        );

        divisionRepository = (DivisionRepository) Proxy.newProxyInstance(
                DivisionRepository.class.getClassLoader(),
                new Class<?>[]{DivisionRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) return Optional.of(div);
                    return Optional.empty();
                }
        );

        itemRepository = (ItemRepository) Proxy.newProxyInstance(
                ItemRepository.class.getClassLoader(),
                new Class<?>[]{ItemRepository.class},
                (proxy, method, args) -> {
                    if ("findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc".equals(method.getName())) {
                        Item it = new Item();
                        it.setId(3L);
                        it.setName("Base Cabinet");
                        it.setDivision(div);
                        return List.of(it);
                    }
                    if ("save".equals(method.getName())) return args[0];
                    return Optional.empty();
                }
        );

        materialRepository = (MaterialRepository) Proxy.newProxyInstance(
                MaterialRepository.class.getClassLoader(),
                new Class<?>[]{MaterialRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        Long id = (Long) args[0];
                        if (Long.valueOf(4L).equals(id)) return Optional.of(mat1);
                        if (Long.valueOf(5L).equals(id)) return Optional.of(mat2);
                    } else if ("findActiveByDivisionId".equals(method.getName()) || "findActiveByDivisionIdAndPricingGrade".equals(method.getName())) {
                        return List.of(mat1, mat2);
                    }
                    if (method.getReturnType().equals(List.class)) {
                        return Collections.emptyList();
                    }
                    if (method.getReturnType().equals(Optional.class)) {
                        return Optional.empty();
                    }
                    return null;
                }
        );

        materialOptionRepository = (MaterialOptionRepository) Proxy.newProxyInstance(
                MaterialOptionRepository.class.getClassLoader(),
                new Class<?>[]{MaterialOptionRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        MaterialOption mo = (MaterialOption) args[0];
                        if (mo.getId() == null) mo.setId(10L);
                        return mo;
                    }
                    if (method.getReturnType().equals(List.class)) {
                        return Collections.emptyList();
                    }
                    if (method.getReturnType().equals(Optional.class)) {
                        return Optional.empty();
                    }
                    return null;
                }
        );

        materialSpecificationRepository = createMock(MaterialSpecificationRepository.class);
        pricingRepository = createMock(PricingRepository.class);
        districtPricingRepository = createMock(DistrictPricingRepository.class);

        quotationRoomRepository = (QuotationRoomRepository) Proxy.newProxyInstance(
                QuotationRoomRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRoomRepository.class},
                (proxy, method, args) -> {
                    String mName = method.getName();
                    if ("save".equals(mName)) {
                        QuotationRoom r = (QuotationRoom) args[0];
                        if (r.getId() == null) {
                            r.setId(roomIdGen.incrementAndGet());
                            roomStore.add(r);
                        } else {
                            for (int i = 0; i < roomStore.size(); i++) {
                                if (r.getId().equals(roomStore.get(i).getId())) {
                                    roomStore.set(i, r);
                                    break;
                                }
                            }
                        }
                        return r;
                    } else if ("findById".equals(mName)) {
                        Long id = (Long) args[0];
                        return roomStore.stream().filter(r -> id.equals(r.getId())).findFirst();
                    } else if ("findByQuotationIdAndActiveTrueOrderByIdAsc".equals(mName)) {
                        Long qId = (Long) args[0];
                        return roomStore.stream().filter(r -> r.getQuotation() != null && qId.equals(r.getQuotation().getId()) && Boolean.TRUE.equals(r.getActive())).toList();
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    return null;
                }
        );

        quotationItemRepository = (QuotationItemRepository) Proxy.newProxyInstance(
                QuotationItemRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemRepository.class},
                (proxy, method, args) -> {
                    String mName = method.getName();
                    if ("save".equals(mName)) {
                        QuotationItem qi = (QuotationItem) args[0];
                        if (qi.getId() == null) {
                            qi.setId(itemIdGen.incrementAndGet());
                            itemStore.add(qi);
                        } else {
                            for (int i = 0; i < itemStore.size(); i++) {
                                if (qi.getId().equals(itemStore.get(i).getId())) {
                                    itemStore.set(i, qi);
                                    break;
                                }
                            }
                        }
                        return qi;
                    } else if ("findById".equals(mName)) {
                        Long id = (Long) args[0];
                        return itemStore.stream().filter(it -> id.equals(it.getId())).findFirst();
                    } else if ("findByQuotationRoomIdAndActiveTrueOrderByIdAsc".equals(mName)) {
                        Long rId = (Long) args[0];
                        return itemStore.stream().filter(it -> it.getQuotationRoom() != null && rId.equals(it.getQuotationRoom().getId()) && Boolean.TRUE.equals(it.getActive())).toList();
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    return null;
                }
        );

        quotationItemSizeRepository = (QuotationItemSizeRepository) Proxy.newProxyInstance(
                QuotationItemSizeRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemSizeRepository.class},
                (proxy, method, args) -> {
                    String mName = method.getName();
                    if ("save".equals(mName)) {
                        QuotationItemSize sz = (QuotationItemSize) args[0];
                        if (sz.getId() == null) {
                            sz.setId(sizeIdGen.incrementAndGet());
                            sizeStore.add(sz);
                        } else {
                            for (int i = 0; i < sizeStore.size(); i++) {
                                if (sz.getId().equals(sizeStore.get(i).getId())) {
                                    sizeStore.set(i, sz);
                                    break;
                                }
                            }
                        }
                        return sz;
                    } else if ("findById".equals(mName)) {
                        Long id = (Long) args[0];
                        return sizeStore.stream().filter(s -> id.equals(s.getId())).findFirst();
                    } else if ("findByQuotationItemIdAndActiveTrueOrderByDisplayOrderAsc".equals(mName)) {
                        Long itId = (Long) args[0];
                        return sizeStore.stream().filter(s -> s.getQuotationItem() != null && itId.equals(s.getQuotationItem().getId()) && Boolean.TRUE.equals(s.getActive())).toList();
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
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

        quotationCalculationService = new QuotationCalculationService(quotationRepository, quotationRoomRepository, quotationItemRepository, null, null, null, null) {
            @Override
            public Quotation calculateQuotation(Long id) {
                Quotation q = quotationStore.get(id);
                if (q != null) {
                    if (q.getSubtotal() == null) q.setSubtotal(BigDecimal.ZERO);
                    if (q.getGrandTotal() == null) q.setGrandTotal(q.getSubtotal());
                }
                return q;
            }

            @Override
            public Map<PricingMode, BigDecimal> calculateGradeSubtotals(Long quotationId) {
                Map<PricingMode, BigDecimal> map = new HashMap<>();
                map.put(PricingMode.PREMIUM, BigDecimal.ZERO);
                map.put(PricingMode.MIDDLE, BigDecimal.ZERO);
                map.put(PricingMode.ECONOMY, BigDecimal.ZERO);
                return map;
            }
        };

        quotationRoomService = new QuotationRoomService(quotationRoomRepository, quotationWorkflowService, quotationRepository);
        quotationItemService = new QuotationItemService(quotationItemRepository, quotationWorkflowService);
        quotationItemSizeService = new QuotationItemSizeService(quotationItemSizeRepository);

        builderService = new AdminQuotationBuilderService(
                quotationRepository, customerRepository, userRepository, branchRepository,
                workCategoryRepository, divisionRepository, itemRepository, materialRepository,
                materialOptionRepository, materialSpecificationRepository, pricingRepository,
                districtPricingRepository, quotationRoomService, quotationItemService,
                quotationItemSizeService, quotationCalculationService
        );
    }

    @Test
    @DisplayName("1. Auto-save preserves multiple materials and subsequent auto-save keeps all materials active")
    public void testAutoSaveMultipleMaterialsPreservesAll() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Pravin Customer", "9876543210", "cust@example.com", "Kanniyakumari"
        );

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(44.0), BigDecimal.valueOf(22.0), BigDecimal.valueOf(12.0), BigDecimal.valueOf(7700.0)
        );
        AdminQuotationBuilderService.ItemRequest item1 = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 4L, "Marine Ply Lamination Box Type Premium", null, null, "Box Item", "Hai Hello Bye", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1400.0), null,
                PricingMode.PREMIUM, 4L, PricingMode.PREMIUM, "Marine Ply Lamination Box Type Premium", "Hai Hello Bye", 0
        );

        AdminQuotationBuilderService.SizeRequest size2 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(34.0), BigDecimal.valueOf(22.0), BigDecimal.valueOf(12.0), BigDecimal.valueOf(5604.0)
        );
        AdminQuotationBuilderService.ItemRequest item2 = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 5L, "Marine Ply Lamination Partition Type Premium", null, null, "Partition Item", "16 Mm Thickness", List.of(size2),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0), null,
                PricingMode.PREMIUM, 5L, PricingMode.PREMIUM, "Marine Ply Lamination Partition Type Premium", "16 Mm Thickness", 1
        );

        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen Work", List.of(item1, item2)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "", "", List.of(room),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );

        Quotation saved = builderService.autoSave(request, "admin@pravinkitchens.com");
        assertNotNull(saved);
        Long qId = saved.getId();

        List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(qId);
        assertEquals(1, rooms.size());
        List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(rooms.get(0).getId());
        assertEquals(2, items.size(), "Both Material 1 and Material 2 must be saved and active");

        // Subsequent auto-save updating state must maintain both items
        AdminQuotationBuilderService.BuilderRequest request2 = new AdminQuotationBuilderService.BuilderRequest(
                qId, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "", "Updated note", List.of(room),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );
        Quotation saved2 = builderService.autoSave(request2, "admin@pravinkitchens.com");
        assertEquals(qId, saved2.getId());

        List<QuotationRoom> rooms2 = quotationRoomService.getActiveByQuotationId(qId);
        assertEquals(1, rooms2.size());
        List<QuotationItem> items2 = quotationItemService.getActiveByQuotationRoomId(rooms2.get(0).getId());
        assertEquals(2, items2.size(), "Both materials must remain active after subsequent auto-save");
        assertTrue(items2.get(0).getMaterial().getName().equalsIgnoreCase("Marine Ply Lamination Box Type Premium"));
        assertTrue(items2.get(1).getMaterial().getName().equalsIgnoreCase("Marine Ply Lamination Partition Type Premium"));
    }

    @Test
    @DisplayName("2. Rapid consecutive saves (Material 1 alone, then Material 1 + Material 2) preserves all materials")
    public void testRapidConsecutiveSavesPreserveAllMaterials() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Rapid Customer", "9876543211", "rapid@example.com", "Kanniyakumari"
        );

        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(44.0), BigDecimal.valueOf(22.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest item1 = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 4L, "Marine Ply Lamination Box Type Premium", null, null, "Box Item", "Hai Hello Bye", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1400.0), null,
                PricingMode.PREMIUM, 4L, PricingMode.PREMIUM, "Marine Ply Lamination Box Type Premium", "Hai Hello Bye", 0
        );

        // First save with 1 item
        AdminQuotationBuilderService.RoomRequest roomStep1 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Work", List.of(item1)
        );
        AdminQuotationBuilderService.BuilderRequest req1 = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "", "", List.of(roomStep1),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );
        Quotation q = builderService.autoSave(req1, "admin@pravinkitchens.com");
        Long qId = q.getId();

        // Rapid second save with 2 items
        AdminQuotationBuilderService.SizeRequest size2 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(34.0), BigDecimal.valueOf(22.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest item2 = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 5L, "Marine Ply Lamination Partition Type Premium", null, null, "Partition Item", "16 Mm", List.of(size2),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0), null,
                PricingMode.PREMIUM, 5L, PricingMode.PREMIUM, "Marine Ply Lamination Partition Type Premium", "16 Mm", 1
        );
        AdminQuotationBuilderService.RoomRequest roomStep2 = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Work", List.of(item1, item2)
        );
        AdminQuotationBuilderService.BuilderRequest req2 = new AdminQuotationBuilderService.BuilderRequest(
                qId, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "", "", List.of(roomStep2),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );
        Quotation q2 = builderService.autoSave(req2, "admin@pravinkitchens.com");
        assertEquals(qId, q2.getId());

        List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(qId);
        assertEquals(1, rooms.size());
        List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(rooms.get(0).getId());
        assertEquals(2, items.size(), "Both materials must be present in database after rapid successive save");
    }

    @Test
    @DisplayName("3. Auto-save preserves unavailable material using originalMaterialId")
    public void testAutoSavePreservesUnavailableMaterial() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Unavailable Test", "9876543212", "unavail@example.com", "Kanniyakumari"
        );

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(34.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, null, "Material Unavailable (Economy Grade)", null, null, "Unavailable Item", "Spec", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1400.0), null,
                PricingMode.ECONOMY, 4L, PricingMode.PREMIUM, "Marine Ply Lamination Box Type Premium", "Spec", 0
        );
        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Work", List.of(item)
        );
        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.ECONOMY, "", "", List.of(room),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );

        Quotation q = builderService.autoSave(req, "admin@pravinkitchens.com");
        assertNotNull(q);

        List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(q.getId());
        assertEquals(1, rooms.size());
        List<QuotationItem> items = quotationItemService.getActiveByQuotationRoomId(rooms.get(0).getId());
        assertEquals(1, items.size(), "Item with null materialId must be preserved using originalMaterialId");
        assertEquals(4L, items.get(0).getMaterial().getId());
    }

    @Test
    @DisplayName("4. Admin controller loadQuotation returns all saved materials and consolidates rooms")
    public void testAdminControllerLoadQuotationRestoresAllMaterials() {
        AdminQuotationBuilderController controller = new AdminQuotationBuilderController(
                branchRepository, customerRepository, workCategoryRepository, divisionRepository,
                itemRepository, materialRepository, materialOptionRepository, materialSpecificationRepository,
                builderService, quotationRepository, quotationRoomRepository, quotationItemRepository,
                quotationItemSizeRepository, pricingRepository, null, quotationCalculationService
        );

        // Pre-save quotation with 2 materials
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Load Test Cust", "9876543213", "load@example.com", "Kanniyakumari"
        );
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(44.0), BigDecimal.valueOf(22.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest item1 = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 4L, "Marine Ply Lamination Box Type Premium", null, null, "Box Item", "Spec 1", List.of(size1),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1400.0), null,
                PricingMode.PREMIUM, 4L, PricingMode.PREMIUM, "Marine Ply Lamination Box Type Premium", "Spec 1", 0
        );
        AdminQuotationBuilderService.SizeRequest size2 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(34.0), BigDecimal.valueOf(22.0), BigDecimal.valueOf(12.0), null
        );
        AdminQuotationBuilderService.ItemRequest item2 = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 5L, "Marine Ply Lamination Partition Type Premium", null, null, "Partition Item", "Spec 2", List.of(size2),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(1200.0), null,
                PricingMode.PREMIUM, 5L, PricingMode.PREMIUM, "Marine Ply Lamination Partition Type Premium", "Spec 2", 1
        );
        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Work", List.of(item1, item2)
        );
        AdminQuotationBuilderService.BuilderRequest req = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.PREMIUM, "", "", List.of(room),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );
        Quotation q = builderService.autoSave(req, "admin@pravinkitchens.com");

        ResponseEntity<?> response = controller.loadQuotation(q.getId(), null);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody();
        assertNotNull(data);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> loadedRooms = (List<Map<String, Object>>) data.get("rooms");
        assertNotNull(loadedRooms);
        assertEquals(1, loadedRooms.size(), "Must have 1 consolidated room");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> loadedItems = (List<Map<String, Object>>) loadedRooms.get(0).get("items");
        assertEquals(2, loadedItems.size(), "Both materials must be returned in loadQuotation response");
        assertEquals("MARINE PLY LAMINATION BOX TYPE PREMIUM", loadedItems.get(0).get("materialName"));
        assertEquals("MARINE PLY LAMINATION PARTITION TYPE PREMIUM", loadedItems.get(1).get("materialName"));
    }

    @Test
    @DisplayName("5. Both HTML templates contain auto-save drain loop and navigation guard logic")
    public void testHtmlTemplatesContainNavigationGuards() throws IOException {
        String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/quotation-builder.html"));
        String employeeHtml = Files.readString(Path.of("src/main/resources/templates/employee/quotation-builder.html"));

        // Admin template checks
        assertTrue(adminHtml.contains("flushPendingAutoSave"), "Admin builder must define flushPendingAutoSave");
        assertTrue(adminHtml.contains("Saving changes before leaving..."), "Admin builder must show saving indicator on navigation");
        assertTrue(adminHtml.contains("originalMaterialId:"), "Admin builder auto-save must send originalMaterialId");
        assertTrue(adminHtml.contains("originalPosition:"), "Admin builder auto-save must send originalPosition");
        assertTrue(adminHtml.contains("while (true)"), "Admin builder auto-save must use drain loop");

        // Employee template checks
        assertTrue(employeeHtml.contains("flushPendingAutoSave"), "Employee builder must define flushPendingAutoSave");
        assertTrue(employeeHtml.contains("Saving changes before leaving..."), "Employee builder must show saving indicator on navigation");
        assertTrue(employeeHtml.contains("originalMaterialId:"), "Employee builder auto-save must send originalMaterialId");
        assertTrue(employeeHtml.contains("originalPosition:"), "Employee builder auto-save must send originalPosition");
        assertTrue(employeeHtml.contains("while (true)"), "Employee builder auto-save must use drain loop");
    }
}

