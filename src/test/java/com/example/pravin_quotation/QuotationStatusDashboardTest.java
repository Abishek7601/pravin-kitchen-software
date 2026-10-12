package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.AdminDashboardService;
import com.example.pravin_quotation.service.QuotationCalculationService;
import com.example.pravin_quotation.service.QuotationRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuotationStatusDashboardTest {

    @Autowired
    private AdminDashboardService adminDashboardService;

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
    private QuotationCalculationService quotationCalculationService;

    @Test
    public void testQuotationStatusLifecycleAndDashboardCounts() {
        // Step 0: Record initial counts from database
        long initialPending = adminDashboardService.getPendingQuotationCount();
        long initialWorking = adminDashboardService.getWorkingQuotationCount();
        long initialSent = adminDashboardService.getSentQuotationCount();

        // Customer, Branch, Employee
        Customer customer = customerRepository.findAll().stream().findFirst().orElseGet(() -> {
            Customer c = new Customer();
            c.setName("Test Customer");
            c.setPhone("9876543210");
            c.setEmail("test@customer.com");
            return customerRepository.save(c);
        });

        Branch branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = new Branch();
            b.setName("Test Branch");
            b.setEmail("test@branch.com");
            return branchRepository.save(b);
        });

        User employee = userRepository.findAll().stream().findFirst().orElse(null);

        // Scenario 1: A newly created and saved quotation defaults to PENDING
        Quotation quotation = new Quotation();
        quotation.setQuotationNumber("QT-TEST-STATUS-" + System.currentTimeMillis());
        quotation.setCustomer(customer);
        quotation.setBranch(branch);
        quotation.setEmployee(employee);
        quotation.setPricingMode(PricingMode.MIDDLE);
        quotation.setQuotationDate(LocalDate.now());
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setGrandTotal(BigDecimal.ZERO);
        quotation.setSubtotal(BigDecimal.ZERO);
        Quotation saved = quotationRepository.save(quotation);

        assertEquals(QuotationStatus.PENDING, saved.getStatus());
        assertEquals(initialPending + 1, adminDashboardService.getPendingQuotationCount(),
                "Pending count should increase by 1 after saving a new quotation");
        assertEquals(initialWorking, adminDashboardService.getWorkingQuotationCount(),
                "Working count should remain unchanged");
        assertEquals(initialSent, adminDashboardService.getSentQuotationCount(),
                "Sent count should remain unchanged");

        // Scenario 2: Calculation or viewing of an empty quotation does not change it from PENDING
        Quotation calculatedEmpty = quotationCalculationService.calculateQuotation(saved.getId());
        assertEquals(QuotationStatus.PENDING, calculatedEmpty.getStatus());
        assertEquals(initialPending + 1, adminDashboardService.getPendingQuotationCount());
        assertEquals(initialWorking, adminDashboardService.getWorkingQuotationCount());

        // Scenario 3: Adding a room / building the quotation moves it from PENDING to WORKING
        quotationRoomService.create(saved, "Ground Floor", "Living Room", "Full interior woodwork");
        Quotation inProgress = quotationRepository.findById(saved.getId()).orElseThrow();
        assertEquals(QuotationStatus.WORKING, inProgress.getStatus(),
                "Quotation status should automatically become WORKING once quotation building has started");

        assertEquals(initialPending, adminDashboardService.getPendingQuotationCount(),
                "Pending count should return to initial");
        assertEquals(initialWorking + 1, adminDashboardService.getWorkingQuotationCount(),
                "Working count should increase by 1");
        assertEquals(initialSent, adminDashboardService.getSentQuotationCount(),
                "Sent count should remain unchanged");

        // Scenario 4: Further calculations / edits keep it in WORKING
        Quotation recalculated = quotationCalculationService.calculateQuotation(inProgress.getId());
        assertEquals(QuotationStatus.WORKING, recalculated.getStatus());
        assertEquals(initialWorking + 1, adminDashboardService.getWorkingQuotationCount());

        // Scenario 5: Sending the quotation transitions it to SENT
        if (recalculated.getStatus() == QuotationStatus.PENDING
                || recalculated.getStatus() == QuotationStatus.WORKING) {
            recalculated.setStatus(QuotationStatus.SENT);
            recalculated = quotationRepository.save(recalculated);
        }

        assertEquals(QuotationStatus.SENT, recalculated.getStatus());
        assertEquals(initialWorking, adminDashboardService.getWorkingQuotationCount(),
                "Working count should decrease by 1 after sending");
        assertEquals(initialSent + 1, adminDashboardService.getSentQuotationCount(),
                "Sent count should increase by 1 after sending");
        assertEquals(initialPending, adminDashboardService.getPendingQuotationCount(),
                "Pending count remains at initial");

        // Scenario 6: Sending again does not duplicate or modify counts
        if (recalculated.getStatus() == QuotationStatus.PENDING
                || recalculated.getStatus() == QuotationStatus.WORKING) {
            recalculated.setStatus(QuotationStatus.SENT);
            recalculated = quotationRepository.save(recalculated);
        }
        assertEquals(initialSent + 1, adminDashboardService.getSentQuotationCount(),
                "Sent count should stay the same on subsequent sends");
        assertEquals(initialWorking, adminDashboardService.getWorkingQuotationCount(),
                "Working count should stay the same on subsequent sends");
    }
}
