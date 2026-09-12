package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.ReviewFraudAssessmentRequest;
import com.digitalwallet.api.dto.response.FraudReviewResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.request.RequestMetadata;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.AdminFraudReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/fraud-assessments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFraudReviewController {

    private final AdminFraudReviewService adminFraudReviewService;

    public AdminFraudReviewController(AdminFraudReviewService adminFraudReviewService) {
        this.adminFraudReviewService = adminFraudReviewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FraudReviewResponse>>> list(
            @RequestParam(defaultValue = "PENDING_REVIEW") FraudReviewStatus reviewStatus,
            Pageable pageable, HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(),
                adminFraudReviewService.list(reviewStatus, pageable)));
    }

    @PostMapping("/{assessmentId}/review")
    public ResponseEntity<ApiResponse<FraudReviewResponse>> review(
            @CurrentUser User admin, @PathVariable UUID assessmentId,
            @Valid @RequestBody ReviewFraudAssessmentRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        FraudReviewResponse response = adminFraudReviewService.review(
                admin, assessmentId, request.getAction(), request.getNote(), RequestMetadata.from(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), response));
    }
}