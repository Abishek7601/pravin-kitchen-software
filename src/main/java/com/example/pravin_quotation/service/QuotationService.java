package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.QuotationWorkflow;
import com.example.pravin_quotation.model.User;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.model.QuotationItem;
import com.example.pravin_quotation.model.QuotationItemSize;
import com.example.pravin_quotation.model.QuotationRoom;
import com.example.pravin_quotation.model.QuotationStatusHistory;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.CustomerRepository;
import com.example.pravin_quotation.repository.QuotationCommunicationRepository;
import com.example.pravin_quotation.repository.QuotationItemRepository;
import com.example.pravin_quotation.repository.QuotationItemSizeRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import com.example.pravin_quotation.repository.QuotationRoomRepository;
import com.example.pravin_quotation.repository.QuotationStatusHistoryRepository;
import com.example.pravin_quotation.repository.QuotationWorkflowRepository;
import com.example.pravin_quotation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final QuotationWorkflowService quotationWorkflowService;
    private final QuotationRoomRepository quotationRoomRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationItemSizeRepository quotationItemSizeRepository;
    private final QuotationStatusHistoryRepository quotationStatusHistoryRepository;
    private final QuotationWorkflowRepository quotationWorkflowRepository;
    private final QuotationCommunicationRepository quotationCommunicationRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public void setEntityManager(jakarta.persistence.EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public QuotationService(
            QuotationRepository quotationRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            QuotationWorkflowService quotationWorkflowService,
            QuotationRoomRepository quotationRoomRepository,
            QuotationItemRepository quotationItemRepository,
            QuotationItemSizeRepository quotationItemSizeRepository,
            QuotationStatusHistoryRepository quotationStatusHistoryRepository,
            QuotationWorkflowRepository quotationWorkflowRepository,
            QuotationCommunicationRepository quotationCommunicationRepository
    ) {
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.quotationWorkflowService = quotationWorkflowService;
        this.quotationRoomRepository = quotationRoomRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.quotationItemSizeRepository = quotationItemSizeRepository;
        this.quotationStatusHistoryRepository = quotationStatusHistoryRepository;
        this.quotationWorkflowRepository = quotationWorkflowRepository;
        this.quotationCommunicationRepository = quotationCommunicationRepository;
    }

    // ---------------------------------------------------------
    // Get all quotations
    // ---------------------------------------------------------

    public List<Quotation> getAllQuotations() {

        return quotationRepository.findByParentIsNullOrderByCreatedAtDesc();
    }

    // ---------------------------------------------------------
    // Get quotation by ID
    // ---------------------------------------------------------

    public Quotation getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Quotation ID is required."
            );
        }

        return quotationRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."
                        )
                );
    }

    // ---------------------------------------------------------
    // Get quotation by quotation number
    // ---------------------------------------------------------

    public Quotation getByQuotationNumber(
            String quotationNumber
    ) {

        if (quotationNumber == null
                || quotationNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Quotation number is required."
            );
        }

        return quotationRepository
                .findByQuotationNumber(
                        quotationNumber.trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quotation not found."
                        )
                );
    }

    // ---------------------------------------------------------
    // Get quotations by customer
    // ---------------------------------------------------------

    public List<Quotation> getByCustomerId(
            Long customerId
    ) {

        validateId(customerId, "Customer ID");

        return quotationRepository
                .findByParentIsNullAndCustomerIdOrderByCreatedAtDesc(
                        customerId
                );
    }

    // ---------------------------------------------------------
    // Get quotations by employee
    // ---------------------------------------------------------

    public List<Quotation> getByEmployeeId(
            Long employeeId
    ) {

        validateId(employeeId, "Employee ID");

        return quotationRepository
                .findByParentIsNullAndEmployeeIdOrderByCreatedAtDesc(
                        employeeId
                );
    }

    // ---------------------------------------------------------
    // Get quotations by branch / district
    // ---------------------------------------------------------

    public List<Quotation> getByBranchId(
            Long branchId
    ) {

        validateId(branchId, "Branch ID");

        return quotationRepository
                .findByParentIsNullAndBranchIdOrderByCreatedAtDesc(
                        branchId
                );
    }

    // ---------------------------------------------------------
    // Get quotations by branch and status
    // ---------------------------------------------------------

    public List<Quotation> getByBranchIdAndStatus(
            Long branchId,
            QuotationStatus status
    ) {

        validateId(branchId, "Branch ID");

        if (status == null) {
            return getByBranchId(branchId);
        }

        return quotationRepository
                .findByParentIsNullAndBranchIdAndStatusOrderByCreatedAtDesc(
                        branchId,
                        status
                );
    }

    public long getCountByBranchIdAndStatus(
            Long branchId,
            QuotationStatus status
    ) {

        validateId(branchId, "Branch ID");

        if (status == null) {
            return quotationRepository.countByParentIsNullAndBranchId(branchId);
        }

        return quotationRepository.countByParentIsNullAndBranchIdAndStatus(
                branchId,
                status
        );
    }

    // ---------------------------------------------------------
    // Get quotations by status
    // ---------------------------------------------------------

    public List<Quotation> getByStatus(
            QuotationStatus status
    ) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Quotation status is required."
            );
        }

        return quotationRepository
                .findByParentIsNullAndStatusOrderByCreatedAtDesc(
                        status
                );
    }

    // ---------------------------------------------------------
    // Get quotations between dates
    // ---------------------------------------------------------

    public List<Quotation> getByDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {

        validateDateRange(startDate, endDate);

        return quotationRepository
                .findByParentIsNullAndQuotationDateBetweenOrderByQuotationDateDesc(
                        startDate,
                        endDate
                );
    }

    // ---------------------------------------------------------
    // Get branch quotations between dates
    // ---------------------------------------------------------

    public List<Quotation> getByBranchAndDateRange(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        validateId(branchId, "Branch ID");

        validateDateRange(
                startDate,
                endDate
        );

        return quotationRepository
                .findByParentIsNullAndBranchIdAndQuotationDateBetweenOrderByQuotationDateDesc(
                        branchId,
                        startDate,
                        endDate
                );
    }

    // ---------------------------------------------------------
    // Get 60-day quotations
    // ---------------------------------------------------------

    public List<Quotation> getLast60DaysQuotations() {

        LocalDate today = LocalDate.now();

        LocalDate startDate =
                today.minusDays(59);

        return getByDateRange(
                startDate,
                today
        );
    }

    public List<Quotation> getOneYearQuotations() {

        LocalDate today = LocalDate.now();

        LocalDate startDate =
                today.minusYears(1);

        return getByDateRange(
                startDate,
                today
        );
    }

    // ---------------------------------------------------------
    // Get branch 60-day quotations
    // ---------------------------------------------------------

    public List<Quotation> getLast60DaysQuotationsByBranch(
            Long branchId
    ) {

        LocalDate today = LocalDate.now();

        LocalDate startDate =
                today.minusDays(59);

        return getByBranchAndDateRange(
                branchId,
                startDate,
                today
        );
    }

    // ---------------------------------------------------------
    // Get approved quotations
    // ---------------------------------------------------------

    public List<Quotation> getApprovedQuotations() {

        return quotationRepository
                .findByParentIsNullAndStatusOrderByCreatedAtDesc(
                        QuotationStatus.SENT
                );
    }

    public List<Quotation> getWorkingQuotations() {

        return quotationRepository
                .findByParentIsNullAndStatusOrderByCreatedAtDesc(
                        QuotationStatus.WORKING
                );
    }

    // ---------------------------------------------------------
    // Get pending quotations
    // ---------------------------------------------------------

    public List<Quotation> getPendingQuotations() {

        return quotationRepository
                .findByParentIsNullAndStatusOrderByCreatedAtDesc(
                        QuotationStatus.PENDING
                );
    }

    // ---------------------------------------------------------
    // Create quotation
    // ---------------------------------------------------------

    @Transactional
    public Quotation create(
            String quotationNumber,
            Long customerId,
            Long employeeId,
            Long branchId,
            LocalDate quotationDate,
            PricingMode pricingMode,
            String customerRequirements,
            String notes
    ) {

        String cleanQuotationNumber =
                cleanText(quotationNumber);

        validateQuotationNumber(
                cleanQuotationNumber
        );

        if (quotationRepository.existsByQuotationNumber(
                cleanQuotationNumber
        )) {

            throw new IllegalArgumentException(
                    "Quotation number already exists."
            );
        }

        validateId(
                customerId,
                "Customer ID"
        );

        validateId(
                employeeId,
                "Employee ID"
        );

        validateId(
                branchId,
                "Branch ID"
        );

        if (pricingMode == null) {

            throw new IllegalArgumentException(
                    "Pricing mode is required."
            );
        }

        // -----------------------------------------------------
        // Find customer
        // -----------------------------------------------------

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found."
                        )
                );

        // -----------------------------------------------------
        // Find employee
        // -----------------------------------------------------

        User employee = userRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        )
                );

        // -----------------------------------------------------
        // Find branch
        // -----------------------------------------------------

        Branch branch = branchRepository
                .findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Branch not found."
                        )
                );

        // -----------------------------------------------------
        // Validate active customer
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                customer.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected customer is inactive."
            );
        }

        // -----------------------------------------------------
        // Validate active employee
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                employee.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected employee is inactive."
            );
        }

        // -----------------------------------------------------
        // Validate active branch
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                branch.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected branch is inactive."
            );
        }

        // -----------------------------------------------------
        // Create quotation
        // -----------------------------------------------------

        Quotation quotation = new Quotation();

        quotation.setQuotationNumber(
                cleanQuotationNumber
        );

        quotation.setCustomer(
                customer
        );

        quotation.setEmployee(
                employee
        );

        quotation.setBranch(
                branch
        );

        LocalDate effectiveQuotationDate = quotationDate != null
                ? quotationDate
                : LocalDate.now();
        quotation.setQuotationDate(effectiveQuotationDate);
        quotation.setValidUntil(effectiveQuotationDate.plusDays(365));

        quotation.setStatus(
                QuotationStatus.PENDING
        );

        quotation.setPricingMode(
                pricingMode
        );

        quotation.setCustomerRequirements(
                cleanText(customerRequirements)
        );

        quotation.setNotes(
                cleanText(notes)
        );

        // -----------------------------------------------------
        // Initialize financial values
        // -----------------------------------------------------

        quotation.setSubtotal(
                BigDecimal.ZERO
        );

        quotation.setAccessoriesAmount(
                BigDecimal.ZERO
        );

        quotation.setTravelCharge(
                BigDecimal.ZERO
        );

        quotation.setOtherCharges(
                BigDecimal.ZERO
        );

        quotation.setDiscountAmount(
                BigDecimal.ZERO
        );

        quotation.setTaxableAmount(
                BigDecimal.ZERO
        );

        quotation.setGstPercentage(
                BigDecimal.ZERO
        );

        quotation.setGstAmount(
                BigDecimal.ZERO
        );

        quotation.setTotalAmount(
                BigDecimal.ZERO
        );

        quotation.setOfferPercentage(
                BigDecimal.ZERO
        );

        quotation.setOfferAmount(
                BigDecimal.ZERO
        );

        quotation.setGrandTotal(
                BigDecimal.ZERO
        );

        // -----------------------------------------------------
        // Save quotation first
        // -----------------------------------------------------

        Quotation savedQuotation =
                quotationRepository.save(
                        quotation
                );

        // -----------------------------------------------------
        // WORKFLOW TRACKING
        // -----------------------------------------------------
        //
        // Record that this quotation was created.
        //
        // Example:
        //
        // Action      : CREATED
        // User        : Anish
        // Description : Quotation created.
        // New Value   : Quotation QT-00007
        //
        // -----------------------------------------------------

        quotationWorkflowService.record(
                savedQuotation,
                QuotationWorkflow.WorkflowAction.CREATED,
                savedQuotation.getEmployee(),
                "Quotation created.",
                null,
                "Quotation "
                        + savedQuotation.getQuotationNumber()
        );

        return savedQuotation;
    }

    // ---------------------------------------------------------
    // Update quotation basic information
    // ---------------------------------------------------------

    @Transactional
    public Quotation updateBasicDetails(
            Long id,
            Long customerId,
            Long employeeId,
            Long branchId,
            LocalDate quotationDate,
            PricingMode pricingMode,
            String customerRequirements,
            String notes
    ) {

        Quotation quotation =
                getById(id);

        // -----------------------------------------------------
        // Store old values for workflow tracking
        // -----------------------------------------------------

        String oldCustomer =
                quotation.getCustomer() != null
                        ? quotation.getCustomer().getName()
                        : null;

        String oldEmployee =
                quotation.getEmployee() != null
                        ? quotation.getEmployee().getName()
                        : null;

        String oldBranch =
                quotation.getBranch() != null
                        ? quotation.getBranch().getName()
                        : null;

        PricingMode oldPricingMode =
                quotation.getPricingMode();

        validateId(
                customerId,
                "Customer ID"
        );

        validateId(
                employeeId,
                "Employee ID"
        );

        validateId(
                branchId,
                "Branch ID"
        );

        if (pricingMode == null) {

            throw new IllegalArgumentException(
                    "Pricing mode is required."
            );
        }

        // -----------------------------------------------------
        // Find customer
        // -----------------------------------------------------

        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Customer not found."
                                )
                        );

        // -----------------------------------------------------
        // Find employee
        // -----------------------------------------------------

        User employee =
                userRepository
                        .findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        // -----------------------------------------------------
        // Find branch
        // -----------------------------------------------------

        Branch branch =
                branchRepository
                        .findById(branchId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Branch not found."
                                )
                        );

        // -----------------------------------------------------
        // Validate customer
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                customer.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected customer is inactive."
            );
        }

        // -----------------------------------------------------
        // Validate employee
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                employee.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected employee is inactive."
            );
        }

        // -----------------------------------------------------
        // Validate branch
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                branch.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected branch is inactive."
            );
        }

        // -----------------------------------------------------
        // Update quotation
        // -----------------------------------------------------

        quotation.setCustomer(
                customer
        );

        quotation.setEmployee(
                employee
        );

        quotation.setBranch(
                branch
        );

        // Preserve original quotation date and valid-until when updating existing quotation
        if (quotation.getQuotationDate() == null) {
            quotation.setQuotationDate(quotationDate != null ? quotationDate : LocalDate.now());
        }
        if (quotation.getValidUntil() == null && quotation.getQuotationDate() != null) {
            quotation.setValidUntil(quotation.getQuotationDate().plusDays(365));
        }

        quotation.setPricingMode(
                pricingMode
        );

        quotation.setCustomerRequirements(
                cleanText(customerRequirements)
        );

        quotation.setNotes(
                cleanText(notes)
        );

        Quotation updatedQuotation =
                quotationRepository.save(
                        quotation
                );

        // -----------------------------------------------------
        // WORKFLOW - CUSTOMER CHANGED
        // -----------------------------------------------------

        String newCustomer =
                updatedQuotation.getCustomer() != null
                        ? updatedQuotation.getCustomer().getName()
                        : null;

        if (!safeEquals(
                oldCustomer,
                newCustomer
        )) {

            quotationWorkflowService.record(
                    updatedQuotation,
                    QuotationWorkflow.WorkflowAction.CUSTOMER_CHANGED,
                    updatedQuotation.getEmployee(),
                    "Customer changed.",
                    oldCustomer,
                    newCustomer
            );
        }

        // -----------------------------------------------------
        // WORKFLOW - DISTRICT / BRANCH CHANGED
        // -----------------------------------------------------

        String newBranch =
                updatedQuotation.getBranch() != null
                        ? updatedQuotation.getBranch().getName()
                        : null;

        if (!safeEquals(
                oldBranch,
                newBranch
        )) {

            quotationWorkflowService.record(
                    updatedQuotation,
                    QuotationWorkflow.WorkflowAction.DISTRICT_CHANGED,
                    updatedQuotation.getEmployee(),
                    "District / branch changed.",
                    oldBranch,
                    newBranch
            );
        }

        // -----------------------------------------------------
        // WORKFLOW - EMPLOYEE CHANGED
        // -----------------------------------------------------

        String newEmployee =
                updatedQuotation.getEmployee() != null
                        ? updatedQuotation.getEmployee().getName()
                        : null;

        if (!safeEquals(
                oldEmployee,
                newEmployee
        )) {

            quotationWorkflowService.record(
                    updatedQuotation,
                    QuotationWorkflow.WorkflowAction.UPDATED,
                    updatedQuotation.getEmployee(),
                    "Quotation employee changed.",
                    oldEmployee,
                    newEmployee
            );
        }

        // -----------------------------------------------------
        // WORKFLOW - PRICING MODE CHANGED
        // -----------------------------------------------------

        PricingMode newPricingMode =
                updatedQuotation.getPricingMode();

        if (!safeEquals(
                oldPricingMode,
                newPricingMode
        )) {

            quotationWorkflowService.record(
                    updatedQuotation,
                    QuotationWorkflow.WorkflowAction.PRICING_MODE_CHANGED,
                    updatedQuotation.getEmployee(),
                    "Quotation type changed.",
                    oldPricingMode != null
                            ? oldPricingMode.name()
                            : null,
                    newPricingMode != null
                            ? newPricingMode.name()
                            : null
            );
        }

        // -----------------------------------------------------
        // GENERAL UPDATE
        // -----------------------------------------------------

        quotationWorkflowService.record(
                updatedQuotation,
                QuotationWorkflow.WorkflowAction.UPDATED,
                updatedQuotation.getEmployee(),
                "Quotation basic information updated.",
                null,
                null
        );

        return updatedQuotation;
    }

    // ---------------------------------------------------------
    // Change quotation status
    // ---------------------------------------------------------

    @Transactional
    public Quotation changeStatus(
            Long id,
            QuotationStatus status
    ) {

        Quotation quotation =
                getById(id);

        if (status == null) {

            throw new IllegalArgumentException(
                    "Quotation status is required."
            );
        }

        QuotationStatus oldStatus =
                quotation.getStatus();

        quotation.setStatus(
                status
        );

        Quotation updatedQuotation =
                quotationRepository.save(
                        quotation
                );

        // -----------------------------------------------------
        // WORKFLOW - STATUS CHANGE
        // -----------------------------------------------------

        if (!safeEquals(
                oldStatus,
                status
        )) {

            quotationWorkflowService.record(
                    updatedQuotation,
                    QuotationWorkflow.WorkflowAction.STATUS_CHANGED,
                    updatedQuotation.getEmployee(),
                    "Quotation status changed.",
                    oldStatus != null
                            ? oldStatus.name()
                            : null,
                    status.name()
            );
        }

        return updatedQuotation;
    }

    // ---------------------------------------------------------
    // Delete quotation (Admin)
    // ---------------------------------------------------------

    @Transactional
    public void delete(Long id) {
        delete(id, null);
    }

    // ---------------------------------------------------------
    // Delete quotation with permission verification
    // ---------------------------------------------------------

    @Transactional
    public void delete(Long id, Long employeeId) {

        if (id == null) {
            throw new IllegalArgumentException("Quotation ID is required.");
        }

        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found with id: " + id));

        // Enforce employee ownership permissions in the backend
        if (employeeId != null) {
            if (quotation.getEmployee() == null
                    || quotation.getEmployee().getId() == null
                    || !quotation.getEmployee().getId().equals(employeeId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorized to delete this quotation."
                );
            }
        }

        // Identify all quotation IDs that belong to this quotation hierarchy
        List<Long> targetQuotationIds = new java.util.ArrayList<>();
        targetQuotationIds.add(quotation.getId());

        List<Quotation> childQuotations = new java.util.ArrayList<>();
        if (quotation.getParent() == null) {
            // Parent quotation: find all child grade quotations
            List<Quotation> children = quotationRepository.findByParentIdOrderByIdAsc(quotation.getId());
            if (children != null && !children.isEmpty()) {
                childQuotations.addAll(children);
                for (Quotation child : children) {
                    if (child.getId() != null && !targetQuotationIds.contains(child.getId())) {
                        targetQuotationIds.add(child.getId());
                    }
                }
            }
        } else {
            // Child quotation being deleted independently: unlink from parent
            Quotation parent = quotation.getParent();
            if (parent != null && parent.getChildQuotations() != null) {
                parent.getChildQuotations().remove(quotation);
            }
            quotation.setParent(null);
            quotationRepository.save(quotation);
        }

        // 1. Delete all QuotationItemSize records for items belonging to these quotations
        for (Long qId : targetQuotationIds) {
            List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(qId);
            if (rooms != null) {
                for (QuotationRoom room : rooms) {
                    List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(room.getId());
                    if (items != null) {
                        for (QuotationItem item : items) {
                            List<QuotationItemSize> sizes = quotationItemSizeRepository.findByQuotationItemIdOrderByDisplayOrderAsc(item.getId());
                            if (sizes != null && !sizes.isEmpty()) {
                                quotationItemSizeRepository.deleteAll(sizes);
                            }
                        }
                    }
                }
            }
        }

        // 2. Delete all QuotationItem records
        for (Long qId : targetQuotationIds) {
            List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(qId);
            if (rooms != null) {
                for (QuotationRoom room : rooms) {
                    List<QuotationItem> items = quotationItemRepository.findByQuotationRoomId(room.getId());
                    if (items != null && !items.isEmpty()) {
                        quotationItemRepository.deleteAll(items);
                    }
                }
            }
        }

        // 3. Delete all QuotationRoom records
        for (Long qId : targetQuotationIds) {
            List<QuotationRoom> rooms = quotationRoomRepository.findByQuotationId(qId);
            if (rooms != null && !rooms.isEmpty()) {
                quotationRoomRepository.deleteAll(rooms);
            }
        }

        // 4. Delete communication records for ALL target quotation IDs
        for (Long qId : targetQuotationIds) {
            List<QuotationCommunication> communications = quotationCommunicationRepository.findByQuotationIdOrderBySentAtDesc(qId);
            if (communications != null && !communications.isEmpty()) {
                quotationCommunicationRepository.deleteAll(communications);
            }
        }

        // 5. Delete status history records for ALL target quotation IDs
        for (Long qId : targetQuotationIds) {
            List<QuotationStatusHistory> history = quotationStatusHistoryRepository.findByQuotationIdOrderByIdDesc(qId);
            if (history != null && !history.isEmpty()) {
                quotationStatusHistoryRepository.deleteAll(history);
            }
        }

        // 6. Delete workflow records for ALL target quotation IDs
        for (Long qId : targetQuotationIds) {
            List<QuotationWorkflow> workflows = quotationWorkflowRepository.findByQuotationIdOrderByCreatedAtDesc(qId);
            if (workflows != null && !workflows.isEmpty()) {
                quotationWorkflowRepository.deleteAll(workflows);
            }
        }

        // 7. Flush repository deletes before executing native cleanups
        try {
            quotationRoomRepository.flush();
            quotationCommunicationRepository.flush();
            quotationStatusHistoryRepository.flush();
            quotationWorkflowRepository.flush();
        } catch (Exception ignored) {}

        // 8. Execute native SQL deletes in exact foreign-key order
        // This handles unmapped legacy tables (quotation_grade_options & quotation_grade_confirmation_history)
        // and guarantees foreign key constraints are cleanly satisfied in the database engine
        if (entityManager != null && !targetQuotationIds.isEmpty()) {
            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_item_sizes WHERE quotation_item_id IN (" +
                    "  SELECT id FROM quotation_items WHERE quotation_room_id IN (" +
                    "    SELECT id FROM quotation_rooms WHERE quotation_id IN (:ids)" +
                    "  )" +
                    ")"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_items WHERE quotation_room_id IN (" +
                    "  SELECT id FROM quotation_rooms WHERE quotation_id IN (:ids)" +
                    ")"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_rooms WHERE quotation_id IN (:ids)"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_grade_options WHERE quotation_id IN (:ids)"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_grade_confirmation_history WHERE quotation_id IN (:ids)"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_communications WHERE quotation_id IN (:ids)"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_status_history WHERE quotation_id IN (:ids)"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.createNativeQuery(
                    "DELETE FROM quotation_workflow WHERE quotation_id IN (:ids)"
                ).setParameter("ids", targetQuotationIds).executeUpdate();
            } catch (Exception ignored) {}

            try {
                entityManager.flush();
            } catch (Exception ignored) {}
        }

        // 9. Delete child quotations if this is a parent quotation
        if (!childQuotations.isEmpty()) {
            for (Quotation child : childQuotations) {
                child.setParent(null);
                quotationRepository.delete(child);
            }
            quotationRepository.flush();
        }

        // 10. Delete the parent quotation record
        quotationRepository.delete(quotation);
        quotationRepository.flush();
    }

    // ---------------------------------------------------------
    // Dashboard counts
    // ---------------------------------------------------------

    public long getTotalQuotationCount() {

        return quotationRepository.count();
    }

    public long getApprovedQuotationCount() {

        return quotationRepository.countByStatus(
                QuotationStatus.SENT
        );
    }

    public long getWorkingQuotationCount() {

        return quotationRepository.countByStatus(
                QuotationStatus.WORKING
        );
    }

    public long getPendingQuotationCount() {

        return quotationRepository.countByStatus(
                QuotationStatus.PENDING
        );
    }

    public long getSentQuotationCount() {

        return quotationRepository.countByStatus(
                QuotationStatus.SENT
        );
    }

    public long getLast60DaysQuotationCount() {

        LocalDate today =
                LocalDate.now();

        LocalDate startDate =
                today.minusDays(59);

        return quotationRepository
                .countByQuotationDateBetween(
                        startDate,
                        today
                );
    }

    public long getOneYearQuotationCount() {

        LocalDate today =
                LocalDate.now();

        LocalDate startDate =
                today.minusYears(1);

        return quotationRepository
                .countByQuotationDateBetween(
                        startDate,
                        today
                );
    }

    public long getLast60DaysQuotationCountByBranch(
            Long branchId
    ) {

        validateId(
                branchId,
                "Branch ID"
        );

        LocalDate today =
                LocalDate.now();

        LocalDate startDate =
                today.minusDays(59);

        return quotationRepository
                .countByBranchIdAndQuotationDateBetween(
                        branchId,
                        startDate,
                        today
                );
    }

    // ---------------------------------------------------------
    // Save quotation
    // ---------------------------------------------------------

    @Transactional
    public Quotation save(
            Quotation quotation
    ) {

        if (quotation == null) {

            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        return quotationRepository.save(
                quotation
        );
    }

    // ---------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------

    private void validateId(
            Long id,
            String fieldName
    ) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }
    }

    private void validateQuotationNumber(
            String quotationNumber
    ) {

        if (quotationNumber == null
                || quotationNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "Quotation number is required."
            );
        }

        if (quotationNumber.length() > 50) {

            throw new IllegalArgumentException(
                    "Quotation number cannot exceed 50 characters."
            );
        }
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null) {

            throw new IllegalArgumentException(
                    "Start date is required."
            );
        }

        if (endDate == null) {

            throw new IllegalArgumentException(
                    "End date is required."
            );
        }

        if (startDate.isAfter(endDate)) {

            throw new IllegalArgumentException(
                    "Start date cannot be after end date."
            );
        }
    }

    private String cleanText(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    // ---------------------------------------------------------
    // Safe comparison helper
    // ---------------------------------------------------------

    private boolean safeEquals(
            Object oldValue,
            Object newValue
    ) {

        if (oldValue == null
                && newValue == null) {

            return true;
        }

        if (oldValue == null
                || newValue == null) {

            return false;
        }

        return oldValue.equals(
                newValue
        );
    }

    @Transactional
    public Quotation confirmCustomerGrade(Long parentQuotationId, PricingMode grade, String loggedInEmail) {
        if (parentQuotationId == null) {
            throw new IllegalArgumentException("Quotation ID is required.");
        }
        if (grade == null) {
            throw new IllegalArgumentException("Customer selected grade is required.");
        }
        Quotation parent = getById(parentQuotationId);
        if (parent.getParent() != null) {
            parent = parent.getParent();
        }
        Quotation child = parent.getChildForGrade(grade);
        if (child == null && !parent.isParentQuotation()) {
            if (parent.getPricingMode() != grade) {
                throw new IllegalArgumentException("Grade " + grade + " has not been prepared for this quotation.");
            }
        } else if (child == null) {
            throw new IllegalArgumentException("Grade " + grade + " has not been prepared for this quotation.");
        }

        parent.setSelectedGrade(grade);
        return quotationRepository.save(parent);
    }
}