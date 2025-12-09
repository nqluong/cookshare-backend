package com.backend.cookshare.system.service.notification.sender;

import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.user.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationSender {
    private final NotificationWebSocketHandler webSocketHandler;

    public void sendToUser(String username, NotificationMessage message) {
        try {
            webSocketHandler.sendToUser(username, message);
            log.debug("Sent WebSocket notification to user: {}", username);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}", username, e);
        }
    }

    public void broadcastToUsers(List<String> usernames, NotificationMessage message) {
        usernames.forEach(username -> {
            try {
                webSocketHandler.sendToUser(username, message);
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification to user {}", username, e);
            }
        });

        log.info("Broadcasted WebSocket notification to {} users", usernames.size());
    }
}
