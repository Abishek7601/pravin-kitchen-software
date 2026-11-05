package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminQuotationBuilderService;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
public class ItemDiscountGrandTotalCalculationTest {

    @Autowired
    private AdminQuotationBuilderService builderService;

    @Autowired
    private QuotationCalculationService calculationService;

    @Autowired
    private QuotationPdfService quotationPdfService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

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
    private UserRepository userRepository;

    private Branch branch;
    private WorkCategory category;
    private Division division;
    private Item item;
    private Material material;
    private MaterialOption option;
    private User adminUser;
    private User employeeUser;

    @BeforeEach
    public void setup() {
        branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Coimbatore Branch");
            b.setBranchAddress("Avinashi Road");
            b.setWhatsapp("9876543210");
            b.setEmail("cbe@pravinkitchens.com");
            return branchRepository.save(b);
        });

        adminUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .findFirst()
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("Super Admin");
                    u.setEmail("admin@test.com");
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
                    u.setName("Test Employee");
                    u.setEmail("employee@test.com");
                    u.setPassword("password");
                    u.setRole(Role.EMPLOYEE);
                    u.setBranch(branch);
                    u.setActive(true);
                    return userRepository.save(u);
                });

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

        material = new Material();
        material.setName("Material " + ts);
        material.setItem(item);
        material.setActive(true);
        material.setDisplayOrder(1);
        material = materialRepository.save(material);

        option = new MaterialOption();
        option.setName("Option " + ts);
        option.setMaterial(material);
        option.setActive(true);
        option = materialOptionRepository.save(option);

        // Pricing: rate = 500.00
        Pricing pricing = new Pricing();
        pricing.setMaterialOption(option);
        pricing.setPricingMode(PricingMode.PREMIUM);
        pricing.setRate(new BigDecimal("500.00"));
        pricing.setFormula("(L + W) × H");
        pricing.setUnit("Quantity");
        pricing.setActive(true);
        pricingRepository.save(pricing);
    }

    private AdminQuotationBuilderService.CustomerRequest createCustomerRequest() {
        return new AdminQuotationBuilderService.CustomerRequest(
                null, "Test Client", "9876543210", "client@test.com", "Main Street"
        );
    }

    @Test
    @DisplayName("Scenario 1: ₹500 material with None discount -> Grand Total ₹500")
    public void testMaterialWithNoneDiscount() {
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "None Discount", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        assertNotNull(saved);

        Quotation calculated = calculationService.calculateQuotation(saved.getId());
        assertEquals(new BigDecimal("500.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("500.00"), calculated.getGrandTotal());
    }

    @Test
    @DisplayName("Scenario 2: ₹500 material with -5% discount (Offer Price 475) -> Grand Total ₹475")
    public void testMaterialWith5PercentDiscount() {
        // Offer Price = 475.00
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("475.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "5% Discount", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        assertNotNull(saved);

        Quotation calculated = calculationService.calculateQuotation(saved.getId());
        assertEquals(new BigDecimal("475.00"), calculated.getSubtotal(), "Subtotal should reflect discounted Offer Price");
        assertEquals(new BigDecimal("475.00"), calculated.getGrandTotal(), "Grand Total should be ₹475.00");

        // Verify room offer price
        List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(saved.getId());
        assertFalse(rooms.isEmpty());
        QuotationRoom room = rooms.get(0);
        assertEquals(new BigDecimal("475.00"), room.getOfferPrice());
        assertEquals(new BigDecimal("500.00"), room.getSubtotal());
    }

    @Test
    @DisplayName("Scenario 3: ₹500 material with -10% discount (Offer Price 450) -> Grand Total ₹450")
    public void testMaterialWith10PercentDiscount() {
        // Offer Price = 450.00
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("450.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "10% Discount", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        assertNotNull(saved);

        Quotation calculated = calculationService.calculateQuotation(saved.getId());
        assertEquals(new BigDecimal("450.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("450.00"), calculated.getGrandTotal());
    }

    @Test
    @DisplayName("Scenario 4: Multiple materials with different item discounts")
    public void testMultipleMaterialsWithDifferentDiscounts() {
        // Material 1: 500 with -5% -> 475
        AdminQuotationBuilderService.SizeRequest size1 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("475.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq1 = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size1),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );

        // Material 2: 500 with -10% -> 450
        AdminQuotationBuilderService.SizeRequest size2 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("450.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq2 = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Cutlery Basket", "Files", List.of(size2),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );

        // Material 3: 500 with None -> 500
        AdminQuotationBuilderService.SizeRequest size3 = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReq3 = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Pullout", "Files", List.of(size3),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );

        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Kitchen", "Kitchen Setup", List.of(itemReq1, itemReq2, itemReq3)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Multi Discount", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        Quotation calculated = calculationService.calculateQuotation(saved.getId());

        // Expected Subtotal = 475 + 450 + 500 = 1425.00
        assertEquals(new BigDecimal("1425.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("1425.00"), calculated.getGrandTotal());
    }

    @Test
    @DisplayName("Scenario 5: Change item discount from -5% to None and recalculate")
    public void testChangeDiscountFrom5PercentToNone() {
        // Step A: Save with 5% discount (Offer Price 475)
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("475.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request1 = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Discount Change Test", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request1, adminUser.getEmail());
        assertEquals(new BigDecimal("475.00"), saved.getGrandTotal());

        // Step B: Update same quotation setting discount back to None (offerPrice = null)
        AdminQuotationBuilderService.SizeRequest sizeNone = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null
        );
        AdminQuotationBuilderService.ItemRequest itemReqNone = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(sizeNone),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReqNone = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReqNone)
        );

        AdminQuotationBuilderService.BuilderRequest request2 = new AdminQuotationBuilderService.BuilderRequest(
                saved.getId(), createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Discount Change Test", "",
                List.of(roomReqNone),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation updated = builderService.save(request2, adminUser.getEmail());
        assertEquals(new BigDecimal("500.00"), updated.getSubtotal(), "Restored to full price");
        assertEquals(new BigDecimal("500.00"), updated.getGrandTotal(), "Grand Total restored to 500");
    }

    @Test
    @DisplayName("Scenario 6: Apply Travel Charges and Other Charges along with item discounts")
    public void testItemDiscountWithTravelAndOtherCharges() {
        // Material 500 with -5% -> 475.00
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("475.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        BigDecimal travel = new BigDecimal("100.00");
        BigDecimal other = new BigDecimal("50.00");

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Charges Test", "",
                List.of(roomReq),
                BigDecimal.ZERO, travel, other, "Delivery & Handling",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        Quotation calculated = calculationService.calculateQuotation(saved.getId());

        // Net Subtotal = 475.00
        // Travel = 100.00
        // Other = 50.00
        // Grand Total = 475 + 100 + 50 = 625.00
        assertEquals(new BigDecimal("475.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("100.00"), calculated.getTravelCharge());
        assertEquals(new BigDecimal("50.00"), calculated.getOtherCharges());
        assertEquals(new BigDecimal("625.00"), calculated.getGrandTotal());
    }

    @Test
    @DisplayName("Scenario 7: Full calculation order - Item Discount, Travel, Other, Quotation Discount, Offer %, GST")
    public void testFullCalculationOrder() {
        // Material: 500 with -5% -> Net item subtotal = 475.00
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("475.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        BigDecimal travel = new BigDecimal("100.00");
        BigDecimal other = new BigDecimal("50.00");
        BigDecimal discount = new BigDecimal("25.00");
        BigDecimal offerPct = new BigDecimal("10.00");
        BigDecimal gstPct = new BigDecimal("18.00");

        // 1. Net Item Subtotal = 475.00
        // 2. Amount Before Discount = 475 + 100 + 50 = 625.00
        // 3. Amount After Discount = 625 - 25 = 600.00
        // 4. Offer Amount = 600 * 10% = 60.00
        // 5. Amount After Offer (Taxable) = 600 - 60 = 540.00
        // 6. GST Amount = 540 * 18% = 97.20
        // 7. Grand Total = 540 + 97.20 = 637.20

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "Full Order Test", "",
                List.of(roomReq),
                BigDecimal.ZERO, travel, other, "Freight",
                discount, gstPct, offerPct, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        Quotation calculated = calculationService.calculateQuotation(saved.getId());

        assertEquals(new BigDecimal("475.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("600.00"), calculated.getTotalAmount());
        assertEquals(new BigDecimal("60.00"), calculated.getOfferAmount());
        assertEquals(new BigDecimal("540.00"), calculated.getTaxableAmount());
        assertEquals(new BigDecimal("97.20"), calculated.getGstAmount());
        assertEquals(new BigDecimal("637.20"), calculated.getGrandTotal());
    }

    @Test
    @DisplayName("Scenario 8 & 9: PDF generation and Quotation Details match exact calculated Grand Total")
    public void testPdfAndDetailsMatchCalculatedGrandTotal() {
        // Material: 500 with -5% -> 475
        AdminQuotationBuilderService.SizeRequest size = new AdminQuotationBuilderService.SizeRequest(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("475.00")
        );
        AdminQuotationBuilderService.ItemRequest itemReq = new AdminQuotationBuilderService.ItemRequest(
                category.getId(), division.getId(), item.getId(), material.getId(), option.getId(),
                null, "Plain Basket", "Files", List.of(size),
                MeasurementType.QUANTITY, BigDecimal.ONE, null
        );
        AdminQuotationBuilderService.RoomRequest roomReq = new AdminQuotationBuilderService.RoomRequest(
                "Ground Floor", "Room 1", "Modular Kitchen", List.of(itemReq)
        );

        AdminQuotationBuilderService.BuilderRequest request = new AdminQuotationBuilderService.BuilderRequest(
                null, createCustomerRequest(), branch.getId(), LocalDate.now(), null,
                PricingMode.PREMIUM, "PDF Match Test", "",
                List.of(roomReq),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );

        Quotation saved = builderService.save(request, adminUser.getEmail());
        Quotation calculated = calculationService.calculateQuotation(saved.getId());

        assertEquals(new BigDecimal("475.00"), calculated.getGrandTotal());

        // Generate PDF
        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(saved.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "Generated PDF must not be empty");
    }
}
