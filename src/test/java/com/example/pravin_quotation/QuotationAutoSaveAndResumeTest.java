package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class QuotationAutoSaveAndResumeTest {

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

    private AdminQuotationBuilderService builderService;

    private User adminUser;
    private User employeeUser;
    private Branch branchKk;
    private Branch branchMadurai;

    private final Map<Long, Quotation> quotationStore = new HashMap<>();
    private final AtomicLong quotationIdGen = new AtomicLong(100L);

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

        branchKk = new Branch();
        branchKk.setId(1L);
        branchKk.setName("Kanniyakumari");
        branchKk.setActive(true);

        branchMadurai = new Branch();
        branchMadurai.setId(2L);
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
        employeeUser.setName("John Employee");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setBranch(branchKk);
        employeeUser.setActive(true);

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
                    } else if ("existsByQuotationNumber".equals(name)) {
                        return false;
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
                        if (Long.valueOf(1L).equals(id)) return Optional.of(branchKk);
                        if (Long.valueOf(2L).equals(id)) return Optional.of(branchMadurai);
                    } else if ("findAll".equals(method.getName())) {
                        return List.of(branchKk, branchMadurai);
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
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        WorkCategory cat = new WorkCategory();
        cat.setId(1L);
        cat.setName("Kitchen");

        Division div = new Division();
        div.setId(2L);
        div.setName("Cabinets");
        div.setWorkCategory(cat);

        Material mat = new Material();
        mat.setId(4L);
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

        itemRepository = (ItemRepository) Proxy.newProxyInstance(
                ItemRepository.class.getClassLoader(),
                new Class<?>[]{ItemRepository.class},
                (proxy, method, args) -> {
                    if ("findByDivisionIdAndActiveTrueOrderByDisplayOrderAsc".equals(method.getName())) {
                        Item it = new Item();
                        it.setId(3L);
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

        materialOptionRepository = (MaterialOptionRepository) Proxy.newProxyInstance(
                MaterialOptionRepository.class.getClassLoader(),
                new Class<?>[]{MaterialOptionRepository.class},
                (proxy, method, args) -> {
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
        pricingRepository = createMock(PricingRepository.class);
        districtPricingRepository = createMock(DistrictPricingRepository.class);

        QuotationRoomRepository quotationRoomRepository = (QuotationRoomRepository) Proxy.newProxyInstance(
                QuotationRoomRepository.class.getClassLoader(),
                new Class<?>[]{QuotationRoomRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        QuotationRoom r = (QuotationRoom) args[0];
                        if (r.getId() == null) r.setId(101L);
                        return r;
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        QuotationItemRepository quotationItemRepository = (QuotationItemRepository) Proxy.newProxyInstance(
                QuotationItemRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        QuotationItem qi = (QuotationItem) args[0];
                        if (qi.getId() == null) qi.setId(201L);
                        return qi;
                    }
                    if (method.getReturnType().equals(List.class)) return Collections.emptyList();
                    if (method.getReturnType().equals(Optional.class)) return Optional.empty();
                    return null;
                }
        );

        QuotationItemSizeRepository quotationItemSizeRepository = (QuotationItemSizeRepository) Proxy.newProxyInstance(
                QuotationItemSizeRepository.class.getClassLoader(),
                new Class<?>[]{QuotationItemSizeRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        QuotationItemSize sz = (QuotationItemSize) args[0];
                        if (sz.getId() == null) sz.setId(301L);
                        return sz;
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
    @DisplayName("1. Initial autoSave saves draft in PENDING status with quotation number and navigation state")
    public void testAutoSaveInitialDraftPreservesPendingStatus() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Arun Kumar", "9876543210", "arun@example.com", "Nagercoil"
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, // new quotation
                customerReq,
                1L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                PricingMode.MIDDLE,
                "Needs kitchen modular cabinets",
                "Customer requested teak finish",
                Collections.emptyList(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2",
                "Ground Floor",
                "Kitchen",
                5L,
                12L,
                "{\"floor\":\"Ground Floor\",\"room\":\"Kitchen\"}"
        );

        Quotation saved = builderService.autoSave(request, "admin@pravinkitchens.com");

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(QuotationStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getQuotationNumber());
        assertEquals("Ground Floor", saved.getLastActiveFloor());
        assertEquals("Kitchen", saved.getLastActiveRoom());
        assertEquals(5L, saved.getLastActiveCategoryId());
        assertEquals(12L, saved.getLastActiveDivisionId());
        assertEquals("{\"floor\":\"Ground Floor\",\"room\":\"Kitchen\"}", saved.getBuilderState());
    }

    @Test
    @DisplayName("2. Subsequent auto-saves update the SAME single quotation record without creating duplicates")
    public void testAutoSaveSubsequentUpdatesSameRecordWithoutDuplicates() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Meena", "9876543211", "meena@example.com", "Marthandam"
        );

        AdminQuotationBuilderService.BuilderRequest request1 = new AdminQuotationBuilderService.BuilderRequest(
                null,
                customerReq,
                1L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                PricingMode.MIDDLE,
                "", "", Collections.emptyList(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );

        Quotation firstSave = builderService.autoSave(request1, "admin@pravinkitchens.com");
        Long firstId = firstSave.getId();
        int initialCount = quotationStore.size();

        // Second auto-save with updated navigation and details
        AdminQuotationBuilderService.BuilderRequest request2 = new AdminQuotationBuilderService.BuilderRequest(
                firstId, // passing existing ID
                customerReq,
                1L,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                PricingMode.MIDDLE,
                "", "Updated notes during auto-save", Collections.emptyList(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "1st Floor", "Master Bedroom", 2L, 4L, "{\"floor\":\"1st Floor\"}"
        );

        Quotation secondSave = builderService.autoSave(request2, "admin@pravinkitchens.com");

        assertEquals(firstId, secondSave.getId(), "Must update the same quotation record");
        assertEquals(initialCount, quotationStore.size(), "No duplicate quotation records should be created");
        assertEquals(QuotationStatus.PENDING, secondSave.getStatus());
        assertEquals("1st Floor", secondSave.getLastActiveFloor());
        assertEquals("Master Bedroom", secondSave.getLastActiveRoom());
        assertEquals("Updated notes during auto-save", secondSave.getNotes());
    }

    @Test
    @DisplayName("3. Final save updates the existing draft record and transitions status to WORKING")
    public void testFinalSaveUpdatesExistingDraftAndTransitionsToWorking() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Kavitha", "9876543212", "kavitha@example.com", "Kanyakumari"
        );

        // Step 1: Draft auto-saved as PENDING
        AdminQuotationBuilderService.BuilderRequest draftReq = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "", "", Collections.emptyList(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );
        Quotation draft = builderService.autoSave(draftReq, "admin@pravinkitchens.com");
        Long draftId = draft.getId();
        assertEquals(QuotationStatus.PENDING, draft.getStatus());

        // Step 2: Final submit via save(...) with rooms
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), null
        );
        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 4L, null, null, "Item desc", "Teak", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(100.0), null
        );
        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen work", List.of(item)
        );

        AdminQuotationBuilderService.BuilderRequest finalReq = new AdminQuotationBuilderService.BuilderRequest(
                draftId, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "", "", List.of(room),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Kitchen", 1L, 2L, "{}"
        );

        Quotation finalSaved = builderService.save(finalReq, "admin@pravinkitchens.com");

        assertEquals(draftId, finalSaved.getId(), "Must preserve the same quotation ID");
        assertEquals(QuotationStatus.WORKING, finalSaved.getStatus(), "Final save transitions status to WORKING");
    }

    private AdminQuotationBuilderService.RoomRequest createSampleRoom(String floor, String roomName) {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), null
        );
        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 4L, null, null, "Item desc", "Teak", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(100.0), null
        );
        return new AdminQuotationBuilderService.RoomRequest(floor, roomName, "Sample work", List.of(item));
    }

    @Test
    @DisplayName("3b. Previously saved WORKING quotation preserves WORKING when edited and saved")
    public void testEditingWorkingQuotationPreservesWorkingStatus() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Suresh", "9876543213", "suresh@example.com", "Nagercoil"
        );

        AdminQuotationBuilderService.BuilderRequest saveReq = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Initial requirements", "", List.of(createSampleRoom("Ground Floor", "Living")),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "1", "Ground Floor", "Living", 1L, 2L, "{}"
        );
        Quotation saved = builderService.save(saveReq, "admin@pravinkitchens.com");
        assertEquals(QuotationStatus.WORKING, saved.getStatus());

        // Now edit the saved quotation
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                saved.getId(), customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Updated requirements during edit", "Extra notes", List.of(createSampleRoom("Ground Floor", "Living")),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Living", 1L, 2L, "{}"
        );
        Quotation edited = builderService.save(editReq, "admin@pravinkitchens.com");

        assertEquals(saved.getId(), edited.getId());
        assertEquals(QuotationStatus.WORKING, edited.getStatus(), "Editing a WORKING quotation must preserve WORKING status and not become PENDING");
    }

    @Test
    @DisplayName("3c. Previously SENT quotation preserves SENT status when edited and saved")
    public void testEditingSentQuotationPreservesSentStatus() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Priya", "9876543214", "priya@example.com", "Kanyakumari"
        );

        AdminQuotationBuilderService.BuilderRequest saveReq = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "", "", List.of(createSampleRoom("Ground Floor", "Bedroom")),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "1", "Ground Floor", "Bedroom", 1L, 2L, "{}"
        );
        Quotation saved = builderService.save(saveReq, "admin@pravinkitchens.com");
        saved.setStatus(QuotationStatus.SENT);
        quotationStore.put(saved.getId(), saved);

        // Edit and save the SENT quotation
        AdminQuotationBuilderService.BuilderRequest editReq = new AdminQuotationBuilderService.BuilderRequest(
                saved.getId(), customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Updated after sent", "", List.of(createSampleRoom("Ground Floor", "Bedroom")),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Bedroom", 1L, 2L, "{}"
        );
        Quotation edited = builderService.save(editReq, "admin@pravinkitchens.com");

        assertEquals(saved.getId(), edited.getId());
        assertEquals(QuotationStatus.SENT, edited.getStatus(), "Editing a SENT quotation must preserve SENT status");
    }

    @Test
    @DisplayName("3d. Incomplete auto-save with rooms and materials strictly remains PENDING")
    public void testIncompleteAutoSaveWithRoomsStrictlyRemainsPending() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Manoj", "9876543215", "manoj@example.com", "Nagercoil"
        );

        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.valueOf(5.0), BigDecimal.valueOf(8.0), BigDecimal.valueOf(2.0), null
        );
        AdminQuotationBuilderService.ItemRequest item = new AdminQuotationBuilderService.ItemRequest(
                1L, 2L, 3L, 4L, null, null, "Wardrobe", "Laminate", List.of(size),
                MeasurementType.SQFT, BigDecimal.ONE, null, BigDecimal.valueOf(250.0), null
        );
        AdminQuotationBuilderService.RoomRequest room = new AdminQuotationBuilderService.RoomRequest(
                "1st Floor", "Master Bedroom", "Wardrobe work", List.of(item)
        );

        AdminQuotationBuilderService.BuilderRequest autoSaveReq = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 1L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "Partial specs", "Draft notes", List.of(room),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "3", "1st Floor", "Master Bedroom", 1L, 2L, "{\"step\":3}"
        );

        Quotation autoSaved = builderService.autoSave(autoSaveReq, "admin@pravinkitchens.com");

        assertNotNull(autoSaved.getId());
        assertEquals(QuotationStatus.PENDING, autoSaved.getStatus(),
                "Incomplete quotation must remain PENDING even when rooms and materials are added during auto-save");
    }

    @Test
    @DisplayName("4. Employee branch security is enforced on autoSave")
    public void testEmployeeBranchSecurityEnforced() {
        AdminQuotationBuilderService.CustomerRequest customerReq = new AdminQuotationBuilderService.CustomerRequest(
                null, "Unauthorized User", "9876543299", "unauth@example.com", "Madurai"
        );

        // Employee from Branch 1 (Kanniyakumari) attempts to auto-save for Branch 2 (Madurai)
        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, customerReq, 2L, LocalDate.now(), LocalDate.now().plusDays(30),
                PricingMode.MIDDLE, "", "", Collections.emptyList(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true,
                "2", "Ground Floor", "Room 1", 1L, 2L, "{}"
        );

        assertThrows(AccessDeniedException.class, () -> {
            builderService.autoSave(request, "employee@pravinkitchens.com");
        });
    }

    @Test
    @DisplayName("5. Quotation builder HTML templates contain auto-save indicator and trigger functions")
    public void testBuilderHtmlContainsAutoSaveUiAndLogic() throws IOException {
        String adminHtml = Files.readString(Path.of("src/main/resources/templates/admin/quotation-builder.html"));
        String employeeHtml = Files.readString(Path.of("src/main/resources/templates/employee/quotation-builder.html"));

        // Verify Auto Save Indicator UI
        assertTrue(adminHtml.contains("id=\"autoSaveIndicator\""), "Admin builder must contain autoSaveIndicator");
        assertTrue(adminHtml.contains("id=\"autoSaveDot\""), "Admin builder must contain autoSaveDot");
        assertTrue(adminHtml.contains("id=\"autoSaveText\""), "Admin builder must contain autoSaveText");
        assertTrue(adminHtml.contains("All changes saved"), "Admin builder must have default All changes saved text");

        assertTrue(employeeHtml.contains("id=\"autoSaveIndicator\""), "Employee builder must contain autoSaveIndicator");
        assertTrue(employeeHtml.contains("id=\"autoSaveDot\""), "Employee builder must contain autoSaveDot");
        assertTrue(employeeHtml.contains("id=\"autoSaveText\""), "Employee builder must contain autoSaveText");

        // Verify Javascript Auto Save functions
        assertTrue(adminHtml.contains("function triggerAutoSave("), "Admin builder must define triggerAutoSave");
        assertTrue(adminHtml.contains("async function performAutoSave("), "Admin builder must define performAutoSave");
        assertTrue(adminHtml.contains("loadQuotationData("), "Admin builder must define loadQuotationData");
        assertTrue(adminHtml.contains("window.history.replaceState"), "Admin builder must update URL with replaceState");

        assertTrue(employeeHtml.contains("function triggerAutoSave("), "Employee builder must define triggerAutoSave");
        assertTrue(employeeHtml.contains("async function performAutoSave("), "Employee builder must define performAutoSave");
        assertTrue(employeeHtml.contains("loadQuotationData("), "Employee builder must define loadQuotationData");
    }

    @Test
    @DisplayName("6. Quotation listings contain Resume action button for PENDING status")
    public void testQuotationListingsContainResumeButton() throws IOException {
        String adminListHtml = Files.readString(Path.of("src/main/resources/templates/admin/quotations.html"));
        String employeeListHtml = Files.readString(Path.of("src/main/resources/templates/employee/quotations.html"));
        String employeeDashboardHtml = Files.readString(Path.of("src/main/resources/templates/employee/dashboard.html"));

        // Admin quotations list
        assertTrue(adminListHtml.contains("Resume"), "Admin quotations list must have Resume button");
        assertTrue(adminListHtml.contains("resume-btn"), "Admin quotations list must have resume-btn class");
        assertTrue(adminListHtml.contains("quotation.status.name() == 'PENDING'"), "Admin quotations list must check for PENDING status");

        // Employee quotations list
        assertTrue(employeeListHtml.contains("Resume"), "Employee quotations list must have Resume button");
        assertTrue(employeeListHtml.contains("resume-btn"), "Employee quotations list must have resume-btn class");
        assertTrue(employeeListHtml.contains("quotation.status.name() == 'PENDING'"), "Employee quotations list must check for PENDING status");

        // Employee dashboard recent quotations
        assertTrue(employeeDashboardHtml.contains("quotation.status.name() == 'PENDING'"), "Employee dashboard must link pending quotations to builder");
    }
}
