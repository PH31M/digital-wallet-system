package com.digitalwallet.common.response;

import java.time.Instant;

/**
 * Error details returned to API clients.
 */
public class ErrorInfo {

    private String code;
    private String message;
    private String path;
    private Instant timestamp;
    private String field;

    public ErrorInfo() {
    }

    public ErrorInfo(String code, String message, String path, Instant timestamp, String field) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
