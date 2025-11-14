package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.user.dto.NotificationDto;
import com.backend.cookshare.user.dto.NotificationResponse;
import com.backend.cookshare.user.dto.NotificationWebSocketMessage;
import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.enums.RelatedType;
import com.backend.cookshare.user.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Lấy danh sách notification của user
    public Page<NotificationResponse> getUserNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.map(this::convertToResponse);
    }

    // Đếm notification chưa đọc
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    // Đánh dấu một notification là đã đọc
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);

            // Send WebSocket message
            sendNotificationWebSocketMessage("READ", convertToDto(notification), userId);
        }

        return convertToResponse(notification);
    }

    // Đánh dấu tất cả notification là đã đọc
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);

        // Send WebSocket message
        NotificationWebSocketMessage message = NotificationWebSocketMessage.builder()
                .action("READ_ALL")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
        );
    }

    // Xóa notification
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        notificationRepository.delete(notification);

        // Send WebSocket message
        NotificationDto dto = NotificationDto.builder()
                .notificationId(notificationId)
                .userId(userId)
                .build();

        sendNotificationWebSocketMessage("DELETE", dto, userId);
    }

    // Tạo notification cho comment
    @Transactional
    public void createCommentNotification(UUID recipientId, UUID actorId, UUID commentId, UUID recipeId, boolean isReply) {
        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

        String title = isReply ? "Trả lời bình luận" : "Bình luận mới";
        String message = actor.getUsername() + (isReply ? " đã trả lời bình luận của bạn" : " đã bình luận về công thức của bạn");

        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(NotificationType.COMMENT)
                .title(title)
                .message(message)
                .relatedId(commentId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);

        // Send WebSocket immediately
        sendNotificationWebSocketMessage("NEW", convertToDto(notification), recipientId);
    }

    // Tạo notification cho like
    @Transactional
    public void createLikeNotification(UUID recipientId, UUID actorId, UUID recipeId) {
        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

        String title = "Yêu thích mới";
        String message = actor.getUsername() + " đã thích công thức của bạn";

        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(NotificationType.LIKE)
                .title(title)
                .message(message)
                .relatedId(recipeId)
                .relatedType(RelatedType.record)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);

        sendNotificationWebSocketMessage("NEW", convertToDto(notification), recipientId);
    }

    // Tạo notification cho follow
    @Transactional
    public void createFollowNotification(UUID recipientId, UUID actorId) {
        User actor = userRepository.findById(recipientId).orElse(null);
        if (actor == null) return;

        String title = "Người theo dõi mới";
        String message = actor.getFullName() + " đã bắt đầu theo dõi bạn";

        Notification notification = Notification.builder()
                .userId(actorId)
                .type(NotificationType.FOLLOW)
                .title(title)
                .message(message)
                .relatedId(recipientId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);

        sendNotificationWebSocketMessage("NEW", convertToDto(notification), actorId);
    }

    // Convert entity to DTO
    private NotificationDto convertToDto(Notification notification) {
        return NotificationDto.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // Send WebSocket message for notification
    private void sendNotificationWebSocketMessage(String action, NotificationDto notification, UUID userId) {
        NotificationWebSocketMessage message = NotificationWebSocketMessage.builder()
                .action(action)
                .notification(notification)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        // Send to specific user
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
        );
    }
}