package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminDashboardVolumeResponse(
        int days,
        List<DailyVolumePoint> series,
        Instant generatedAt) {
}