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
public class NotificationDto {
    UUID notificationId;
    UUID userId;
    NotificationType type;
    String title;
    String message;
    UUID relatedId;
    RelatedType relatedType;
    Boolean isRead;
    LocalDateTime createdAt;
    LocalDateTime readAt;
}