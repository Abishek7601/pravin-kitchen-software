package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationWorkflowRepository
        extends JpaRepository<QuotationWorkflow, Long> {

    List<QuotationWorkflow> findByQuotationIdOrderByCreatedAtDesc(
            Long quotationId
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM QuotationWorkflow w WHERE w.quotation.id IN :ids")
    void deleteByQuotationIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}