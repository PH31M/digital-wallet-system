package com.digitalwallet.config;

import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test cho DWS-228: xác nhận /actuator/health public (permitAll)
 * và /actuator/** (khác health/prometheus) yêu cầu role ADMIN, đúng thiết kế DWS-225.
 *
 * Dùng một SpringBootConfiguration tối giản (không JPA/Flyway/Redis thật) tương tự
 * WebSocketNotificationIntegrationTest, nhưng KHÔNG loại trừ SecurityAutoConfiguration
 * vì đây chính là phần cần test.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ActuatorSecurityTest.TestApplication.class,
        properties = {
                "jwt.secret=unit-test-secret-key-must-be-at-least-32-chars-long",
                "jwt.expiration-ms=3600000",
                "jwt.refresh-expiration-ms=604800000",
                "app.cors.allowed-origins=http://localhost:3000",
                "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
                "management.endpoint.health.show-details=when-authorized"
        })
class ActuatorSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void health_withoutAuth_returns200Up() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void health_withUserJwt_returns200Up() {
        String token = jwtTokenProvider.generateAccessToken(user("user@test.com", UserRole.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/health", HttpMethod.GET, authorizedRequest(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void metrics_withoutAuth_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void metrics_withUserJwt_returns403() {
        String token = jwtTokenProvider.generateAccessToken(user("user@test.com", UserRole.USER));

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/metrics", HttpMethod.GET, authorizedRequest(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void metrics_withAdminJwt_returns200() {
        String token = jwtTokenProvider.generateAccessToken(user("admin@test.com", UserRole.ADMIN));

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/metrics", HttpMethod.GET, authorizedRequest(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpEntity<Void> authorizedRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(headers);
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
            when(repository.findByEmail(eq("admin@test.com")))
                    .thenReturn(Optional.of(user("admin@test.com", UserRole.ADMIN)));
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
    }
}
