package com.careidentityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = getVerificationUrl(token);
        String subject = "CareMatch360 - Verify Your Email";
        String text = String.format(
                "Welcome to CareMatch360!\n\n" +
                "Please click the link below to verify your email address:\n\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't create an account with CareMatch360, please ignore this email.\n\n" +
                "Best regards,\n" +
                "CareMatch360 Team",
                verificationUrl
        );
        
        sendEmail(to, subject, text);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = getPasswordResetUrl(token);
        String subject = "CareMatch360 - Password Reset Request";
        String text = String.format(
                "Hello,\n\n" +
                "We received a request to reset your password.\n\n" +
                "Please click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "CareMatch360 Team",
                resetUrl
        );
        
        sendEmail(to, subject, text);
    }

    @Value("${app.email.verification.url}")
    private String verificationBaseUrl;

    @Value("${app.email.reset-password.url}")
    private String resetPasswordBaseUrl;

    private String getVerificationUrl(String token) {
        return verificationBaseUrl + token;
    }

    private String getPasswordResetUrl(String token) {
        return resetPasswordBaseUrl + token;
    }
}