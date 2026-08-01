package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Notification;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.NotificationType;
import com.digitalwallet.domain.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void transactionCompleted(User user, NotificationType type, Transaction transaction) {
        notificationRepository.save(notification(user, type, title(type), message(type, transaction), transaction));
    }

    public void transactionFailed(User user, Transaction transaction, String reason) {
        notificationRepository.save(notification(user, NotificationType.TRANSACTION_FAILED,
                "Transaction failed", reason, transaction));
    }

    public void fraudAlert(User user, Transaction transaction) {
        notificationRepository.save(notification(user, NotificationType.FRAUD_ALERT,
                "Transaction under review",
                "Your transaction %s requires additional fraud review.".formatted(transaction.getReferenceNumber()),
                transaction));
    }

    private Notification notification(User user, NotificationType type, String title,
            String message, Transaction transaction) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTransaction(transaction);
        return notification;
    }

    private String title(NotificationType type) {
        return switch (type) {
            case DEPOSIT_SUCCESS -> "Deposit completed";
            case WITHDRAW_SUCCESS -> "Withdrawal completed";
            case TRANSFER_SENT -> "Transfer sent";
            case TRANSFER_RECEIVED -> "Transfer received";
            default -> "Wallet notification";
        };
    }

    private String message(NotificationType type, Transaction transaction) {
        return switch (type) {
            case DEPOSIT_SUCCESS -> "Deposit %s completed successfully.".formatted(transaction.getReferenceNumber());
            case WITHDRAW_SUCCESS -> "Withdrawal %s completed successfully.".formatted(transaction.getReferenceNumber());
            case TRANSFER_SENT -> "Transfer %s was sent successfully.".formatted(transaction.getReferenceNumber());
            case TRANSFER_RECEIVED -> "Transfer %s was received successfully.".formatted(transaction.getReferenceNumber());
            default -> "Transaction %s was updated.".formatted(transaction.getReferenceNumber());
        };
    }
}