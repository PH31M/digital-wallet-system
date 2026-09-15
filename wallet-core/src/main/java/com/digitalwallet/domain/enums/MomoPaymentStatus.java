package com.digitalwallet.domain.enums;

/**
 * Lifecycle của một yêu cầu nạp tiền qua MoMo — tách biệt với {@link TransactionStatus} của
 * transaction ví tương ứng (xem MomoPaymentRequest): PENDING = đã tạo payUrl, chờ IPN;
 * CONFIRMED = IPN xác nhận resultCode=0, đã ghi ledger; FAILED = MoMo tạo request lỗi, hoặc
 * IPN báo resultCode != 0.
 */
public enum MomoPaymentStatus {
    PENDING,
    CONFIRMED,
    FAILED
}
