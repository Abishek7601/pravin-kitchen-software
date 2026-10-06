package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationWorkflowRepository
        extends JpaRepository<QuotationWorkflow, Long> {

    List<QuotationWorkflow> findByQuotationIdOrderByCreatedAtDesc(
            Long quotationId
    );
}