package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.TransferRequest;
import com.digitalwallet.api.dto.request.WalletAmountRequest;
import com.digitalwallet.api.dto.response.LedgerEntryResponse;
import com.digitalwallet.api.dto.response.TransactionResponse;
import com.digitalwallet.api.dto.response.WalletResponse;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.entity.LedgerEntry;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.FraudDecision;
import com.digitalwallet.domain.enums.LedgerAccountType;
import com.digitalwallet.domain.enums.NotificationType;
import com.digitalwallet.domain.enums.TransactionStatus;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.domain.enums.WalletStatus;
import com.digitalwallet.domain.event.BalanceUpdatedEvent;
import com.digitalwallet.domain.event.FraudAlertEvent;
import com.digitalwallet.domain.event.TransactionCompletedEvent;
import com.digitalwallet.domain.event.TransactionFailedEvent;
import com.digitalwallet.domain.repository.LedgerEntryRepository;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.exception.InsufficientBalanceException;
import com.digitalwallet.exception.WalletFrozenException;
import com.digitalwallet.util.ReferenceNumberGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FraudService fraudService;
    private final TransactionLimitService transactionLimitService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository, FraudService fraudService,
            TransactionLimitService transactionLimitService, AuditService auditService,
            ApplicationEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fraudService = fraudService;
        this.transactionLimitService = transactionLimitService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID walletId, User currentUser) {
        Wallet wallet = findOwnedWallet(walletId, currentUser);
        return toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getCurrentUserWallet(User currentUser) {
        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return toWalletResponse(wallet);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public TransactionResponse deposit(User currentUser, WalletAmountRequest request, RequestMetadata metadata) {
        Optional<TransactionResponse> idempotentResponse = existingTransaction(request.getIdempotencyKey());
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }

        Wallet wallet = lockCurrentUserWallet(currentUser);
        ensureActive(wallet);

        Transaction transaction = newTransaction(TransactionType.DEPOSIT, request.getAmount(), request.getIdempotencyKey());
        transaction.setReceiverWallet(wallet);
        assertWithinLimitOrFail(transaction, currentUser, metadata, wallet);

        transaction.setStatus(TransactionStatus.PROCESSING);
        Transaction savedTransaction = transactionRepository.save(transaction);
        if (holdForReviewIfNecessary(savedTransaction, currentUser, metadata)) {
            return TransactionResponse.from(savedTransaction);
        }

        wallet.credit(request.getAmount());
        walletRepository.save(wallet);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(savedTransaction, null, LedgerAccountType.CASH_ACCOUNT, request.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(savedTransaction, wallet, LedgerAccountType.USER_WALLET, BigDecimal.ZERO, request.getAmount())));

        savedTransaction.complete();
        Transaction completed = transactionRepository.save(savedTransaction);
        audit(completed, currentUser, AuditAction.DEPOSIT_COMPLETED, metadata);
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(wallet));
        publishTransactionCompleted(completed,
                TransactionCompletedEvent.Recipient.of(currentUser, NotificationType.TRANSACTION_RECEIVED));
        return TransactionResponse.from(completed);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public TransactionResponse withdraw(User currentUser, WalletAmountRequest request, RequestMetadata metadata) {
        Optional<TransactionResponse> idempotentResponse = existingTransaction(request.getIdempotencyKey());
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }

        Wallet wallet = lockCurrentUserWallet(currentUser);
        ensureActive(wallet);
        ensureSufficientBalance(wallet, request.getAmount());

        Transaction transaction = newTransaction(TransactionType.WITHDRAW, request.getAmount(), request.getIdempotencyKey());
        transaction.setSenderWallet(wallet);
        assertWithinLimitOrFail(transaction, currentUser, metadata, wallet);

        transaction.setStatus(TransactionStatus.PROCESSING);
        Transaction savedTransaction = transactionRepository.save(transaction);
        if (holdForReviewIfNecessary(savedTransaction, currentUser, metadata)) {
            return TransactionResponse.from(savedTransaction);
        }

        wallet.debit(request.getAmount());
        walletRepository.save(wallet);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(savedTransaction, wallet, LedgerAccountType.USER_WALLET, request.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(savedTransaction, null, LedgerAccountType.CASH_ACCOUNT, BigDecimal.ZERO, request.getAmount())));

        savedTransaction.complete();
        Transaction completed = transactionRepository.save(savedTransaction);
        audit(completed, currentUser, AuditAction.WITHDRAW_COMPLETED, metadata);
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(wallet));
        publishTransactionCompleted(completed,
                TransactionCompletedEvent.Recipient.of(currentUser, NotificationType.WITHDRAWAL_APPROVED));
        return TransactionResponse.from(completed);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public TransactionResponse transfer(User currentUser, TransferRequest request, RequestMetadata metadata) {
        Optional<TransactionResponse> idempotentResponse = existingTransaction(request.getIdempotencyKey());
        if (idempotentResponse.isPresent()) {
            return idempotentResponse.get();
        }

        Wallet senderWallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (senderWallet.getId().equals(request.getReceiverWalletId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Receiver wallet must be different from sender wallet");
        }

        List<UUID> walletIds = List.of(senderWallet.getId(), request.getReceiverWalletId()).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        Wallet firstLocked = lockWallet(walletIds.get(0));
        Wallet secondLocked = lockWallet(walletIds.get(1));

        Wallet lockedSender = firstLocked.getId().equals(senderWallet.getId()) ? firstLocked : secondLocked;
        Wallet lockedReceiver = firstLocked.getId().equals(request.getReceiverWalletId()) ? firstLocked : secondLocked;

        ensureOwned(lockedSender, currentUser);
        ensureActive(lockedSender);
        ensureActive(lockedReceiver);
        ensureSufficientBalance(lockedSender, request.getAmount());

        Transaction transaction = newTransaction(TransactionType.TRANSFER, request.getAmount(), request.getIdempotencyKey());
        transaction.setSenderWallet(lockedSender);
        transaction.setReceiverWallet(lockedReceiver);
        assertWithinLimitOrFail(transaction, currentUser, metadata, lockedSender, lockedReceiver);

        transaction.setStatus(TransactionStatus.PROCESSING);
        Transaction savedTransaction = transactionRepository.save(transaction);
        if (holdForReviewIfNecessary(savedTransaction, currentUser, metadata)) {
            return TransactionResponse.from(savedTransaction);
        }

        lockedSender.debit(request.getAmount());
        lockedReceiver.credit(request.getAmount());
        walletRepository.saveAll(List.of(lockedSender, lockedReceiver));
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(savedTransaction, lockedSender, LedgerAccountType.USER_WALLET,
                        request.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(savedTransaction, lockedReceiver, LedgerAccountType.USER_WALLET,
                        BigDecimal.ZERO, request.getAmount())));

        savedTransaction.complete();
        Transaction completed = transactionRepository.save(savedTransaction);
        audit(completed, currentUser, AuditAction.TRANSFER_COMPLETED, metadata);
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(lockedSender));
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(lockedReceiver));
        publishTransactionCompleted(completed,
                TransactionCompletedEvent.Recipient.of(currentUser, NotificationType.TRANSACTION_SENT),
                TransactionCompletedEvent.Recipient.of(lockedReceiver.getUser(), NotificationType.TRANSACTION_RECEIVED));
        return TransactionResponse.from(completed);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactions(User currentUser) {
        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return transactionRepository.findBySenderWalletIdOrReceiverWalletId(wallet.getId(), wallet.getId()).stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedgerEntries(UUID transactionId, User currentUser) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!isParticipant(transaction, currentUser)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return ledgerEntryRepository.findByTransactionId(transactionId).stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionResponse approvePendingReview(Transaction transaction, RequestMetadata metadata) {
        if (!transaction.isPendingReview()) {
            throw new BusinessException(ErrorCode.FRAUD_REVIEW_NOT_PENDING);
        }

        User initiator = initiatorFor(transaction);
        transaction.setStatus(TransactionStatus.PROCESSING);
        List<TransactionCompletedEvent.Recipient> recipients = executePendingTransaction(transaction, initiator);

        transaction.complete();
        Transaction completed = transactionRepository.save(transaction);
        audit(completed, initiator, completionAction(completed), metadata);
        publishTransactionCompleted(completed, recipients.toArray(TransactionCompletedEvent.Recipient[]::new));
        return TransactionResponse.from(completed);
    }

    private List<TransactionCompletedEvent.Recipient> executePendingTransaction(Transaction transaction, User initiator) {
        return switch (transaction.getTransactionType()) {
            case DEPOSIT -> completePendingDeposit(transaction, initiator);
            case WITHDRAW -> completePendingWithdrawal(transaction, initiator);
            case TRANSFER -> completePendingTransfer(transaction, initiator);
            default -> throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Unsupported transaction type for fraud review");
        };
    }

    private List<TransactionCompletedEvent.Recipient> completePendingDeposit(Transaction transaction, User initiator) {
        Wallet wallet = lockWallet(transaction.getReceiverWallet().getId());
        ensureActive(wallet);
        transactionLimitService.assertWithinDailyLimit(wallet, transaction.getAmount());
        wallet.credit(transaction.getAmount());
        walletRepository.save(wallet);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(transaction, null, LedgerAccountType.CASH_ACCOUNT, transaction.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(transaction, wallet, LedgerAccountType.USER_WALLET, BigDecimal.ZERO, transaction.getAmount())));
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(wallet));
        return List.of(TransactionCompletedEvent.Recipient.of(initiator, NotificationType.TRANSACTION_RECEIVED));
    }

    private List<TransactionCompletedEvent.Recipient> completePendingWithdrawal(Transaction transaction, User initiator) {
        Wallet wallet = lockWallet(transaction.getSenderWallet().getId());
        ensureActive(wallet);
        ensureSufficientBalance(wallet, transaction.getAmount());
        transactionLimitService.assertWithinDailyLimit(wallet, transaction.getAmount());
        wallet.debit(transaction.getAmount());
        walletRepository.save(wallet);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(transaction, wallet, LedgerAccountType.USER_WALLET, transaction.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(transaction, null, LedgerAccountType.CASH_ACCOUNT, BigDecimal.ZERO, transaction.getAmount())));
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(wallet));
        return List.of(TransactionCompletedEvent.Recipient.of(initiator, NotificationType.WITHDRAWAL_APPROVED));
    }

    private List<TransactionCompletedEvent.Recipient> completePendingTransfer(Transaction transaction, User initiator) {
        List<UUID> walletIds = List.of(transaction.getSenderWallet().getId(), transaction.getReceiverWallet().getId()).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        Wallet firstLocked = lockWallet(walletIds.get(0));
        Wallet secondLocked = lockWallet(walletIds.get(1));
        Wallet sender = firstLocked.getId().equals(transaction.getSenderWallet().getId()) ? firstLocked : secondLocked;
        Wallet receiver = firstLocked.getId().equals(transaction.getReceiverWallet().getId()) ? firstLocked : secondLocked;

        ensureActive(sender);
        ensureActive(receiver);
        ensureSufficientBalance(sender, transaction.getAmount());
        transactionLimitService.assertWithinDailyLimit(sender, transaction.getAmount());
        transactionLimitService.assertWithinDailyLimit(receiver, transaction.getAmount());
        sender.debit(transaction.getAmount());
        receiver.credit(transaction.getAmount());
        walletRepository.saveAll(List.of(sender, receiver));
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(transaction, sender, LedgerAccountType.USER_WALLET, transaction.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(transaction, receiver, LedgerAccountType.USER_WALLET, BigDecimal.ZERO, transaction.getAmount())));
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(sender));
        eventPublisher.publishEvent(BalanceUpdatedEvent.of(receiver));
        return List.of(
                TransactionCompletedEvent.Recipient.of(initiator, NotificationType.TRANSACTION_SENT),
                TransactionCompletedEvent.Recipient.of(receiver.getUser(), NotificationType.TRANSACTION_RECEIVED));
    }

    private User initiatorFor(Transaction transaction) {
        Wallet wallet = transaction.getTransactionType() == TransactionType.DEPOSIT
                ? transaction.getReceiverWallet()
                : transaction.getSenderWallet();
        if (wallet == null || wallet.getUser() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return wallet.getUser();
    }

    private AuditAction completionAction(Transaction transaction) {
        return switch (transaction.getTransactionType()) {
            case DEPOSIT -> AuditAction.DEPOSIT_COMPLETED;
            case WITHDRAW -> AuditAction.WITHDRAW_COMPLETED;
            case TRANSFER -> AuditAction.TRANSFER_COMPLETED;
            default -> AuditAction.TRANSACTION_FAILED;
        };
    }

    private Transaction newTransaction(TransactionType type, BigDecimal amount, String idempotencyKey) {
        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(ReferenceNumberGenerator.generateReference());
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setIdempotencyKey(normalizeIdempotencyKey(idempotencyKey));
        return transaction;
    }

    private void assertWithinLimitOrFail(Transaction transaction, User currentUser,
            RequestMetadata metadata, Wallet... affectedWallets) {
        try {
            for (Wallet wallet : affectedWallets) {
                transactionLimitService.assertWithinDailyLimit(wallet, transaction.getAmount());
            }
        } catch (BusinessException ex) {
            transaction.fail();
            Transaction failed = transactionRepository.save(transaction);
            audit(failed, currentUser, AuditAction.TRANSACTION_FAILED, metadata);
            eventPublisher.publishEvent(TransactionFailedEvent.of(currentUser, failed, ex.getMessage()));
            throw ex;
        }
    }

    private boolean holdForReviewIfNecessary(Transaction transaction, User currentUser, RequestMetadata metadata) {
        FraudDecision decision = assessFraud(transaction);
        if (decision == FraudDecision.CHALLENGE) {
            transaction.awaitReview();
            Transaction held = transactionRepository.save(transaction);
            audit(held, currentUser, AuditAction.FRAUD_CHALLENGE, metadata);
            eventPublisher.publishEvent(FraudAlertEvent.of(currentUser, held));
            return true;
        }

        if (decision == FraudDecision.BLOCK) {
            transaction.fail();
            Transaction failed = transactionRepository.save(transaction);
            audit(failed, currentUser, AuditAction.FRAUD_BLOCKED, metadata);
            eventPublisher.publishEvent(FraudAlertEvent.of(currentUser, failed));
            eventPublisher.publishEvent(TransactionFailedEvent.of(
                    currentUser, failed, ErrorCode.FRAUD_BLOCKED.getDefaultMessage()));
            throw new BusinessException(ErrorCode.FRAUD_BLOCKED);
        }

        return false;
    }

    private FraudDecision assessFraud(Transaction transaction) {
        FraudAssessment assessment = fraudService.assess(transaction);
        if (assessment != null && assessment.getDecision() != null) {
            return assessment.getDecision();
        }
        return transaction.getFraudDecision() == null ? FraudDecision.ALLOW : transaction.getFraudDecision();
    }

    private void audit(Transaction transaction, User currentUser, AuditAction action, RequestMetadata metadata) {
        auditService.log(currentUser, AuditActorType.USER, action,
                "TRANSACTION", transaction.getId(), metadata.ipAddress(), metadata.userAgent(), metadata.requestId());
    }

    private void publishTransactionCompleted(Transaction transaction, TransactionCompletedEvent.Recipient... recipients) {
        eventPublisher.publishEvent(new TransactionCompletedEvent(transaction, List.of(recipients)));
    }

    private Optional<TransactionResponse> existingTransaction(String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey == null) {
            return Optional.empty();
        }
        return transactionRepository.findByIdempotencyKey(normalizedKey).map(TransactionResponse::from);
    }

    private Wallet lockCurrentUserWallet(User currentUser) {
        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return lockWallet(wallet.getId());
    }

    private Wallet lockWallet(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Wallet findOwnedWallet(UUID walletId, User currentUser) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ensureOwned(wallet, currentUser);
        return wallet;
    }

    private void ensureOwned(Wallet wallet, User currentUser) {
        if (!Objects.equals(wallet.getUser().getId(), currentUser.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void ensureActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Wallet is frozen");
        }
    }

    private void ensureSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }

    private boolean isParticipant(Transaction transaction, User currentUser) {
        return ownsWallet(transaction.getSenderWallet(), currentUser) || ownsWallet(transaction.getReceiverWallet(), currentUser);
    }

    private boolean ownsWallet(Wallet wallet, User currentUser) {
        return wallet != null && Objects.equals(wallet.getUser().getId(), currentUser.getId());
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getCurrency(), wallet.getStatus().name(), wallet.getBalance());
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }
}