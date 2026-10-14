package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.QuotationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/quotations")
public class PublicQuotationPdfController {

    private final QuotationPdfService quotationPdfService;
    private final QuotationService quotationService;

    public PublicQuotationPdfController(
            QuotationPdfService quotationPdfService,
            QuotationService quotationService
    ) {
        this.quotationPdfService = quotationPdfService;
        this.quotationService = quotationService;
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> getPublicQuotationPdf(@PathVariable Long id) {
        Quotation quotation = quotationService.getById(id);
        byte[] pdf = quotationPdfService.generateQuotationPdf(quotation.getId());

        String filename = "quotation-" + quotation.getQuotationNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
