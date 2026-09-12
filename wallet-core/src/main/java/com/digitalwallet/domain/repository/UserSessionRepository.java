package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenId(String refreshTokenId);

    @Query("""
            select s from UserSession s
            where s.user.id = :userId
              and s.revokedAt is null
              and s.expiresAt > :now
            order by s.lastUsedAt desc
            """)
    List<UserSession> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("""
            update UserSession s
            set s.revokedAt = :revokedAt
            where s.user.id = :userId
              and s.revokedAt is null
            """)
    int revokeActiveByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}