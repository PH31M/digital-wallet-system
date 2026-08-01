package com.digitalwallet.security.jwt;

import com.digitalwallet.config.JwtConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage for DWS-45: JwtTokenProvider token generation/parsing/validation,
 * including expiry and tampering edge cases the ticket calls out
 * (generateAccessToken, generateRefreshToken, parseToken, isValid, getEmail).
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-chars-long";

    private JwtTokenProvider provider(long accessExpiryMs, long refreshExpiryMs) {
        JwtConfig config = new JwtConfig();
        config.setSecret(SECRET);
        config.setExpirationMs(accessExpiryMs);
        config.setRefreshExpirationMs(refreshExpiryMs);
        return new JwtTokenProvider(config);
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Nguyen Van A");
        user.setRole(UserRole.USER);
        return user;
    }

    @Test
    void generateAccessToken_isValidAndTypedAsAccess() {
        JwtTokenProvider provider = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);
        User user = user("user@example.com");

        String token = provider.generateAccessToken(user);

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isTrue();
        assertThat(provider.getEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void generateRefreshToken_isValidButNotTypedAsAccess() {
        JwtTokenProvider provider = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);
        User user = user("user@example.com");

        String token = provider.generateRefreshToken(user);

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isFalse();
        assertThat(provider.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void token_containsUserIdAndRoleClaims() {
        JwtTokenProvider provider = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);
        User user = user("user@example.com");

        String token = provider.generateAccessToken(user);
        Claims claims = provider.parseClaims(token);

        assertThat(claims.get("userId", String.class)).isEqualTo(user.getId().toString());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getSubject()).isEqualTo("user@example.com");
    }

    @Test
    void isValid_returnsFalseForAlreadyExpiredToken() {
        // Negative expiry => token's "exp" claim is already in the past at creation time.
        JwtTokenProvider provider = provider(-1000L, 7L * 24 * 60 * 60 * 1000);
        User user = user("user@example.com");

        String token = provider.generateAccessToken(user);

        assertThat(provider.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForTamperedSignature() {
        JwtTokenProvider provider = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);
        User user = user("user@example.com");
        String token = provider.generateAccessToken(user);

        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(provider.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForGarbageInput() {
        JwtTokenProvider provider = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);

        assertThat(provider.isValid("not-a-jwt-at-all")).isFalse();
    }

    @Test
    void isValid_returnsFalseForTokenSignedWithDifferentSecret() {
        JwtTokenProvider providerA = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);
        JwtConfig otherConfig = new JwtConfig();
        otherConfig.setSecret("a-completely-different-secret-key-32-chars-min");
        otherConfig.setExpirationMs(60 * 60 * 1000L);
        otherConfig.setRefreshExpirationMs(7L * 24 * 60 * 60 * 1000);
        JwtTokenProvider providerB = new JwtTokenProvider(otherConfig);

        String tokenFromB = providerB.generateAccessToken(user("user@example.com"));

        assertThat(providerA.isValid(tokenFromB)).isFalse();
    }

    @Test
    void constructor_rejectsBlankSecret() {
        JwtConfig config = new JwtConfig();
        config.setSecret("   ");

        assertThatThrownBy(() -> new JwtTokenProvider(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret must be configured");
    }

    @Test
    void constructor_rejectsShortSecret() {
        JwtConfig config = new JwtConfig();
        config.setSecret("too-short");

        assertThatThrownBy(() -> new JwtTokenProvider(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void constructor_rejectsDefaultPlaceholderSecret() {
        JwtConfig config = new JwtConfig();
        config.setSecret("change-this-in-env-min-32-chars-long");

        assertThatThrownBy(() -> new JwtTokenProvider(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default placeholder");
    }

    @Test
    void accessAndRefreshTokensForSameUser_areDifferentStrings() {
        JwtTokenProvider provider = provider(60 * 60 * 1000L, 7L * 24 * 60 * 60 * 1000);
        User user = user("user@example.com");

        String access = provider.generateAccessToken(user);
        String refresh = provider.generateRefreshToken(user);

        assertThat(access).isNotEqualTo(refresh);
    }
}
