package com.digitalwallet.service;

import com.digitalwallet.config.MomoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gọi MoMo AIO v2 create-payment API - port từ KHM-Foodie/app/service/momoService.py
 * (create_momo_payment), đã chạy thật trên sandbox MoMo. requestType đổi thành
 * "captureWallet" (thanh toán qua app MoMo) thay vì "payWithATM" bản gốc - xem task brief mục 1.
 */
@Service
public class MomoClient {

    private static final Logger log = LoggerFactory.getLogger(MomoClient.class);
    static final String REQUEST_TYPE = "captureWallet";

    private final RestTemplate restTemplate;
    private final MomoProperties momoProperties;
    private final MomoSignatureService signatureService;
    private final ObjectMapper objectMapper;

    public MomoClient(RestTemplate momoRestTemplate, MomoProperties momoProperties,
            MomoSignatureService signatureService, ObjectMapper objectMapper) {
        this.restTemplate = momoRestTemplate;
        this.momoProperties = momoProperties;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
    }

    public MomoCreateResult createPayment(BigDecimal amount, String orderId, String requestId, String orderInfo) {
        String amountStr = amount.toBigInteger().toString();
        String signature = signatureService.buildCreateSignature(amountStr, orderId, orderInfo,
                momoProperties.getRedirectUrl(), momoProperties.getIpnUrl(), requestId, REQUEST_TYPE);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", momoProperties.getPartnerCode());
        payload.put("accessKey", momoProperties.getAccessKey());
        payload.put("requestId", requestId);
        payload.put("amount", amountStr);
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momoProperties.getRedirectUrl());
        payload.put("ipnUrl", momoProperties.getIpnUrl());
        payload.put("extraData", "");
        payload.put("requestType", REQUEST_TYPE);
        payload.put("signature", signature);
        payload.put("lang", "vi");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    momoProperties.getEndpoint(), HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() { });

            Map<String, Object> body = response.getBody() == null ? Map.of() : response.getBody();
            String rawResponse = toJson(body);
            log.info("[MomoClient] create payment orderId={} httpStatus={} resultCode={}",
                    orderId, response.getStatusCode(), body.get("resultCode"));

            if (response.getStatusCode() != HttpStatus.OK) {
                return MomoCreateResult.failure("MoMo API HTTP " + response.getStatusCode(), rawResponse);
            }

            Object resultCodeRaw = body.get("resultCode");
            int resultCode = resultCodeRaw instanceof Number number ? number.intValue() : -1;
            if (resultCode != 0) {
                String message = String.valueOf(body.getOrDefault("message", "MoMo tu choi tao thanh toan"));
                return MomoCreateResult.failure(message, rawResponse);
            }

            String payUrl = String.valueOf(body.get("payUrl"));
            return MomoCreateResult.success(payUrl, rawResponse);
        } catch (RestClientException e) {
            log.error("[MomoClient] create payment orderId={} failed", orderId, e);
            return MomoCreateResult.failure("Khong goi duoc MoMo: " + e.getMessage(), null);
        }
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    public record MomoCreateResult(boolean success, String payUrl, String message, String rawResponse) {
        static MomoCreateResult success(String payUrl, String rawResponse) {
            return new MomoCreateResult(true, payUrl, null, rawResponse);
        }

        static MomoCreateResult failure(String message, String rawResponse) {
            return new MomoCreateResult(false, null, message, rawResponse);
        }
    }
}
