package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationCommunicationRepository
        extends JpaRepository<QuotationCommunication, Long> {

    List<QuotationCommunication> findByQuotationIdOrderBySentAtDesc(
            Long quotationId
    );
}