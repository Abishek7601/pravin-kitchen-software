package com.example.pravin_quotation.repository;

import com.example.pravin_quotation.model.EmployeeDailyOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EmployeeDailyOtpVerificationRepository
        extends JpaRepository<EmployeeDailyOtpVerification, Long> {

    boolean existsByUserIdAndVerificationDateAndActiveTrue(Long userId, LocalDate verificationDate);

    Optional<EmployeeDailyOtpVerification> findByUserIdAndVerificationDateAndActiveTrue(Long userId, LocalDate verificationDate);

    void deleteByUserIdAndVerificationDate(Long userId, LocalDate verificationDate);
}

