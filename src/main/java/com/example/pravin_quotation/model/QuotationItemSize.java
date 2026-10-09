package com.example.pravin_quotation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_item_sizes")
public class QuotationItemSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // PARENT QUOTATION ITEM
    // =========================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "quotation_item_id",
            nullable = false
    )
    private QuotationItem quotationItem;


    // =========================================================
    // DIMENSIONS
    // =========================================================

    @Column(
            name = "length_value",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal lengthValue;


    @Column(
            name = "width_value",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal widthValue;


    @Column(
            name = "height_value",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal heightValue;


    // =========================================================
    // CALCULATED SQ.FT
    // =========================================================

    @Column(
            name = "calculated_sqft",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal calculatedSqft = BigDecimal.ZERO;


    // =========================================================
    // OFFER PRICE
    // =========================================================

    @Column(
            name = "offer_price",
            precision = 15,
            scale = 2
    )
    private BigDecimal offerPrice = BigDecimal.ZERO;


    // =========================================================
    // DISPLAY ORDER
    // =========================================================

    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 0;


    // =========================================================
    // ACTIVE
    // =========================================================

    @Column(
            nullable = false
    )
    private Boolean active = true;


    // =========================================================
    // TIMESTAMPS
    // =========================================================

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at"
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public QuotationItemSize() {
    }


    // =========================================================
    // PRE PERSIST
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (calculatedSqft == null) {
            calculatedSqft = BigDecimal.ZERO;
        }

        if (offerPrice == null) {
            offerPrice = BigDecimal.ZERO;
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (active == null) {
            active = true;
        }
    }


    // =========================================================
    // PRE UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public QuotationItem getQuotationItem() {
        return quotationItem;
    }

    public void setQuotationItem(QuotationItem quotationItem) {
        this.quotationItem = quotationItem;
    }


    public BigDecimal getLengthValue() {
        return lengthValue;
    }

    public void setLengthValue(BigDecimal lengthValue) {
        this.lengthValue = lengthValue;
    }


    public BigDecimal getWidthValue() {
        return widthValue;
    }

    public void setWidthValue(BigDecimal widthValue) {
        this.widthValue = widthValue;
    }


    public BigDecimal getHeightValue() {
        return heightValue;
    }

    public void setHeightValue(BigDecimal heightValue) {
        this.heightValue = heightValue;
    }


    public BigDecimal getCalculatedSqft() {
        return calculatedSqft;
    }

    public void setCalculatedSqft(BigDecimal calculatedSqft) {
        this.calculatedSqft = calculatedSqft;
    }


    public BigDecimal getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(BigDecimal offerPrice) {
        this.offerPrice = offerPrice;
    }


    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }


    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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