package com.backend.cookshare.user.dto;

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
    Boolean isRead;
    LocalDateTime createdAt;
    String message_;
}