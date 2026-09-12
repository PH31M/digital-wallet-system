package com.digitalwallet.security;

import com.digitalwallet.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    @Test
    void underLimit_continuesRequestAndSetsTtlOnFirstHit() throws Exception {
        RateLimitFilter filter = filterWithAuthLimit(2);
        MockHttpServletRequest request = request("/api/auth/login", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:auth:203.0.113.10")).thenReturn(1L);

        filter.doFilter(request, response, filterChain);

        verify(redisTemplate).expire("rate-limit:auth:203.0.113.10", Duration.ofMinutes(1));
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void overLimit_returnsConsistent429JsonAndDoesNotContinueChain() throws Exception {
        RateLimitFilter filter = filterWithAuthLimit(2);
        MockHttpServletRequest request = request("/api/auth/login", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:auth:203.0.113.10")).thenReturn(3L);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("RATE_LIMITED");
        assertThat(response.getContentAsString()).contains("/api/auth/login");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void staleApiV1AuthPath_usesDefaultBucketNotAuthBucket() throws Exception {
        RateLimitFilter filter = filterWithAuthLimit(2);
        MockHttpServletRequest request = request("/api/v1/auth/login", "203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:default:203.0.113.10")).thenReturn(1L);

        filter.doFilter(request, response, filterChain);

        verify(valueOperations).increment("rate-limit:default:203.0.113.10");
        verify(valueOperations, never()).increment("rate-limit:auth:203.0.113.10");
        verify(filterChain).doFilter(request, response);
    }

    private RateLimitFilter filterWithAuthLimit(int limit) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setWindow(Duration.ofMinutes(1));
        properties.setAuthLimit(limit);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return new RateLimitFilter(redisTemplate, properties, new SecurityErrorResponseWriter(objectMapper));
    }

    private MockHttpServletRequest request(String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return request;
    }
}