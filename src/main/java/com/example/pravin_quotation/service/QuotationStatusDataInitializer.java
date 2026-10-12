package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.model.QuotationStatus;
import com.example.pravin_quotation.repository.QuotationCommunicationRepository;
import com.example.pravin_quotation.repository.QuotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ensures existing quotations are classified correctly on application startup
 * based on their actual workflow status and sending history.
 */
@Component
public class QuotationStatusDataInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(QuotationStatusDataInitializer.class);

    private final QuotationRepository quotationRepository;
    private final QuotationCommunicationRepository communicationRepository;

    public QuotationStatusDataInitializer(
            QuotationRepository quotationRepository,
            QuotationCommunicationRepository communicationRepository
    ) {
        this.quotationRepository = quotationRepository;
        this.communicationRepository = communicationRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // Find IDs of quotations that were sent via EMAIL or WHATSAPP
            List<QuotationCommunication> sentCommunications =
                    communicationRepository.findByStatus(
                            QuotationCommunication.CommunicationStatus.SENT
                    );

            Set<Long> sentQuotationIds = sentCommunications.stream()
                    .filter(c -> c.getType() == QuotationCommunication.CommunicationType.EMAIL
                            || c.getType() == QuotationCommunication.CommunicationType.WHATSAPP)
                    .map(c -> c.getQuotation().getId())
                    .collect(Collectors.toSet());

            List<Quotation> allQuotations = quotationRepository.findAll();

            int updatedToSent = 0;

            for (Quotation quotation : allQuotations) {
                if (sentQuotationIds.contains(quotation.getId())
                        && quotation.getStatus() != QuotationStatus.SENT) {
                    quotation.setStatus(QuotationStatus.SENT);
                    quotationRepository.save(quotation);
                    updatedToSent++;
                }
            }

            if (updatedToSent > 0) {
                log.info("Quotation status alignment complete: {} updated to SENT", updatedToSent);
            }
        } catch (Exception e) {
            log.warn("Quotation status alignment skipped or encountered an issue: {}", e.getMessage());
        }
    }
}
