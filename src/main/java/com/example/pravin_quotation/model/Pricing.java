package com.example.pravin_quotation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pricing",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pricing_option_mode",
                        columnNames = {"material_option_id", "pricing_mode"}
                )
        }
)
public class Pricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Material option for which this price is defined.
     *
     * Example:
     * Marine Ply -> 18mm
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_option_id", nullable = false)
    private MaterialOption materialOption;

    /*
     * Pricing level:
     *
     * PREMIUM
     * MIDDLE
     * ECONOMY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 30)
    private PricingMode pricingMode;

    /*
     * Base rate before district-specific adjustments.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal rate;

    /*
     * Unit used for calculation.
     *
     * Examples:
     * SQFT
     * NOS
     * RUNNING_FEET
     * LUMP_SUM
     */
    @Column(nullable = false, length = 30)
    private String unit;

    /*
     * Optional description/specification for the rate.
     */
    @Column(length = 500)
    private String description;

    /*
     * Allows the admin to enable/disable a pricing record
     * without deleting historical pricing information.
     */
    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Pricing() {
    }

    public Pricing(
            Long id,
            MaterialOption materialOption,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String description,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.materialOption = materialOption;
        this.pricingMode = pricingMode;
        this.rate = rate;
        this.unit = unit;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MaterialOption getMaterialOption() {
        return materialOption;
    }

    public void setMaterialOption(MaterialOption materialOption) {
        this.materialOption = materialOption;
    }

    public PricingMode getPricingMode() {
        return pricingMode;
    }

    public void setPricingMode(PricingMode pricingMode) {
        this.pricingMode = pricingMode;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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