package com.example.pravin_quotation.service;

import com.example.pravin_quotation.model.Branch;
import com.example.pravin_quotation.model.TravelCharge;
import com.example.pravin_quotation.repository.BranchRepository;
import com.example.pravin_quotation.repository.TravelChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TravelChargeService {

    private final TravelChargeRepository travelChargeRepository;
    private final BranchRepository branchRepository;

    public TravelChargeService(
            TravelChargeRepository travelChargeRepository,
            BranchRepository branchRepository
    ) {
        this.travelChargeRepository = travelChargeRepository;
        this.branchRepository = branchRepository;
    }

    public List<TravelCharge> getAllTravelCharges() {
        return travelChargeRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<TravelCharge> getActiveTravelCharges() {
        return travelChargeRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public TravelCharge getById(Long id) {
        return travelChargeRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Travel charge not found: " + id)
                );
    }

    public TravelCharge getByBranchId(Long branchId) {
        return travelChargeRepository.findByBranchId(branchId)
                .orElse(null);
    }

    public List<TravelCharge> getAllByBranchId(Long branchId) {
        return travelChargeRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
    }

    public TravelCharge getActiveByBranchId(Long branchId) {
        return travelChargeRepository
                .findByBranchIdAndActiveTrueOrderByCreatedAtDesc(branchId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public TravelCharge create(
            Long branchId,
            BigDecimal amount,
            String description
    ) {
        validateBranchId(branchId);
        validateAmount(amount);

        if (travelChargeRepository.existsByBranchId(branchId)) {
            throw new IllegalArgumentException(
                    "A travel charge already exists for this district."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("District not found: " + branchId)
                );

        TravelCharge travelCharge = new TravelCharge();

        travelCharge.setBranch(branch);
        travelCharge.setAmount(amount);
        travelCharge.setDescription(cleanText(description));
        travelCharge.setActive(true);

        return travelChargeRepository.save(travelCharge);
    }

    @Transactional
    public TravelCharge update(
            Long id,
            Long branchId,
            BigDecimal amount,
            String description
    ) {
        validateId(id);
        validateBranchId(branchId);
        validateAmount(amount);

        TravelCharge existing = getById(id);

        if (travelChargeRepository.existsByBranchIdAndIdNot(branchId, id)) {
            throw new IllegalArgumentException(
                    "A travel charge already exists for this district."
            );
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new IllegalArgumentException("District not found: " + branchId)
                );

        existing.setBranch(branch);
        existing.setAmount(amount);
        existing.setDescription(cleanText(description));

        return travelChargeRepository.save(existing);
    }

    @Transactional
    public TravelCharge toggleStatus(Long id) {
        TravelCharge travelCharge = getById(id);

        boolean currentStatus =
                Boolean.TRUE.equals(travelCharge.getActive());

        travelCharge.setActive(!currentStatus);

        return travelChargeRepository.save(travelCharge);
    }

    @Transactional
    public void delete(Long id) {
        TravelCharge travelCharge = getById(id);

        travelChargeRepository.delete(travelCharge);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Invalid travel charge ID."
            );
        }
    }

    private void validateBranchId(Long branchId) {
        if (branchId == null || branchId <= 0) {
            throw new IllegalArgumentException(
                    "Please select a district."
            );
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException(
                    "Travel charge amount is required."
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Travel charge cannot be negative."
            );
        }
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}