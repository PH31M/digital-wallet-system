package com.digitalwallet.service;

import com.digitalwallet.config.MomoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MomoSignatureServiceTest {

    // Bộ credential test công khai MoMo công bố sẵn cho dev (developers.momo.vn) - không phải
    // secret thật, dùng chung được, xem task brief mục 1.
    private static final String ACCESS_KEY = "F8BBA842ECF85";
    private static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    private MomoSignatureService momoSignatureService;

    @BeforeEach
    void setUp() {
        MomoProperties properties = new MomoProperties();
        properties.setPartnerCode("MOMO");
        properties.setAccessKey(ACCESS_KEY);
        properties.setSecretKey(SECRET_KEY);
        momoSignatureService = new MomoSignatureService(properties);
    }

    @Test
    void buildCreateSignature_isDeterministicForSameInput() {
        String signature1 = momoSignatureService.buildCreateSignature(
                "50000", "order-1", "Nap tien PMPay", "http://localhost/return",
                "http://localhost/ipn", "request-1", "captureWallet");
        String signature2 = momoSignatureService.buildCreateSignature(
                "50000", "order-1", "Nap tien PMPay", "http://localhost/return",
                "http://localhost/ipn", "request-1", "captureWallet");

        assertThat(signature1).isEqualTo(signature2);
        assertThat(signature1).hasSize(64); // hex-encoded SHA-256 = 32 bytes = 64 hex chars
    }

    @Test
    void buildCreateSignature_differentAmount_producesDifferentSignature() {
        String signature1 = momoSignatureService.buildCreateSignature(
                "50000", "order-1", "Nap tien PMPay", "http://localhost/return",
                "http://localhost/ipn", "request-1", "captureWallet");
        String signature2 = momoSignatureService.buildCreateSignature(
                "60000", "order-1", "Nap tien PMPay", "http://localhost/return",
                "http://localhost/ipn", "request-1", "captureWallet");

        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void verifyCallbackSignature_validSignature_returnsTrue() {
        Map<String, String> params = validCallbackParams();
        params.put("signature", computeHmac(SECRET_KEY, params));

        assertThat(momoSignatureService.verifyCallbackSignature(params)).isTrue();
    }

    @Test
    void verifyCallbackSignature_tamperedAmount_returnsFalse() {
        Map<String, String> params = validCallbackParams();
        params.put("signature", computeHmac(SECRET_KEY, params));

        // Kẻ tấn công sửa amount sau khi chữ ký đã được tính - phải bị phát hiện.
        params.put("amount", "999999999");

        assertThat(momoSignatureService.verifyCallbackSignature(params)).isFalse();
    }

    @Test
    void verifyCallbackSignature_missingSignature_returnsFalse() {
        Map<String, String> params = validCallbackParams();

        assertThat(momoSignatureService.verifyCallbackSignature(params)).isFalse();
    }

    private Map<String, String> validCallbackParams() {
        Map<String, String> params = new HashMap<>();
        params.put("amount", "50000");
        params.put("extraData", "");
        params.put("message", "Success");
        params.put("orderId", "order-1");
        params.put("orderInfo", "Nap tien PMPay");
        params.put("orderType", "momo_wallet");
        params.put("partnerCode", "MOMO");
        params.put("payType", "qr");
        params.put("requestId", "request-1");
        params.put("responseTime", "1700000000000");
        params.put("resultCode", "0");
        params.put("transId", "123456789");
        return params;
    }

    /**
     * Tính lại HMAC bằng đúng field set/order mà MomoSignatureService dùng cho callback, để
     * test độc lập với việc service có method build-signature riêng cho callback hay không.
     */
    private String computeHmac(String secretKey, Map<String, String> params) {
        String raw = "accessKey=" + ACCESS_KEY
                + "&amount=" + params.getOrDefault("amount", "")
                + "&extraData=" + params.getOrDefault("extraData", "")
                + "&message=" + params.getOrDefault("message", "")
                + "&orderId=" + params.getOrDefault("orderId", "")
                + "&orderInfo=" + params.getOrDefault("orderInfo", "")
                + "&orderType=" + params.getOrDefault("orderType", "")
                + "&partnerCode=" + params.getOrDefault("partnerCode", "")
                + "&payType=" + params.getOrDefault("payType", "")
                + "&requestId=" + params.getOrDefault("requestId", "")
                + "&responseTime=" + params.getOrDefault("responseTime", "")
                + "&resultCode=" + params.getOrDefault("resultCode", "")
                + "&transId=" + params.getOrDefault("transId", "");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
