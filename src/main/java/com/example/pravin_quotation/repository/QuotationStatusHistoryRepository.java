package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationStatusHistoryRepository
        extends JpaRepository<QuotationStatusHistory, Long> {

    List<QuotationStatusHistory> findByQuotationIdOrderByChangedAtDesc(Long quotationId);

    List<QuotationStatusHistory> findByQuotationIdOrderByIdDesc(Long quotationId);

    long countByQuotationId(Long quotationId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM QuotationStatusHistory h WHERE h.quotation.id IN :ids")
    void deleteByQuotationIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}