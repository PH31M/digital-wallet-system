package com.digitalwallet.common.request;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public record RequestMetadata(UUID requestId, String ipAddress, String userAgent) {

    public static RequestMetadata from(HttpServletRequest request) {
        return new RequestMetadata(
                RequestIds.getUuid(request),
                clientIp(request),
                request.getHeader("User-Agent"));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}