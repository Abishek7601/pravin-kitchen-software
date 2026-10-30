package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationOfferCalculationTest {

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
    private DivisionRepository divisionRepository;

    @Autowired
    private WorkCategoryRepository workCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Quotation quotation;

    @BeforeEach
    void setUp() {
        Branch branch = new Branch();
        branch.setName("Chennai Offer Branch " + System.currentTimeMillis());
        branch.setBranchAddress("123 Test Road, Chennai");
        branch = branchRepository.save(branch);

        WorkCategory category = workCategoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            WorkCategory wc = new WorkCategory();
            wc.setName("Kitchen");
            return workCategoryRepository.save(wc);
        });

        Division division = new Division();
        division.setName("Interior Design " + System.currentTimeMillis());
        division.setWorkCategory(category);
        division = divisionRepository.save(division);

        Customer customer = new Customer();
        customer.setName("Kavitha Devi");
        customer.setPhone("9876543210");
        customer.setAddress("45 North St, Chennai");
        customer = customerRepository.save(customer);

        User employee = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = new User();
            u.setName("Test Employee");
            u.setEmail("testemp" + System.currentTimeMillis() + "@example.com");
            u.setPassword("password");
            u.setRole(Role.EMPLOYEE);
            return userRepository.save(u);
        });

        quotation = new Quotation();
        quotation.setQuotationNumber("QT-OFFER-" + System.currentTimeMillis());
        quotation.setCustomer(customer);
        quotation.setEmployee(employee);
        quotation.setBranch(branch);
        quotation.setDivision(division);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setSubtotal(new BigDecimal("100000.00"));
        quotation.setAccessoriesAmount(BigDecimal.ZERO);
        quotation.setTravelCharge(BigDecimal.ZERO);
        quotation.setOtherCharges(BigDecimal.ZERO);
        quotation.setDiscountAmount(BigDecimal.ZERO);
        quotation.setGstPercentage(BigDecimal.ZERO);
        quotation.setOfferPercentage(BigDecimal.ZERO);
        quotation.setOfferAmount(BigDecimal.ZERO);
        quotation.setGrandTotal(new BigDecimal("100000.00"));
        quotation = quotationRepository.save(quotation);

        // Add Room and Item so calculateQuotation yields subtotal = 100,000.00
        QuotationRoom room = new QuotationRoom();
        room.setQuotation(quotation);
        room.setFloor("Ground Floor");
        room.setRoom("Living Room");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setItemDescription("Living Unit");
        item.setAmount(new BigDecimal("100000.00"));
        item.setOfferPrice(new BigDecimal("100000.00"));
        item.setActive(true);
        quotationItemRepository.save(item);
    }

    @Test
    void testStandardOfferCalculationFormula() {
        // Total Amount: ₹1,00,000, Offer Percentage: 10%
        // Offer Amount = Total Amount * 10 / 100 = ₹10,000
        // Grand Total = Total Amount - Offer Amount = ₹90,000
        quotation.setOfferPercentage(new BigDecimal("10.00"));
        quotation = quotationRepository.save(quotation);

        Quotation calculated = calculationService.calculateQuotation(quotation.getId());

        assertEquals(new BigDecimal("100000.00"), calculated.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), calculated.getOfferPercentage());
        assertEquals(new BigDecimal("10000.00"), calculated.getOfferAmount());
        assertEquals(new BigDecimal("90000.00"), calculated.getGrandTotal());
    }

    @Test
    void testZeroOfferPercentageDefaults() {
        // Default 0% offer
        quotation.setOfferPercentage(BigDecimal.ZERO);
        quotation = quotationRepository.save(quotation);

        Quotation calculated = calculationService.calculateQuotation(quotation.getId());

        assertEquals(new BigDecimal("100000.00"), calculated.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), calculated.getOfferPercentage());
        assertEquals(new BigDecimal("0.00"), calculated.getOfferAmount());
        assertEquals(new BigDecimal("100000.00"), calculated.getGrandTotal());
    }

    @Test
    void testOfferWithTaxesAndAdditionalCharges() {
        // Subtotal = 100,000, Accessories = 10,000, Travel = 5,000, Discount = 15,000
        // Amount before discount = 100,000 + 10,000 + 5,000 = 115,000
        // Amount after discount = 115,000 - 15,000 = 100,000
        // Offer Percentage = 15% -> Offer Amount = 100,000 * 0.15 = 15,000.00
        // Amount after offer (Taxable Amount) = 100,000 - 15,000 = 85,000.00
        // GST (18%) on 85,000 = 15,300.00
        // Grand Total = 85,000 + 15,300 = 100,300.00
        quotation.setAccessoriesAmount(new BigDecimal("10000.00"));
        quotation.setTravelCharge(new BigDecimal("5000.00"));
        quotation.setDiscountAmount(new BigDecimal("15000.00"));
        quotation.setGstPercentage(new BigDecimal("18.00"));
        quotation.setOfferPercentage(new BigDecimal("15.00"));
        quotation = quotationRepository.save(quotation);

        Quotation calculated = calculationService.calculateQuotation(quotation.getId());

        assertEquals(new BigDecimal("85000.00"), calculated.getTaxableAmount());
        assertEquals(new BigDecimal("15300.00"), calculated.getGstAmount());
        assertEquals(new BigDecimal("100000.00"), calculated.getTotalAmount());
        assertEquals(new BigDecimal("15.00"), calculated.getOfferPercentage());
        assertEquals(new BigDecimal("15000.00"), calculated.getOfferAmount());
        assertEquals(new BigDecimal("100300.00"), calculated.getGrandTotal());
    }

    @Test
    void testUserSpecifiedOfferBeforeGstExample() {
        // Example from user prompt:
        // Subtotal: ₹10,000, Travel: ₹500, Other: ₹500, Discount: ₹0
        // Amount Before Discount = 10,000 + 500 + 500 = 11,000
        // Amount After Discount = 11,000 - 0 = 11,000
        // Offer %: 10% -> Offer Amount = ₹1,100
        // Amount After Offer (Taxable) = ₹9,900
        // GST %: 18% -> GST Amount = ₹1,782
        // Grand Total = ₹11,682
        Quotation q = new Quotation();
        q.setQuotationNumber("QT-USER-EXAMPLE-" + System.currentTimeMillis());
        q.setCustomer(quotation.getCustomer());
        q.setEmployee(quotation.getEmployee());
        q.setBranch(quotation.getBranch());
        q.setDivision(quotation.getDivision());
        q.setQuotationDate(LocalDate.now());
        q.setStatus(QuotationStatus.PENDING);
        q.setPricingMode(PricingMode.MIDDLE);
        q.setSubtotal(new BigDecimal("10000.00"));
        q.setTravelCharge(new BigDecimal("500.00"));
        q.setOtherCharges(new BigDecimal("500.00"));
        q.setDiscountAmount(BigDecimal.ZERO);
        q.setOfferPercentage(new BigDecimal("10.00"));
        q.setGstPercentage(new BigDecimal("18.00"));
        q = quotationRepository.save(q);

        QuotationRoom room = new QuotationRoom();
        room.setQuotation(q);
        room.setFloor("Ground Floor");
        room.setRoom("Room 1");
        room.setActive(true);
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setItemDescription("Custom Item");
        item.setAmount(new BigDecimal("10000.00"));
        item.setActive(true);
        quotationItemRepository.save(item);

        Quotation calculated = calculationService.calculateQuotation(q.getId());

        assertEquals(new BigDecimal("10000.00"), calculated.getSubtotal());
        assertEquals(new BigDecimal("11000.00"), calculated.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), calculated.getOfferPercentage());
        assertEquals(new BigDecimal("11000.00").multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP), calculated.getOfferAmount());
        assertEquals(new BigDecimal("1100.00"), calculated.getOfferAmount());
        assertEquals(new BigDecimal("9900.00"), calculated.getTaxableAmount());
        assertEquals(new BigDecimal("18.00"), calculated.getGstPercentage());
        assertEquals(new BigDecimal("1782.00"), calculated.getGstAmount());
        assertEquals(new BigDecimal("11682.00"), calculated.getGrandTotal());
    }

    @Test
    void testReCalculationDoesNotDoubleApplyOffer() {
        quotation.setOfferPercentage(new BigDecimal("20.00"));
        quotation = quotationRepository.save(quotation);

        // First calculation
        Quotation calc1 = calculationService.calculateQuotation(quotation.getId());
        assertEquals(new BigDecimal("100000.00"), calc1.getTotalAmount());
        assertEquals(new BigDecimal("20000.00"), calc1.getOfferAmount());
        assertEquals(new BigDecimal("80000.00"), calc1.getGrandTotal());

        // Second calculation (re-save simulation)
        Quotation calc2 = calculationService.calculateQuotation(quotation.getId());
        assertEquals(new BigDecimal("100000.00"), calc2.getTotalAmount());
        assertEquals(new BigDecimal("20000.00"), calc2.getOfferAmount());
        assertEquals(new BigDecimal("80000.00"), calc2.getGrandTotal());

        // Third calculation via updateCharges
        Quotation updated = calculationService.updateCharges(
                quotation.getId(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20.00")
        );
        assertEquals(new BigDecimal("100000.00"), updated.getTotalAmount());
        assertEquals(new BigDecimal("20000.00"), updated.getOfferAmount());
        assertEquals(new BigDecimal("80000.00"), updated.getGrandTotal());
    }

    @Test
    void testValidationRejectsInvalidOfferPercentages() {
        assertThrows(IllegalArgumentException.class, () ->
                calculationService.validateOfferPercentage(new BigDecimal("-5"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                calculationService.validateOfferPercentage(new BigDecimal("105"))
        );

        // Valid boundaries
        assertDoesNotThrow(() -> calculationService.validateOfferPercentage(BigDecimal.ZERO));
        assertDoesNotThrow(() -> calculationService.validateOfferPercentage(new BigDecimal("50.50")));
        assertDoesNotThrow(() -> calculationService.validateOfferPercentage(new BigDecimal("100")));
    }

    @Test
    void testPdfGenerationWithOffer() {
        quotation.setOfferPercentage(new BigDecimal("10.00"));
        quotation = quotationRepository.save(quotation);
        Quotation calculated = calculationService.calculateQuotation(quotation.getId());

        byte[] pdfBytes = quotationPdfService.generateQuotationPdf(calculated.getId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
