package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.TransactionHistoryFilter;
import com.digitalwallet.api.dto.response.TransactionHistoryItemResponse;
import com.digitalwallet.domain.entity.LedgerEntry;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.enums.LedgerAccountType;
import com.digitalwallet.domain.enums.TransactionStatus;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.enums.WalletStatus;
import com.digitalwallet.domain.repository.LedgerEntryRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void getHistory_resolvesOwnedWalletsAndMapsLedgerEntry() {
        User user = user("owner@example.com");
        Wallet wallet = wallet(user);
        Transaction transaction = transaction(TransactionType.DEPOSIT, TransactionStatus.COMPLETED);
        LedgerEntry entry = new LedgerEntry(transaction, wallet, LedgerAccountType.USER_WALLET,
                BigDecimal.ZERO, new BigDecimal("25.00"));
        entry.setId(UUID.randomUUID());
        entry.setCreatedAt(Instant.parse("2026-08-19T10:00:00Z"));

        when(walletRepository.findWalletIdsByUserId(user.getId())).thenReturn(List.of(wallet.getId()));
        when(ledgerEntryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<TransactionHistoryItemResponse> result = service().getHistory(
                user.getId(), new TransactionHistoryFilter("DEPOSIT", "COMPLETED", null, null, null, null),
                PageRequest.of(0, 20));

        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.walletId()).isEqualTo(wallet.getId());
            assertThat(item.transactionId()).isEqualTo(transaction.getId());
            assertThat(item.type()).isEqualTo("DEPOSIT");
            assertThat(item.direction()).isEqualTo("CREDIT");
            assertThat(item.amount()).isEqualByComparingTo("25.00");
            assertThat(item.balanceAfter()).isNull();
            assertThat(item.description()).isEqualTo(transaction.getReferenceNumber());
        });
        verify(walletRepository).findWalletIdsByUserId(user.getId());
    }

    @Test
    void getHistory_withoutOwnedWalletsReturnsEmptyAndDoesNotQueryLedger() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findWalletIdsByUserId(userId)).thenReturn(List.of());

        Page<TransactionHistoryItemResponse> result = service().getHistory(
                userId, new TransactionHistoryFilter(null, null, null, null, null, null), PageRequest.of(2, 20));

        assertThat(result).isEmpty();
        assertThat(result.getNumber()).isEqualTo(2);
        verify(ledgerEntryRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getHistory_clampsPageSizeAndFallsBackFromUnsafeSort() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findWalletIdsByUserId(userId)).thenReturn(List.of(walletId));
        when(ledgerEntryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service().getHistory(userId, new TransactionHistoryFilter(null, null, null, null, null, null),
                PageRequest.of(0, 500, Sort.by("internalSecretField")));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(ledgerEntryRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void getHistory_rejectsInvalidRangeAndUnknownEnumFilters() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service().getHistory(userId,
                new TransactionHistoryFilter("INVALID", null, null, null, null, null), PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> service().getHistory(userId,
                new TransactionHistoryFilter(null, null, Instant.parse("2026-08-20T00:00:00Z"),
                        Instant.parse("2026-08-19T00:00:00Z"), null, null), PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class);

        verify(walletRepository, never()).findWalletIdsByUserId(userId);
    }

    @Test
    void getHistory_alwaysUsesWalletOwnershipEvenWhenClientAddsUntrustedParameters() {
        UUID userId = UUID.randomUUID();
        UUID ownedWalletId = UUID.randomUUID();
        UUID foreignWalletId = UUID.randomUUID();
        when(walletRepository.findWalletIdsByUserId(userId)).thenReturn(List.of(ownedWalletId));
        when(ledgerEntryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service().getHistory(userId, new TransactionHistoryFilter(null, null, null, null, null, null),
                PageRequest.of(0, 20));

        ArgumentCaptor<Specification<LedgerEntry>> specification = ArgumentCaptor.forClass(Specification.class);
        verify(ledgerEntryRepository).findAll(specification.capture(), any(Pageable.class));
        assertThat(specification.getValue()).isNotNull();
        assertThat(foreignWalletId).isNotEqualTo(ownedWalletId);
    }

    private TransactionService service() {
        return new TransactionService(walletRepository, ledgerEntryRepository);
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test User");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        return user;
    }

    private Wallet wallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUser(user);
        wallet.setStatus(WalletStatus.ACTIVE);
        return wallet;
    }

    private Transaction transaction(TransactionType type, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setReferenceNumber("TX-001");
        transaction.setTransactionType(type);
        transaction.setStatus(status);
        transaction.setAmount(new BigDecimal("25.00"));
        transaction.setCreatedAt(Instant.parse("2026-08-19T10:00:00Z"));
        return transaction;
    }
}