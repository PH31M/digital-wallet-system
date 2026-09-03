package com.digitalwallet.exception.handler;

import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

/**
 * Maps application exceptions to a consistent API error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String field = fieldError != null ? toWireField(fieldError.getField()) : null;
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Invalid request data";
        ErrorCode errorCode = resolveValidationErrorCode(fieldError);

        log.warn("[{}] Validation failed: field={}, message={}", requestId, field, message);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(
                ApiResponse.error(errorCode.name(), message, field, request.getRequestURI(), requestId));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.VALIDATION_FAILED.getDefaultMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(), message, null,
                        request.getRequestURI(), requestId));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        ErrorCode code = ex.getErrorCode();

        log.warn("[{}] Business exception: code={}, message={}", requestId, code, ex.getMessage());

        return ResponseEntity.status(code.getHttpStatus()).body(
                ApiResponse.error(code.name(), clientMessage(ex), toWireField(ex.getField()),
                        request.getRequestURI(), requestId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.warn("[{}] Access denied: {}", requestId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(ErrorCode.ACCESS_DENIED.name(),
                        ErrorCode.ACCESS_DENIED.getDefaultMessage(), null, request.getRequestURI(), requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(
            Exception ex, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        log.error("[{}] Unhandled exception at {}", requestId, request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(), null, request.getRequestURI(), requestId));
    }

    private ErrorCode resolveValidationErrorCode(FieldError fieldError) {
        if (fieldError == null || !"password".equals(fieldError.getField())) {
            return ErrorCode.VALIDATION_FAILED;
        }

        String[] codes = fieldError.getCodes();
        boolean weakPassword = "StrongPassword".equals(fieldError.getCode())
                || "Size".equals(fieldError.getCode())
                || (codes != null && Arrays.asList(codes).stream()
                        .anyMatch(code -> code.contains("StrongPassword")));

        return weakPassword ? ErrorCode.WEAK_PASSWORD : ErrorCode.VALIDATION_FAILED;
    }

    private String clientMessage(BusinessException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getErrorCode().getDefaultMessage();
        }
        return message;
    }

    private String toWireField(String field) {
        if (field == null) {
            return null;
        }
        return switch (field) {
            case "fullName" -> "full_name";
            case "phoneNumber" -> "phone_number";
            case "refreshToken" -> "refresh_token";
            case "receiverWalletId" -> "receiver_wallet_id";
            case "idempotencyKey" -> "idempotency_key";
            default -> field;
        };
    }

    private String resolveRequestId(HttpServletRequest request) {
        return RequestIds.get(request);
    }
}
