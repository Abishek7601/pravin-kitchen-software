package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.QuotationItemSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationItemSizeRepository
        extends JpaRepository<QuotationItemSize, Long> {

    // =========================================================
    // GET ALL ACTIVE SIZES FOR A QUOTATION ITEM
    // =========================================================

    List<QuotationItemSize> findByQuotationItemIdAndActiveTrueOrderByDisplayOrderAsc(
            Long quotationItemId
    );


    // =========================================================
    // GET ALL SIZES FOR A QUOTATION ITEM
    // =========================================================

    List<QuotationItemSize> findByQuotationItemIdOrderByDisplayOrderAsc(
            Long quotationItemId
    );


    // =========================================================
    // COUNT SIZES
    // =========================================================

    long countByQuotationItemIdAndActiveTrue(
            Long quotationItemId
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("DELETE FROM QuotationItemSize s WHERE s.quotationItem.id IN (" +
            "SELECT i.id FROM QuotationItem i WHERE i.quotationRoom.id IN (" +
            "SELECT r.id FROM QuotationRoom r WHERE r.quotation.id IN :ids))")
    void deleteByQuotationIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}