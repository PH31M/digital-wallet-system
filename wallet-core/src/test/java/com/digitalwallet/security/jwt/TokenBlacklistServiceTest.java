package com.digitalwallet.security.jwt;

import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void isBlacklisted_whenRedisHasKeyReturnsTrue_returnsTrue() {
        when(redisTemplate.hasKey("blacklist:token:token-1")).thenReturn(true);

        assertThat(service().isBlacklisted("token-1")).isTrue();
    }

    @Test
    void isBlacklisted_whenRedisFails_failsClosed() {
        when(redisTemplate.hasKey("blacklist:token:token-1")).thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> service().isBlacklisted("token-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE);
    }

    @Test
    void blacklist_whenRedisFails_failsClosed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set("blacklist:token:token-1", "revoked", Duration.ofMinutes(5));

        assertThatThrownBy(() -> service().blacklist("token-1", Duration.ofMinutes(5)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE);
    }

    @Test
    void blacklist_withExpiredTtl_doesNotWriteRedis() {
        service().blacklist("token-1", Duration.ZERO);

        verify(redisTemplate, org.mockito.Mockito.never()).opsForValue();
    }

    private TokenBlacklistService service() {
        return new TokenBlacklistService(redisTemplate);
    }
}