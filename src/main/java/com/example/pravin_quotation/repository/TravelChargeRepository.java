package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.TravelCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelChargeRepository extends JpaRepository<TravelCharge, Long> {

    Optional<TravelCharge> findByBranchId(Long branchId);

    boolean existsByBranchId(Long branchId);

    boolean existsByBranchIdAndIdNot(Long branchId, Long id);

    List<TravelCharge> findAllByOrderByCreatedAtDesc();

    List<TravelCharge> findByActiveTrueOrderByCreatedAtDesc();

    List<TravelCharge> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<TravelCharge> findByBranchIdAndActiveTrueOrderByCreatedAtDesc(Long branchId);
}