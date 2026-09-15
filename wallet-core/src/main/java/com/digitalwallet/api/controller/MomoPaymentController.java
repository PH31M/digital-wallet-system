package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.MomoPaymentStatusResponse;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.service.MomoPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Callback công khai từ MoMo - KHÔNG có JWT (MoMo gọi thẳng server-to-server/redirect trình
 * duyệt), permit riêng trong SecurityConfig. Xem task brief mục 3.2: chỉ /ipn được phép
 * finalize giao dịch, /return chỉ đọc trạng thái.
 */
@RestController
@RequestMapping("/api/momo")
public class MomoPaymentController {

    private final MomoPaymentService momoPaymentService;

    public MomoPaymentController(MomoPaymentService momoPaymentService) {
        this.momoPaymentService = momoPaymentService;
    }

    @GetMapping("/return")
    public ResponseEntity<ApiResponse<MomoPaymentStatusResponse>> paymentReturn(
            @RequestParam("orderId") String momoOrderId,
            HttpServletRequest httpRequest) {
        RequestMetadata metadata = RequestMetadata.from(httpRequest);
        MomoPaymentStatusResponse status = momoPaymentService.getStatus(momoOrderId);
        return ResponseEntity.ok(ApiResponse.success(metadata.requestId().toString(), Instant.now(), status));
    }

    /**
     * MoMo yêu cầu phản hồi HTTP 204 trong vòng 15s (developers.momo.vn/.../result-handling/
     * notification) - KHÔNG dùng body {"message": ...} như bản Flask gốc (đó chỉ là placeholder
     * chưa verify theo đúng spec, xem task brief Phần D).
     */
    @PostMapping("/ipn")
    public ResponseEntity<Void> paymentIpn(@RequestBody Map<String, Object> params, HttpServletRequest httpRequest) {
        RequestMetadata metadata = RequestMetadata.from(httpRequest);
        momoPaymentService.handleIpn(params, metadata);
        return ResponseEntity.noContent().build();
    }
}
