package com.digitalwallet.security.websocket;

import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PUBLIC_ID_CLAIM = "public_id";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtStompChannelInterceptor(JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractBearerToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token) || tokenBlacklistService.isBlacklisted(token)) {
            throw invalidToken();
        }

        Claims claims = jwtTokenProvider.extractClaims(token);
        if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw invalidToken();
        }

        Principal principal = new StompPrincipal(extractPublicId(claims));
        try {
            accessor.setUser(principal);
            return message;
        } catch (IllegalStateException ex) {
            StompHeaderAccessor authenticatedAccessor = StompHeaderAccessor.wrap(message);
            authenticatedAccessor.setUser(principal);
            authenticatedAccessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(message.getPayload(), authenticatedAccessor.getMessageHeaders());
        }
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private UUID extractPublicId(Claims claims) {
        try {
            return UUID.fromString(claims.get(PUBLIC_ID_CLAIM, String.class));
        } catch (RuntimeException ex) {
            throw invalidToken();
        }
    }

    private AuthenticationException invalidToken() {
        return new BadCredentialsException("Invalid WebSocket authentication token");
    }
}