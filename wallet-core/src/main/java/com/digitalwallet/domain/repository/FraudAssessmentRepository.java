package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.FraudAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, UUID> {
}