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
import com.digitalwallet.domain.repository.LedgerEntryRepository;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.exception.InsufficientBalanceException;
import com.digitalwallet.exception.WalletFrozenException;
import com.digitalwallet.util.ReferenceNumberGenerator;
import org.springframework.stereotype.Service;
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
    private final NotificationService notificationService;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository, FraudService fraudService,
            TransactionLimitService transactionLimitService, AuditService auditService,
            NotificationService notificationService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fraudService = fraudService;
        this.transactionLimitService = transactionLimitService;
        this.auditService = auditService;
        this.notificationService = notificationService;
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
        enforceFraudDecision(savedTransaction, currentUser, metadata);

        wallet.credit(request.getAmount());
        walletRepository.save(wallet);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(savedTransaction, null, LedgerAccountType.CASH_ACCOUNT, request.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(savedTransaction, wallet, LedgerAccountType.USER_WALLET, BigDecimal.ZERO, request.getAmount())));

        savedTransaction.complete();
        Transaction completed = transactionRepository.save(savedTransaction);
        audit(completed, currentUser, AuditAction.DEPOSIT_COMPLETED, metadata);
        notificationService.transactionCompleted(currentUser, NotificationType.DEPOSIT_SUCCESS, completed);
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
        enforceFraudDecision(savedTransaction, currentUser, metadata);

        wallet.debit(request.getAmount());
        walletRepository.save(wallet);
        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry(savedTransaction, wallet, LedgerAccountType.USER_WALLET, request.getAmount(), BigDecimal.ZERO),
                new LedgerEntry(savedTransaction, null, LedgerAccountType.CASH_ACCOUNT, BigDecimal.ZERO, request.getAmount())));

        savedTransaction.complete();
        Transaction completed = transactionRepository.save(savedTransaction);
        audit(completed, currentUser, AuditAction.WITHDRAW_COMPLETED, metadata);
        notificationService.transactionCompleted(currentUser, NotificationType.WITHDRAW_SUCCESS, completed);
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
        enforceFraudDecision(savedTransaction, currentUser, metadata);

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
        notificationService.transactionCompleted(currentUser, NotificationType.TRANSFER_SENT, completed);
        notificationService.transactionCompleted(lockedReceiver.getUser(), NotificationType.TRANSFER_RECEIVED, completed);
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
            notificationService.transactionFailed(currentUser, failed, ex.getMessage());
            throw ex;
        }
    }

    private void enforceFraudDecision(Transaction transaction, User currentUser, RequestMetadata metadata) {
        FraudDecision decision = assessFraud(transaction);
        if (decision == FraudDecision.CHALLENGE) {
            audit(transaction, currentUser, AuditAction.FRAUD_CHALLENGE, metadata);
            notificationService.fraudAlert(currentUser, transaction);
            return;
        }

        if (decision == FraudDecision.BLOCK) {
            transaction.fail();
            Transaction failed = transactionRepository.save(transaction);
            audit(failed, currentUser, AuditAction.FRAUD_BLOCKED, metadata);
            notificationService.fraudAlert(currentUser, failed);
            notificationService.transactionFailed(currentUser, failed, ErrorCode.FRAUD_BLOCKED.getDefaultMessage());
            throw new BusinessException(ErrorCode.FRAUD_BLOCKED);
        }
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