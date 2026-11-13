package com.example.pravin_quotation.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_daily_otp_verifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_daily_otp_user_date", columnNames = {"user_id", "verification_date"})
        },
        indexes = {
                @Index(name = "idx_emp_daily_otp_user_date", columnList = "user_id, verification_date"),
                @Index(name = "idx_emp_daily_otp_date", columnList = "verification_date")
        }
)
public class EmployeeDailyOtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "verification_date", nullable = false)
    private LocalDate verificationDate;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private Boolean active = true;

    public EmployeeDailyOtpVerification() {
    }

    public EmployeeDailyOtpVerification(User user, LocalDate verificationDate, LocalDateTime verifiedAt) {
        this.user = user;
        this.verificationDate = verificationDate;
        this.verifiedAt = verifiedAt;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getVerificationDate() {
        return verificationDate;
    }

    public void setVerificationDate(LocalDate verificationDate) {
        this.verificationDate = verificationDate;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
