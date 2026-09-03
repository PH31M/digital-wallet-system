package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Notification;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.event.BalanceUpdatedEvent;
import com.digitalwallet.domain.event.FraudAlertEvent;
import com.digitalwallet.domain.event.TransactionCompletedEvent;
import com.digitalwallet.domain.event.TransactionFailedEvent;
import com.digitalwallet.domain.enums.NotificationType;
import com.digitalwallet.domain.enums.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventListener {

    private static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";
    private static final String USER_BALANCE_DESTINATION = "/queue/balance";

    private final NotificationService notificationService;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final EmailNotificationService emailNotificationService;
    private final BigDecimal largeTransactionEmailThreshold;

    public NotificationEventListener(NotificationService notificationService,
            WebSocketEventPublisher webSocketEventPublisher,
            EmailNotificationService emailNotificationService,
            @Value("${wallet.notifications.large-transaction-email-threshold:10000000}")
            BigDecimal largeTransactionEmailThreshold) {
        this.notificationService = notificationService;
        this.webSocketEventPublisher = webSocketEventPublisher;
        this.emailNotificationService = emailNotificationService;
        this.largeTransactionEmailThreshold = largeTransactionEmailThreshold;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        event.recipients().forEach(recipient -> {
            Notification notification = notificationService.createForTransaction(
                    recipient.user(), recipient.notificationType(), event.transaction());
            publishNotification(recipient.userPublicId(), notification);
            sendCompletedEmailIfRequired(recipient.user(), recipient.notificationType(), event.transaction());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionFailed(TransactionFailedEvent event) {
        Notification notification = notificationService.createForTransaction(event.user(),
                notificationService.failedType(event.transaction()),
                "Transaction failed", event.reason(), event.transaction());
        publishNotification(event.userPublicId(), notification);
        if (isWithdrawal(event.transaction())) {
            emailNotificationService.sendWithdrawalStatusEmail(event.user(), event.transaction(), event.reason());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFraudAlert(FraudAlertEvent event) {
        Notification notification = notificationService.createForTransaction(event.user(),
                NotificationType.FRAUD_ALERT,
                "Transaction under review",
                "Your transaction %s requires additional fraud review."
                        .formatted(event.transaction().getReferenceNumber()),
                event.transaction());
        publishNotification(event.userPublicId(), notification);
        emailNotificationService.sendSecurityAlertEmail(event.user(), event.transaction(),
                "Giao dich cua ban can duoc xem xet them vi co dau hieu rui ro.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBalanceUpdated(BalanceUpdatedEvent event) {
        webSocketEventPublisher.publishToUser(event.userPublicId(),
                USER_BALANCE_DESTINATION,
                new BalanceUpdatedPayload(event.walletId(), event.balance()));
    }

    private void publishNotification(UUID userPublicId, Notification notification) {
        webSocketEventPublisher.publishToUser(userPublicId,
                USER_NOTIFICATION_DESTINATION,
                NotificationRealtimePayload.from(notification));
    }

    private void sendCompletedEmailIfRequired(User user, NotificationType notificationType, Transaction transaction) {
        if (notificationType == NotificationType.WITHDRAWAL_APPROVED) {
            emailNotificationService.sendWithdrawalStatusEmail(user, transaction, "Yeu cau rut tien da duoc phe duyet");
            return;
        }
        if (isLargeTransaction(transaction)) {
            emailNotificationService.sendTransactionEmail(user, transaction);
        }
    }

    private boolean isLargeTransaction(Transaction transaction) {
        return transaction != null
                && transaction.getAmount() != null
                && transaction.getAmount().compareTo(largeTransactionEmailThreshold) >= 0;
    }

    private boolean isWithdrawal(Transaction transaction) {
        return transaction != null && transaction.getTransactionType() == TransactionType.WITHDRAW;
    }

    private record BalanceUpdatedPayload(UUID walletId, BigDecimal balance) {
    }

    private record NotificationRealtimePayload(UUID publicId, String type, String title,
            String message, Map<String, Object> metadata, boolean read) {

        private static NotificationRealtimePayload from(Notification notification) {
            return new NotificationRealtimePayload(
                    notification.getPublicId(),
                    notification.getType().name(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getMetadata(),
                    notification.isRead());
        }
    }
}