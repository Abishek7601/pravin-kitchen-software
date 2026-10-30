package com.example.pravin_quotation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "materials"
)
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "formula", length = 50)
    private String formula = "(L + W) × H";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = true)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id", nullable = true)
    private Division division;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_grade", length = 30)
    private PricingMode pricingGrade;

    @Column(name = "rate", precision = 15, scale = 2)
    private BigDecimal rate;

    @Column(name = "unit", length = 30)
    private String unit = "Sq.ft";

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", length = 30)
    private MeasurementType measurementType = MeasurementType.SQFT;

    @Column(name = "sqft_enabled")
    private Boolean sqftEnabled = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "specification", columnDefinition = "TEXT")
    private String specification;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "division_materials",
            joinColumns = @JoinColumn(name = "material_id"),
            inverseJoinColumns = @JoinColumn(name = "division_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_division_material",
                            columnNames = {"division_id", "material_id"}
                    )
            }
    )
    private Set<Division> divisions = new HashSet<>();

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Material() {
    }

    public Material(
            Long id,
            String name,
            String description,
            Item item,
            Boolean active,
            Integer displayOrder,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.item = item;
        this.active = active;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.formula = "(L + W) × H";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (active == null) {
            active = true;
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (name != null) {
            name = name.trim().toUpperCase();
        }

        if (specification != null && !specification.isBlank()) {
            specification = com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(specification.trim());
        }

        if (description != null && !description.isBlank()) {
            description = com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(description.trim());
        }

        if (measurementType == null || measurementType.isSqft()) {
            if (formula == null || formula.isBlank()) {
                formula = "(L + W) × H";
            }
        } else {
            formula = null;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (name != null) {
            name = name.trim().toUpperCase();
        }

        if (specification != null && !specification.isBlank()) {
            specification = com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(specification.trim());
        }

        if (description != null && !description.isBlank()) {
            description = com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(description.trim());
        }

        if (measurementType == null || measurementType.isSqft()) {
            if (formula == null || formula.isBlank()) {
                formula = "(L + W) × H";
            }
        } else {
            formula = null;
        }
    }

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
        this.name = name != null ? name.trim().toUpperCase() : null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = (description != null && !description.isBlank())
                ? com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(description.trim())
                : description;
    }

    public String getFormula() {
        if (measurementType == MeasurementType.QUANTITY || measurementType == MeasurementType.FILES) {
            return formula != null && !formula.isBlank() ? formula : null;
        }
        return (formula != null && !formula.isBlank()) ? formula : "(L + W) × H";
    }

    public String getCalculationFormula() {
        return getFormula();
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Division getDivision() {
        if (division != null) {
            return division;
        }
        if (divisions != null && !divisions.isEmpty()) {
            return divisions.iterator().next();
        }
        if (item != null && item.getDivision() != null) {
            return item.getDivision();
        }
        return null;
    }

    public void setDivision(Division division) {
        this.division = division;
        if (division != null) {
            addDivision(division);
        }
    }

    public PricingMode getPricingGrade() {
        return pricingGrade;
    }

    public void setPricingGrade(PricingMode pricingGrade) {
        this.pricingGrade = pricingGrade;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getUnit() {
        return (unit != null && !unit.isBlank()) ? unit : "Sq.ft";
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public MeasurementType getMeasurementType() {
        return measurementType != null ? measurementType : MeasurementType.SQFT;
    }

    public void setMeasurementType(MeasurementType measurementType) {
        this.measurementType = measurementType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getSqftEnabled() {
        return sqftEnabled != null ? sqftEnabled : Boolean.TRUE;
    }

    public boolean isSqftEnabled() {
        return Boolean.TRUE.equals(sqftEnabled);
    }

    public void setSqftEnabled(Boolean sqftEnabled) {
        this.sqftEnabled = sqftEnabled != null ? sqftEnabled : Boolean.TRUE;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = (specification != null && !specification.isBlank())
                ? com.example.pravin_quotation.util.TitleCaseUtils.toTitleCase(specification.trim())
                : specification;
    }

    public Set<Division> getDivisions() {
        return divisions;
    }

    public void setDivisions(Set<Division> divisions) {
        this.divisions = divisions;
    }

    public void addDivision(Division division) {
        if (division != null) {
            this.divisions.add(division);
        }
    }

    public void removeDivision(Division division) {
        if (division != null) {
            this.divisions.remove(division);
        }
    }

    public List<String> getDivisionNames() {
        if (division != null) {
            return List.of(division.getName());
        }
        if (divisions == null || divisions.isEmpty()) {
            if (item != null && item.getDivision() != null) {
                return List.of(item.getDivision().getName());
            }
            return Collections.emptyList();
        }
        return divisions.stream().map(Division::getName).sorted().toList();
    }

    public String getDivisionNamesString() {
        List<String> names = getDivisionNames();
        if (names == null || names.isEmpty()) return "-";
        return String.join(", ", names);
    }

    public boolean hasDivision(Long divisionId) {
        if (divisionId == null) return false;
        if (division != null && divisionId.equals(division.getId())) {
            return true;
        }
        if (divisions != null) {
            for (Division d : divisions) {
                if (d != null && divisionId.equals(d.getId())) {
                    return true;
                }
            }
        }
        if (item != null && item.getDivision() != null && divisionId.equals(item.getDivision().getId())) {
            return true;
        }
        return false;
    }

    public Boolean getActive() {
        return active;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}