package com.example.pravin_quotation.util;

import com.example.pravin_quotation.model.Customer;
import com.example.pravin_quotation.model.Quotation;
import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;

public final class QuotationPdfFilenameUtils {

    public static final String DEFAULT_PDF_FILENAME = "Customer_Quotation.pdf";

    private QuotationPdfFilenameUtils() {
    }

    /**
     * Builds a clean, sanitized PDF filename using ONLY the customer's name.
     * Does not include quotation numbers or grade names.
     */
    public static String buildPdfFilename(Quotation quotation) {
        String customerName = null;
        if (quotation != null) {
            if (quotation.getCustomer() != null && quotation.getCustomer().getName() != null) {
                customerName = quotation.getCustomer().getName();
            } else if (quotation.getParent() != null && quotation.getParent().getCustomer() != null
                    && quotation.getParent().getCustomer().getName() != null) {
                customerName = quotation.getParent().getCustomer().getName();
            }
        }
        return sanitizeCustomerFilename(customerName);
    }

    /**
     * Sanitizes customer name for use as a PDF download filename.
     * - Uses customer name only
     * - Replaces spaces and invalid characters with underscores
     * - Preserves Unicode letters, marks, digits and hyphens (safe for Tamil/non-English)
     * - Collapses consecutive underscores and trims edge underscores/hyphens
     * - Falls back to Customer_Quotation.pdf if missing or blank
     */
    public static String sanitizeCustomerFilename(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return DEFAULT_PDF_FILENAME;
        }

        String cleaned = customerName.trim();
        // Replace all characters other than Unicode letters, combining marks, numbers, and hyphens with underscores
        cleaned = cleaned.replaceAll("[^\\p{L}\\p{M}\\p{N}\\-]+", "_");
        // Collapse multiple consecutive underscores
        cleaned = cleaned.replaceAll("_+", "_");
        // Trim leading and trailing underscores and hyphens
        cleaned = cleaned.replaceAll("^[\\-_]+|[\\-_]+$", "");

        if (cleaned.isEmpty()) {
            return DEFAULT_PDF_FILENAME;
        }

        return cleaned + ".pdf";
    }

    /**
     * Builds an RFC 5987 / RFC 6266 compliant ContentDisposition attachment header
     * supporting UTF-8 (Tamil/non-English) customer names with safe ASCII fallback.
     */
    public static ContentDisposition buildAttachmentContentDisposition(String filename) {
        return ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
    }

    /**
     * Builds an RFC 5987 / RFC 6266 compliant ContentDisposition inline header.
     */
    public static ContentDisposition buildInlineContentDisposition(String filename) {
        return ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
    }
}

