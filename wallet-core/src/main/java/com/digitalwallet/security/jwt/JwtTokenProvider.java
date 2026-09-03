package com.digitalwallet.security.jwt;

import com.digitalwallet.config.JwtConfig;
import com.digitalwallet.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getValidatedSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = jwtConfig.getExpirationMs();
        this.refreshTokenExpiryMs = jwtConfig.getRefreshExpirationMs();
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenExpiryMs, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenExpiryMs, "refresh");
    }

    public boolean validateToken(String token) {
        return isValid(token);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return parseClaims(token);
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String getTokenType(String token) {
        return parseClaims(token).get("tokenType", String.class);
    }

    public Integer getTokenVersion(String token) {
        return parseClaims(token).get("tokenVersion", Integer.class);
    }

    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant getExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(User user, long expiryMs, String tokenType) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expiryMs);

        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("public_id", user.getPublicId().toString())
                .claim("role", user.getRole().name())
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if ("refresh".equals(tokenType)) {
            builder
                    .id(UUID.randomUUID().toString())
                    .claim("tokenVersion", user.getTokenVersion());
        }

        return builder.signWith(secretKey).compact();
    }
}
