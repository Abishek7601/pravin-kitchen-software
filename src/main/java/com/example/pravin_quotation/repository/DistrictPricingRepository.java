package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.DistrictPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DistrictPricingRepository extends JpaRepository<DistrictPricing, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM DistrictPricing dp WHERE dp.pricing.id = :pricingId")
    void deleteByPricingId(@Param("pricingId") Long pricingId);

    Optional<DistrictPricing> findByBranchIdAndPricingId(
            Long branchId,
            Long pricingId
    );

    boolean existsByBranchIdAndPricingId(
            Long branchId,
            Long pricingId
    );

    boolean existsByBranchIdAndPricingIdAndIdNot(
            Long branchId,
            Long pricingId,
            Long id
    );

    boolean existsByPricingId(Long pricingId);

    List<DistrictPricing> findAllByOrderByCreatedAtDesc();

    List<DistrictPricing> findByActiveTrueOrderByCreatedAtDesc();

    List<DistrictPricing> findByBranchIdOrderByCreatedAtDesc(
            Long branchId
    );

    List<DistrictPricing> findByBranchIdAndActiveTrueOrderByCreatedAtDesc(
            Long branchId
    );

    List<DistrictPricing> findByPricingIdOrderByCreatedAtDesc(
            Long pricingId
    );
}