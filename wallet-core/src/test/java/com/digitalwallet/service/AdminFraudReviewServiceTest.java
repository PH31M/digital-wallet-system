package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.FraudReviewResponse;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.FraudDecision;
import com.digitalwallet.domain.enums.FraudReviewAction;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.event.TransactionFailedEvent;
import com.digitalwallet.domain.repository.FraudAssessmentRepository;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFraudReviewServiceTest {

    @Mock
    private FraudAssessmentRepository fraudAssessmentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void review_approvedExecutesPendingTransactionAndRecordsAdminDecision() {
        AdminFraudReviewService service = service();
        User admin = user(UserRole.ADMIN);
        Transaction transaction = pendingTransaction();
        FraudAssessment assessment = pendingAssessment(transaction);
        RequestMetadata metadata = metadata();

        stubPendingAssessment(assessment, transaction);
        when(fraudAssessmentRepository.save(assessment)).thenReturn(assessment);

        FraudReviewResponse response = service.review(admin, assessment.getId(), FraudReviewAction.APPROVED,
                "Verified with customer", metadata);

        assertThat(response.reviewStatus()).isEqualTo(FraudReviewStatus.CLEARED.name());
        assertThat(assessment.getReviewAction()).isEqualTo(FraudReviewAction.APPROVED);
        assertThat(assessment.getReviewedBy()).isSameAs(admin);
        verify(walletService).approvePendingReview(transaction, metadata);
        verify(auditService).log(admin, AuditActorType.ADMIN, AuditAction.FRAUD_REVIEW_APPROVED,
                "FRAUD_ASSESSMENT", assessment.getId(), metadata.ipAddress(), metadata.userAgent(), metadata.requestId());
        verify(eventPublisher, never()).publishEvent(any(TransactionFailedEvent.class));
    }

    @Test
    void review_rejectedFailsTransactionAndNotifiesInitiator() {
        AdminFraudReviewService service = service();
        User admin = user(UserRole.ADMIN);
        Transaction transaction = pendingTransaction();
        FraudAssessment assessment = pendingAssessment(transaction);
        RequestMetadata metadata = metadata();

        stubPendingAssessment(assessment, transaction);
        when(fraudAssessmentRepository.save(assessment)).thenReturn(assessment);

        FraudReviewResponse response = service.review(admin, assessment.getId(), FraudReviewAction.REJECTED,
                "High-risk activity confirmed", metadata);

        assertThat(response.reviewStatus()).isEqualTo(FraudReviewStatus.REVIEWED.name());
        assertThat(transaction.getStatus().name()).isEqualTo("FAILED");
        verify(transactionRepository).save(transaction);
        verify(eventPublisher).publishEvent(any(TransactionFailedEvent.class));
        verify(auditService).log(admin, AuditActorType.ADMIN, AuditAction.FRAUD_REVIEW_REJECTED,
                "FRAUD_ASSESSMENT", assessment.getId(), metadata.ipAddress(), metadata.userAgent(), metadata.requestId());
        verify(walletService, never()).approvePendingReview(any(), any());
    }

    @Test
    void review_completedAssessmentRejectsDuplicateDecision() {
        AdminFraudReviewService service = service();
        FraudAssessment assessment = pendingAssessment(pendingTransaction());
        assessment.setReviewStatus(FraudReviewStatus.CLEARED);
        when(fraudAssessmentRepository.findByIdForUpdate(assessment.getId())).thenReturn(Optional.of(assessment));

        assertThatThrownBy(() -> service.review(user(UserRole.ADMIN), assessment.getId(), FraudReviewAction.APPROVED,
                null, metadata()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FRAUD_REVIEW_NOT_PENDING);

        verify(transactionRepository, never()).findByIdForUpdate(any());
        verify(walletService, never()).approvePendingReview(any(), any());
    }

    private void stubPendingAssessment(FraudAssessment assessment, Transaction transaction) {
        when(fraudAssessmentRepository.findByIdForUpdate(assessment.getId())).thenReturn(Optional.of(assessment));
        when(transactionRepository.findByIdForUpdate(transaction.getId())).thenReturn(Optional.of(transaction));
    }

    private AdminFraudReviewService service() {
        return new AdminFraudReviewService(fraudAssessmentRepository, transactionRepository, walletService,
                auditService, eventPublisher);
    }

    private FraudAssessment pendingAssessment(Transaction transaction) {
        FraudAssessment assessment = new FraudAssessment();
        assessment.setId(UUID.randomUUID());
        assessment.setTransaction(transaction);
        assessment.setDecision(FraudDecision.CHALLENGE);
        assessment.setRiskScore(new BigDecimal("70.00"));
        assessment.setReviewStatus(FraudReviewStatus.PENDING_REVIEW);
        return assessment;
    }

    private Transaction pendingTransaction() {
        User user = user(UserRole.USER);
        Wallet sender = new Wallet();
        sender.setId(UUID.randomUUID());
        sender.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setReferenceNumber("TX-REVIEW-1");
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setSenderWallet(sender);
        transaction.awaitReview();
        return transaction;
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPublicId(UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setFullName("Test User");
        user.setRole(role);
        return user;
    }

    private RequestMetadata metadata() {
        return new RequestMetadata(UUID.randomUUID(), "127.0.0.1", "JUnit");
    }
}