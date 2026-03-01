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
    public void sendVerificationEmail(String to, String code) {
        String subject = "CareMatch360 - Your Verification Code";
        String text = String.format(
                "Welcome to CareMatch360!\n\n" +
                "Please use the verification code below to confirm your email address:\n\n" +
                "    %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't create an account with CareMatch360, please ignore this email.\n\n" +
                "Best regards,\n" +
                "CareMatch360 Team",
                code
        );

        sendEmail(to, subject, text);
    }

    @Async
    public void sendPasswordResetEmail(String to, String code) {
        String subject = "CareMatch360 - Your Password Reset Code";
        String text = String.format(
                "Hello,\n\n" +
                "We received a request to reset your password.\n\n" +
                "Please use the code below to reset your password:\n\n" +
                "    %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "CareMatch360 Team",
                code
        );

        sendEmail(to, subject, text);
    }
}