package com.example.pravin_quotation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "quotations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_quotation_number",
                        columnNames = "quotation_number"
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
    @JoinColumn(name = "customer_id", nullable = false)
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
    private QuotationStatus status = QuotationStatus.DRAFT;

    /*
     * Pricing mode selected for this quotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 20)
    private PricingMode pricingMode;

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

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", precision = 15, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 15, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 15, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    /*
     * Customer requirements and internal notes.
     */
    @Column(name = "customer_requirements", columnDefinition = "TEXT")
    private String customerRequirements;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

        if (status == null) {
            status = QuotationStatus.DRAFT;
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
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
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
}