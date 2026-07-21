package com.digitalwallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 *
 * Map values from application.yml or application.properties under jwt.*.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret = "change-me";
    private long expirationMs = 3600000;
    private long refreshExpirationMs = 604800000;

    public String getSecret() {
        return secret;
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
