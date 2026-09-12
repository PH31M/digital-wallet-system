package com.digitalwallet.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * JWT configuration properties.
 *
 * Map values from application.yml or application.properties under jwt.*.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private static final int MIN_SECRET_BYTES = 32;
    private static final Set<String> DISALLOWED_SECRETS = Set.of(
            "change-me",
            "change-this-in-env-min-32-chars-long");

    private String secret = "change-me";
    private long expirationMs = 3600000;
    private long refreshExpirationMs = 604800000;

    @PostConstruct
    public void validate() {
        getValidatedSecret();
    }

    public String getSecret() {
        return secret;
    }

    public String getValidatedSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        String normalizedSecret = secret.trim();
        if (DISALLOWED_SECRETS.contains(normalizedSecret)) {
            throw new IllegalStateException("JWT secret must not use the default placeholder value");
        }

        if (normalizedSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        }

        return normalizedSecret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public void setRefreshExpirationMs(long refreshExpirationMs) {
        this.refreshExpirationMs = refreshExpirationMs;
    }
}
