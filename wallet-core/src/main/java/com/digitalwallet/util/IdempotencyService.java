package com.digitalwallet.util;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-based idempotency service.
 */
@Service
public class IdempotencyService {

    private final ReactiveStringRedisTemplate redisTemplate;

    public IdempotencyService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> claim(String key, Duration ttl) {
        return redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl);
    }
}
