package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class QuotationEmailService {

    private final JavaMailSender mailSender;
    private final QuotationPdfService quotationPdfService;
    private final QuotationCommunicationService quotationCommunicationService;

    public QuotationEmailService(
            JavaMailSender mailSender,
            QuotationPdfService quotationPdfService,
            QuotationCommunicationService quotationCommunicationService) {

        this.mailSender = mailSender;
        this.quotationPdfService = quotationPdfService;
        this.quotationCommunicationService =
                quotationCommunicationService;
    }

    public void sendQuotationEmail(Quotation quotation) {

        if (quotation == null) {
            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        if (quotation.getCustomer() == null) {
            throw new IllegalArgumentException(
                    "Quotation customer is not available."
            );
        }

        String customerEmail =
                quotation.getCustomer().getEmail();

        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer email address is not available."
            );
        }

        try {

            byte[] pdf =
                    quotationPdfService.generateQuotationPdf(
                            quotation.getId()
                    );

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            String customerName =
                    quotation.getCustomer().getName();

            String quotationNumber =
                    quotation.getQuotationNumber();

            helper.setTo(customerEmail);

            helper.setSubject(
                    "Quotation " +
                            quotationNumber +
                            " - Pravin KITCHENS & INTERIORSS & INTERIORS"
            );

            String emailBody =
                    "Dear " + customerName + ",\n\n" +

                            "Please find attached your quotation " +
                            quotationNumber +
                            " from Pravin KITCHENS & INTERIORSS & INTERIORS.\n\n" +

                            "Quotation Amount: ₹" +
                            quotation.getGrandTotal() +
                            "\n\n" +

                            "Please review the attached quotation " +
                            "and feel free to contact us if you have " +
                            "any questions.\n\n" +

                            "Thank you,\n" +
                            "Pravin KITCHENS & INTERIORSS & INTERIORS\n" +
                            "+91 9787769970";

            helper.setText(emailBody);

            helper.addAttachment(
                    "quotation-" +
                            quotationNumber +
                            ".pdf",
                    new ByteArrayResource(pdf)
            );

            mailSender.send(message);

            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.EMAIL,
                    customerEmail,
                    QuotationCommunication.CommunicationStatus.SENT,
                    "Quotation PDF emailed successfully."
            );

        } catch (Exception e) {

            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.EMAIL,
                    quotation.getCustomer().getEmail(),
                    QuotationCommunication.CommunicationStatus.FAILED,
                    "Email failed: " + e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to send quotation email: "
                            + e.getMessage(),
                    e
            );
        }
    }
}