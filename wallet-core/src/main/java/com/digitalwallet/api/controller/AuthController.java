package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.ForgotPasswordRequest;
import com.digitalwallet.api.dto.request.LoginRequest;
import com.digitalwallet.api.dto.request.RefreshTokenRequest;
import com.digitalwallet.api.dto.request.RegisterRequest;
import com.digitalwallet.api.dto.request.ResendVerificationRequest;
import com.digitalwallet.api.dto.request.ResetPasswordRequest;
import com.digitalwallet.api.dto.request.VerifyEmailRequest;
import com.digitalwallet.api.dto.request.VerifyMfaRequest;
import com.digitalwallet.api.dto.response.AuthResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication REST controller.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        AuthResponse response = authService.login(request, httpRequest, requestId);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        AuthResponse response = authService.register(request, httpRequest, requestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        AuthResponse response = authService.refreshToken(request, httpRequest, requestId);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        authService.logout(request.getRefreshToken(), httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                null));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfa(
            @Valid @RequestBody VerifyMfaRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        AuthResponse response = authService.verifyMfa(request, httpRequest, requestId);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        authService.forgotPassword(request, httpRequest, requestId);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        authService.resetPassword(request, httpRequest, requestId);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        authService.verifyEmail(request.getUserId(), request.getOtp(), requestId);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        authService.resendVerification(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(),
                Instant.now(),
                null));
    }
}
