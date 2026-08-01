package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.TransferRequest;
import com.digitalwallet.api.dto.request.WalletAmountRequest;
import com.digitalwallet.api.dto.response.TransactionResponse;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.entity.LedgerEntry;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.FraudDecision;
import com.digitalwallet.domain.enums.NotificationType;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.enums.WalletStatus;
import com.digitalwallet.domain.repository.LedgerEntryRepository;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.DailyLimitExceededException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private FraudService fraudService;

    @Mock
    private TransactionLimitService transactionLimitService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Test
    void deposit_creditsWalletWritesBalancedLedgerAndEmitsControls() {
        WalletService walletService = walletService();
        RequestMetadata metadata = metadata();
        User user = user();
        Wallet wallet = wallet(user, new BigDecimal("100.00"));
        WalletAmountRequest request = new WalletAmountRequest(new BigDecimal("25.00"), "deposit-1");

        when(transactionRepository.findByIdempotencyKey("deposit-1")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(user.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = walletService.deposit(user, request, metadata);

        assertThat(wallet.getBalance()).isEqualByComparingTo("125.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT.name());
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertBalancedLedger("25.00");
        verify(transactionLimitService).assertWithinDailyLimit(wallet, request.getAmount());
        verify(fraudService).assess(any(Transaction.class));
        verify(auditService).log(eq(user), eq(AuditActorType.USER), eq(AuditAction.DEPOSIT_COMPLETED),
                eq("TRANSACTION"), any(), eq("127.0.0.1"), eq("JUnit"), eq(metadata.requestId()));
        verify(notificationService).transactionCompleted(eq(user), eq(NotificationType.DEPOSIT_SUCCESS), any(Transaction.class));
    }

    @Test
    void withdraw_insufficientBalance_doesNotCreateTransaction() {
        WalletService walletService = walletService();
        User user = user();
        Wallet wallet = wallet(user, new BigDecimal("10.00"));
        WalletAmountRequest request = new WalletAmountRequest(new BigDecimal("25.00"), "withdraw-1");

        when(transactionRepository.findByIdempotencyKey("withdraw-1")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(user.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.withdraw(user, request, metadata()))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(ledgerEntryRepository, never()).saveAll(any());
        verify(fraudService, never()).assess(any(Transaction.class));
    }

    @Test
    void withdraw_dailyLimitExceeded_recordsFailedTransactionAndDoesNotDebit() {
        WalletService walletService = walletService();
        RequestMetadata metadata = metadata();
        User user = user();
        Wallet wallet = wallet(user, new BigDecimal("100.00"));
        WalletAmountRequest request = new WalletAmountRequest(new BigDecimal("25.00"), "withdraw-limit");

        when(transactionRepository.findByIdempotencyKey("withdraw-limit")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(user.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.findByIdForUpdate(wallet.getId())).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DailyLimitExceededException("Daily transaction limit exceeded"))
                .when(transactionLimitService).assertWithinDailyLimit(wallet, request.getAmount());

        assertThatThrownBy(() -> walletService.withdraw(user, request, metadata))
                .isInstanceOf(DailyLimitExceededException.class);

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository).save(any(Transaction.class));
        verify(auditService).log(eq(user), eq(AuditActorType.USER), eq(AuditAction.TRANSACTION_FAILED),
                eq("TRANSACTION"), any(), eq("127.0.0.1"), eq("JUnit"), eq(metadata.requestId()));
        verify(notificationService).transactionFailed(eq(user), any(Transaction.class), eq("Daily transaction limit exceeded"));
        verify(ledgerEntryRepository, never()).saveAll(any());
        verify(fraudService, never()).assess(any(Transaction.class));
    }

    @Test
    void transfer_movesBalanceWritesLedgerAuditAndNotifiesBothParties() {
        WalletService walletService = walletService();
        RequestMetadata metadata = metadata();
        User sender = user();
        User receiver = user();
        Wallet senderWallet = wallet(sender, new BigDecimal("100.00"));
        Wallet receiverWallet = wallet(receiver, new BigDecimal("5.00"));
        TransferRequest request = new TransferRequest(receiverWallet.getId(), new BigDecimal("30.00"), "transfer-1");

        when(transactionRepository.findByIdempotencyKey("transfer-1")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(sender.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(senderWallet.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWallet.getId())).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = walletService.transfer(sender, request, metadata);

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("35.00");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.TRANSFER.name());
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertBalancedLedger("30.00");
        verify(transactionLimitService).assertWithinDailyLimit(senderWallet, request.getAmount());
        verify(transactionLimitService).assertWithinDailyLimit(receiverWallet, request.getAmount());
        verify(auditService).log(eq(sender), eq(AuditActorType.USER), eq(AuditAction.TRANSFER_COMPLETED),
                eq("TRANSACTION"), any(), eq("127.0.0.1"), eq("JUnit"), eq(metadata.requestId()));
        verify(notificationService).transactionCompleted(eq(sender), eq(NotificationType.TRANSFER_SENT), any(Transaction.class));
        verify(notificationService).transactionCompleted(eq(receiver), eq(NotificationType.TRANSFER_RECEIVED), any(Transaction.class));
    }

    @Test
    void transfer_fraudBlock_recordsFailedTransactionAndDoesNotMoveFunds() {
        WalletService walletService = walletService();
        RequestMetadata metadata = metadata();
        User sender = user();
        User receiver = user();
        Wallet senderWallet = wallet(sender, new BigDecimal("100.00"));
        Wallet receiverWallet = wallet(receiver, new BigDecimal("5.00"));
        TransferRequest request = new TransferRequest(receiverWallet.getId(), new BigDecimal("30.00"), "fraud-block");

        when(transactionRepository.findByIdempotencyKey("fraud-block")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(sender.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(senderWallet.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWallet.getId())).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudService.assess(any(Transaction.class))).thenReturn(fraudAssessment(FraudDecision.BLOCK));

        assertThatThrownBy(() -> walletService.transfer(sender, request, metadata))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FRAUD_BLOCKED);

        assertThat(senderWallet.getBalance()).isEqualByComparingTo("100.00");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("5.00");
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(auditService).log(eq(sender), eq(AuditActorType.USER), eq(AuditAction.FRAUD_BLOCKED),
                eq("TRANSACTION"), any(), eq("127.0.0.1"), eq("JUnit"), eq(metadata.requestId()));
        verify(notificationService).fraudAlert(eq(sender), any(Transaction.class));
        verify(notificationService).transactionFailed(eq(sender), any(Transaction.class), eq(ErrorCode.FRAUD_BLOCKED.getDefaultMessage()));
        verify(ledgerEntryRepository, never()).saveAll(any());
    }

    @Test
    void transfer_fraudChallengeAllowsTransferButAddsFraudAlert() {
        WalletService walletService = walletService();
        RequestMetadata metadata = metadata();
        User sender = user();
        User receiver = user();
        Wallet senderWallet = wallet(sender, new BigDecimal("100.00"));
        Wallet receiverWallet = wallet(receiver, new BigDecimal("5.00"));
        TransferRequest request = new TransferRequest(receiverWallet.getId(), new BigDecimal("30.00"), "fraud-challenge");

        when(transactionRepository.findByIdempotencyKey("fraud-challenge")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(sender.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(senderWallet.getId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(receiverWallet.getId())).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudService.assess(any(Transaction.class))).thenReturn(fraudAssessment(FraudDecision.CHALLENGE));

        TransactionResponse response = walletService.transfer(sender, request, metadata);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(senderWallet.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("35.00");
        verify(auditService).log(eq(sender), eq(AuditActorType.USER), eq(AuditAction.FRAUD_CHALLENGE),
                eq("TRANSACTION"), any(), eq("127.0.0.1"), eq("JUnit"), eq(metadata.requestId()));
        verify(notificationService).fraudAlert(eq(sender), any(Transaction.class));
        verify(notificationService).transactionCompleted(eq(sender), eq(NotificationType.TRANSFER_SENT), any(Transaction.class));
        verify(notificationService).transactionCompleted(eq(receiver), eq(NotificationType.TRANSFER_RECEIVED), any(Transaction.class));
    }

    @Test
    void transfer_toSameWalletIsRejected() {
        WalletService walletService = walletService();
        User user = user();
        Wallet wallet = wallet(user, new BigDecimal("100.00"));
        TransferRequest request = new TransferRequest(wallet.getId(), new BigDecimal("10.00"), "same-wallet");

        when(transactionRepository.findByIdempotencyKey("same-wallet")).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(user.getId())).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.transfer(user, request, metadata()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    private WalletService walletService() {
        return new WalletService(walletRepository, transactionRepository, ledgerEntryRepository,
                fraudService, transactionLimitService, auditService, notificationService);
    }

    private RequestMetadata metadata() {
        return new RequestMetadata(UUID.randomUUID(), "127.0.0.1", "JUnit");
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setFullName("Nguyen Van A");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        return user;
    }

    private Wallet wallet(User user, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setCurrency("VND");
        wallet.setBalance(balance);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private FraudAssessment fraudAssessment(FraudDecision decision) {
        FraudAssessment assessment = new FraudAssessment();
        assessment.setDecision(decision);
        return assessment;
    }

    private void assertBalancedLedger(String amount) {
        ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(ledgerEntryRepository).saveAll(captor.capture());

        List<LedgerEntry> entries = captor.getValue();
        BigDecimal debitTotal = entries.stream()
                .map(LedgerEntry::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = entries.stream()
                .map(LedgerEntry::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(entries).hasSize(2);
        assertThat(debitTotal).isEqualByComparingTo(amount);
        assertThat(creditTotal).isEqualByComparingTo(amount);
    }
}