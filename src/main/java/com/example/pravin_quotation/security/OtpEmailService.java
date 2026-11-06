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

    public void sendSuperAdminOtp(
            String adminEmail,
            String adminName,
            String otp
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("Pravin KITCHENS & INTERIORS - Super Admin Login OTP");
        message.setText(
                "Hello " + (adminName != null && !adminName.trim().isEmpty() ? adminName.trim() : "Super Admin") + ",\n\n"
                        + "A login attempt was made for your Super Admin account in the "
                        + "Pravin KITCHENS & INTERIORS Quotation Management System.\n\n"
                        + "Your Login Verification OTP: " + otp + "\n\n"
                        + "This OTP is valid for 5 minutes.\n\n"
                        + "Maximum verification attempts: 5.\n\n"
                        + "If you did not initiate this login request, please change your password immediately.\n\n"
                        + "Regards,\n"
                        + "Pravin KITCHENS & INTERIORS\n"
                        + "Quotation Management System"
        );

        try {
            mailSender.send(message);
            log.info("Super Admin login OTP email successfully sent to [{}]", adminEmail);
        } catch (MailException e) {
            log.error("Failed to send Super Admin login OTP email to [{}]: {}", adminEmail, e.getMessage(), e);
            throw e;
        }
    }

    public void sendPasswordResetOtp(
            String adminEmail,
            String employeeName,
            String employeeIdentifier,
            String otp
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("Pravin KITCHENS & INTERIORS - Employee Password Reset OTP");
        message.setText(
                "Hello Admin,\n\n"
                        + "A password reset request has been initiated for an employee account in the "
                        + "Pravin KITCHENS & INTERIORS Quotation Management System.\n\n"
                        + "Employee Name: " + employeeName + "\n"
                        + "Employee Identifier: " + employeeIdentifier + "\n\n"
                        + "Password Reset OTP: " + otp + "\n\n"
                        + "This OTP is valid for 5 minutes.\n\n"
                        + "Maximum verification attempts: 5.\n\n"
                        + "If you or your staff did not initiate this request, please review account security.\n\n"
                        + "Regards,\n"
                        + "Pravin KITCHENS & INTERIORS\n"
                        + "Quotation Management System"
        );

        try {
            mailSender.send(message);
            log.info("Employee password reset OTP email successfully sent to Super Admin for employee [{}]", employeeName);
        } catch (MailException e) {
            log.error("Failed to send employee password reset OTP email for employee [{}]: {}", employeeName, e.getMessage(), e);
            throw e;
        }
    }
}