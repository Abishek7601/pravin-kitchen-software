package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    List<QuotationItem> findByQuotationRoomIdOrderByIdAsc(Long quotationRoomId);

    List<QuotationItem> findByQuotationRoomIdAndActiveTrueOrderByIdAsc(Long quotationRoomId);

    long countByQuotationRoomId(Long quotationRoomId);

    long countByQuotationRoomIdAndActiveTrue(Long quotationRoomId);

    List<QuotationItem> findByItemIdOrderByCreatedAtDesc(Long itemId);

    List<QuotationItem> findByMaterialIdOrderByCreatedAtDesc(Long materialId);

    List<QuotationItem> findByMaterialOptionIdOrderByCreatedAtDesc(Long materialOptionId);

    List<QuotationItem> findByQuotationRoomId(Long quotationRoomId);
}