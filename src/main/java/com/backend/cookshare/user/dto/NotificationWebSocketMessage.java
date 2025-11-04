package com.backend.cookshare.user.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Message cho WebSocket Notification
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWebSocketMessage {
    private String action; // NEW, READ, DELETE
    private NotificationDto notification;
    private UUID userId;
    private LocalDateTime timestamp;
}
