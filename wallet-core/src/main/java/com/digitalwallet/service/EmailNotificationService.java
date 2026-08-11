package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    public EmailNotificationService(EmailService emailService, EmailTemplateService emailTemplateService) {
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
    }

    @Async("emailTaskExecutor")
    public void sendTransactionEmail(User user, Transaction transaction) {
        if (!canSend(user)) {
            return;
        }

        try {
            String subject = "Thong bao giao dich Digital Wallet";
            emailService.sendHtmlEmail(user.getEmail(), subject,
                    emailTemplateService.render("transaction-notification.html", Map.of(
                            "subject", subject,
                            "fullName", user.getFullName(),
                            "referenceNumber", reference(transaction),
                            "amount", amount(transaction),
                            "status", status(transaction))));
        } catch (MailException ex) {
            log.warn("Failed to send transaction email to userId={}, transactionId={}",
                    user.getId(), transactionId(transaction), ex);
        }
    }

    @Async("emailTaskExecutor")
    public void sendWithdrawalStatusEmail(User user, Transaction transaction, String statusMessage) {
        if (!canSend(user)) {
            return;
        }

        try {
            String subject = "Cap nhat rut tien Digital Wallet";
            emailService.sendHtmlEmail(user.getEmail(), subject,
                    emailTemplateService.render("withdrawal-status.html", Map.of(
                            "subject", subject,
                            "fullName", user.getFullName(),
                            "referenceNumber", reference(transaction),
                            "amount", amount(transaction),
                            "statusMessage", statusMessage)));
        } catch (MailException ex) {
            log.warn("Failed to send withdrawal status email to userId={}, transactionId={}",
                    user.getId(), transactionId(transaction), ex);
        }
    }

    @Async("emailTaskExecutor")
    public void sendSecurityAlertEmail(User user, Transaction transaction, String alertMessage) {
        if (!canSend(user)) {
            return;
        }

        try {
            String subject = "Canh bao bao mat Digital Wallet";
            emailService.sendHtmlEmail(user.getEmail(), subject,
                    emailTemplateService.render("security-alert.html", Map.of(
                            "subject", subject,
                            "fullName", user.getFullName(),
                            "alertMessage", alertMessage,
                            "referenceNumber", reference(transaction))));
        } catch (MailException ex) {
            log.error("Failed to send security alert email to userId={}, transactionId={}",
                    user.getId(), transactionId(transaction), ex);
        }
    }

    private boolean canSend(User user) {
        return user != null && user.getEmail() != null && !user.getEmail().isBlank();
    }

    private String reference(Transaction transaction) {
        if (transaction == null || transaction.getReferenceNumber() == null) {
            return "N/A";
        }
        return transaction.getReferenceNumber();
    }

    private Object amount(Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return "N/A";
        }
        return transaction.getAmount();
    }

    private String status(Transaction transaction) {
        if (transaction == null || transaction.getStatus() == null) {
            return "N/A";
        }
        return transaction.getStatus().name();
    }

    private Object transactionId(Transaction transaction) {
        return transaction == null ? null : transaction.getId();
    }
}