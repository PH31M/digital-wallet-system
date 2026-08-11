package com.digitalwallet.websocket;

import com.digitalwallet.config.JwtConfig;
import com.digitalwallet.config.WebSocketConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.security.websocket.JwtStompChannelInterceptor;
import com.digitalwallet.service.WebSocketEventPublisher;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = WebSocketNotificationIntegrationTest.TestApplication.class,
        properties = {
                "jwt.secret=unit-test-secret-key-must-be-at-least-32-chars-long",
                "jwt.expiration-ms=3600000",
                "jwt.refresh-expiration-ms=604800000",
                "app.websocket.allowed-origins=http://localhost:3000"
        })
class WebSocketNotificationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TestRestTemplate restTemplate;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    @AfterEach
    void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void websocketClient_receivesUserNotificationAfterRestTransactionSignal() throws Exception {
        User user = user();
        String token = jwtTokenProvider.generateAccessToken(user);
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        stompSession = stompClient.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<NotificationPayload> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NotificationPayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((NotificationPayload) payload);
            }
        });
        TimeUnit.MILLISECONDS.sleep(250);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/test/transactions",
                new TestTransactionRequest(user.getPublicId(), "TRANSFER_SENT", "TX-WS-1"),
                Void.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(messages.poll(5, TimeUnit.SECONDS))
                .isEqualTo(new NotificationPayload("TRANSFER_SENT", "TX-WS-1"));
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPublicId(UUID.randomUUID());
        user.setEmail("ws-user@example.com");
        user.setFullName("Nguyen Van A");
        user.setRole(UserRole.USER);
        return user;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
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
            WebSocketConfig.class,
            JwtConfig.class,
            JwtTokenProvider.class,
            JwtStompChannelInterceptor.class,
            WebSocketEventPublisher.class,
            TestTransactionController.class
    })
    static class TestApplication {
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

    @RestController
    static class TestTransactionController {

        private final WebSocketEventPublisher webSocketEventPublisher;

        TestTransactionController(WebSocketEventPublisher webSocketEventPublisher) {
            this.webSocketEventPublisher = webSocketEventPublisher;
        }

        @PostMapping("/test/transactions")
        ResponseEntity<Void> createTransaction(@RequestBody TestTransactionRequest request) {
            webSocketEventPublisher.publishToUser(
                    request.userPublicId(),
                    "/queue/notifications",
                    new NotificationPayload(request.type(), request.referenceNumber()));
            return ResponseEntity.accepted().build();
        }
    }

    private record TestTransactionRequest(UUID userPublicId, String type, String referenceNumber) {
    }

    private record NotificationPayload(String type, String referenceNumber) {
    }
}