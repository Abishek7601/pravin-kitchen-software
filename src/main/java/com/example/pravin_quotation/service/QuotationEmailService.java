package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.repository.QuotationRepository;
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
    private final QuotationRepository quotationRepository;

    public QuotationEmailService(
            JavaMailSender mailSender,
            QuotationPdfService quotationPdfService,
            QuotationCommunicationService quotationCommunicationService,
            QuotationRepository quotationRepository) {

        this.mailSender = mailSender;
        this.quotationPdfService = quotationPdfService;
        this.quotationCommunicationService =
                quotationCommunicationService;
        this.quotationRepository = quotationRepository;
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

            String gradeTitle = "";
            if (quotation.getPricingMode() != null) {
                switch (quotation.getPricingMode()) {
                    case PREMIUM: gradeTitle = " (Premium Grade)"; break;
                    case MIDDLE: gradeTitle = " (Middle Grade)"; break;
                    case ECONOMY: gradeTitle = " (Economy Grade)"; break;
                    default: gradeTitle = " (" + quotation.getPricingMode().name() + " Grade)"; break;
                }
            }

            helper.setTo(customerEmail);

            helper.setSubject(
                    "Quotation " +
                            quotationNumber +
                            gradeTitle +
                            " - Pravin KITCHENS & INTERIORS"
            );

            String gradeLine = quotation.getPricingMode() != null
                    ? "Grade: " + quotation.getPricingMode().name() + " Grade\n"
                    : "";

            String emailBody =
                    "Dear " + customerName + ",\n\n" +

                            "Please find attached your quotation " +
                            quotationNumber +
                            gradeTitle +
                            " from Pravin KITCHENS & INTERIORS.\n\n" +

                            gradeLine +
                            "Quotation Amount: ₹" +
                            quotation.getGrandTotal() +
                            "\n\n" +

                            "Please review the attached quotation " +
                            "and feel free to contact us if you have " +
                            "any questions.\n\n" +

                            "Thank you,\n" +
                            "Pravin KITCHENS & INTERIORS\n" +
                            "+91 9787769970";

            helper.setText(emailBody);

            String gradeSuffix = quotation.getPricingMode() != null ? "-" + quotation.getPricingMode().name() : "";
            helper.addAttachment(
                    "quotation-" +
                            quotationNumber +
                            gradeSuffix +
                            ".pdf",
                    new ByteArrayResource(pdf)
            );

            mailSender.send(message);

            quotationCommunicationService.record(
                    quotation,
                    QuotationCommunication.CommunicationType.EMAIL,
                    customerEmail,
                    QuotationCommunication.CommunicationStatus.SENT,
                    "Quotation PDF" + gradeTitle + " emailed successfully."
            );

            if (quotation.getStatus() == QuotationStatus.PENDING
                    || quotation.getStatus() == QuotationStatus.WORKING) {
                quotation.setStatus(QuotationStatus.SENT);
                quotationRepository.save(quotation);
            }

            if (quotation.getParent() != null) {
                Quotation parent = quotation.getParent();
                if (parent.getStatus() == QuotationStatus.PENDING
                        || parent.getStatus() == QuotationStatus.WORKING) {
                    parent.setStatus(QuotationStatus.SENT);
                    quotationRepository.save(parent);
                }
            }

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