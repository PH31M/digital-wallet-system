package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.ConfirmTransactionOtpRequest;
import com.digitalwallet.api.dto.request.TransactionHistoryFilter;
import com.digitalwallet.api.dto.response.PageResponse;
import com.digitalwallet.api.dto.response.TransactionHistoryItemResponse;
import com.digitalwallet.api.dto.response.TransactionResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.TransactionService;
import com.digitalwallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping({"/api/transactions", "/api/v1/transactions"})
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;

    public TransactionController(TransactionService transactionService, WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<TransactionHistoryItemResponse>>> getHistory(
            @CurrentUser User currentUser,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        TransactionHistoryFilter filter = new TransactionHistoryFilter(
                type, status, dateFrom, dateTo, minAmount, maxAmount);
        Page<TransactionHistoryItemResponse> history = transactionService.getHistory(
                currentUser.getId(), filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                RequestIds.get(request), Instant.now(), PageResponse.from(history)));
    }

    @PostMapping("/{transactionId}/confirm-otp")
    public ResponseEntity<ApiResponse<TransactionResponse>> confirmOtp(
            @CurrentUser User currentUser,
            @PathVariable UUID transactionId,
            @Valid @RequestBody ConfirmTransactionOtpRequest request,
            HttpServletRequest httpRequest) {
        RequestMetadata metadata = RequestMetadata.from(httpRequest);
        TransactionResponse response = walletService.confirmOtp(
                currentUser, transactionId, request.getOtp(), metadata);
        return ResponseEntity.ok(ApiResponse.success(
                RequestIds.get(httpRequest), Instant.now(), response));
    }
}