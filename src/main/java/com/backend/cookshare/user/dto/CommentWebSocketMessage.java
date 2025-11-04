package com.backend.cookshare.user.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Message cho WebSocket Comment
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentWebSocketMessage {
    private String action; // CREATE, UPDATE, DELETE
    private CommentResponse comment;
    private UUID recipeId;
    private LocalDateTime timestamp;
}
