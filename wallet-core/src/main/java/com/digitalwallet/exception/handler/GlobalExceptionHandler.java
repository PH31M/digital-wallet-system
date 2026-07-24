package com.digitalwallet.exception.handler;

import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * Maps application exceptions to a consistent API error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // 1. Lỗi validation (@Valid trên DTO — VD RegisterRequest)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidation(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                String requestId = resolveRequestId(request);
                FieldError fe = ex.getBindingResult().getFieldError();
                String field = fe != null ? fe.getField() : null;
                String message = fe != null ? fe.getDefaultMessage() : "Dữ liệu không hợp lệ";

                log.warn("[{}] Validation failed: field={}, message={}", requestId, field, message);

                return ResponseEntity.badRequest().body(
                                ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(), message, field, requestId));
        }

        // 2. Business exception tự định nghĩa (EMAIL_ALREADY_EXISTS, WEAK_PASSWORD,
        // OTP_EXPIRED...)
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusiness(
                        BusinessException ex, HttpServletRequest request) {
                String requestId = resolveRequestId(request);
                ErrorCode code = ex.getErrorCode();

                log.warn("[{}] Business exception: code={}, message={}", requestId, code, ex.getMessage());

                return ResponseEntity.status(code.getHttpStatus()).body(
                                ApiResponse.error(code.name(), ex.getMessage(), ex.getField(), requestId));
        }

        // 3. Spring Security từ chối quyền truy cập
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
                        AccessDeniedException ex, HttpServletRequest request) {
                String requestId = resolveRequestId(request);
                log.warn("[{}] Access denied: {}", requestId, ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                ApiResponse.error(ErrorCode.ACCESS_DENIED.name(),
                                                ErrorCode.ACCESS_DENIED.getDefaultMessage(), null, requestId));
        }

        // 4. Bắt tất cả — không được để lộ exception message thật ra ngoài
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleUnhandled(
                        Exception ex, HttpServletRequest request) {
                String requestId = resolveRequestId(request);
                log.error("[{}] Unhandled exception at {}", requestId, request.getRequestURI(), ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.name(),
                                                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(), null, requestId));
        }

        private String resolveRequestId(HttpServletRequest request) {
                // Tạm thời: generate mới mỗi lần. Khi làm DWS-126 (RequestId filter),
                // thay bằng: (String) request.getAttribute("requestId")
                return UUID.randomUUID().toString();
        }
}
