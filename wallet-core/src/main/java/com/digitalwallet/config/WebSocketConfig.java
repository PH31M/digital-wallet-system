package com.digitalwallet.config;

import com.digitalwallet.security.websocket.JwtStompChannelInterceptor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.List;

/**
 * STOMP WebSocket configuration.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Value("${app.websocket.transport.message-size-limit:65536}")
    private int messageSizeLimit;

    @Value("${app.websocket.transport.send-buffer-size-limit:524288}")
    private int sendBufferSizeLimit;

    @Value("${app.websocket.transport.send-time-limit:15000}")
    private int sendTimeLimit;

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    public WebSocketConfig(JwtStompChannelInterceptor jwtStompChannelInterceptor) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
    }

    @PostConstruct
    void validateAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("At least one WebSocket allowed origin must be configured");
        }
        if (allowedOrigins.stream().map(String::trim).anyMatch(origin -> origin.isBlank() || "*".equals(origin))) {
            throw new IllegalStateException("WebSocket allowed origins must be explicit and must not contain '*'");
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.stream().map(String::trim).toArray(String[]::new));
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(messageSizeLimit);
        registry.setSendBufferSizeLimit(sendBufferSizeLimit);
        registry.setSendTimeLimit(sendTimeLimit);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}