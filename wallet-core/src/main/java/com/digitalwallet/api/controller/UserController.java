package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.ChangePasswordRequest;
import com.digitalwallet.api.dto.request.UpdateProfileRequest;
import com.digitalwallet.api.dto.response.UserProfileResponse;
import com.digitalwallet.api.dto.response.UserSessionResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        UserProfileResponse response = userService.getProfile(currentUser);

        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @CurrentUser User currentUser,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        UserProfileResponse response = userService.updateProfile(currentUser, request);

        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), response));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentUser User currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        userService.changePassword(currentUser, request, httpRequest, requestId);

        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), null));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> listSessions(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), userService.listSessions(currentUser)));
    }

    @DeleteMapping("/me/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @CurrentUser User currentUser,
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        userService.revokeSession(currentUser, sessionId, httpRequest, requestId);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), null));
    }

    @PostMapping("/me/mfa/enable")
    public ResponseEntity<ApiResponse<Void>> enableMfa(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        userService.setMfa(currentUser, true, httpRequest, requestId);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), null));
    }

    @PostMapping("/me/mfa/disable")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        userService.setMfa(currentUser, false, httpRequest, requestId);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), null));
    }
}