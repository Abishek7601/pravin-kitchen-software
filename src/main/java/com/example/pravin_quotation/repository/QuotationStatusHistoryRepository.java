package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationStatusHistoryRepository
        extends JpaRepository<QuotationStatusHistory, Long> {

    List<QuotationStatusHistory> findByQuotationIdOrderByChangedAtDesc(Long quotationId);

    List<QuotationStatusHistory> findByQuotationIdOrderByIdDesc(Long quotationId);

    long countByQuotationId(Long quotationId);
}