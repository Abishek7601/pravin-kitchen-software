package com.example.pravin_quotation.security;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {

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
                "Pravin Kitchens - Employee Login OTP"
        );

        message.setText(
                "Hello Admin,\n\n"

                        + "An employee is trying to log in to the "
                        + "Pravin Kitchens & Interiors "
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
                        + "Pravin Kitchens & Interiors\n"
                        + "Quotation Management System"
        );

        mailSender.send(message);
    }
}