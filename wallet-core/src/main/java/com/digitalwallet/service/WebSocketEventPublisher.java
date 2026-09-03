package com.digitalwallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WebSocketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    private final SimpMessagingTemplate simpMessagingTemplate;

    public WebSocketEventPublisher(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void publishToUser(UUID userPublicId, String destination, Object payload) {
        try {
            simpMessagingTemplate.convertAndSendToUser(userPublicId.toString(), destination, payload);
        } catch (RuntimeException ex) {
            log.warn("Could not publish WebSocket event to user. destination={}", destination, ex);
        }
    }

    public void publishBroadcast(String topic, Object payload) {
        try {
            simpMessagingTemplate.convertAndSend("/topic/" + topic, payload);
        } catch (RuntimeException ex) {
            log.warn("Could not publish broadcast WebSocket event. topic={}", topic, ex);
        }
    }
}