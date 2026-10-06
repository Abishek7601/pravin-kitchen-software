package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationRoomRepository extends JpaRepository<QuotationRoom, Long> {

    List<QuotationRoom> findByQuotationIdOrderByIdAsc(Long quotationId);

    List<QuotationRoom> findByQuotationIdAndActiveTrueOrderByIdAsc(Long quotationId);

    List<QuotationRoom> findByQuotationId(Long quotationId);

    long countByQuotationId(Long quotationId);

    long countByQuotationIdAndActiveTrue(Long quotationId);
}