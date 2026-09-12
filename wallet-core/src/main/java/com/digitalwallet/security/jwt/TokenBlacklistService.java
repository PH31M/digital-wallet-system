package com.digitalwallet.security.jwt;

import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String PREFIX = "blacklist:token:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (RuntimeException ex) {
            log.error("Could not check token blacklist in Redis", ex);
            throw tokenBlacklistUnavailable(ex);
        }
    }

    public void blacklist(String token, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(PREFIX + token, "revoked", ttl);
        } catch (RuntimeException ex) {
            log.error("Could not blacklist token in Redis", ex);
            throw tokenBlacklistUnavailable(ex);
        }
    }

    private BusinessException tokenBlacklistUnavailable(RuntimeException cause) {
        return new BusinessException(
                ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE,
                ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE.getDefaultMessage(),
                cause);
    }
}
