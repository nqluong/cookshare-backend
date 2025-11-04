package com.backend.cookshare.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    /**
     * Handle ping from clients to keep connection alive
     * Client sends to: /app/ping
     */
    @MessageMapping("/ping")
    public void handlePing(@Payload Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Long timestamp = ((Number) payload.get("timestamp")).longValue();

        log.debug("💓 Received ping from user: {} at {}", userId, timestamp);

        // No need to send response, just log
        // The heartbeat mechanism handles keep-alive
    }
}