package com.digitalwallet.security.websocket;

import com.digitalwallet.config.JwtConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.AuthenticationException;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtStompChannelInterceptorTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-chars-long";

    private final JwtTokenProvider jwtTokenProvider = jwtTokenProvider(60 * 60 * 1000L);
    private final TokenBlacklistService tokenBlacklistService = new TokenBlacklistService(null) {
        @Override
        public boolean isBlacklisted(String token) {
            return false;
        }
    };
    private final JwtStompChannelInterceptor interceptor = new JwtStompChannelInterceptor(
            jwtTokenProvider, tokenBlacklistService);

    @Test
    void preSend_validConnectToken_setsPrincipalFromPublicId() {
        User user = user();
        String token = jwtTokenProvider.generateAccessToken(user);
        Message<?> message = interceptor.preSend(connectMessage("Bearer " + token), null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(message);
        Principal principal = resultAccessor.getUser();
        assertThat(principal).isInstanceOf(StompPrincipal.class);
        assertThat(principal.getName()).isEqualTo(user.getPublicId().toString());
    }

    @Test
    void preSend_missingToken_rejectsConnection() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), null))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void preSend_invalidToken_rejectsConnection() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer not-a-jwt"), null))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void preSend_expiredToken_rejectsConnection() {
        JwtTokenProvider expiredTokenProvider = jwtTokenProvider(-1000L);
        JwtStompChannelInterceptor expiredInterceptor = new JwtStompChannelInterceptor(
                expiredTokenProvider, tokenBlacklistService);
        String token = expiredTokenProvider.generateAccessToken(user());

        assertThatThrownBy(() -> expiredInterceptor.preSend(connectMessage("Bearer " + token), null))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void preSend_nonConnectFrame_doesNotAuthenticate() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    private Message<?> connectMessage(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private JwtTokenProvider jwtTokenProvider(long accessExpiryMs) {
        JwtConfig config = new JwtConfig();
        config.setSecret(SECRET);
        config.setExpirationMs(accessExpiryMs);
        config.setRefreshExpirationMs(7L * 24 * 60 * 60 * 1000);
        return new JwtTokenProvider(config);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPublicId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Nguyen Van A");
        user.setRole(UserRole.USER);
        return user;
    }
}