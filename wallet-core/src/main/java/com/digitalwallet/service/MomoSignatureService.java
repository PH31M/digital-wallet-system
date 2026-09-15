package com.digitalwallet.service;

import com.digitalwallet.config.MomoProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Ký/xác thực chữ ký HMAC-SHA256 cho MoMo AIO v2 gateway — port từ
 * KHM-Foodie/app/service/momoService.py (đã chạy thật trên sandbox MoMo), xem task brief
 * "Tích hợp MoMo" (15/09/2026).
 *
 * LƯU Ý (giữ nguyên từ bản gốc): thứ tự/field chính xác của chữ ký callback (return/IPN) cần
 * đối chiếu lại với sandbox thật khi test qua tunnel (Phần E của brief) — MoMo có nhiều luồng
 * API (payWithATM/captureWallet) với field hơi khác nhau, bản này theo đúng field set đã verify
 * chạy được ở KHM-Foodie.
 */
@Service
public class MomoSignatureService {

    private final MomoProperties momoProperties;

    public MomoSignatureService(MomoProperties momoProperties) {
        this.momoProperties = momoProperties;
    }

    /** Chữ ký cho bước tạo yêu cầu thanh toán (POST .../v2/gateway/api/create). */
    public String buildCreateSignature(String amount, String orderId, String orderInfo,
            String redirectUrl, String ipnUrl, String requestId, String requestType) {
        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData="
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
        return hmacSha256Hex(rawSignature);
    }

    /**
     * Xác thực chữ ký MoMo gửi kèm ở return (query string) hoặc IPN (JSON body) — cùng field
     * set, chỉ khác cách truyền. Dùng {@link MessageDigest#isEqual(byte[], byte[])} thay vì
     * so sánh String thường để tránh timing attack (yêu cầu chốt trong task brief).
     */
    public boolean verifyCallbackSignature(Map<String, String> params) {
        String signature = params.get("signature");
        if (signature == null || signature.isBlank()) {
            return false;
        }

        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + nullToEmpty(params.get("amount"))
                + "&extraData=" + nullToEmpty(params.get("extraData"))
                + "&message=" + nullToEmpty(params.get("message"))
                + "&orderId=" + nullToEmpty(params.get("orderId"))
                + "&orderInfo=" + nullToEmpty(params.get("orderInfo"))
                + "&orderType=" + nullToEmpty(params.get("orderType"))
                + "&partnerCode=" + nullToEmpty(params.get("partnerCode"))
                + "&payType=" + nullToEmpty(params.get("payType"))
                + "&requestId=" + nullToEmpty(params.get("requestId"))
                + "&responseTime=" + nullToEmpty(params.get("responseTime"))
                + "&resultCode=" + nullToEmpty(params.get("resultCode"))
                + "&transId=" + nullToEmpty(params.get("transId"));

        String expectedSignature = hmacSha256Hex(rawSignature);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(momoProperties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute MoMo HMAC-SHA256 signature", e);
        }
    }
}
