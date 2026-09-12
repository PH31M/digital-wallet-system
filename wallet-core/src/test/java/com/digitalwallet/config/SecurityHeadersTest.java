package com.digitalwallet.config;

import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DWS-148: xác nhận security headers (CSP, Referrer-Policy, X-Content-Type-Options,
 * X-Frame-Options) được filter chain gắn vào mọi response.
 *
 * Dùng lại pattern TestApplication tối giản của ActuatorSecurityTest (không JPA/Flyway/Redis
 * thật), chỉ gọi 1 endpoint public để request đi qua được filter chain.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SecurityHeadersTest.TestApplication.class,
        properties = {
                "jwt.secret=unit-test-secret-key-must-be-at-least-32-chars-long",
                "jwt.expiration-ms=3600000",
                "jwt.refresh-expiration-ms=604800000",
                "app.cors.allowed-origins=http://localhost:3000",
                "management.health.redis.enabled=false",
                "management.health.db.enabled=false",
                "management.health.mail.enabled=false"
        })
class SecurityHeadersTest {

    @org.springframework.beans.factory.annotation.Autowired
    private TestRestTemplate restTemplate;

    @Test
    void anyResponse_includesSecurityHeaders() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeaders().getFirst("Content-Security-Policy")).isNotBlank();
        assertThat(response.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisReactiveAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class,
            JwtConfig.class,
            JwtTokenProvider.class,
            SecurityErrorResponseWriter.class,
            CustomUserDetailsService.class
    })
    static class TestApplication {

        @Bean
        UserRepository userRepository() {
            UserRepository repository = mock(UserRepository.class);
            when(repository.findByEmail(eq("user@test.com")))
                    .thenReturn(Optional.of(user("user@test.com", UserRole.USER)));
            return repository;
        }

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            return new TokenBlacklistService(null) {
                @Override
                public boolean isBlacklisted(String token) {
                    return false;
                }
            };
        }

        private static User user(String email, UserRole role) {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setPublicId(UUID.randomUUID());
            user.setEmail(email);
            user.setFullName("Test User");
            user.setRole(role);
            return user;
        }
    }
}
