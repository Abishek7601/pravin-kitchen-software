package com.example.pravin_quotation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class TemporaryMediaTokenService {

    private static final Logger log = LoggerFactory.getLogger(TemporaryMediaTokenService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_EXPIRY_MILLIS = 30 * 60 * 1000L; // 30 minutes

    @Value("${whatsapp.api.media-token-secret:pravin-kitchens-default-media-token-secret-2026}")
    private String secretKey;

    @Value("${whatsapp.api.media-token-required:false}")
    private boolean tokenRequired;

    public boolean isTokenRequired() {
        return tokenRequired;
    }

    public void setTokenRequired(boolean tokenRequired) {
        this.tokenRequired = tokenRequired;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Generate a signed, time-limited token for a quotation PDF.
     */
    public String generateToken(Long quotationId) {
        if (quotationId == null) {
            throw new IllegalArgumentException("Quotation ID is required for generating media token.");
        }
        long expiresAt = System.currentTimeMillis() + DEFAULT_EXPIRY_MILLIS;
        String payload = quotationId + ":" + expiresAt;
        String signature = computeHmac(payload);
        String rawToken = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build the secure public URL for the quotation PDF with temporary token.
     */
    public String buildSecureMediaUrl(String baseUrl, Long quotationId) {
        String token = generateToken(quotationId);
        String cleanBase = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";
        return cleanBase + "/public/quotations/pdf/" + quotationId + "?token=" + token;
    }

    /**
     * Validate whether the provided token is valid, unexpired, and matches the quotation ID.
     */
    public boolean isValidToken(Long quotationId, String token) {
        if (quotationId == null || token == null || token.isBlank()) {
            return false;
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(token.trim());
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                return false;
            }

            Long tokenQuotationId = Long.parseLong(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);
            String providedSignature = parts[2];

            if (!quotationId.equals(tokenQuotationId)) {
                log.warn("Media token quotation ID mismatch: expected {}, got {}", quotationId, tokenQuotationId);
                return false;
            }

            if (System.currentTimeMillis() > expiresAt) {
                log.warn("Media token for quotation {} has expired.", quotationId);
                return false;
            }

            String expectedSignature = computeHmac(tokenQuotationId + ":" + expiresAt);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.warn("Failed to validate media token for quotation {}: {}", quotationId, e.getMessage());
            return false;
        }
    }

    private String computeHmac(String data) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Could not compute HMAC signature for media token", e);
        }
    }
}
