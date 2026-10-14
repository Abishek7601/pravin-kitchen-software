package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.model.WhatsAppResult;
import com.example.pravin_quotation.repository.QuotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final QuotationPdfService quotationPdfService;
    private final QuotationCommunicationService quotationCommunicationService;
    private final QuotationRepository quotationRepository;

    @Value("${whatsapp.api.enabled:false}")
    private boolean apiEnabled;

    @Value("${whatsapp.api.provider:none}")
    private String apiProvider;

    @Value("${whatsapp.api.url:}")
    private String apiUrl;

    @Value("${whatsapp.api.token:}")
    private String apiToken;

    public WhatsAppService(
            QuotationPdfService quotationPdfService,
            QuotationCommunicationService quotationCommunicationService,
            QuotationRepository quotationRepository
    ) {
        this.quotationPdfService = quotationPdfService;
        this.quotationCommunicationService = quotationCommunicationService;
        this.quotationRepository = quotationRepository;
    }

    public boolean isApiProviderConfigured() {
        return apiEnabled && apiUrl != null && !apiUrl.isBlank();
    }

    public String getApiProviderName() {
        return apiProvider;
    }

    @Transactional
    public WhatsAppResult sendOrPrepareQuotationPdf(Quotation quotation, String baseUrl) {
        if (quotation == null) {
            throw new IllegalArgumentException("Quotation is required.");
        }

        if (quotation.getCustomer() == null) {
            throw new IllegalArgumentException("Customer not found.");
        }

        String rawPhone = quotation.getCustomer().getPhone();
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Customer phone number is not available.");
        }

        String phone = rawPhone.replaceAll("[^0-9]", "");
        if (phone.startsWith("0") && phone.length() == 11) {
            phone = phone.substring(1);
        }
        if (phone.length() == 10) {
            phone = "91" + phone;
        }

        if (!phone.matches("[1-9][0-9]{7,14}")) {
            throw new IllegalArgumentException("Customer phone number format is invalid: " + rawPhone);
        }

        // 1. Mandatory Requirement: Generate or retrieve final quotation PDF before initiating WhatsApp sharing
        byte[] pdfBytes;
        try {
            pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new IllegalStateException("Generated quotation PDF is empty.");
            }
        } catch (Exception e) {
            log.error("Failed to generate quotation PDF for WhatsApp sharing: {}", e.getMessage(), e);
            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.WHATSAPP,
                    phone,
                    QuotationCommunication.CommunicationStatus.FAILED,
                    "Failed to generate PDF for WhatsApp: " + e.getMessage()
            );
            throw new RuntimeException("Could not generate quotation PDF: " + e.getMessage(), e);
        }

        String quotationNumber = quotation.getQuotationNumber();
        String customerName = quotation.getCustomer().getName() != null ? quotation.getCustomer().getName() : "Customer";
        String totalAmount = quotation.getGrandTotal() != null ? quotation.getGrandTotal().toPlainString() : "0.00";
        String pdfDownloadUrl = (baseUrl != null ? baseUrl : "") + "/public/quotations/pdf/" + quotation.getId();

        // 2. If a WhatsApp API provider is configured, attempt direct document delivery via API
        if (isApiProviderConfigured()) {
            return sendViaApiProvider(quotation, phone, pdfBytes, quotationNumber, customerName, totalAmount, pdfDownloadUrl);
        }

        // 3. Client-side WhatsApp Web / app (wa.me)
        String message = "Hello " + customerName + ",\n\n"
                + "Your quotation has been prepared.\n\n"
                + "Quotation No: " + quotationNumber + "\n"
                + "Quotation Amount: ₹" + totalAmount + "\n\n"
                + "Thank you,\n"
                + "Pravin KITCHENS & INTERIORS";

        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = "https://wa.me/" + phone + "?text=" + encodedMessage;

        // Record communication
        quotationCommunicationService.record(
                quotation,
                QuotationCommunication.CommunicationType.WHATSAPP,
                phone,
                QuotationCommunication.CommunicationStatus.SENT,
                "Quotation PDF generated (" + pdfBytes.length + " bytes). WhatsApp sharing initiated for " + phone + "."
        );

        // Update quotation workflow status
        if (quotation.getStatus() == QuotationStatus.PENDING || quotation.getStatus() == QuotationStatus.WORKING) {
            quotation.setStatus(QuotationStatus.SENT);
            quotationRepository.save(quotation);
        }

        return new WhatsAppResult(
                true,
                "CLIENT_REDIRECT",
                "Quotation PDF generated successfully (" + pdfBytes.length + " bytes). Opening WhatsApp sharing.",
                whatsappUrl,
                phone,
                customerName,
                quotationNumber,
                pdfDownloadUrl,
                pdfBytes.length
        );
    }

    private WhatsAppResult sendViaApiProvider(
            Quotation quotation,
            String phone,
            byte[] pdfBytes,
            String quotationNumber,
            String customerName,
            String totalAmount,
            String pdfDownloadUrl
    ) {
        log.info("Dispatching quotation PDF document to WhatsApp API provider '{}' for number {}", apiProvider, phone);

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            String jsonPayload = String.format(
                    "{\"to\":\"%s\",\"filename\":\"quotation-%s.pdf\",\"documentUrl\":\"%s\",\"caption\":\"Quotation %s - Pravin Kitchens & Interiors\"}",
                    phone, quotationNumber, pdfDownloadUrl, quotationNumber
            );

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8));

            if (apiToken != null && !apiToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiToken.trim());
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                quotationCommunicationService.record(
                        quotation,
                        QuotationCommunication.CommunicationType.WHATSAPP,
                        phone,
                        QuotationCommunication.CommunicationStatus.SENT,
                        "Quotation PDF document sent successfully via WhatsApp API (" + apiProvider + ")."
                );

                if (quotation.getStatus() == QuotationStatus.PENDING || quotation.getStatus() == QuotationStatus.WORKING) {
                    quotation.setStatus(QuotationStatus.SENT);
                    quotationRepository.save(quotation);
                }

                return new WhatsAppResult(
                        true,
                        "API",
                        "Quotation PDF document sent successfully to " + phone + " via WhatsApp API.",
                        null,
                        phone,
                        customerName,
                        quotationNumber,
                        pdfDownloadUrl,
                        pdfBytes.length
                );
            } else {
                throw new RuntimeException("API returned status " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            log.error("Failed to send quotation PDF via WhatsApp API: {}", e.getMessage(), e);
            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.WHATSAPP,
                    phone,
                    QuotationCommunication.CommunicationStatus.FAILED,
                    "WhatsApp API delivery failed: " + e.getMessage()
            );

            throw new RuntimeException("WhatsApp API delivery failed: " + e.getMessage(), e);
        }
    }
}
