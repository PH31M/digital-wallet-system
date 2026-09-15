package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.MomoDepositInitiateResponse;
import com.digitalwallet.api.dto.response.MomoPaymentStatusResponse;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.domain.entity.MomoPaymentRequest;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.MomoPaymentStatus;
import com.digitalwallet.domain.repository.MomoPaymentRequestRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrator cho luồng nạp tiền qua MoMo. Theo đúng task brief mục 3.2: CHỈ {@link #handleIpn}
 * (server-to-server) được phép finalize giao dịch - {@link #getStatus} (dùng cho GET
 * /momo/return, redirect trình duyệt) chỉ đọc, không bao giờ mutate state, vì return có thể bị
 * người dùng mở lại/replay dễ hơn IPN.
 */
@Service
public class MomoPaymentService {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentService.class);

    private final WalletService walletService;
    private final MomoClient momoClient;
    private final MomoSignatureService signatureService;
    private final MomoPaymentRequestRepository momoPaymentRequestRepository;

    public MomoPaymentService(WalletService walletService, MomoClient momoClient,
            MomoSignatureService signatureService, MomoPaymentRequestRepository momoPaymentRequestRepository) {
        this.walletService = walletService;
        this.momoClient = momoClient;
        this.signatureService = signatureService;
        this.momoPaymentRequestRepository = momoPaymentRequestRepository;
    }

    /**
     * Bước 1: tạo transaction PENDING (commit riêng, xem WalletService.initiateExternalDeposit)
     * rồi mới gọi MoMo - CỐ Ý không gộp 2 việc này vào 1 transaction DB duy nhất vì bước gọi
     * MoMo là 1 HTTP call ra ngoài, không nên giữ transaction DB (và khoá ví) mở trong lúc chờ
     * mạng.
     */
    public MomoDepositInitiateResponse initiate(User currentUser, BigDecimal amount, RequestMetadata metadata) {
        String momoOrderId = UUID.randomUUID().toString();
        String momoRequestId = UUID.randomUUID().toString();
        String idempotencyKey = "momo:" + momoOrderId;

        Transaction transaction = walletService.initiateExternalDeposit(currentUser, amount, idempotencyKey, metadata);

        MomoClient.MomoCreateResult result = momoClient.createPayment(
                amount, momoOrderId, momoRequestId, "Nap tien PMPay #" + transaction.getReferenceNumber());

        MomoPaymentRequest paymentRequest = new MomoPaymentRequest();
        paymentRequest.setTransaction(transaction);
        paymentRequest.setMomoOrderId(momoOrderId);
        paymentRequest.setMomoRequestId(momoRequestId);
        paymentRequest.setRequestType(MomoClient.REQUEST_TYPE);
        paymentRequest.setRawResponse(result.rawResponse());

        if (!result.success()) {
            paymentRequest.setStatus(MomoPaymentStatus.FAILED);
            momoPaymentRequestRepository.save(paymentRequest);
            walletService.failExternalDeposit(transaction, metadata, result.message());
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR, result.message());
        }

        paymentRequest.setStatus(MomoPaymentStatus.PENDING);
        momoPaymentRequestRepository.save(paymentRequest);
        return new MomoDepositInitiateResponse(result.payUrl(), transaction.getId(), momoOrderId);
    }

    /**
     * Nguồn DUY NHẤT được phép chuyển trạng thái sang CONFIRMED/FAILED và ghi ledger. Lock đúng
     * dòng momo_payment_requests theo momo_order_id trước khi đổi trạng thái - tránh race
     * condition nếu MoMo gọi IPN trùng lặp (sandbox MoMo có retry khi không nhận được 204 kịp).
     */
    @Transactional
    public void handleIpn(Map<String, Object> rawParams, RequestMetadata metadata) {
        Map<String, String> params = normalize(rawParams);

        if (!signatureService.verifyCallbackSignature(params)) {
            log.warn("[MomoPaymentService] IPN signature invalid, orderId={}", params.get("orderId"));
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }

        String momoOrderId = params.get("orderId");
        MomoPaymentRequest paymentRequest = momoPaymentRequestRepository.findByMomoOrderIdForUpdate(momoOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!paymentRequest.isPending()) {
            // Đã xử lý ở lần gọi IPN trước (retry từ MoMo, hoặc gọi trùng) - no-op, đúng ý
            // "chỉ lần gọi đầu tiên mới thực sự cập nhật" trong task brief mục 2.
            log.info("[MomoPaymentService] IPN duplicate, orderId={} status={}", momoOrderId, paymentRequest.getStatus());
            return;
        }

        Transaction transaction = paymentRequest.getTransaction();
        BigDecimal callbackAmount = parseAmount(params.get("amount"));
        if (callbackAmount == null || callbackAmount.compareTo(transaction.getAmount()) != 0) {
            paymentRequest.setStatus(MomoPaymentStatus.FAILED);
            momoPaymentRequestRepository.save(paymentRequest);
            walletService.failExternalDeposit(transaction, metadata, "MoMo IPN amount mismatch");
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        int resultCode = parseResultCode(params.get("resultCode"));
        if (resultCode == 0) {
            paymentRequest.setStatus(MomoPaymentStatus.CONFIRMED);
            paymentRequest.setMomoTransId(params.get("transId"));
            momoPaymentRequestRepository.save(paymentRequest);
            walletService.completeMomoDeposit(transaction, metadata);
        } else {
            paymentRequest.setStatus(MomoPaymentStatus.FAILED);
            momoPaymentRequestRepository.save(paymentRequest);
            walletService.failExternalDeposit(transaction, metadata,
                    "MoMo resultCode=" + resultCode + " message=" + params.get("message"));
        }
    }

    /** Chỉ đọc - dùng cho GET /momo/return, KHÔNG được finalize giao dịch (xem class-level doc). */
    @Transactional(readOnly = true)
    public MomoPaymentStatusResponse getStatus(String momoOrderId) {
        MomoPaymentRequest paymentRequest = momoPaymentRequestRepository.findByMomoOrderId(momoOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        Transaction transaction = paymentRequest.getTransaction();
        return new MomoPaymentStatusResponse(momoOrderId, paymentRequest.getStatus().name(),
                transaction.getId(), transaction.getStatus().name());
    }

    private Map<String, String> normalize(Map<String, Object> rawParams) {
        Map<String, String> params = new LinkedHashMap<>();
        rawParams.forEach((key, value) -> params.put(key, value == null ? "" : String.valueOf(value)));
        return params;
    }

    private BigDecimal parseAmount(String amount) {
        try {
            return amount == null ? null : new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseResultCode(String resultCode) {
        try {
            return resultCode == null ? -1 : Integer.parseInt(resultCode);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
