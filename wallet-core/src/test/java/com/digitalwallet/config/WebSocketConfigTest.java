package com.digitalwallet.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSocketConfigTest {

    @Test
    void validateAllowedOrigins_rejectsWildcardOrigin() {
        WebSocketConfig config = new WebSocketConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("*"));

        assertThatThrownBy(config::validateAllowedOrigins)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not contain '*'");
    }

    @Test
    void validateAllowedOrigins_acceptsExplicitOrigins() {
        WebSocketConfig config = new WebSocketConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("http://localhost:3000"));

        assertThatCode(config::validateAllowedOrigins).doesNotThrowAnyException();
    }
}