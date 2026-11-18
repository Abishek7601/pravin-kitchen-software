package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationCommunicationRepository
        extends JpaRepository<QuotationCommunication, Long> {

    List<QuotationCommunication> findByQuotationIdOrderBySentAtDesc(
            Long quotationId
    );

    List<QuotationCommunication> findByStatus(
            QuotationCommunication.CommunicationStatus status
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM QuotationCommunication c WHERE c.quotation.id IN :ids")
    void deleteByQuotationIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}