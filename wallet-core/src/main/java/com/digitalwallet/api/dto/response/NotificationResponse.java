package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.Notification;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        @JsonProperty("public_id") UUID publicId,
        String type,
        String title,
        String message,
        Map<String, Object> metadata,
        boolean read,
        @JsonProperty("read_at") Instant readAt,
        @JsonProperty("created_at") Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getPublicId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getMetadata(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}