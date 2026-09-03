package com.digitalwallet.service;

import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.enums.FraudDecision;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.repository.FraudAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudServiceTest {

    @Mock
    private FraudAssessmentRepository fraudAssessmentRepository;

    @Test
    void assess_lowValueTransaction_allowsAndRecordsAssessment() {
        FraudService fraudService = fraudService();
        Transaction transaction = transaction("100.00");
        when(fraudAssessmentRepository.save(any(FraudAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudAssessment assessment = fraudService.assess(transaction);

        assertThat(assessment.getDecision()).isEqualTo(FraudDecision.ALLOW);
        assertThat(transaction.getFraudDecision()).isEqualTo(FraudDecision.ALLOW);
        assertThat(transaction.getFraudScore()).isEqualByComparingTo("0.00");
        assertThat(assessment.getTriggeredRulesJson()).isEqualTo("[]");
    }

    @Test
    void assess_highValueTransaction_challengesAndRecordsRule() {
        FraudService fraudService = fraudService();
        Transaction transaction = transaction("10000000.00");
        when(fraudAssessmentRepository.save(any(FraudAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudAssessment assessment = fraudService.assess(transaction);

        assertThat(assessment.getDecision()).isEqualTo(FraudDecision.CHALLENGE);
        assertThat(assessment.getReviewStatus()).isEqualTo(FraudReviewStatus.PENDING_REVIEW);
        assertThat(assessment.getTriggeredRulesJson()).contains("HIGH_VALUE_TRANSACTION");
        verify(fraudAssessmentRepository).save(any(FraudAssessment.class));
    }

    @Test
    void assess_blockAmount_blocksAndPersistsLinkedTransaction() {
        FraudService fraudService = fraudService();
        Transaction transaction = transaction("100000000.00");
        when(fraudAssessmentRepository.save(any(FraudAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fraudService.assess(transaction);

        ArgumentCaptor<FraudAssessment> captor = ArgumentCaptor.forClass(FraudAssessment.class);
        verify(fraudAssessmentRepository).save(captor.capture());
        assertThat(captor.getValue().getTransaction()).isEqualTo(transaction);
        assertThat(captor.getValue().getDecision()).isEqualTo(FraudDecision.BLOCK);
        assertThat(captor.getValue().getTriggeredRulesJson()).contains("BLOCK_AMOUNT_EXCEEDED");
    }

    private FraudService fraudService() {
        return new FraudService(fraudAssessmentRepository,
                new BigDecimal("10000000.00"), new BigDecimal("100000000.00"));
    }

    private Transaction transaction(String amount) {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }
}