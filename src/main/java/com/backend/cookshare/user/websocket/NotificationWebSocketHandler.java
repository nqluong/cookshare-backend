package com.backend.cookshare.user.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    /**
     * Map lưu connection của user: userId -> List<WebSocketSession>
     * Một user có thể kết nối từ nhiều thiết bị
     */
    private static final Map<String, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserIdFromQuery(session);

        if (userId != null) {
            userSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(session);
            log.info("✅ User {} connected via WebSocket. Total sessions: {}", userId, userSessions.get(userId).size());

            // Gửi tin nhắn chào mừng
            sendToSession(session, createMessage("success", "Đã kết nối thành công"));
        } else {
            log.warn("❌ Cannot extract userId from WebSocket session");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received message from {}: {}", session.getId(), payload);

            // Parse message để xử lý ping/pong hoặc custom commands
            Map<String, String> data = objectMapper.readValue(payload, Map.class);
            String type = data.getOrDefault("type", "");

            switch (type) {
                case "ping":
                    sendToSession(session, createMessage("pong", "Server is alive"));
                    break;
                case "mark-as-read":
                    log.info("User marked notifications as read");
                    break;
                default:
                    log.debug("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserIdFromSession(session);

        if (userId != null) {
            List<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    log.info("❌ User {} disconnected. No active sessions", userId);
                } else {
                    log.info("⏸️ User {} closed one session. Remaining: {}", userId, sessions.size());
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Gửi notification realtime tới user (một device)
     */
    public static void sendNotificationToUser(String userId, Map<String, Object> notification) {
        List<WebSocketSession> sessions = userSessions.get(userId);

        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        String message = new ObjectMapper().writeValueAsString(
                                Map.of(
                                        "type", "notification",
                                        "data", notification,
                                        "timestamp", System.currentTimeMillis()
                                )
                        );
                        session.sendMessage(new TextMessage(message));
                        log.info("📤 Sent notification to user {} via WebSocket", userId);
                    } catch (IOException e) {
                        log.error("Failed to send notification to user {}", userId, e);
                    }
                }
            }
        } else {
            log.debug("ℹ️ User {} is not connected via WebSocket", userId);
        }
    }

    /**
     * Gửi notification tới tất cả user online
     */
    public static void broadcastNotification(Map<String, Object> notification) {
        for (List<WebSocketSession> sessions : userSessions.values()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        String message = new ObjectMapper().writeValueAsString(
                                Map.of(
                                        "type", "broadcast",
                                        "data", notification,
                                        "timestamp", System.currentTimeMillis()
                                )
                        );
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Failed to broadcast notification", e);
                    }
                }
            }
        }
    }

    /**
     * Gửi update unread count tới user
     */
    public static void sendUnreadCountUpdate(String userId, long unreadCount) {
        Map<String, Object> update = Map.of(
                "type", "unread-count",
                "unreadCount", unreadCount,
                "timestamp", System.currentTimeMillis()
        );

        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(update)));
                        log.info("📊 Sent unread count update to user {}", userId);
                    } catch (IOException e) {
                        log.error("Failed to send unread count", e);
                    }
                }
            }
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Extract userId từ WebSocket URI query parameter
     * ws://localhost:8080/ws/notifications?userId=550e8400-e29b-41d4-a716-446655440000
     */
    private String extractUserIdFromQuery(WebSocketSession session) {
        String uri = session.getUri().toString();
        String query = session.getUri().getQuery();

        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }

        return null;
    }

    /**
     * Extract userId từ session (từ attributes)
     */
    private String extractUserIdFromSession(WebSocketSession session) {
        for (Map.Entry<String, List<WebSocketSession>> entry : userSessions.entrySet()) {
            if (entry.getValue().contains(session)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gửi message tới một session
     */
    private void sendToSession(WebSocketSession session, Map<String, Object> message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(message)));
        }
    }

    /**
     * Tạo message chuẩn
     */
    private Map<String, Object> createMessage(String status, String message) {
        return Map.of(
                "status", status,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Lấy số user online
     */
    public static int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * Lấy tất cả user online
     */
    public static Set<String> getOnlineUsers() {
        return new HashSet<>(userSessions.keySet());
    }
}
