package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.FraudReviewResponse;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.FraudReviewAction;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.domain.event.TransactionFailedEvent;
import com.digitalwallet.domain.repository.FraudAssessmentRepository;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminFraudReviewService {

    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminFraudReviewService(FraudAssessmentRepository fraudAssessmentRepository,
            TransactionRepository transactionRepository, WalletService walletService,
            AuditService auditService, ApplicationEventPublisher eventPublisher) {
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<FraudReviewResponse> list(FraudReviewStatus reviewStatus, Pageable pageable) {
        return fraudAssessmentRepository.findByReviewStatus(reviewStatus, pageable).map(FraudReviewResponse::from);
    }

    @Transactional
    public FraudReviewResponse review(User admin, UUID assessmentId, FraudReviewAction action,
            String note, RequestMetadata metadata) {
        FraudAssessment assessment = fraudAssessmentRepository.findByIdForUpdate(assessmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (assessment.getReviewStatus() != FraudReviewStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.FRAUD_REVIEW_NOT_PENDING);
        }

        Transaction transaction = transactionRepository.findByIdForUpdate(assessment.getTransaction().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!transaction.isPendingReview()) {
            throw new BusinessException(ErrorCode.FRAUD_REVIEW_NOT_PENDING);
        }

        String normalizedNote = normalizeNote(note);
        if (action == FraudReviewAction.REJECTED && normalizedNote == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Review note is required when rejecting");
        }

        if (action == FraudReviewAction.APPROVED) {
            walletService.approvePendingReview(transaction, metadata);
        } else {
            transaction.fail();
            transactionRepository.save(transaction);
            eventPublisher.publishEvent(TransactionFailedEvent.of(
                    initiatorFor(transaction), transaction, "Transaction rejected after fraud review"));
        }

        assessment.review(admin, action, normalizedNote);
        FraudAssessment reviewed = fraudAssessmentRepository.save(assessment);
        auditService.log(admin, AuditActorType.ADMIN,
                action == FraudReviewAction.APPROVED
                        ? AuditAction.FRAUD_REVIEW_APPROVED
                        : AuditAction.FRAUD_REVIEW_REJECTED,
                "FRAUD_ASSESSMENT", reviewed.getId(), metadata.ipAddress(), metadata.userAgent(), metadata.requestId());
        return FraudReviewResponse.from(reviewed);
    }

    private User initiatorFor(Transaction transaction) {
        var wallet = transaction.getTransactionType() == TransactionType.DEPOSIT
                ? transaction.getReceiverWallet()
                : transaction.getSenderWallet();
        if (wallet == null || wallet.getUser() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return wallet.getUser();
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}