package com.digitalwallet.service;

import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.enums.FraudDecision;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.repository.FraudAssessmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FraudService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final BigDecimal challengeAmount;
    private final BigDecimal blockAmount;

    public FraudService(FraudAssessmentRepository fraudAssessmentRepository,
            @Value("${wallet.fraud.challenge-amount:10000000}") BigDecimal challengeAmount,
            @Value("${wallet.fraud.block-amount:100000000}") BigDecimal blockAmount) {
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.challengeAmount = challengeAmount;
        this.blockAmount = blockAmount;
    }

    public FraudAssessment assess(Transaction transaction) {
        FraudResult result = score(transaction.getAmount());

        FraudAssessment assessment = new FraudAssessment();
        assessment.setTransaction(transaction);
        assessment.setRiskScore(result.riskScore());
        assessment.setRuleScore(result.riskScore());
        assessment.setAiAnomalyScore(ZERO);
        assessment.setModelVersion("rules-v1");
        assessment.setTriggeredRulesJson(toJsonArray(result.triggeredRules()));
        assessment.setDecision(result.decision());
        assessment.setReviewStatus(result.decision() == FraudDecision.CHALLENGE
                ? FraudReviewStatus.PENDING_REVIEW
                : FraudReviewStatus.NOT_REQUIRED);

        transaction.setFraudScore(result.riskScore());
        transaction.setFraudDecision(result.decision());
        return fraudAssessmentRepository.save(assessment);
    }

    private FraudResult score(BigDecimal amount) {
        List<String> rules = new ArrayList<>();
        BigDecimal riskScore = ZERO;
        FraudDecision decision = FraudDecision.ALLOW;

        if (amount.compareTo(challengeAmount) >= 0) {
            riskScore = new BigDecimal("70.00");
            decision = FraudDecision.CHALLENGE;
            rules.add("HIGH_VALUE_TRANSACTION");
        }

        if (amount.compareTo(blockAmount) >= 0) {
            riskScore = new BigDecimal("100.00");
            decision = FraudDecision.BLOCK;
            rules.add("BLOCK_AMOUNT_EXCEEDED");
        }

        return new FraudResult(riskScore, decision, rules);
    }

    private String toJsonArray(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\",\"", values) + "\"]";
    }

    private record FraudResult(BigDecimal riskScore, FraudDecision decision, List<String> triggeredRules) {
    }
}