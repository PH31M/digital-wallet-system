package com.digitalwallet.security;

import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(RequestIds.HEADER, requestId);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(errorCode.name(), errorCode.getDefaultMessage(), null,
                        request.getRequestURI(), requestId));
    }

    private String resolveRequestId(HttpServletRequest request) {
        return RequestIds.get(request);
    }
}