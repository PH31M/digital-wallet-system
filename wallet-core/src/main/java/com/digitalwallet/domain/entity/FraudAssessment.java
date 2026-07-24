package com.digitalwallet.domain.entity;

import com.digitalwallet.domain.enums.FraudDecision;
import com.digitalwallet.domain.enums.FraudReviewAction;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "rule_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal ruleScore;

    @Column(name = "ai_anomaly_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal aiAnomalyScore;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "triggered_rules", columnDefinition = "jsonb")
    private String triggeredRulesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private FraudReviewStatus reviewStatus = FraudReviewStatus.NOT_REQUIRED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_action", length = 20)
    private FraudReviewAction reviewAction;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // getters/setters
    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public BigDecimal getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(BigDecimal ruleScore) {
        this.ruleScore = ruleScore;
    }

    public BigDecimal getAiAnomalyScore() {
        return aiAnomalyScore;
    }

    public void setAiAnomalyScore(BigDecimal aiAnomalyScore) {
        this.aiAnomalyScore = aiAnomalyScore;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getTriggeredRulesJson() {
        return triggeredRulesJson;
    }

    public void setTriggeredRulesJson(String triggeredRulesJson) {
        this.triggeredRulesJson = triggeredRulesJson;
    }

    public FraudDecision getDecision() {
        return decision;
    }

    public void setDecision(FraudDecision decision) {
        this.decision = decision;
    }

    public FraudReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(FraudReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public FraudReviewAction getReviewAction() {
        return reviewAction;
    }

    public void setReviewAction(FraudReviewAction reviewAction) {
        this.reviewAction = reviewAction;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

}
