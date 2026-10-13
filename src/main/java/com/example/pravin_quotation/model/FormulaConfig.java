package com.example.pravin_quotation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "formula_configs")
public class FormulaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String expression;

    /**
     * Calculation Type: "SQFT" or "INCHES"
     */
    @Column(name = "calculation_type", nullable = false, length = 30)
    private String calculationType = "SQFT";

    /**
     * Unit Type: "AREA" (Square dimensions) or "LINEAR" (Linear dimensions)
     */
    @Column(name = "unit_type", nullable = false, length = 30)
    private String unitType = "AREA";

    /**
     * Measurement Unit displayed for calculated value: "SQFT", "SQIN", "INCH"
     */
    @Column(name = "measurement_unit", nullable = false, length = 30)
    private String measurementUnit = "SQFT";

    /**
     * Pricing Unit displayed for rate: "₹ / SQFT", "₹ / SQIN", "₹ / INCH"
     */
    @Column(name = "pricing_unit", nullable = false, length = 50)
    private String pricingUnit = "₹ / SQFT";

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FormulaConfig() {
    }

    public FormulaConfig(
            String name,
            String expression,
            String calculationType,
            String unitType,
            String measurementUnit,
            String pricingUnit,
            String description,
            Integer displayOrder,
            Boolean isSystem
    ) {
        this.name = name;
        this.expression = expression;
        this.calculationType = calculationType;
        this.unitType = unitType;
        this.measurementUnit = measurementUnit;
        this.pricingUnit = pricingUnit;
        this.description = description;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isSystem = isSystem != null ? isSystem : false;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (active == null) {
            active = true;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (isSystem == null) {
            isSystem = false;
        }
        if (calculationType == null) {
            calculationType = "SQFT";
        }
        if (unitType == null) {
            unitType = "AREA";
        }
        if (measurementUnit == null) {
            measurementUnit = "SQFT".equalsIgnoreCase(calculationType) ? "SQFT" : "SQIN";
        }
        if (pricingUnit == null) {
            pricingUnit = "₹ / " + measurementUnit;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCalculationType() {
        return calculationType;
    }

    public void setCalculationType(String calculationType) {
        this.calculationType = calculationType;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public String getPricingUnit() {
        return pricingUnit;
    }

    public void setPricingUnit(String pricingUnit) {
        this.pricingUnit = pricingUnit;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
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
