package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.model.PricingMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingRepository
        extends JpaRepository<Pricing, Long> {

    Optional<Pricing> findByMaterialOptionIdAndPricingMode(
            Long materialOptionId,
            PricingMode pricingMode
    );

    boolean existsByMaterialOptionIdAndPricingMode(
            Long materialOptionId,
            PricingMode pricingMode
    );

    boolean existsByMaterialOptionIdAndPricingModeAndIdNot(
            Long materialOptionId,
            PricingMode pricingMode,
            Long id
    );

    List<Pricing> findAllByOrderByCreatedAtDesc();

    List<Pricing> findByActiveTrueOrderByCreatedAtDesc();

    List<Pricing> findByMaterialOptionIdOrderByPricingModeAsc(
            Long materialOptionId
    );

    List<Pricing> findByPricingModeOrderByCreatedAtDesc(
            PricingMode pricingMode
    );
}