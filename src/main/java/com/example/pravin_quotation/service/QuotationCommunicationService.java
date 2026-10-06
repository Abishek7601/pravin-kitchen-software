package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Quotation;
import com.example.pravin_quotation.model.QuotationCommunication;
import com.example.pravin_quotation.repository.QuotationCommunicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuotationCommunicationService {

    private final QuotationCommunicationRepository repository;


    public QuotationCommunicationService(
            QuotationCommunicationRepository repository) {
        this.repository = repository;
    }

    // ---------------------------------------------------------
    // Record communication
    // ---------------------------------------------------------

    @Transactional
    public QuotationCommunication record(
            Quotation quotation,
            QuotationCommunication.CommunicationType type,
            String recipient,
            QuotationCommunication.CommunicationStatus status,
            String message
    ) {

        if (quotation == null) {
            throw new IllegalArgumentException(
                    "Quotation is required."
            );
        }

        QuotationCommunication communication =
                new QuotationCommunication();

        communication.setQuotation(quotation);
        communication.setType(type);
        communication.setRecipient(recipient);
        communication.setStatus(status);
        communication.setMessage(message);
        communication.setSentAt(LocalDateTime.now());

        return repository.save(communication);
    }

    // ---------------------------------------------------------
    // Get quotation communications
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<QuotationCommunication> getByQuotationId(
            Long quotationId) {

        return repository
                .findByQuotationIdOrderBySentAtDesc(
                        quotationId
                );
    }
}