package com.example.pravin_quotation;

import com.example.pravin_quotation.model.*;
import com.example.pravin_quotation.repository.*;
import com.example.pravin_quotation.service.QuotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class QuotationSafeDeletionWorkflowTest {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private QuotationRoomRepository quotationRoomRepository;

    @Autowired
    private QuotationItemRepository quotationItemRepository;

    @Autowired
    private QuotationItemSizeRepository quotationItemSizeRepository;

    @Autowired
    private QuotationCommunicationRepository quotationCommunicationRepository;

    @Autowired
    private QuotationStatusHistoryRepository quotationStatusHistoryRepository;

    @Autowired
    private QuotationWorkflowRepository quotationWorkflowRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    private Branch testBranch;
    private User employee1;
    private User employee2;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        testBranch = new Branch();
        testBranch.setName("Test-Branch-" + ts);
        testBranch.setBranchAddress("123 Delete Street");
        testBranch = branchRepository.save(testBranch);

        employee1 = new User();
        employee1.setName("Employee One " + ts);
        employee1.setEmail("emp1_" + ts + "@example.com");
        employee1.setPassword("hash");
        employee1.setRole(Role.EMPLOYEE);
        employee1.setBranch(testBranch);
        employee1 = userRepository.save(employee1);

        employee2 = new User();
        employee2.setName("Employee Two " + ts);
        employee2.setEmail("emp2_" + ts + "@example.com");
        employee2.setPassword("hash");
        employee2.setRole(Role.EMPLOYEE);
        employee2.setBranch(testBranch);
        employee2 = userRepository.save(employee2);

        testCustomer = new Customer();
        testCustomer.setName("Customer-" + ts);
        testCustomer.setPhone("98765" + (ts % 100000));
        testCustomer.setEmail("cust_" + ts + "@example.com");
        testCustomer.setBranch(testBranch);
        testCustomer = customerRepository.save(testCustomer);
    }

    private Quotation createQuotation(String quoteNum, User emp, Customer cust, QuotationStatus status, PricingMode mode, Quotation parent) {
        Quotation q = new Quotation();
        q.setQuotationNumber(quoteNum);
        q.setEmployee(emp);
        q.setCustomer(cust);
        q.setBranch(emp.getBranch());
        q.setQuotationDate(LocalDate.now());
        q.setStatus(status);
        q.setPricingMode(mode);
        q.setParent(parent);
        q.setGrandTotal(new BigDecimal("25000.00"));
        q.setSubtotal(new BigDecimal("25000.00"));
        return quotationRepository.save(q);
    }

    @Test
    @DisplayName("Req 1 & 3: Delete quotation with communication history removes dependent records and leaves master data intact")
    void testDeleteQuotationWithCommunicationHistory() {
        long ts = System.currentTimeMillis();
        Quotation q = createQuotation("DEL-COMM-" + ts, employee1, testCustomer, QuotationStatus.SENT, PricingMode.PREMIUM, null);
        Long qId = q.getId();

        // Add rooms, items, sizes
        QuotationRoom room = new QuotationRoom(q, "Ground Floor", "Living Room", "Wall unit");
        room = quotationRoomRepository.save(room);

        QuotationItem item = new QuotationItem();
        item.setQuotationRoom(room);
        item.setPricingMode(PricingMode.PREMIUM);
        item.setRate(new BigDecimal("1500.00"));
        item = quotationItemRepository.save(item);

        QuotationItemSize size = new QuotationItemSize();
        size.setQuotationItem(item);
        size.setLengthValue(new BigDecimal("10.0"));
        size.setWidthValue(new BigDecimal("10.0"));
        size.setHeightValue(new BigDecimal("1.0"));
        size.setCalculatedSqft(new BigDecimal("100.0"));
        size.setOfferPrice(new BigDecimal("15000.00"));
        quotationItemSizeRepository.save(size);

        // Add communication history
        QuotationCommunication comm1 = new QuotationCommunication(
                q, QuotationCommunication.CommunicationType.EMAIL, testCustomer.getEmail(),
                QuotationCommunication.CommunicationStatus.SENT, "Email sent successfully", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(comm1);

        QuotationCommunication comm2 = new QuotationCommunication(
                q, QuotationCommunication.CommunicationType.PDF, "Quotation PDF",
                QuotationCommunication.CommunicationStatus.SENT, "PDF generated", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(comm2);

        QuotationCommunication comm3 = new QuotationCommunication(
                q, QuotationCommunication.CommunicationType.WHATSAPP, testCustomer.getPhone(),
                QuotationCommunication.CommunicationStatus.SENT, "WhatsApp sent", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(comm3);

        // Verify comms are saved
        assertEquals(3, quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(qId).size());

        // Perform deletion
        quotationService.delete(qId);

        // Verify quotation and all quotation-specific child records are deleted
        assertFalse(quotationRepository.findById(qId).isPresent(), "Quotation must be deleted");
        assertTrue(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(qId).isEmpty(), "Quotation communications must be deleted");
        assertTrue(quotationRoomRepository.findByQuotationId(qId).isEmpty(), "Quotation rooms must be deleted");

        // Verify shared master data remains intact
        assertTrue(customerRepository.findById(testCustomer.getId()).isPresent(), "Customer master record must NOT be deleted");
        assertTrue(userRepository.findById(employee1.getId()).isPresent(), "Employee master record must NOT be deleted");
        assertTrue(branchRepository.findById(testBranch.getId()).isPresent(), "Branch master record must NOT be deleted");
    }

    @Test
    @DisplayName("Req 2: Delete parent quotation containing multiple grades and child communications")
    void testDeleteQuotationWithMultipleGradesAndChildCommunications() {
        long ts = System.currentTimeMillis();
        Quotation parentQ = createQuotation("DEL-MULTI-" + ts, employee1, testCustomer, QuotationStatus.WORKING, PricingMode.PREMIUM, null);
        Long parentId = parentQ.getId();

        Quotation childMiddle = createQuotation("DEL-MULTI-" + ts + "-MID", employee1, testCustomer, QuotationStatus.WORKING, PricingMode.MIDDLE, parentQ);
        Quotation childEconomy = createQuotation("DEL-MULTI-" + ts + "-ECO", employee1, testCustomer, QuotationStatus.WORKING, PricingMode.ECONOMY, parentQ);

        Long midId = childMiddle.getId();
        Long ecoId = childEconomy.getId();

        // Add communications directly to child quotations (which previously caused FK constraint failure)
        QuotationCommunication midComm = new QuotationCommunication(
                childMiddle, QuotationCommunication.CommunicationType.PDF, "Quotation PDF (MIDDLE)",
                QuotationCommunication.CommunicationStatus.SENT, "Middle grade PDF", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(midComm);

        QuotationCommunication ecoComm = new QuotationCommunication(
                childEconomy, QuotationCommunication.CommunicationType.EMAIL, testCustomer.getEmail(),
                QuotationCommunication.CommunicationStatus.SENT, "Economy grade email", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(ecoComm);

        // Verify child communications exist
        assertFalse(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(midId).isEmpty());
        assertFalse(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(ecoId).isEmpty());

        // Perform deletion of parent quotation
        quotationService.delete(parentId);

        // Verify parent and all child quotations are deleted
        assertFalse(quotationRepository.findById(parentId).isPresent(), "Parent quotation must be deleted");
        assertFalse(quotationRepository.findById(midId).isPresent(), "Child middle quotation must be deleted");
        assertFalse(quotationRepository.findById(ecoId).isPresent(), "Child economy quotation must be deleted");

        // Verify child communications are deleted
        assertTrue(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(midId).isEmpty());
        assertTrue(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(ecoId).isEmpty());

        // Verify customer and employee master records remain intact
        assertTrue(customerRepository.findById(testCustomer.getId()).isPresent());
        assertTrue(userRepository.findById(employee1.getId()).isPresent());
    }

    @Test
    @DisplayName("Req 8: Employee ownership restrictions prevent unauthorized quotation deletion")
    void testEmployeeOwnershipRestrictions() {
        long ts = System.currentTimeMillis();
        Quotation q = createQuotation("DEL-SEC-" + ts, employee1, testCustomer, QuotationStatus.PENDING, PricingMode.PREMIUM, null);
        Long qId = q.getId();

        // Employee 2 attempts to delete employee 1's quotation -> must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () -> {
            quotationService.delete(qId, employee2.getId());
        }, "Employee 2 must not be able to delete Employee 1's quotation");

        // Verify quotation was NOT deleted
        assertTrue(quotationRepository.findById(qId).isPresent(), "Quotation must remain intact after unauthorized attempt");

        // Employee 1 deletes own quotation -> succeeds
        quotationService.delete(qId, employee1.getId());
        assertFalse(quotationRepository.findById(qId).isPresent(), "Employee 1 must be able to delete own quotation");
    }

    @Test
    @DisplayName("Req 7: Deleting one quotation does not delete or alter unrelated quotations or communications")
    void testUnrelatedQuotationsRemainIntact() {
        long ts = System.currentTimeMillis();
        Quotation qA = createQuotation("DEL-A-" + ts, employee1, testCustomer, QuotationStatus.PENDING, PricingMode.PREMIUM, null);
        Quotation qB = createQuotation("DEL-B-" + ts, employee1, testCustomer, QuotationStatus.SENT, PricingMode.MIDDLE, null);

        Long qAId = qA.getId();
        Long qBId = qB.getId();

        QuotationCommunication commA = new QuotationCommunication(
                qA, QuotationCommunication.CommunicationType.PDF, "PDF A",
                QuotationCommunication.CommunicationStatus.SENT, "Msg A", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(commA);

        QuotationCommunication commB = new QuotationCommunication(
                qB, QuotationCommunication.CommunicationType.PDF, "PDF B",
                QuotationCommunication.CommunicationStatus.SENT, "Msg B", LocalDateTime.now()
        );
        quotationCommunicationRepository.save(commB);

        // Delete quotation A
        quotationService.delete(qAId);

        // Quotation A is gone
        assertFalse(quotationRepository.findById(qAId).isPresent());
        assertTrue(quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(qAId).isEmpty());

        // Quotation B and its communication are completely preserved!
        assertTrue(quotationRepository.findById(qBId).isPresent(), "Unrelated quotation B must NOT be deleted");
        assertEquals(1, quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(qBId).size(), "Quotation B communications must NOT be deleted");
    }

    @Test
    @DisplayName("Req 3, 4, 5: Safely delete Pending, Working, and Sent quotations")
    void testDeletePendingWorkingAndSentQuotations() {
        long ts = System.currentTimeMillis();
        Quotation qPending = createQuotation("DEL-PENDING-" + ts, employee1, testCustomer, QuotationStatus.PENDING, PricingMode.ECONOMY, null);
        Quotation qWorking = createQuotation("DEL-WORKING-" + ts, employee1, testCustomer, QuotationStatus.WORKING, PricingMode.MIDDLE, null);
        Quotation qSent = createQuotation("DEL-SENT-" + ts, employee1, testCustomer, QuotationStatus.SENT, PricingMode.PREMIUM, null);

        Long pId = qPending.getId();
        Long wId = qWorking.getId();
        Long sId = qSent.getId();

        quotationService.delete(pId);
        assertFalse(quotationRepository.findById(pId).isPresent(), "Pending quotation must be deleted");

        quotationService.delete(wId);
        assertFalse(quotationRepository.findById(wId).isPresent(), "Working quotation must be deleted");

        quotationService.delete(sId);
        assertFalse(quotationRepository.findById(sId).isPresent(), "Sent quotation must be deleted");
    }

    @Test
    @DisplayName("Req 9: Quotation counts correctly track parent quotations without double counting child grades")
    void testQuotationCountsReflectParentQuotationsOnly() {
        long ts = System.currentTimeMillis();

        long initialPending = quotationRepository.countByParentIsNullAndEmployeeIdAndStatus(employee1.getId(), QuotationStatus.PENDING);

        // Create 1 parent quotation and 2 child quotations with PENDING status
        Quotation parentQ = createQuotation("DEL-COUNT-" + ts, employee1, testCustomer, QuotationStatus.PENDING, PricingMode.PREMIUM, null);
        Quotation child1 = createQuotation("DEL-COUNT-" + ts + "-M", employee1, testCustomer, QuotationStatus.PENDING, PricingMode.MIDDLE, parentQ);
        Quotation child2 = createQuotation("DEL-COUNT-" + ts + "-E", employee1, testCustomer, QuotationStatus.PENDING, PricingMode.ECONOMY, parentQ);

        // countByParentIsNullAndEmployeeIdAndStatus must only increment by 1 (the parent)
        long afterCreatePending = quotationRepository.countByParentIsNullAndEmployeeIdAndStatus(employee1.getId(), QuotationStatus.PENDING);
        assertEquals(initialPending + 1, afterCreatePending, "Only the parent quotation must be counted in dashboard tabs");

        // Delete the parent quotation
        quotationService.delete(parentQ.getId());

        // Count must return to initialPending
        long afterDeletePending = quotationRepository.countByParentIsNullAndEmployeeIdAndStatus(employee1.getId(), QuotationStatus.PENDING);
        assertEquals(initialPending, afterDeletePending, "Count must decrement back after deletion");
    }
}
