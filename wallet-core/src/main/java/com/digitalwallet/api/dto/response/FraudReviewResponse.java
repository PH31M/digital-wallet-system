package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.FraudAssessment;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudReviewResponse(
        UUID id,
        TransactionResponse transaction,
        String decision,
        @JsonProperty("risk_score") BigDecimal riskScore,
        @JsonProperty("triggered_rules") String triggeredRules,
        @JsonProperty("review_status") String reviewStatus,
        @JsonProperty("review_action") String reviewAction,
        @JsonProperty("review_note") String reviewNote,
        @JsonProperty("reviewed_at") Instant reviewedAt,
        @JsonProperty("reviewed_by") UUID reviewedBy) {

    public static FraudReviewResponse from(FraudAssessment assessment) {
        return new FraudReviewResponse(
                assessment.getId(),
                TransactionResponse.from(assessment.getTransaction()),
                assessment.getDecision().name(),
                assessment.getRiskScore(),
                assessment.getTriggeredRulesJson(),
                assessment.getReviewStatus().name(),
                assessment.getReviewAction() == null ? null : assessment.getReviewAction().name(),
                assessment.getReviewNote(),
                assessment.getReviewedAt(),
                assessment.getReviewedBy() == null ? null : assessment.getReviewedBy().getId());
    }
}