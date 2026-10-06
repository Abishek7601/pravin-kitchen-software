package com.example.pravin_quotation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_workflow")
public class QuotationWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------------------------------------------------
    // Quotation
    // ---------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    // ---------------------------------------------------------
    // Action type
    // ---------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowAction action;

    // ---------------------------------------------------------
    // User who performed the action
    // ---------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    // ---------------------------------------------------------
    // Description
    // ---------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String description;

    // ---------------------------------------------------------
    // Old value
    // ---------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    // ---------------------------------------------------------
    // New value
    // ---------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String newValue;

    // ---------------------------------------------------------
    // Date & time
    // ---------------------------------------------------------

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------

    public QuotationWorkflow() {
    }

    public QuotationWorkflow(
            Quotation quotation,
            WorkflowAction action,
            User performedBy,
            String description,
            String oldValue,
            String newValue,
            LocalDateTime createdAt
    ) {
        this.quotation = quotation;
        this.action = action;
        this.performedBy = performedBy;
        this.description = description;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = createdAt;
    }

    // ---------------------------------------------------------
    // Getters & Setters
    // ---------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quotation getQuotation() {
        return quotation;
    }

    public void setQuotation(Quotation quotation) {
        this.quotation = quotation;
    }

    public WorkflowAction getAction() {
        return action;
    }

    public void setAction(WorkflowAction action) {
        this.action = action;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(User performedBy) {
        this.performedBy = performedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ---------------------------------------------------------
    // Workflow Actions
    // ---------------------------------------------------------

    public enum WorkflowAction {

        CREATED,

        UPDATED,

        ROOM_ADDED,

        DELETED,

        ROOM_UPDATED,

        ROOM_DELETED,

        ITEM_ADDED,

        ITEM_UPDATED,

        ITEM_DELETED,

        PRICE_CHANGED,

        CUSTOMER_CHANGED,

        DISTRICT_CHANGED,

        DIVISION_CHANGED,

        PRICING_MODE_CHANGED,

        STATUS_CHANGED,

        PDF_GENERATED,

        WHATSAPP_SENT,

        EMAIL_SENT,

        APPROVED,

        REJECTED,

        CANCELLED,

        VIEWED
    }

    // ---------------------------------------------------------
    // Auto date
    // ---------------------------------------------------------

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}