package com.example.pravin_quotation.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {

        int otp =
                100000 + secureRandom.nextInt(900000);

        return String.valueOf(otp);
    }

    public LocalDateTime getExpiryTime() {

        return LocalDateTime.now()
                .plusMinutes(OTP_EXPIRY_MINUTES);
    }

    public boolean isExpired(LocalDateTime expiryTime) {

        if (expiryTime == null) {
            return true;
        }

        return LocalDateTime.now()
                .isAfter(expiryTime);
    }

    public boolean isResendAllowed(
            LocalDateTime lastSentTime
    ) {

        if (lastSentTime == null) {
            return true;
        }

        return LocalDateTime.now()
                .isAfter(
                        lastSentTime.plusSeconds(
                                RESEND_COOLDOWN_SECONDS
                        )
                );
    }

    public boolean isAttemptsExceeded(
            Integer attempts
    ) {

        if (attempts == null) {
            return false;
        }

        return attempts >= MAX_ATTEMPTS;
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }
}