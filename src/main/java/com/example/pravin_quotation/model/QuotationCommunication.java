package com.example.pravin_quotation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_communications")
public class QuotationCommunication {

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
    // Communication type
    // ---------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunicationType type;

    // ---------------------------------------------------------
    // Recipient
    // ---------------------------------------------------------

    @Column(length = 255)
    private String recipient;

    // ---------------------------------------------------------
    // Status
    // ---------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunicationStatus status;

    // ---------------------------------------------------------
    // Message / details
    // ---------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String message;

    // ---------------------------------------------------------
    // Date & time
    // ---------------------------------------------------------

    @Column(nullable = false)
    private LocalDateTime sentAt;

    // ---------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------

    public QuotationCommunication() {
    }

    public QuotationCommunication(
            Quotation quotation,
            CommunicationType type,
            String recipient,
            CommunicationStatus status,
            String message,
            LocalDateTime sentAt
    ) {
        this.quotation = quotation;
        this.type = type;
        this.recipient = recipient;
        this.status = status;
        this.message = message;
        this.sentAt = sentAt;
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

    public CommunicationType getType() {
        return type;
    }

    public void setType(CommunicationType type) {
        this.type = type;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public CommunicationStatus getStatus() {
        return status;
    }

    public void setStatus(CommunicationStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    // ---------------------------------------------------------
    // Enums
    // ---------------------------------------------------------

    public enum CommunicationType {
        EMAIL,
        WHATSAPP,
        PDF
    }

    public enum CommunicationStatus {
        SENT,
        FAILED
    }

    // ---------------------------------------------------------
    // Auto date
    // ---------------------------------------------------------

    @PrePersist
    protected void onCreate() {

        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }

        if (status == null) {
            status = CommunicationStatus.SENT;
        }
    }
}