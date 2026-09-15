package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Kết quả tra cứu ví người nhận theo email/SĐT (GET /wallets/lookup) — CHỈ trả field tối
 * thiểu để người dùng xác nhận đúng người trước khi chuyển tiền, không lộ email/SĐT/số dư
 * thật của người khác (giống cách app ngân hàng che tên khi tra cứu số tài khoản).
 */
public record WalletLookupResponse(
        @JsonProperty("wallet_id") UUID walletId,
        @JsonProperty("masked_name") String maskedName,
        String status) {
}
