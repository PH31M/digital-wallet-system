package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.TransferRequest;
import com.digitalwallet.api.dto.request.WalletAmountRequest;
import com.digitalwallet.api.dto.response.LedgerEntryResponse;
import com.digitalwallet.api.dto.response.TransactionResponse;
import com.digitalwallet.api.dto.response.WalletResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getCurrentUserWallet(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        WalletResponse wallet = walletService.getCurrentUserWallet(currentUser);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), wallet));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @CurrentUser User currentUser,
            @PathVariable("id") UUID id,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        WalletResponse wallet = walletService.getWallet(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), wallet));
    }

    @PostMapping("/me/deposits")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @CurrentUser User currentUser,
            @Valid @RequestBody WalletAmountRequest request,
            HttpServletRequest httpRequest) {
        RequestMetadata metadata = RequestMetadata.from(httpRequest);
        TransactionResponse transaction = walletService.deposit(currentUser, request, metadata);
        return ResponseEntity.ok(ApiResponse.success(metadata.requestId().toString(), Instant.now(), transaction));
    }

    @PostMapping("/me/withdrawals")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @CurrentUser User currentUser,
            @Valid @RequestBody WalletAmountRequest request,
            HttpServletRequest httpRequest) {
        RequestMetadata metadata = RequestMetadata.from(httpRequest);
        TransactionResponse transaction = walletService.withdraw(currentUser, request, metadata);
        return ResponseEntity.ok(ApiResponse.success(metadata.requestId().toString(), Instant.now(), transaction));
    }

    @PostMapping("/me/transfers")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @CurrentUser User currentUser,
            @Valid @RequestBody TransferRequest request,
            HttpServletRequest httpRequest) {
        RequestMetadata metadata = RequestMetadata.from(httpRequest);
        TransactionResponse transaction = walletService.transfer(currentUser, request, metadata);
        return ResponseEntity.ok(ApiResponse.success(metadata.requestId().toString(), Instant.now(), transaction));
    }

    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> listTransactions(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        List<TransactionResponse> transactions = walletService.listTransactions(currentUser);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), transactions));
    }

    @GetMapping("/transactions/{transactionId}/ledger")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getLedgerEntries(
            @CurrentUser User currentUser,
            @PathVariable("transactionId") UUID transactionId,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        List<LedgerEntryResponse> entries = walletService.getLedgerEntries(transactionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), entries));
    }
}