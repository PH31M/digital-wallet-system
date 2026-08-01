package com.digitalwallet.security;

import com.digitalwallet.config.RateLimitProperties;
import com.digitalwallet.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public RateLimitFilter(StringRedisTemplate redisTemplate,
            RateLimitProperties properties,
            SecurityErrorResponseWriter errorResponseWriter) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        LimitBucket bucket = resolveBucket(request);
        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate-limit:%s:%s".formatted(bucket.name(), clientIp(request));
        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (Long.valueOf(1L).equals(count)) {
                redisTemplate.expire(key, properties.getWindow());
            }
        } catch (RuntimeException ex) {
            log.error("Could not apply IP rate limit", ex);
            errorResponseWriter.write(request, response, ErrorCode.RATE_LIMIT_UNAVAILABLE);
            return;
        }

        if (count != null && count > bucket.limit()) {
            response.setHeader("Retry-After", String.valueOf(properties.getWindow().toSeconds()));
            errorResponseWriter.write(request, response, ErrorCode.RATE_LIMITED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private LimitBucket resolveBucket(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase(Locale.ROOT);

        if (path.startsWith("/actuator/health")) {
            return null;
        }

        if (path.startsWith("/api/auth/")) {
            return new LimitBucket("auth", properties.getAuthLimit());
        }

        if ("POST".equals(method) && path.startsWith("/api/wallets/me/")) {
            return new LimitBucket("money", properties.getMoneyMovementLimit());
        }

        return new LimitBucket("default", properties.getDefaultLimit());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record LimitBucket(String name, int limit) {
    }
}