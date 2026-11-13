package com.example.pravin_quotation.controller;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.service.QuotationCommunicationService;
import com.example.pravin_quotation.service.QuotationPdfService;
import com.example.pravin_quotation.service.QuotationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/quotations")
public class QuotationPdfController {

    private final QuotationPdfService quotationPdfService;
    private final QuotationService quotationService;
    private final QuotationCommunicationService quotationCommunicationService;

    public QuotationPdfController(
            QuotationPdfService quotationPdfService,
            QuotationService quotationService,
            QuotationCommunicationService quotationCommunicationService
    ) {
        this.quotationPdfService = quotationPdfService;
        this.quotationService = quotationService;
        this.quotationCommunicationService =
                quotationCommunicationService;
    }

    // ---------------------------------------------------------
    // Download quotation PDF
    // ---------------------------------------------------------

    public ResponseEntity<byte[]> downloadQuotationPdf(Long id) {
        return downloadQuotationPdf(id, null);
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> downloadQuotationPdf(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) com.example.pravin_quotation.model.PricingMode grade) {

        Quotation quotation =
                quotationService.getById(id);

        Quotation targetQuotation = quotation;
        if (grade != null) {
            Quotation child = quotation.getChildForGrade(grade);
            if (child != null) {
                targetQuotation = child;
            }
        } else if (quotation.getSelectedGrade() != null) {
            Quotation child = quotation.getChildForGrade(quotation.getSelectedGrade());
            if (child != null) {
                targetQuotation = child;
            }
        } else if (quotation.isParentQuotation() && quotation.getChildQuotations() != null && !quotation.getChildQuotations().isEmpty()) {
            targetQuotation = quotation.getChildQuotations().get(0);
        }

        byte[] pdf =
                quotationPdfService.generateQuotationPdf(targetQuotation.getId());

        quotationCommunicationService.record(
                targetQuotation,
                QuotationCommunication.CommunicationType.PDF,
                "Quotation PDF" + (targetQuotation.getPricingMode() != null ? " (" + targetQuotation.getPricingMode().name() + ")" : ""),
                QuotationCommunication.CommunicationStatus.SENT,
                "Quotation PDF generated/downloaded successfully."
        );

        String filename = com.example.pravin_quotation.util.QuotationPdfFilenameUtils.buildPdfFilename(targetQuotation);

        return ResponseEntity.ok()
                .headers(headers -> headers.setContentDisposition(
                        com.example.pravin_quotation.util.QuotationPdfFilenameUtils.buildAttachmentContentDisposition(filename)
                ))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}