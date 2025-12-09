package com.backend.cookshare.system.service.notification.persistence;

import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    public void saveNotification(
            UUID userId,
            String title,
            String message,
            NotificationType type,
            UUID relatedId) {

        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(type)
                    .relatedId(relatedId)
                    .build();

            notificationRepository.save(notification);

            log.debug("Saved notification for user {}: {}", userId, title);
        } catch (Exception e) {
            log.error("Failed to save notification for user {}", userId, e);
        }
    }
}
