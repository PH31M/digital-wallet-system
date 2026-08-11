package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.NotificationResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.Notification;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @CurrentUser User currentUser,
            Pageable pageable,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        Page<NotificationResponse> notifications = notificationService
                .getUserNotifications(currentUser, pageable, unreadOnly)
                .map(NotificationResponse::from);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        long unreadCount = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), Map.of("unreadCount", unreadCount)));
    }

    @PatchMapping("/{publicId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @CurrentUser User currentUser,
            @PathVariable UUID publicId,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        Notification notification = notificationService.markAsRead(currentUser, publicId);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), NotificationResponse.from(notification)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @CurrentUser User currentUser,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        int updatedCount = notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), Map.of("updatedCount", updatedCount)));
    }
}