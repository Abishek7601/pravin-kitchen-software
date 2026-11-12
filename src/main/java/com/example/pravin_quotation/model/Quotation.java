package com.example.pravin_quotation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "quotations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_quotation_number",
                        columnNames = "quotation_number"
                ),
                @UniqueConstraint(
                        name = "uk_quotation_parent_grade",
                        columnNames = {"parent_id", "pricing_mode"}
                )
        }
)
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Business quotation number.
     * Example: PK-2026-000001
     */
    @Column(
            name = "quotation_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String quotationNumber;

    /*
     * Customer for whom this quotation is created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    /*
     * Employee who created the quotation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    /*
     * Branch represents the district in our system.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /*
     * Division selected for this quotation.
     * Example: Marketing
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id")
    private Division division;

    /*
     * Date shown on the quotation.
     */
    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;

    /*
     * Current quotation workflow status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuotationStatus status = QuotationStatus.PENDING;

    /*
     * Pricing mode selected for this quotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 20)
    private PricingMode pricingMode;

    @Transient
    private String pricingModeDisplay;

    /*
     * Parent quotation for multi-grade quotations.
     * Null for parent quotations and legacy single-grade quotations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Quotation parent;

    /*
     * Child quotations representing independent grade estimates (Premium, Middle, Economy).
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<Quotation> childQuotations = new ArrayList<>();

    /*
     * Customer's confirmed/selected grade for this quotation project.
     * Stored on the parent quotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "selected_grade", length = 20)
    private PricingMode selectedGrade;

    /*
     * Financial calculations.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "accessories_amount", precision = 15, scale = 2)
    private BigDecimal accessoriesAmount = BigDecimal.ZERO;

    @Column(name = "travel_charge", precision = 15, scale = 2)
    private BigDecimal travelCharge = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 15, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "other_charges_description", length = 500)
    private String otherChargesDescription;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", precision = 15, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 15, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "offer_percentage", precision = 5, scale = 2)
    private BigDecimal offerPercentage = BigDecimal.ZERO;

    @Column(name = "offer_amount", precision = 15, scale = 2)
    private BigDecimal offerAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 15, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    /*
     * Customer requirements and internal notes.
     */
    @Column(name = "customer_requirements", columnDefinition = "TEXT")
    private String customerRequirements;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "show_sqft_size")
    private Boolean showSqftSize = true;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    /*
     * Auto-save and resume progress fields.
     */
    @Column(name = "last_active_step", length = 50)
    private String lastActiveStep;

    @Column(name = "last_active_floor", length = 100)
    private String lastActiveFloor;

    @Column(name = "last_active_room", length = 100)
    private String lastActiveRoom;

    @Column(name = "last_active_category_id")
    private Long lastActiveCategoryId;

    @Column(name = "last_active_division_id")
    private Long lastActiveDivisionId;

    @Column(name = "builder_state", columnDefinition = "LONGTEXT")
    private String builderState;

    /*
     * Audit timestamps.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------

    public Quotation() {
    }

    public Quotation(
            Long id,
            String quotationNumber,
            Customer customer,
            User employee,
            Branch branch,
            Division division,
            LocalDate quotationDate,
            QuotationStatus status,
            PricingMode pricingMode,
            BigDecimal subtotal,
            BigDecimal accessoriesAmount,
            BigDecimal travelCharge,
            BigDecimal otherCharges,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal gstPercentage,
            BigDecimal gstAmount,
            BigDecimal grandTotal,
            String customerRequirements,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.quotationNumber = quotationNumber;
        this.customer = customer;
        this.employee = employee;
        this.branch = branch;
        this.division = division;
        this.quotationDate = quotationDate;
        this.status = status;
        this.pricingMode = pricingMode;
        this.subtotal = subtotal;
        this.accessoriesAmount = accessoriesAmount;
        this.travelCharge = travelCharge;
        this.otherCharges = otherCharges;
        this.discountAmount = discountAmount;
        this.taxableAmount = taxableAmount;
        this.gstPercentage = gstPercentage;
        this.gstAmount = gstAmount;
        this.grandTotal = grandTotal;
        this.customerRequirements = customerRequirements;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    // ---------------------------------------------------------
    // JPA lifecycle
    // ---------------------------------------------------------

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (quotationDate == null) {
            quotationDate = LocalDate.now();
        }

        if (validUntil == null) {
            validUntil = quotationDate.plusDays(365);
        }

        if (status == null) {
            status = QuotationStatus.PENDING;
        }

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (accessoriesAmount == null) {
            accessoriesAmount = BigDecimal.ZERO;
        }

        if (travelCharge == null) {
            travelCharge = BigDecimal.ZERO;
        }

        if (otherCharges == null) {
            otherCharges = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        if (taxableAmount == null) {
            taxableAmount = BigDecimal.ZERO;
        }

        if (gstPercentage == null) {
            gstPercentage = BigDecimal.ZERO;
        }

        if (gstAmount == null) {
            gstAmount = BigDecimal.ZERO;
        }

        if (totalAmount == null) {
            totalAmount = grandTotal != null ? grandTotal : BigDecimal.ZERO;
        }

        if (offerPercentage == null) {
            offerPercentage = BigDecimal.ZERO;
        }

        if (offerAmount == null) {
            offerAmount = BigDecimal.ZERO;
        }

        if (grandTotal == null) {
            grandTotal = BigDecimal.ZERO;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    // ---------------------------------------------------------
    // Getters and Setters
    // ---------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuotationNumber() {
        if (this.parent != null && this.parent.getQuotationNumber() != null) {
            return this.parent.getQuotationNumber();
        }
        return quotationNumber;
    }

    public String getRawQuotationNumber() {
        return quotationNumber;
    }

    public String getDisplayQuotationNumber() {
        return getQuotationNumber();
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public Quotation getParent() {
        return parent;
    }

    public void setParent(Quotation parent) {
        this.parent = parent;
    }

    public List<Quotation> getChildQuotations() {
        return childQuotations;
    }

    public void setChildQuotations(List<Quotation> childQuotations) {
        this.childQuotations = childQuotations;
    }

    public PricingMode getSelectedGrade() {
        return selectedGrade;
    }

    public void setSelectedGrade(PricingMode selectedGrade) {
        this.selectedGrade = selectedGrade;
    }

    public boolean isParentQuotation() {
        return this.parent == null && this.childQuotations != null && !this.childQuotations.isEmpty();
    }

    public boolean isChildQuotation() {
        return this.parent != null;
    }

    public boolean isMultiGrade() {
        return isParentQuotation() || isChildQuotation();
    }

    public Quotation getChildForGrade(PricingMode mode) {
        if (mode == null) return null;
        if (this.childQuotations != null) {
            for (Quotation child : this.childQuotations) {
                if (child.getPricingMode() == mode) {
                    return child;
                }
            }
        }
        return null;
    }

    public boolean hasGrade(PricingMode mode) {
        return getChildForGrade(mode) != null;
    }

    public BigDecimal getEffectiveGrandTotal() {
        if (this.parent != null) {
            return this.grandTotal;
        }
        if (isParentQuotation()) {
            if (this.selectedGrade != null) {
                Quotation selectedChild = getChildForGrade(this.selectedGrade);
                if (selectedChild != null && selectedChild.getGrandTotal() != null) {
                    return selectedChild.getGrandTotal();
                }
            }
            return null; // Not confirmed yet
        }
        // Legacy single-grade quotation
        return this.grandTotal;
    }

    public PricingMode getEffectivePricingMode() {
        if (isParentQuotation()) {
            return this.selectedGrade;
        }
        return this.pricingMode;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public LocalDate getQuotationDate() {
        return quotationDate;
    }

    public void setQuotationDate(LocalDate quotationDate) {
        this.quotationDate = quotationDate;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public PricingMode getPricingMode() {
        return pricingMode;
    }

    public void setPricingMode(PricingMode pricingMode) {
        this.pricingMode = pricingMode;
    }

    public String getPricingModeDisplay() {
        if (pricingModeDisplay != null && !pricingModeDisplay.trim().isEmpty()) {
            return pricingModeDisplay;
        }
        if (pricingMode == PricingMode.PREMIUM) {
            return "Premium";
        } else if (pricingMode == PricingMode.ECONOMY) {
            return "Economy";
        } else {
            return "Middle / Standard";
        }
    }

    public void setPricingModeDisplay(String pricingModeDisplay) {
        this.pricingModeDisplay = pricingModeDisplay;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getAccessoriesAmount() {
        return accessoriesAmount;
    }

    public void setAccessoriesAmount(BigDecimal accessoriesAmount) {
        this.accessoriesAmount = accessoriesAmount;
    }

    public BigDecimal getTravelCharge() {
        return travelCharge;
    }

    public void setTravelCharge(BigDecimal travelCharge) {
        this.travelCharge = travelCharge;
    }

    public BigDecimal getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(BigDecimal otherCharges) {
        this.otherCharges = otherCharges;
    }

    public String getOtherChargesDescription() {
        if (otherChargesDescription == null) {
            return null;
        }
        String trimmed = otherChargesDescription.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        char first = trimmed.charAt(0);
        if (Character.isLowerCase(first)) {
            return Character.toUpperCase(first) + trimmed.substring(1);
        }
        return trimmed;
    }

    public void setOtherChargesDescription(String otherChargesDescription) {
        if (otherChargesDescription == null) {
            this.otherChargesDescription = null;
            return;
        }
        String trimmed = otherChargesDescription.trim();
        if (trimmed.isEmpty()) {
            this.otherChargesDescription = null;
            return;
        }
        char first = trimmed.charAt(0);
        if (Character.isLowerCase(first)) {
            this.otherChargesDescription = Character.toUpperCase(first) + trimmed.substring(1);
        } else {
            this.otherChargesDescription = trimmed;
        }
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getGstPercentage() {
        return gstPercentage;
    }

    public void setGstPercentage(BigDecimal gstPercentage) {
        this.gstPercentage = gstPercentage;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getTotalAmount() {
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmount;
        }
        if (grandTotal != null && grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            if (offerAmount != null && offerAmount.compareTo(BigDecimal.ZERO) > 0) {
                return grandTotal.add(offerAmount);
            }
            return grandTotal;
        }
        return BigDecimal.ZERO;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getOfferPercentage() {
        return offerPercentage != null ? offerPercentage : BigDecimal.ZERO;
    }

    public void setOfferPercentage(BigDecimal offerPercentage) {
        this.offerPercentage = offerPercentage;
    }

    public BigDecimal getOfferAmount() {
        return offerAmount != null ? offerAmount : BigDecimal.ZERO;
    }

    public void setOfferAmount(BigDecimal offerAmount) {
        this.offerAmount = offerAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getCustomerRequirements() {
        return customerRequirements;
    }

    public void setCustomerRequirements(String customerRequirements) {
        this.customerRequirements = customerRequirements;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getShowSqftSize() {
        return showSqftSize;
    }

    public void setShowSqftSize(Boolean showSqftSize) {
        this.showSqftSize = showSqftSize;
    }

    public LocalDate getValidUntil() {
        if (validUntil != null) {
            return validUntil;
        }
        if (quotationDate != null) {
            return quotationDate.plusDays(365);
        }
        return null;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastActiveStep() {
        return lastActiveStep;
    }

    public void setLastActiveStep(String lastActiveStep) {
        this.lastActiveStep = lastActiveStep;
    }

    public String getLastActiveFloor() {
        return lastActiveFloor;
    }

    public void setLastActiveFloor(String lastActiveFloor) {
        this.lastActiveFloor = lastActiveFloor;
    }

    public String getLastActiveRoom() {
        return lastActiveRoom;
    }

    public void setLastActiveRoom(String lastActiveRoom) {
        this.lastActiveRoom = lastActiveRoom;
    }

    public Long getLastActiveCategoryId() {
        return lastActiveCategoryId;
    }

    public void setLastActiveCategoryId(Long lastActiveCategoryId) {
        this.lastActiveCategoryId = lastActiveCategoryId;
    }

    public Long getLastActiveDivisionId() {
        return lastActiveDivisionId;
    }

    public void setLastActiveDivisionId(Long lastActiveDivisionId) {
        this.lastActiveDivisionId = lastActiveDivisionId;
    }

    public String getBuilderState() {
        return builderState;
    }

    public void setBuilderState(String builderState) {
        this.builderState = builderState;
    }
}