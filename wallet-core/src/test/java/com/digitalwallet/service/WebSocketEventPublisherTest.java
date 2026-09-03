package com.digitalwallet.service;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketEventPublisherTest {

    @Test
    void publishToUser_sendsToPrincipalNameMatchingUserPublicId() {
        CapturingSimpMessagingTemplate messagingTemplate = new CapturingSimpMessagingTemplate();
        WebSocketEventPublisher publisher = new WebSocketEventPublisher(messagingTemplate);
        UUID userPublicId = UUID.randomUUID();
        Object payload = new TestPayload("TRANSFER_SENT");

        publisher.publishToUser(userPublicId, "/queue/notifications", payload);

        assertThat(messagingTemplate.user).isEqualTo(userPublicId.toString());
        assertThat(messagingTemplate.destination).isEqualTo("/queue/notifications");
        assertThat(messagingTemplate.payload).isSameAs(payload);
    }

    @Test
    void publishBroadcast_sendsToTopicPrefix() {
        CapturingSimpMessagingTemplate messagingTemplate = new CapturingSimpMessagingTemplate();
        WebSocketEventPublisher publisher = new WebSocketEventPublisher(messagingTemplate);
        Object payload = new TestPayload("MAINTENANCE");

        publisher.publishBroadcast("system", payload);

        assertThat(messagingTemplate.destination).isEqualTo("/topic/system");
        assertThat(messagingTemplate.payload).isSameAs(payload);
    }

    private static class CapturingSimpMessagingTemplate extends SimpMessagingTemplate {

        private String user;
        private String destination;
        private Object payload;

        CapturingSimpMessagingTemplate() {
            super(new NoopMessageChannel());
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload) {
            this.user = user;
            this.destination = destination;
            this.payload = payload;
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            this.destination = destination;
            this.payload = payload;
        }
    }

    private static class NoopMessageChannel implements MessageChannel {
        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    }

    private record TestPayload(String type) {
    }
}