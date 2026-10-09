package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.DistrictPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistrictPricingRepository extends JpaRepository<DistrictPricing, Long> {

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