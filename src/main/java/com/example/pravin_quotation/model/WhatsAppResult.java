package com.example.pravin_quotation.model;

public class WhatsAppResult {

    private boolean success;
    private String method; // "API" or "CLIENT_REDIRECT"
    private String message;
    private String whatsappUrl;
    private String phone;
    private String customerName;
    private String quotationNumber;
    private String pdfDownloadUrl;
    private int pdfSizeBytes;
    private String messageId;
    private String provider;
    private Long quotationId;

    public WhatsAppResult() {
    }

    public WhatsAppResult(boolean success, String method, String message, String whatsappUrl,
                          String phone, String customerName, String quotationNumber,
                          String pdfDownloadUrl, int pdfSizeBytes) {
        this(success, method, message, whatsappUrl, phone, customerName, quotationNumber, pdfDownloadUrl, pdfSizeBytes, null, null);
    }

    public WhatsAppResult(boolean success, String method, String message, String whatsappUrl,
                          String phone, String customerName, String quotationNumber,
                          String pdfDownloadUrl, int pdfSizeBytes, String messageId, String provider) {
        this.success = success;
        this.method = method;
        this.message = message;
        this.whatsappUrl = whatsappUrl;
        this.phone = phone;
        this.customerName = customerName;
        this.quotationNumber = quotationNumber;
        this.pdfDownloadUrl = pdfDownloadUrl;
        this.pdfSizeBytes = pdfSizeBytes;
        this.messageId = messageId;
        this.provider = provider;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWhatsappUrl() {
        return whatsappUrl;
    }

    public void setWhatsappUrl(String whatsappUrl) {
        this.whatsappUrl = whatsappUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public String getPdfDownloadUrl() {
        return pdfDownloadUrl;
    }

    public void setPdfDownloadUrl(String pdfDownloadUrl) {
        this.pdfDownloadUrl = pdfDownloadUrl;
    }

    public int getPdfSizeBytes() {
        return pdfSizeBytes;
    }

    public void setPdfSizeBytes(int pdfSizeBytes) {
        this.pdfSizeBytes = pdfSizeBytes;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Long getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(Long quotationId) {
        this.quotationId = quotationId;
    }
}
