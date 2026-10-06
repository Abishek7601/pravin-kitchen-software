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

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> downloadQuotationPdf(
            @PathVariable Long id) {

        Quotation quotation =
                quotationService.getById(id);

        /*
         * Generate PDF first.
         *
         * We record the communication only after
         * successful PDF generation.
         */
        byte[] pdf =
                quotationPdfService.generateQuotationPdf(id);

        quotationCommunicationService.record(
                quotation,
                QuotationCommunication.CommunicationType.PDF,
                "Quotation PDF",
                QuotationCommunication.CommunicationStatus.SENT,
                "Quotation PDF generated/downloaded successfully."
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=quotation-" + id + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}