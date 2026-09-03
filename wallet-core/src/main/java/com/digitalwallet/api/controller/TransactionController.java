package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.TransactionHistoryFilter;
import com.digitalwallet.api.dto.response.PageResponse;
import com.digitalwallet.api.dto.response.TransactionHistoryItemResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping({"/api/transactions", "/api/v1/transactions"})
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
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
}