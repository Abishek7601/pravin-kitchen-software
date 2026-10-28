package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationMultiRoomTest {

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotationRoomService quotationRoomService;

    @Autowired
    private QuotationItemService quotationItemService;

    @Autowired
    private QuotationItemSizeService quotationItemSizeService;

    @Autowired
    private QuotationCalculationService quotationCalculationService;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private AdminQuotationBuilderService adminQuotationBuilderService;

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
    private QuotationService quotationService;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private QuotationItemSizeRepository quotationItemSizeRepository;

    @Autowired
    private QuotationCommunicationRepository quotationCommunicationRepository;

    @Autowired
    private QuotationWorkflowRepository quotationWorkflowRepository;

    @Autowired
    private QuotationStatusHistoryRepository quotationStatusHistoryRepository;

    @Test
    public void testMultipleRoomsOnSingleQuotationAndCombinedPricing() {
        Customer customer = customerRepository.findAll().stream().findFirst().orElseGet(() -> {
            Customer c = new Customer();
            c.setName("MultiRoom Customer");
            c.setPhone("9988776655");
            c.setEmail("multiroom@customer.com");
            return customerRepository.save(c);
        });

        Branch branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Main Branch");
            b.setEmail("main@branch.com");
            return branchRepository.save(b);
        });

        User employee = userRepository.findAll().stream().findFirst().orElse(null);

        // 1. Create a single quotation
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-MULTI-001");
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setEmployee(employee);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation = quotationRepository.save(quotation);

        Long quotationId = quotation.getId();
        String quotationNumber = quotation.getQuotationNumber();

        // 2. Add Room 1 (Ground Floor - Room 1)
        QuotationRoom room1 = quotationRoomService.create(quotation, "Ground Floor", "1", "Living Room Shoe Rack");
        assertNotNull(room1.getId());
        assertEquals(quotationId, room1.getQuotation().getId());

        // Add Item to Room 1
        BigDecimal length1 = new BigDecimal("36");
        BigDecimal width1 = new BigDecimal("60");
        BigDecimal height1 = new BigDecimal("24");
        BigDecimal sqft1 = quotationItemService.calculateSqft(length1, width1, height1);
        BigDecimal rate1 = new BigDecimal("200.00");
        BigDecimal amount1 = quotationItemService.calculateAmount(sqft1, rate1);

        QuotationItem item1 = quotationItemService.create(
                room1, null, null, null, null, null,
                "Shoe Rack Unit",
                length1, width1, height1,
                sqft1, rate1, amount1, amount1,
                "(L + W) * H / 144",
                "Specification 1"
        );
        quotationItemSizeService.create(item1, length1, width1, height1, amount1, 1);

        // Recalculate quotation with Room 1
        quotation = quotationCalculationService.calculateQuotation(quotationId);
        BigDecimal subtotalAfterRoom1 = quotation.getSubtotal();
        assertTrue(subtotalAfterRoom1.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(amount1.setScale(2, java.math.RoundingMode.HALF_UP), subtotalAfterRoom1);

        // 3. Add Room 2 (First Floor - Room 2) to the SAME quotation
        QuotationRoom room2 = quotationRoomService.create(quotation, "First Floor", "2", "Master Bedroom Wardrobe");
        assertNotNull(room2.getId());
        assertNotEquals(room1.getId(), room2.getId());
        assertEquals(quotationId, room2.getQuotation().getId());

        // Add Item to Room 2
        BigDecimal length2 = new BigDecimal("48");
        BigDecimal width2 = new BigDecimal("72");
        BigDecimal height2 = new BigDecimal("30");
        BigDecimal sqft2 = quotationItemService.calculateSqft(length2, width2, height2);
        BigDecimal rate2 = new BigDecimal("300.00");
        BigDecimal amount2 = quotationItemService.calculateAmount(sqft2, rate2);

        QuotationItem item2 = quotationItemService.create(
                room2, null, null, null, null, null,
                "Wardrobe Unit",
                length2, width2, height2,
                sqft2, rate2, amount2, amount2,
                "(L + W) * H / 144",
                "Specification 2"
        );
        quotationItemSizeService.create(item2, length2, width2, height2, amount2, 1);

        // 4. Recalculate quotation with both rooms
        quotation = quotationCalculationService.calculateQuotation(quotationId);

        // Verify Quotation ID and Quotation Number remain unchanged
        assertEquals(quotationId, quotation.getId());
        assertEquals(quotationNumber, quotation.getQuotationNumber());

        // Verify both rooms exist under quotation
        List<QuotationRoom> activeRooms = quotationRoomService.getActiveByQuotationId(quotationId);
        assertEquals(2, activeRooms.size());

        QuotationRoom loadedRoom1 = activeRooms.stream().filter(r -> r.getId().equals(room1.getId())).findFirst().orElseThrow();
        QuotationRoom loadedRoom2 = activeRooms.stream().filter(r -> r.getId().equals(room2.getId())).findFirst().orElseThrow();

        // Verify each room has its individual subtotal intact
        assertEquals(amount1.setScale(2, java.math.RoundingMode.HALF_UP), loadedRoom1.getSubtotal());
        assertEquals(amount2.setScale(2, java.math.RoundingMode.HALF_UP), loadedRoom2.getSubtotal());

        // Verify combined subtotal is Room 1 + Room 2
        BigDecimal expectedCombinedSubtotal = amount1.add(amount2).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedCombinedSubtotal, quotation.getSubtotal());

        // Verify Grand Total calculation
        assertTrue(quotation.getGrandTotal().compareTo(expectedCombinedSubtotal) >= 0);

        // 5. Test PDF Generation for multi-room quotation
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(quotationId);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    public void testAdminBuilderMultiRoomSaveService() {
        WorkCategory category = workCategoryRepository.findAll().stream().findFirst().orElse(null);
        Division division = divisionRepository.findAll().stream().findFirst().orElse(null);
        Item item = itemRepository.findAll().stream().findFirst().orElse(null);
        Material material = materialRepository.findAll().stream().findFirst().orElse(null);
        MaterialOption option = materialOptionRepository.findAll().stream().findFirst().orElse(null);
        Branch branch = branchRepository.findAll().stream().findFirst().orElse(null);
        User user = userRepository.findAll().stream().findFirst().orElse(null);

        if (category != null && division != null && item != null && material != null && option != null && branch != null && user != null) {
            // Verify hierarchy compatibility
            if (option.getMaterial() != null && option.getMaterial().getId().equals(material.getId()) &&
                material.getItem() != null && material.getItem().getId().equals(item.getId()) &&
                item.getDivision() != null && item.getDivision().getId().equals(division.getId()) &&
                division.getWorkCategory() != null && division.getWorkCategory().getId().equals(category.getId())) {

                AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                        new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("10"), null
                );
                AdminQuotationBuilderService.ItemRequest itemReq1 = new AdminQuotationBuilderService.ItemRequest(
                        category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                        "Item Room 1", "Spec 1", List.of(size1)
                );
                AdminQuotationBuilderService.RoomRequest roomReq1 = new AdminQuotationBuilderService.RoomRequest(
                        "Ground Floor", "1", "Room 1 Work", List.of(itemReq1)
                );

                AdminQuotationBuilderService.SizeRequest size2 = new AdminQuotationBuilderService.SizeRequest(
                        new BigDecimal("25"), new BigDecimal("35"), new BigDecimal("12"), null
                );
                AdminQuotationBuilderService.ItemRequest itemReq2 = new AdminQuotationBuilderService.ItemRequest(
                        category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                        "Item Room 2", "Spec 2", List.of(size2)
                );
                AdminQuotationBuilderService.RoomRequest roomReq2 = new AdminQuotationBuilderService.RoomRequest(
                        "First Floor", "2", "Room 2 Work", List.of(itemReq2)
                );

                AdminQuotationBuilderService.CustomerRequest custReq = new AdminQuotationBuilderService.CustomerRequest(
                        "Builder Test Customer", "9876543299", "builder@test.com", "Sample Address"
                );

                AdminQuotationBuilderService.BuilderRequest builderRequest = new AdminQuotationBuilderService.BuilderRequest(
                        null, custReq, branch.getId(), LocalDate.now(), PricingMode.MIDDLE, "Req", "Notes",
                        List.of(roomReq1, roomReq2)
                );

                Quotation built = adminQuotationBuilderService.save(builderRequest, user.getEmail());
                assertNotNull(built.getId());
                assertNotNull(built.getQuotationNumber());

                List<QuotationRoom> rooms = quotationRoomService.getActiveByQuotationId(built.getId());
                assertEquals(2, rooms.size());
                assertTrue(built.getSubtotal().compareTo(BigDecimal.ZERO) > 0);
                assertEquals(QuotationStatus.WORKING, built.getStatus());
            }
        }
    }

    @Test
    public void testQuotationDeletionPreservesMasterDataAndCascades() {
        Customer customer = customerRepository.findAll().stream().findFirst().orElseGet(() -> {
            Customer c = new Customer();
            c.setName("Delete Test Customer");
            c.setPhone("9911223344");
            c.setEmail("deletetest@customer.com");
            return customerRepository.save(c);
        });

        Branch branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Delete Test Branch");
            b.setEmail("delete@branch.com");
            return branchRepository.save(b);
        });

        User employee = userRepository.findAll().stream().findFirst().orElse(null);

        // 1. Create Quotation
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-DEL-001");
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setEmployee(employee);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation = quotationRepository.save(quotation);

        Long qId = quotation.getId();
        Long custId = customer.getId();
        Long branchId = branch.getId();

        // 2. Add Room, Item, and Size
        QuotationRoom room = quotationRoomService.create(quotation, "Ground Floor", "Living", "Interior");
        BigDecimal len = new BigDecimal("10");
        BigDecimal wid = new BigDecimal("20");
        BigDecimal hgt = new BigDecimal("5");
        BigDecimal amt = new BigDecimal("1000");
        QuotationItem item = quotationItemService.create(
                room, null, null, null, null, null,
                "TV Unit", len, wid, hgt,
                new BigDecimal("50"), new BigDecimal("20"), amt, amt,
                "L*W*H", "Specs"
        );
        quotationItemSizeService.create(item, len, wid, hgt, amt, 1);

        // 3. Add Communication
        QuotationCommunication comm = new QuotationCommunication();
        comm.setQuotation(quotation);
        comm.setType(QuotationCommunication.CommunicationType.EMAIL);
        comm.setRecipient("customer@test.com");
        comm.setStatus(QuotationCommunication.CommunicationStatus.SENT);
        quotationCommunicationRepository.save(comm);

        // 4. Delete the quotation
        quotationService.delete(qId);

        // 5. Verify quotation and child records are deleted
        assertFalse(quotationRepository.findById(qId).isPresent());
        assertTrue(quotationRoomRepository.findByQuotationId(qId).isEmpty());
        assertTrue(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(qId).isEmpty());
        assertTrue(quotationStatusHistoryRepository.findByQuotationIdOrderByIdDesc(qId).isEmpty());
        assertTrue(quotationWorkflowRepository.findByQuotationIdOrderByCreatedAtDesc(qId).isEmpty());

        // 6. Verify shared master records are NOT deleted
        assertTrue(customerRepository.findById(custId).isPresent());
        assertTrue(branchRepository.findById(branchId).isPresent());
        if (employee != null && employee.getId() != null) {
            assertTrue(userRepository.findById(employee.getId()).isPresent());
        }
    }
}

