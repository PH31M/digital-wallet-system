package com.digitalwallet.common.response;

import java.time.Instant;

/**
 * Standard API response wrapper.
 *
 * @param <T> response payload type
 */
public class ApiResponse<T> {

    private boolean success;
    private String requestId;
    private Instant timestamp;
    private T data;
    private ErrorInfo error;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String requestId, Instant timestamp, T data) {
        this(success, requestId, timestamp, data, null);
    }

    public ApiResponse(boolean success, String requestId, Instant timestamp, T data, ErrorInfo error) {
        this.success = success;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(String requestId, Instant timestamp, T data) {
        return new ApiResponse<>(true, requestId, timestamp, data, null);
    }

    public static <T> ApiResponse<T> error(String requestId, Instant timestamp, ErrorInfo error) {
        return new ApiResponse<>(false, requestId, timestamp, null, error);
    }

    public static <T> ApiResponse<T> error(String code, String message, String field, String requestId) {
        ErrorInfo errorInfo = new ErrorInfo(code, message, null, Instant.now(), field);
        return new ApiResponse<>(false, requestId, Instant.now(), null, errorInfo);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorInfo getError() {
        return error;
    }

    public void setError(ErrorInfo error) {
        this.error = error;
    }
}
