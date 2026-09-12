package com.digitalwallet.common.request;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class RequestIds {

    public static final String ATTRIBUTE = RequestIds.class.getName() + ".REQUEST_ID";
    public static final String HEADER = "X-Request-Id";

    private RequestIds() {
    }

    public static UUID getUuid(HttpServletRequest request) {
        return UUID.fromString(get(request));
    }

    public static String get(HttpServletRequest request) {
        String existing = requestIdFromAttribute(request);
        if (existing != null) {
            setAttribute(request, existing);
            return existing;
        }

        String incoming = requestIdFromHeader(request);
        if (incoming != null) {
            setAttribute(request, incoming);
            return incoming;
        }

        String generated = UUID.randomUUID().toString();
        setAttribute(request, generated);
        return generated;
    }

    private static String requestIdFromAttribute(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Object requestId = request.getAttribute(ATTRIBUTE);
        if (requestId instanceof UUID value) {
            return value.toString();
        }
        if (requestId instanceof String value) {
            return normalize(value);
        }
        return null;
    }

    private static String requestIdFromHeader(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return normalize(request.getHeader(HEADER));
    }

    private static String normalize(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(candidate.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void setAttribute(HttpServletRequest request, String requestId) {
        if (request != null) {
            request.setAttribute(ATTRIBUTE, requestId);
        }
    }
}