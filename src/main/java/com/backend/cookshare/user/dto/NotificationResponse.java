package com.backend.cookshare.user.dto;

import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.enums.RelatedType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    UUID notificationId;
    String title;
    String message;
    NotificationType type;
    UUID relatedId;
    RelatedType relatedType;
    Boolean isRead;
    LocalDateTime createdAt;
    LocalDateTime readAt;
    UUID actorId;
    String actorName;
    String actorAvatar;
    UUID recipeId;
    String recipeTitle;
    String recipeImage;
}