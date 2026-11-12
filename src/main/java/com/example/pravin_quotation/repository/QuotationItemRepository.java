package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    List<QuotationItem> findByQuotationRoomIdOrderByIdAsc(Long quotationRoomId);

    List<QuotationItem> findByQuotationRoomIdAndActiveTrueOrderByIdAsc(Long quotationRoomId);

    long countByQuotationRoomId(Long quotationRoomId);

    long countByQuotationRoomIdAndActiveTrue(Long quotationRoomId);

    List<QuotationItem> findByItemIdOrderByCreatedAtDesc(Long itemId);

    List<QuotationItem> findByMaterialIdOrderByCreatedAtDesc(Long materialId);

    List<QuotationItem> findByMaterialOptionIdOrderByCreatedAtDesc(Long materialOptionId);

    List<QuotationItem> findByMaterialSpecificationIdOrderByCreatedAtDesc(Long materialSpecificationId);

    long countByMaterialSpecificationId(Long materialSpecificationId);

    long countByMaterialOptionId(Long materialOptionId);

    long countByMaterialId(Long materialId);

    long countByDivisionId(Long divisionId);

    long countByWorkCategoryId(Long workCategoryId);

    List<QuotationItem> findByQuotationRoomId(Long quotationRoomId);

    @Query("SELECT COUNT(qi) FROM QuotationItem qi WHERE qi.quotationRoom.quotation.id = :quotationId AND qi.material.id = :materialId")
    long countByQuotationIdAndMaterialId(@Param("quotationId") Long quotationId, @Param("materialId") Long materialId);

    @Query("SELECT DISTINCT " +
           "COALESCE(q.parent.id, q.id), " +
           "qi.pricingMode, " +
           "m.pricingGrade, " +
           "q.pricingMode " +
           "FROM QuotationItem qi " +
           "JOIN qi.quotationRoom qr " +
           "JOIN qr.quotation q " +
           "LEFT JOIN qi.material m " +
           "WHERE qi.active = true AND qr.active = true " +
           "AND (q.id IN :quoteIds OR (q.parent IS NOT NULL AND q.parent.id IN :quoteIds))")
    List<Object[]> findDistinctUsedGradesForQuotationIds(@Param("quoteIds") java.util.Collection<Long> quoteIds);
}
