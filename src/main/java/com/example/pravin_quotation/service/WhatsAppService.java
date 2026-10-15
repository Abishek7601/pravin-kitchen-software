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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final QuotationPdfService quotationPdfService;
    private final QuotationCommunicationService quotationCommunicationService;
    private final QuotationRepository quotationRepository;
    private final TemporaryMediaTokenService temporaryMediaTokenService;

    // Track in-progress quotation sends to prevent duplicate sends from repeated clicks
    private final Set<Long> inProgressQuotationIds = ConcurrentHashMap.newKeySet();

    @Value("${whatsapp.api.enabled:false}")
    private boolean apiEnabled;

    @Value("${whatsapp.api.provider:none}")
    private String apiProvider;

    @Value("${whatsapp.api.url:}")
    private String apiUrl;

    @Value("${whatsapp.api.token:}")
    private String apiToken;

    @Value("${whatsapp.api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.api.account-sid:}")
    private String accountSid;

    @Value("${whatsapp.api.from-number:}")
    private String fromNumber;

    private HttpClient httpClient;

    public WhatsAppService(
            QuotationPdfService quotationPdfService,
            QuotationCommunicationService quotationCommunicationService,
            QuotationRepository quotationRepository,
            TemporaryMediaTokenService temporaryMediaTokenService
    ) {
        this.quotationPdfService = quotationPdfService;
        this.quotationCommunicationService = quotationCommunicationService;
        this.quotationRepository = quotationRepository;
        this.temporaryMediaTokenService = temporaryMediaTokenService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean isApiProviderConfigured() {
        if (!apiEnabled) {
            return false;
        }
        if ("mock".equalsIgnoreCase(apiProvider) || "simulation".equalsIgnoreCase(apiProvider)) {
            return true;
        }
        if ("meta".equalsIgnoreCase(apiProvider) || "cloud_api".equalsIgnoreCase(apiProvider)) {
            boolean hasPhoneId = (phoneNumberId != null && !phoneNumberId.isBlank());
            boolean hasToken = (apiToken != null && !apiToken.isBlank());
            boolean hasCustomUrl = (apiUrl != null && !apiUrl.isBlank());
            return (hasPhoneId && hasToken) || (hasCustomUrl && hasToken);
        }
        if ("twilio".equalsIgnoreCase(apiProvider)) {
            boolean hasAccountSid = (accountSid != null && !accountSid.isBlank());
            boolean hasToken = (apiToken != null && !apiToken.isBlank());
            boolean hasFrom = (fromNumber != null && !fromNumber.isBlank());
            return hasAccountSid && hasToken && hasFrom;
        }
        if ("generic".equalsIgnoreCase(apiProvider)) {
            return apiUrl != null && !apiUrl.isBlank();
        }
        return false;
    }

    public String getConfigurationMissingDetails() {
        if (!apiEnabled) {
            return "WhatsApp Business API is disabled ('whatsapp.api.enabled=false'). "
                    + "Direct PDF document delivery requires a configured WhatsApp Business API provider (such as Meta WhatsApp Cloud API or Twilio). "
                    + "WhatsApp Web (wa.me) cannot automatically attach or send PDF files.";
        }
        String p = apiProvider != null ? apiProvider.toLowerCase() : "none";
        switch (p) {
            case "meta":
            case "cloud_api":
                StringBuilder metaMissing = new StringBuilder("Meta WhatsApp Cloud API credentials missing: ");
                boolean needsComma = false;
                if (phoneNumberId == null || phoneNumberId.isBlank()) {
                    metaMissing.append("WHATSAPP_PHONE_NUMBER_ID (whatsapp.api.phone-number-id)");
                    needsComma = true;
                }
                if (apiToken == null || apiToken.isBlank()) {
                    if (needsComma) metaMissing.append(", ");
                    metaMissing.append("WHATSAPP_API_TOKEN (System User Permanent Access Token)");
                }
                return metaMissing.toString();

            case "twilio":
                StringBuilder twilioMissing = new StringBuilder("Twilio WhatsApp credentials missing: ");
                boolean twilioComma = false;
                if (accountSid == null || accountSid.isBlank()) {
                    twilioMissing.append("WHATSAPP_ACCOUNT_SID (whatsapp.api.account-sid)");
                    twilioComma = true;
                }
                if (apiToken == null || apiToken.isBlank()) {
                    if (twilioComma) twilioMissing.append(", ");
                    twilioMissing.append("WHATSAPP_API_TOKEN / Auth Token (whatsapp.api.token)");
                    twilioComma = true;
                }
                if (fromNumber == null || fromNumber.isBlank()) {
                    if (twilioComma) twilioMissing.append(", ");
                    twilioMissing.append("WHATSAPP_FROM_NUMBER (whatsapp.api.from-number, e.g. whatsapp:+14155238886)");
                }
                return twilioMissing.toString();

            case "generic":
                return "Generic WhatsApp provider requires WHATSAPP_API_URL (whatsapp.api.url).";

            case "none":
            default:
                return "WhatsApp Business API provider is set to '" + apiProvider + "'. Supported providers: meta, twilio, generic, mock.";
        }
    }

    public String getApiProviderName() {
        return apiProvider;
    }

    public void setApiEnabled(boolean apiEnabled) {
        this.apiEnabled = apiEnabled;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    /**
     * Normalizes a customer phone number into international E.164 format.
     * Handles Indian 10-digit mobile numbers, leading zeros, country codes, and formatting characters.
     */
    public String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Customer phone number is not available for this quotation.");
        }

        String phone = rawPhone.trim().replaceAll("[^0-9]", "");

        if (phone.startsWith("00")) {
            phone = phone.substring(2);
        }
        if (phone.startsWith("0") && phone.length() == 11) {
            phone = phone.substring(1);
        }
        if (phone.length() == 10) {
            phone = "91" + phone;
        }

        if (!phone.matches("^[1-9][0-9]{9,14}$")) {
            throw new IllegalArgumentException("Customer phone number format is invalid: " + rawPhone);
        }

        if (phone.length() == 12 && phone.startsWith("91")) {
            char firstMobileDigit = phone.charAt(2);
            if (firstMobileDigit < '5' || firstMobileDigit > '9') {
                throw new IllegalArgumentException("Customer mobile number is invalid for India (must start with 6-9): " + rawPhone);
            }
        }

        return phone;
    }

    /**
     * Masks customer phone number for secure logging without exposing sensitive data.
     */
    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }
        return "******" + phone.substring(phone.length() - 4);
    }

    @Transactional
    public WhatsAppResult sendOrPrepareQuotationPdf(Quotation quotation, String baseUrl) {
        if (quotation == null) {
            throw new IllegalArgumentException("Quotation is required.");
        }

        if (quotation.getCustomer() == null) {
            throw new IllegalArgumentException("Customer not found for quotation " + quotation.getId() + ".");
        }

        // Prevent duplicate concurrent requests for the same quotation
        if (!inProgressQuotationIds.add(quotation.getId())) {
            log.warn("Duplicate WhatsApp sending request rejected for quotation ID {}", quotation.getId());
            throw new IllegalStateException("WhatsApp message is already being sent for quotation "
                    + quotation.getQuotationNumber() + ". Please wait for the current request to complete.");
        }

        try {
            String rawPhone = quotation.getCustomer().getPhone();
            String normalizedPhone = normalizePhoneNumber(rawPhone);

            // Generate or retrieve final quotation PDF
            byte[] pdfBytes;
            try {
                pdfBytes = quotationPdfService.generateQuotationPdf(quotation.getId());
                if (pdfBytes == null || pdfBytes.length == 0) {
                    throw new IllegalStateException("Generated quotation PDF is empty.");
                }
            } catch (Exception e) {
                log.error("Failed to generate quotation PDF for WhatsApp document sharing: quotationId={}, error={}",
                        quotation.getId(), e.getMessage(), e);
                quotationCommunicationService.record(
                        quotation,
                        QuotationCommunication.CommunicationType.WHATSAPP,
                        normalizedPhone,
                        QuotationCommunication.CommunicationStatus.FAILED,
                        "Failed to generate PDF for WhatsApp document: " + e.getMessage()
                );
                throw new RuntimeException("Could not generate quotation PDF: " + e.getMessage(), e);
            }

            String quotationNumber = quotation.getQuotationNumber();
            String customerName = quotation.getCustomer().getName() != null ? quotation.getCustomer().getName() : "Customer";
            String totalAmount = quotation.getGrandTotal() != null ? quotation.getGrandTotal().toPlainString() : "0.00";

            // Generate a secure, time-limited tokenized media URL for providers that download media
            String secureMediaUrl = temporaryMediaTokenService.buildSecureMediaUrl(baseUrl, quotation.getId());

            // 1. Direct server-to-server document delivery via WhatsApp Business API provider
            if (isApiProviderConfigured()) {
                return sendViaApiProvider(
                        quotation,
                        normalizedPhone,
                        pdfBytes,
                        quotationNumber,
                        customerName,
                        totalAmount,
                        secureMediaUrl
                );
            }

            // 2. Standard direct WhatsApp sharing flow (no API credentials or paid provider required)
            String message = "Hello " + customerName + ",\n\n"
                    + "Please find attached your quotation (" + quotationNumber + ") from Pravin Kitchens & Interiors.\n\n"
                    + "Total Amount: ₹" + totalAmount + "\n\n"
                    + "Thank you,\n"
                    + "Pravin KITCHENS & INTERIORS";

            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String whatsappUrl = "https://wa.me/" + normalizedPhone + "?text=" + encodedMessage;

            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.WHATSAPP,
                    normalizedPhone,
                    QuotationCommunication.CommunicationStatus.SENT,
                    "Quotation PDF generated (" + pdfBytes.length + " bytes). Direct WhatsApp chat opened for customer " + normalizedPhone + " to attach PDF."
            );

            if (quotation.getStatus() == QuotationStatus.PENDING || quotation.getStatus() == QuotationStatus.WORKING) {
                quotation.setStatus(QuotationStatus.SENT);
                quotationRepository.save(quotation);
            }

            log.info("Direct WhatsApp sharing flow prepared for quotation ID {}, customer masked phone: {}",
                    quotation.getId(), maskPhoneNumber(normalizedPhone));

            WhatsAppResult result = new WhatsAppResult(
                    true,
                    "CLIENT_REDIRECT",
                    "Quotation PDF generated successfully (" + pdfBytes.length + " bytes). Ready for WhatsApp sharing.",
                    whatsappUrl,
                    normalizedPhone,
                    customerName,
                    quotationNumber,
                    secureMediaUrl,
                    pdfBytes.length,
                    null,
                    "whatsapp-direct"
            );
            result.setQuotationId(quotation.getId());
            return result;

        } finally {
            inProgressQuotationIds.remove(quotation.getId());
        }
    }

    /**
     * Sends the quotation PDF document directly to the customer via WhatsApp Business API provider.
     */
    private WhatsAppResult sendViaApiProvider(
            Quotation quotation,
            String normalizedPhone,
            byte[] pdfBytes,
            String quotationNumber,
            String customerName,
            String totalAmount,
            String secureMediaUrl
    ) {
        String filename = "Quotation-" + quotationNumber + ".pdf";
        String caption = "Your quotation from Pravin Kitchens & Interiors";

        log.info("Sending quotation PDF document to customer WhatsApp via provider '{}'. Quotation ID: {}, Masked Phone: {}",
                apiProvider, quotation.getId(), maskPhoneNumber(normalizedPhone));

        String messageId;

        try {
            String providerType = apiProvider != null ? apiProvider.toLowerCase() : "none";

            switch (providerType) {
                case "mock":
                case "simulation":
                    messageId = "MOCK-WA-" + UUID.randomUUID();
                    log.info("Simulated WhatsApp PDF document delivery for quotation ID {}", quotation.getId());
                    break;

                case "meta":
                case "cloud_api":
                    messageId = sendViaMetaCloudApi(normalizedPhone, filename, caption, pdfBytes, secureMediaUrl);
                    break;

                case "twilio":
                    messageId = sendViaTwilioApi(normalizedPhone, caption, secureMediaUrl);
                    break;

                case "generic":
                default:
                    messageId = sendViaGenericApi(normalizedPhone, filename, caption, pdfBytes, secureMediaUrl);
                    break;
            }

            // Record communication upon confirmed provider acceptance
            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.WHATSAPP,
                    normalizedPhone,
                    QuotationCommunication.CommunicationStatus.SENT,
                    "Quotation PDF document sent successfully via WhatsApp API (" + apiProvider + "). Message ID: " + messageId
            );

            if (quotation.getStatus() == QuotationStatus.PENDING || quotation.getStatus() == QuotationStatus.WORKING) {
                quotation.setStatus(QuotationStatus.SENT);
                quotationRepository.save(quotation);
            }

            log.info("Quotation PDF document successfully accepted by WhatsApp provider '{}' for quotation ID {}. Message ID: {}",
                    apiProvider, quotation.getId(), messageId);

            return new WhatsAppResult(
                    true,
                    "API",
                    "Quotation PDF document sent successfully to " + normalizedPhone + " via WhatsApp API.",
                    null,
                    normalizedPhone,
                    customerName,
                    quotationNumber,
                    secureMediaUrl,
                    pdfBytes.length,
                    messageId,
                    apiProvider
            );

        } catch (Exception e) {
            log.error("Failed to deliver quotation PDF document via WhatsApp provider '{}' for quotation ID {}: {}",
                    apiProvider, quotation.getId(), e.getMessage());

            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.WHATSAPP,
                    normalizedPhone,
                    QuotationCommunication.CommunicationStatus.FAILED,
                    "WhatsApp API delivery failed (" + apiProvider + "): " + e.getMessage()
            );

            throw new RuntimeException("WhatsApp API delivery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delivers document message via Meta WhatsApp Business Cloud API.
     * Supports direct binary upload to /media or link-based document messages to /messages.
     */
    private String sendViaMetaCloudApi(
            String phone,
            String filename,
            String caption,
            byte[] pdfBytes,
            String secureMediaUrl
    ) throws IOException, InterruptedException {
        String effectivePhoneId = phoneNumberId != null && !phoneNumberId.isBlank() ? phoneNumberId.trim() : "";
        String messagesEndpoint = apiUrl != null && !apiUrl.isBlank()
                ? apiUrl.trim()
                : "https://graph.facebook.com/v21.0/" + effectivePhoneId + "/messages";

        // Try direct binary upload to Meta /media if phoneNumberId is present and apiUrl is not custom
        String mediaId = null;
        if (!effectivePhoneId.isBlank() && (apiUrl == null || apiUrl.isBlank())) {
            try {
                mediaId = uploadMediaToMeta(effectivePhoneId, filename, pdfBytes);
            } catch (Exception e) {
                log.warn("Meta Cloud API binary media upload failed: {}", e.getMessage());
                if (secureMediaUrl != null && !secureMediaUrl.contains("localhost") && !secureMediaUrl.contains("127.0.0.1")) {
                    log.info("Falling back to secure public URL for Meta document message");
                } else {
                    throw new RuntimeException("Meta WhatsApp media upload failed: " + e.getMessage(), e);
                }
            }
        }

        String jsonPayload;
        if (mediaId != null && !mediaId.isBlank()) {
            jsonPayload = String.format(
                    "{\"messaging_product\":\"whatsapp\",\"recipient_type\":\"individual\",\"to\":\"%s\",\"type\":\"document\",\"document\":{\"id\":\"%s\",\"caption\":\"%s\",\"filename\":\"%s\"}}",
                    phone, mediaId, escapeJson(caption), escapeJson(filename)
            );
        } else {
            jsonPayload = String.format(
                    "{\"messaging_product\":\"whatsapp\",\"recipient_type\":\"individual\",\"to\":\"%s\",\"type\":\"document\",\"document\":{\"link\":\"%s\",\"caption\":\"%s\",\"filename\":\"%s\"}}",
                    phone, secureMediaUrl, escapeJson(caption), escapeJson(filename)
            );
        }

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(messagesEndpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8));

        if (apiToken != null && !apiToken.isBlank()) {
            reqBuilder.header("Authorization", "Bearer " + apiToken.trim());
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return extractMetaMessageId(response.body());
        } else {
            throw new RuntimeException("Meta WhatsApp API error (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Uploads the PDF file binary to Meta Cloud API /media endpoint.
     */
    private String uploadMediaToMeta(String phoneId, String filename, byte[] pdfBytes) throws IOException, InterruptedException {
        String uploadEndpoint = "https://graph.facebook.com/v21.0/" + phoneId + "/media";
        String boundary = "----WhatsAppBoundary" + System.currentTimeMillis();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeFormField(baos, boundary, "messaging_product", "whatsapp");
        writeFormField(baos, boundary, "type", "application/pdf");

        // Write file part
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: application/pdf\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(pdfBytes);
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uploadEndpoint))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()));

        if (apiToken != null && !apiToken.isBlank()) {
            reqBuilder.header("Authorization", "Bearer " + apiToken.trim());
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
            if (m.find()) {
                return m.group(1);
            }
        }
        throw new RuntimeException("Failed to upload media to Meta (HTTP " + response.statusCode() + "): " + response.body());
    }

    /**
     * Delivers document message via Twilio WhatsApp API.
     */
    private String sendViaTwilioApi(
            String phone,
            String caption,
            String secureMediaUrl
    ) throws IOException, InterruptedException {
        String endpoint = (apiUrl != null && !apiUrl.isBlank())
                ? apiUrl.trim()
                : "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        String from = fromNumber != null && !fromNumber.isBlank() ? fromNumber.trim() : "";
        if (!from.startsWith("whatsapp:")) {
            from = "whatsapp:" + from;
        }

        String to = "whatsapp:+" + phone;

        String formData = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                + "&From=" + URLEncoder.encode(from, StandardCharsets.UTF_8)
                + "&Body=" + URLEncoder.encode(caption, StandardCharsets.UTF_8)
                + "&MediaUrl=" + URLEncoder.encode(secureMediaUrl, StandardCharsets.UTF_8);

        String basicAuth = Base64.getEncoder().encodeToString(
                ((accountSid != null ? accountSid : "") + ":" + (apiToken != null ? apiToken : "")).getBytes(StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(formData, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            Matcher m = Pattern.compile("\"sid\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
            if (m.find()) {
                return m.group(1);
            }
            return "TWILIO-" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("Twilio WhatsApp API error (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Delivers document message via Generic REST WhatsApp API.
     */
    private String sendViaGenericApi(
            String phone,
            String filename,
            String caption,
            byte[] pdfBytes,
            String secureMediaUrl
    ) throws IOException, InterruptedException {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("API URL is required for generic WhatsApp provider.");
        }

        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

        String jsonPayload = String.format(
                "{\"to\":\"%s\",\"filename\":\"%s\",\"caption\":\"%s\",\"documentUrl\":\"%s\",\"pdfBase64\":\"%s\"}",
                phone, escapeJson(filename), escapeJson(caption), secureMediaUrl, pdfBase64
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
            Matcher m = Pattern.compile("\"(?:id|messageId|sid)\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
            if (m.find()) {
                return m.group(1);
            }
            return "GENERIC-" + System.currentTimeMillis();
        } else {
            throw new RuntimeException("WhatsApp API provider error (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    private void writeFormField(ByteArrayOutputStream baos, String boundary, String name, String value) throws IOException {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private String extractMetaMessageId(String responseBody) {
        if (responseBody != null) {
            Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"(wamid\\.[^\"]+|\")").matcher(responseBody);
            if (m.find()) {
                return m.group(1);
            }
            Matcher anyId = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(responseBody);
            if (anyId.find()) {
                return anyId.group(1);
            }
        }
        return "META-" + System.currentTimeMillis();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
