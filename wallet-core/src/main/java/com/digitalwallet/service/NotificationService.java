package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Notification;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.NotificationType;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.domain.repository.NotificationRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification create(NotificationType type, User user, String title,
            String message, Map<String, Object> metadata) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setMetadata(metadata);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(User currentUser, UUID notificationPublicId) {
        Notification notification = notificationRepository
                .findByPublicIdAndUserId(notificationPublicId, currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (notification.isRead()) {
            return notification;
        }
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(User currentUser) {
        return notificationRepository.markAllAsRead(currentUser.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User currentUser, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User currentUser, Pageable pageable, boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(currentUser.getId(), pageable);
        }
        return getUserNotifications(currentUser, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByUserIdAndReadFalse(currentUser.getId());
    }

    Notification createForTransaction(User user, NotificationType type, Transaction transaction) {
        return create(type, user, title(type), message(type, transaction), metadata(transaction));
    }

    Notification createForTransaction(User user, NotificationType type, String title,
            String message, Transaction transaction) {
        return create(type, user, title, message, metadata(transaction));
    }

    NotificationType failedType(Transaction transaction) {
        if (transaction != null && transaction.getTransactionType() == TransactionType.WITHDRAW) {
            return NotificationType.WITHDRAWAL_REJECTED;
        }
        return NotificationType.SYSTEM_ANNOUNCEMENT;
    }

    private String title(NotificationType type) {
        return switch (type) {
            case TRANSACTION_SENT -> "Transaction sent";
            case TRANSACTION_RECEIVED -> "Transaction received";
            case WITHDRAWAL_APPROVED -> "Withdrawal approved";
            case WITHDRAWAL_REJECTED -> "Withdrawal rejected";
            case FRAUD_ALERT -> "Fraud alert";
            case SYSTEM_ANNOUNCEMENT -> "Wallet notification";
        };
    }

    private String message(NotificationType type, Transaction transaction) {
        String reference = transaction == null ? "" : transaction.getReferenceNumber();
        return switch (type) {
            case TRANSACTION_SENT -> "Transaction %s was sent successfully.".formatted(reference);
            case TRANSACTION_RECEIVED -> "Transaction %s was received successfully.".formatted(reference);
            case WITHDRAWAL_APPROVED -> "Withdrawal %s was approved.".formatted(reference);
            case WITHDRAWAL_REJECTED -> "Withdrawal %s was rejected.".formatted(reference);
            case FRAUD_ALERT -> "Transaction %s requires additional fraud review.".formatted(reference);
            case SYSTEM_ANNOUNCEMENT -> "You have a new wallet notification.";
        };
    }

    private Map<String, Object> metadata(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return Map.of(
                "transaction_id", transaction.getId(),
                "reference_number", transaction.getReferenceNumber(),
                "transaction_type", transaction.getTransactionType().name());
    }
}