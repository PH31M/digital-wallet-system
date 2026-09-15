package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** Kết quả khởi tạo nạp tiền qua MoMo - client mở payUrl để người dùng thanh toán trên MoMo. */
public record MomoDepositInitiateResponse(
        @JsonProperty("pay_url") String payUrl,
        @JsonProperty("transaction_id") UUID transactionId,
        @JsonProperty("momo_order_id") String momoOrderId) {
}
