package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.WalletResponse;
import com.digitalwallet.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wallet REST controller.
 */
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable("id") UUID id) {
        WalletResponse wallet = new WalletResponse(id, "VND", "ACTIVE", BigDecimal.ZERO);
        return ResponseEntity.ok(ApiResponse.success(
                UUID.randomUUID().toString(),
                Instant.now(),
                wallet));
    }
}
