package com.digitalwallet.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("asyncExecutor")
    public void sendVerificationEmail(String toEmail, String otp) {
        sendOtpEmail(toEmail, "Xac thuc tai khoan Digital Wallet System",
                "Ma OTP xac thuc cua ban la: %s".formatted(otp));
    }

    @Async("asyncExecutor")
    public void sendPasswordResetEmail(String toEmail, String otp) {
        sendOtpEmail(toEmail, "Dat lai mat khau Digital Wallet System",
                "Ma OTP dat lai mat khau cua ban la: %s".formatted(otp));
    }

    @Async("asyncExecutor")
    public void sendMfaEmail(String toEmail, String otp) {
        sendOtpEmail(toEmail, "Ma xac thuc dang nhap Digital Wallet System",
                "Ma OTP dang nhap cua ban la: %s".formatted(otp));
    }

    public void sendSimpleEmail(String toEmail, String subject, String text) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendHtmlEmail(String toEmail, String subject, String html) throws MailException {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Failed to prepare HTML email", ex);
        }
    }

    private void sendOtpEmail(String toEmail, String subject, String firstLine) {
        try {
            sendSimpleEmail(toEmail, subject, """
                    %s

                    Ma co hieu luc trong 10 phut. Vui long khong chia se ma nay voi bat ky ai.
                    """.formatted(firstLine));
        } catch (MailException ex) {
            log.error("Failed to send OTP email to {}", toEmail, ex);
        }
    }
}