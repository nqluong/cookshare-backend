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
            log.debug("Đã gửi thông báo WebSocket tới người dùng: {}", username);
        } catch (Exception e) {
            log.error("Không thể gửi thông báo WebSocket tới người dùng {}", username, e);
        }
    }

    public void broadcastToUsers(List<String> usernames, NotificationMessage message) {
        usernames.forEach(username -> {
            try {
                webSocketHandler.sendToUser(username, message);
            } catch (Exception e) {
                log.error("Không thể gửi thông báo WebSocket tới người dùng {}", username, e);
            }
        });

        log.info("Đã phát thông báo WebSocket tới {} người dùng", usernames.size());
    }
}
