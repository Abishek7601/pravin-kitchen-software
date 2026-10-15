package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.QuotationService;
import com.example.pravin_quotation.service.TemporaryMediaTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/quotations")
public class PublicQuotationPdfController {

    private static final Logger log = LoggerFactory.getLogger(PublicQuotationPdfController.class);

    private final QuotationPdfService quotationPdfService;
    private final QuotationService quotationService;
    private final TemporaryMediaTokenService temporaryMediaTokenService;

    public PublicQuotationPdfController(
            QuotationPdfService quotationPdfService,
            QuotationService quotationService,
            TemporaryMediaTokenService temporaryMediaTokenService
    ) {
        this.quotationPdfService = quotationPdfService;
        this.quotationService = quotationService;
        this.temporaryMediaTokenService = temporaryMediaTokenService;
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> getPublicQuotationPdf(
            @PathVariable Long id,
            @RequestParam(required = false) String token
    ) {
        // Enforce token validation if required or if a token parameter was supplied
        if (temporaryMediaTokenService.isTokenRequired() || (token != null && !token.isBlank())) {
            boolean valid = temporaryMediaTokenService.isValidToken(id, token);
            if (!valid) {
                log.warn("Unauthorized attempt to access quotation PDF {} with invalid or missing token.", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        Quotation quotation = quotationService.getById(id);
        if (quotation == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdf = quotationPdfService.generateQuotationPdf(quotation.getId());

        String filename = "quotation-" + quotation.getQuotationNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    public ResponseEntity<byte[]> getPublicQuotationPdf(Long id) {
        return getPublicQuotationPdf(id, null);
    }
}
