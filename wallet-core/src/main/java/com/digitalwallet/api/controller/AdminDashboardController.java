package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.AdminDashboardOverviewResponse;
import com.digitalwallet.api.dto.response.AdminDashboardVolumeResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping({"/api/admin/dashboard", "/api/v1/admin/dashboard"})
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> overview(HttpServletRequest request) {
        String requestId = RequestIds.get(request);
        return ResponseEntity.ok(ApiResponse.success(
                requestId, Instant.now(), dashboardService.getOverview()));
    }

    @GetMapping("/volume")
    public ResponseEntity<ApiResponse<AdminDashboardVolumeResponse>> volume(
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days,
            HttpServletRequest request) {
        String requestId = RequestIds.get(request);
        return ResponseEntity.ok(ApiResponse.success(
                requestId, Instant.now(), dashboardService.getDailyVolume(days)));
    }
}