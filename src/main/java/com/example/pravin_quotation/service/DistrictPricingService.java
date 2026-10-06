package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.DistrictPricing;
import com.example.pravin_quotation.model.Pricing;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.DistrictPricingRepository;
import com.example.pravin_quotation.repository.PricingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DistrictPricingService {

    private final DistrictPricingRepository districtPricingRepository;
    private final BranchRepository branchRepository;
    private final PricingRepository pricingRepository;

    public DistrictPricingService(
            DistrictPricingRepository districtPricingRepository,
            BranchRepository branchRepository,
            PricingRepository pricingRepository
    ) {
        this.districtPricingRepository = districtPricingRepository;
        this.branchRepository = branchRepository;
        this.pricingRepository = pricingRepository;
    }


    // Get all district pricing
    public List<DistrictPricing> getAllDistrictPricing() {

        return districtPricingRepository.findAllByOrderByCreatedAtDesc();
    }


    // Get active district pricing
    public List<DistrictPricing> getActiveDistrictPricing() {

        return districtPricingRepository.findByActiveTrueOrderByCreatedAtDesc();
    }


    // Get district pricing by ID
    public DistrictPricing getDistrictPricingById(Long id) {

        return districtPricingRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "District pricing not found with ID: " + id
                        )
                );
    }


    // Get pricing for a specific district
    public List<DistrictPricing> getDistrictPricingByBranch(Long branchId) {

        validateBranchId(branchId);

        return districtPricingRepository
                .findByBranchIdOrderByCreatedAtDesc(branchId);
    }


    // Get active pricing for a specific district
    public List<DistrictPricing> getActiveDistrictPricingByBranch(Long branchId) {

        validateBranchId(branchId);

        return districtPricingRepository
                .findByBranchIdAndActiveTrueOrderByCreatedAtDesc(branchId);
    }


    // Get districts using a particular pricing
    public List<DistrictPricing> getDistrictsByPricing(Long pricingId) {

        validatePricingId(pricingId);

        return districtPricingRepository
                .findByPricingIdOrderByCreatedAtDesc(pricingId);
    }


    // Get one specific district pricing
    public DistrictPricing getDistrictPricing(
            Long branchId,
            Long pricingId
    ) {

        validateBranchId(branchId);
        validatePricingId(pricingId);

        return districtPricingRepository
                .findByBranchIdAndPricingId(branchId, pricingId)
                .orElse(null);
    }


    // Create district pricing
    public DistrictPricing createDistrictPricing(
            Long branchId,
            Long pricingId,
            BigDecimal rate,
            String description
    ) {

        validateBranchId(branchId);
        validatePricingId(pricingId);
        validateRate(rate);

        if (districtPricingRepository
                .existsByBranchIdAndPricingId(branchId, pricingId)) {

            throw new RuntimeException(
                    "Pricing already exists for this district."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "District not found with ID: " + branchId
                        )
                );

        Pricing pricing = pricingRepository.findById(pricingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pricing not found with ID: " + pricingId
                        )
                );

        DistrictPricing districtPricing = new DistrictPricing();

        districtPricing.setBranch(branch);
        districtPricing.setPricing(pricing);
        districtPricing.setRate(rate);
        districtPricing.setDescription(cleanValue(description));
        districtPricing.setActive(true);

        return districtPricingRepository.save(districtPricing);
    }


    // Update district pricing
    public DistrictPricing updateDistrictPricing(
            Long id,
            Long branchId,
            Long pricingId,
            BigDecimal rate,
            String description
    ) {

        validateId(id);
        validateBranchId(branchId);
        validatePricingId(pricingId);
        validateRate(rate);

        DistrictPricing districtPricing =
                getDistrictPricingById(id);

        if (districtPricingRepository
                .existsByBranchIdAndPricingIdAndIdNot(
                        branchId,
                        pricingId,
                        id
                )) {

            throw new RuntimeException(
                    "Pricing already exists for this district."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "District not found with ID: " + branchId
                        )
                );

        Pricing pricing = pricingRepository.findById(pricingId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pricing not found with ID: " + pricingId
                        )
                );

        districtPricing.setBranch(branch);
        districtPricing.setPricing(pricing);
        districtPricing.setRate(rate);
        districtPricing.setDescription(cleanValue(description));

        return districtPricingRepository.save(districtPricing);
    }


    // Toggle active/inactive
    public void toggleDistrictPricingStatus(Long id) {

        DistrictPricing districtPricing =
                getDistrictPricingById(id);

        districtPricing.setActive(
                !Boolean.TRUE.equals(districtPricing.getActive())
        );

        districtPricingRepository.save(districtPricing);
    }


    // Delete district pricing
    public void deleteDistrictPricing(Long id) {

        DistrictPricing districtPricing =
                getDistrictPricingById(id);

        districtPricingRepository.delete(districtPricing);
    }


    // Validation: District
    private void validateBranchId(Long branchId) {

        if (branchId == null || branchId <= 0) {

            throw new IllegalArgumentException(
                    "District is required."
            );
        }
    }


    // Validation: Pricing
    private void validatePricingId(Long pricingId) {

        if (pricingId == null || pricingId <= 0) {

            throw new IllegalArgumentException(
                    "Pricing is required."
            );
        }
    }


    // Validation: ID
    private void validateId(Long id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Invalid district pricing ID."
            );
        }
    }


    // Validation: Rate
    private void validateRate(BigDecimal rate) {

        if (rate == null) {

            throw new IllegalArgumentException(
                    "Rate is required."
            );
        }

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Rate must be greater than zero."
            );
        }
    }


    // Clean text values
    private String cleanValue(String value) {

        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}