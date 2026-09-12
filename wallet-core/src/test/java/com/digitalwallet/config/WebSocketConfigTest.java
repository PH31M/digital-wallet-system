package com.digitalwallet.config;

import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.security.websocket.JwtStompChannelInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConfigTest {

    @Test
    void validateAllowedOrigins_rejectsWildcardOrigin() {
        WebSocketConfig config = new WebSocketConfig(jwtStompChannelInterceptor());
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("*"));

        assertThatThrownBy(config::validateAllowedOrigins)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not contain '*'");
    }

    @Test
    void validateAllowedOrigins_acceptsExplicitOrigins() {
        WebSocketConfig config = new WebSocketConfig(jwtStompChannelInterceptor());
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("http://localhost:3000"));

        assertThatCode(config::validateAllowedOrigins).doesNotThrowAnyException();
    }

    private JwtStompChannelInterceptor jwtStompChannelInterceptor() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("unit-test-secret-key-must-be-at-least-32-chars-long");
        return new JwtStompChannelInterceptor(new JwtTokenProvider(jwtConfig), new TokenBlacklistService(null) {
            @Override
            public boolean isBlacklisted(String token) {
                return false;
            }
        });
    }
}