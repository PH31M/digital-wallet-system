package com.digitalwallet.domain.entity;

import com.digitalwallet.domain.enums.MomoPaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Một lần gọi MoMo AIO create-payment API để nạp tiền vào ví. Tách bảng riêng thay vì thêm cột
 * vào {@code transactions} (đã khoá kiến trúc, dùng chung cho mọi loại giao dịch) — xem
 * docs/dws-momo-integration-brief (task brief 15/09/2026).
 */
@Entity
@Table(name = "momo_payment_requests")
public class MomoPaymentRequest extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "momo_order_id", nullable = false, unique = true)
    private String momoOrderId;

    @Column(name = "momo_request_id", nullable = false)
    private String momoRequestId;

    @Column(name = "momo_trans_id")
    private String momoTransId;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomoPaymentStatus status = MomoPaymentStatus.PENDING;

    @Column(name = "raw_response")
    private String rawResponse;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getMomoOrderId() {
        return momoOrderId;
    }

    public void setMomoOrderId(String momoOrderId) {
        this.momoOrderId = momoOrderId;
    }

    public String getMomoRequestId() {
        return momoRequestId;
    }

    public void setMomoRequestId(String momoRequestId) {
        this.momoRequestId = momoRequestId;
    }

    public String getMomoTransId() {
        return momoTransId;
    }

    public void setMomoTransId(String momoTransId) {
        this.momoTransId = momoTransId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public MomoPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(MomoPaymentStatus status) {
        this.status = status;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public boolean isPending() {
        return status == MomoPaymentStatus.PENDING;
    }
}
