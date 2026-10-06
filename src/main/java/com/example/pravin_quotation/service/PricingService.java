package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.MaterialOption;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.model.PricingMode;
import com.example.pravin_quotation.repository.MaterialOptionRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {

    private final PricingRepository pricingRepository;
    private final MaterialOptionRepository materialOptionRepository;

    public PricingService(
            PricingRepository pricingRepository,
            MaterialOptionRepository materialOptionRepository
    ) {
        this.pricingRepository = pricingRepository;
        this.materialOptionRepository = materialOptionRepository;
    }

    public List<Pricing> getAllPricing() {
        return pricingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Pricing> getActivePricing() {
        return pricingRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public Pricing getPricingById(Long id) {
        return pricingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pricing record not found"
                ));
    }

    public List<Pricing> getPricingByMaterialOption(Long materialOptionId) {
        return pricingRepository
                .findByMaterialOptionIdOrderByPricingModeAsc(
                        materialOptionId
                );
    }

    public Pricing getPricing(
            Long materialOptionId,
            PricingMode pricingMode
    ) {
        return pricingRepository
                .findByMaterialOptionIdAndPricingMode(
                        materialOptionId,
                        pricingMode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pricing not found for the selected material option and pricing mode"
                ));
    }

    public Pricing createPricing(
            Long materialOptionId,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String description
    ) {

        validateMaterialOption(materialOptionId);
        validatePricingMode(pricingMode);
        validateRate(rate);
        validateUnit(unit);

        MaterialOption materialOption =
                materialOptionRepository.findById(materialOptionId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Material option not found"
                        ));

        if (pricingRepository
                .existsByMaterialOptionIdAndPricingMode(
                        materialOptionId,
                        pricingMode
                )) {

            throw new IllegalArgumentException(
                    "Pricing already exists for this material option and pricing mode"
            );
        }

        Pricing pricing = new Pricing();

        pricing.setMaterialOption(materialOption);
        pricing.setPricingMode(pricingMode);
        pricing.setRate(rate);
        pricing.setUnit(unit.trim().toUpperCase());
        pricing.setDescription(cleanValue(description));
        pricing.setActive(true);

        return pricingRepository.save(pricing);
    }

    public Pricing updatePricing(
            Long id,
            Long materialOptionId,
            PricingMode pricingMode,
            BigDecimal rate,
            String unit,
            String description
    ) {

        Pricing pricing = getPricingById(id);

        validateMaterialOption(materialOptionId);
        validatePricingMode(pricingMode);
        validateRate(rate);
        validateUnit(unit);

        MaterialOption materialOption =
                materialOptionRepository.findById(materialOptionId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Material option not found"
                        ));

        if (pricingRepository
                .existsByMaterialOptionIdAndPricingModeAndIdNot(
                        materialOptionId,
                        pricingMode,
                        id
                )) {

            throw new IllegalArgumentException(
                    "Another pricing record already exists for this material option and pricing mode"
            );
        }

        pricing.setMaterialOption(materialOption);
        pricing.setPricingMode(pricingMode);
        pricing.setRate(rate);
        pricing.setUnit(unit.trim().toUpperCase());
        pricing.setDescription(cleanValue(description));

        return pricingRepository.save(pricing);
    }

    public void togglePricingStatus(Long id) {

        Pricing pricing = getPricingById(id);

        pricing.setActive(
                !Boolean.TRUE.equals(pricing.getActive())
        );

        pricingRepository.save(pricing);
    }

    public void deletePricing(Long id) {

        Pricing pricing = getPricingById(id);

        pricingRepository.delete(pricing);
    }

    private void validateMaterialOption(Long materialOptionId) {

        if (materialOptionId == null) {
            throw new IllegalArgumentException(
                    "Material option is required"
            );
        }
    }

    private void validatePricingMode(PricingMode pricingMode) {

        if (pricingMode == null) {
            throw new IllegalArgumentException(
                    "Pricing mode is required"
            );
        }
    }

    private void validateRate(BigDecimal rate) {

        if (rate == null) {
            throw new IllegalArgumentException(
                    "Rate is required"
            );
        }

        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Rate cannot be negative"
            );
        }
    }

    private void validateUnit(String unit) {

        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Unit is required"
            );
        }
    }

    private String cleanValue(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}