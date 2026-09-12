package com.digitalwallet.api.dto.response;

import java.time.Instant;
import java.util.List;

public record AdminDashboardVolumeResponse(
        int days,
        List<DailyVolumePoint> series,
        Instant generatedAt) {
}