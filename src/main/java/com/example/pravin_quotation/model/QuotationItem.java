package com.example.pravin_quotation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotation_items")
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_room_id", nullable = false)
    private QuotationRoom quotationRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_category_id")
    private WorkCategory workCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id")
    private Division division;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_option_id")
    private MaterialOption materialOption;

    @OneToMany(
            mappedBy = "quotationItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    private List<QuotationItemSize> sizes = new ArrayList<>();

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "length_value", precision = 10, scale = 2)
    private BigDecimal lengthValue;

    @Column(name = "width_value", precision = 10, scale = 2)
    private BigDecimal widthValue;

    @Column(name = "height_value", precision = 10, scale = 2)
    private BigDecimal heightValue;

    @Column(name = "calculated_sqft", precision = 15, scale = 2)
    private BigDecimal calculatedSqft = BigDecimal.ZERO;

    @Column(name = "rate", precision = 15, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "offer_price", precision = 15, scale = 2)
    private BigDecimal offerPrice = BigDecimal.ZERO;

    @Column(name = "formula")
    private String formula;

    @Column(name = "specification", columnDefinition = "TEXT")
    private String specification;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public QuotationItem() {
    }


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

        if (rate == null) {
            rate = BigDecimal.ZERO;
        }

        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        if (offerPrice == null) {
            offerPrice = BigDecimal.ZERO;
        }

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


    public QuotationRoom getQuotationRoom() {
        return quotationRoom;
    }

    public void setQuotationRoom(QuotationRoom quotationRoom) {
        this.quotationRoom = quotationRoom;
    }


    public WorkCategory getWorkCategory() {
        return workCategory;
    }

    public void setWorkCategory(WorkCategory workCategory) {
        this.workCategory = workCategory;
    }


    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }


    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }


    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }


    public MaterialOption getMaterialOption() {
        return materialOption;
    }

    public void setMaterialOption(MaterialOption materialOption) {
        this.materialOption = materialOption;
    }

    public void addSize(QuotationItemSize size) {

        if (size == null) {
            return;
        }

        sizes.add(size);
        size.setQuotationItem(this);
    }


    public void removeSize(QuotationItemSize size) {

        if (size == null) {
            return;
        }

        sizes.remove(size);
        size.setQuotationItem(null);
    }

    public List<QuotationItemSize> getSizes() {
        return sizes;
    }

    public void setSizes(List<QuotationItemSize> sizes) {
        this.sizes = sizes;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
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


    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public BigDecimal getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(BigDecimal offerPrice) {
        this.offerPrice = offerPrice;
    }


    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }


    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
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