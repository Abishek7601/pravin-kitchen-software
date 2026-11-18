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

    java.util.Optional<QuotationRoom> findByQuotationIdAndFloorAndRoomAndActiveTrue(
            Long quotationId,
            String floor,
            String room
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM QuotationRoom r WHERE r.quotation.id IN :ids")
    void deleteByQuotationIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}