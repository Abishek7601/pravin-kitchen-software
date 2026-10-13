package com.example.pravin_quotation.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailService.class);

    private final JavaMailSender mailSender;

    public OtpEmailService(
            JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    public void sendEmployeeOtp(
            String adminEmail,
            String employeeName,
            String otp
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(adminEmail);

        message.setSubject(
                "Pravin KITCHENS & INTERIORS - Employee Login OTP"
        );

        message.setText(
                "Hello Admin,\n\n"

                        + "An employee is trying to log in to the "
                        + "Pravin KITCHENS & INTERIORS "
                        + "Quotation Management System.\n\n"

                        + "Employee: "
                        + employeeName
                        + "\n\n"

                        + "Employee Login OTP: "
                        + otp
                        + "\n\n"

                        + "This OTP is valid for 5 minutes.\n\n"

                        + "Maximum verification attempts: 5.\n\n"

                        + "If you did not expect this login request, "
                        + "please ignore this email.\n\n"

                        + "Regards,\n"
                        + "Pravin KITCHENS & INTERIORS\n"
                        + "Quotation Management System"
        );

        try {
            mailSender.send(message);
            log.info("Employee login OTP email successfully sent to Super Admin for employee [{}]", employeeName);
        } catch (MailException e) {
            log.error("Failed to send employee login OTP email for employee [{}]: {}", employeeName, e.getMessage(), e);
            throw e;
        }
    }
}