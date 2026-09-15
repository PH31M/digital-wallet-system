package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Trạng thái hiện tại của 1 yêu cầu nạp tiền MoMo - dùng cho GET /momo/return, CHỈ ĐỌC, không
 * bao giờ tự finalize giao dịch (chỉ IPN mới được phép, xem MomoPaymentService).
 */
public record MomoPaymentStatusResponse(
        @JsonProperty("momo_order_id") String momoOrderId,
        String status,
        @JsonProperty("transaction_id") UUID transactionId,
        @JsonProperty("transaction_status") String transactionStatus) {
}
