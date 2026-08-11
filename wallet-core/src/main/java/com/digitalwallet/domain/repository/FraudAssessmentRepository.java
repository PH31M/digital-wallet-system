package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.FraudAssessment;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, UUID> {
    Page<FraudAssessment> findByReviewStatus(FraudReviewStatus reviewStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FraudAssessment f WHERE f.id = :id")
    Optional<FraudAssessment> findByIdForUpdate(@Param("id") UUID id);
}