package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.MomoPaymentRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MomoPaymentRequestRepository extends JpaRepository<MomoPaymentRequest, UUID> {
    Optional<MomoPaymentRequest> findByMomoOrderId(String momoOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MomoPaymentRequest m WHERE m.momoOrderId = :momoOrderId")
    Optional<MomoPaymentRequest> findByMomoOrderIdForUpdate(@Param("momoOrderId") String momoOrderId);
}
