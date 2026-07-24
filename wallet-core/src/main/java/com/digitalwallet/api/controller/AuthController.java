package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.LoginRequest;
import com.digitalwallet.api.dto.request.RegisterRequest;
import com.digitalwallet.common.response.ApiResponse;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                UUID.randomUUID().toString(),
                Instant.now(),
                "Login not implemented"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                UUID.randomUUID().toString(),
                Instant.now(),
                "Register not implemented"));
    }
}
