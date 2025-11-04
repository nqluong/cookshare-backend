package com.backend.cookshare.user.websocket;

import com.backend.cookshare.authentication.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.security.oauth2.jwt.Jwt;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final SecurityUtil securityUtil;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> recipeRooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        String token = query != null && query.startsWith("token=") ? query.substring(6) : null;

        try {
            Jwt jwt = securityUtil.checkValidAccessToken(token);
            String username = jwt.getSubject();
            session.getAttributes().put("username", username);
            userSessions.put(username, session);
            log.info("✅ Connected user {}", username);
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Map<String, Object> payload = mapper.readValue(message.getPayload(), Map.class);
        if ("join".equals(payload.get("action"))) {
            String recipeId = payload.get("recipeId").toString();
            recipeRooms.computeIfAbsent(recipeId, k -> new HashSet<>()).add(session);
        }
    }

    public void sendToRoom(String recipeId, Object message) {
        recipeRooms.getOrDefault(recipeId, Set.of()).forEach(s -> {
            try {
                s.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
            } catch (Exception e) {
                log.error("Send to room error: {}", e.getMessage());
            }
        });
    }

    public void sendToUser(String username, Object message) {
        WebSocketSession session = userSessions.get(username);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
            } catch (Exception e) {
                log.error("Send to user error: {}", e.getMessage());
            }
        }
    }
}
