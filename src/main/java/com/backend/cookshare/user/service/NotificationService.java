package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.NotificationDto;
import com.backend.cookshare.user.dto.NotificationResponse;
import com.backend.cookshare.user.dto.NotificationWebSocketMessage;
import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.enums.RelatedType;
import com.backend.cookshare.user.repository.CommentRepository;
import com.backend.cookshare.user.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final CommentRepository commentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ActivityLogService activityLogService;
    private final FirebaseStorageService fileStorageService;

    public Page<NotificationResponse> getUserNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::convertToResponse);
    }

    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

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

            // ✅ Gửi đầy đủ thông tin cho READ
            sendNotificationWebSocketMessage("READ", notification, userId);
        }

        return convertToResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);

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

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        notificationRepository.delete(notification);

        // ✅ Dùng method DELETE riêng
        sendDeleteNotificationWebSocketMessage(notificationId, userId);
    }

    @Transactional
    public void createCommentNotification(UUID recipientId, UUID actorId, UUID commentId, UUID recipeId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

        String title ="Bình luận mới";
        String message = actor.getFullName() + " đã bình luận về công thức của bạn";

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
        activityLogService.logCommentActivity(actorId, commentId, recipeId, "CREATE");

        // ✅ Gửi đầy đủ thông tin cho NEW
        sendNotificationWebSocketMessage("NEW", notification, recipientId);
    }

    @Transactional
    public void createCommentReplyNotification(UUID recipientId, UUID actorId, UUID commentId, UUID recipeId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

        String title ="Trả lời bình luận";
        String message = actor.getFullName() + " đã trả lời bình luận của bạn";

        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(NotificationType.MENTION)
                .title(title)
                .message(message)
                .relatedId(commentId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);
        activityLogService.logCommentActivity(actorId, commentId, recipeId, "CREATE");

        // ✅ Gửi đầy đủ thông tin cho NEW
        sendNotificationWebSocketMessage("NEW", notification, recipientId);
    }

    @Transactional
    public void deleteCommentNotifications(UUID commentId, UUID actorId) {
        List<Notification> notifications = notificationRepository.findByRelatedIdAndType(
                commentId,
                NotificationType.COMMENT
        );

        for (Notification notification : notifications) {
            notificationRepository.delete(notification);

            // ✅ Dùng method DELETE riêng
            sendDeleteNotificationWebSocketMessage(
                    notification.getNotificationId(),
                    notification.getUserId()
            );
        }

        activityLogService.logCommentActivity(actorId, commentId, null, "DELETE");
    }

    @Transactional
    public void deleteReplyNotifications(List<UUID> replyCommentIds, UUID actorId) {
        for (UUID replyId : replyCommentIds) {
            deleteCommentNotifications(replyId, actorId);
        }
    }

    @Transactional
    public void createLikeNotification(UUID recipientId, UUID actorId, UUID recipeId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

        String title = "Yêu thích mới";
        String message = actorId + "::" + actor.getFullName() + " đã thích công thức của bạn";

        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(NotificationType.LIKE)
                .title(title)
                .message(message)
                .relatedId(recipeId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);
        activityLogService.logLikeActivity(actorId, recipeId, "CREATE");

        // ✅ Gửi đầy đủ thông tin cho NEW
        sendNotificationWebSocketMessage("NEW", notification, recipientId);
    }

    @Transactional
    public void deleteLikeNotification(UUID recipientId, UUID actorId, UUID recipeId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndRelatedIdAndType(
                recipientId,
                recipeId,
                NotificationType.LIKE
        );

        for (Notification notification : notifications) {
            notificationRepository.delete(notification);

            // ✅ Dùng method DELETE riêng
            sendDeleteNotificationWebSocketMessage(
                    notification.getNotificationId(),
                    notification.getUserId()
            );
        }

        activityLogService.logLikeActivity(actorId, recipeId, "DELETE");
    }

    @Transactional
    public void createFollowNotification(UUID recipientId, UUID actorId) {
        if (recipientId.equals(actorId)) {
            return;
        }

        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

        String title = "Người theo dõi mới";
        String message = actor.getFullName() + " đã bắt đầu theo dõi bạn";

        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(NotificationType.FOLLOW)
                .title(title)
                .message(message)
                .relatedId(actorId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);
        activityLogService.logFollowActivity(actorId, recipientId, "CREATE");

        // ✅ Gửi đầy đủ thông tin cho NEW
        sendNotificationWebSocketMessage("NEW", notification, recipientId);
    }

    @Transactional
    public void deleteFollowNotification(UUID recipientId, UUID actorId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndRelatedIdAndType(
                recipientId,
                actorId,
                NotificationType.FOLLOW
        );

        for (Notification notification : notifications) {
            notificationRepository.delete(notification);

            // ✅ Dùng method DELETE riêng
            sendDeleteNotificationWebSocketMessage(
                    notification.getNotificationId(),
                    notification.getUserId()
            );
        }

        activityLogService.logFollowActivity(actorId, recipientId, "DELETE");
    }

    @Transactional
    public void createRecipeApprovedNotification(UUID recipeOwnerId, UUID recipeId, String recipeTitle) {
        String title = "Công thức được duyệt";
        String message = "Công thức \"" + recipeTitle + "\" của bạn đã được phê duyệt và xuất bản";

        Notification notification = Notification.builder()
                .userId(recipeOwnerId)
                .type(NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .relatedId(recipeId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(false)
                .build();

        notification = notificationRepository.save(notification);

        // ✅ Gửi đầy đủ thông tin cho NEW
        sendNotificationWebSocketMessage("NEW", notification, recipeOwnerId);
    }

    @Transactional
    public void createNewRecipeNotificationForFollowers(
            List<UUID> followerIds,
            UUID recipeOwnerId,
            String ownerName,
            UUID recipeId,
            String recipeTitle) {

        for (UUID followerId : followerIds) {
            if (followerId.equals(recipeOwnerId)) {
                continue;
            }

            String title = "Công thức mới";
            String message = ownerName + " vừa đăng công thức mới: \"" + recipeTitle + "\"";

            Notification notification = Notification.builder()
                    .userId(followerId)
                    .type(NotificationType.RECIPE_PUBLISHED)
                    .title(title)
                    .message(message)
                    .relatedId(recipeId)
                    .relatedType(RelatedType.user)
                    .isRead(false)
                    .isSent(false)
                    .build();

            notification = notificationRepository.save(notification);

            // ✅ Gửi đầy đủ thông tin cho NEW
            sendNotificationWebSocketMessage("NEW", notification, followerId);
        }
    }

    @Transactional
    public void deleteRecipeNotifications(UUID recipeId) {
        List<Notification> notifications = notificationRepository.findByRelatedIdAndTypes(
                recipeId,
                List.of(
                        NotificationType.SYSTEM,
                        NotificationType.RECIPE_PUBLISHED,
                        NotificationType.COMMENT,
                        NotificationType.LIKE
                )
        );

        for (Notification notification : notifications) {
            notificationRepository.delete(notification);

            // ✅ Dùng method DELETE riêng
            sendDeleteNotificationWebSocketMessage(
                    notification.getNotificationId(),
                    notification.getUserId()
            );
        }

        log.info("Deleted {} notifications related to recipe {}", notifications.size(), recipeId);
    }

    /**
     * Convert Notification entity sang NotificationResponse với đầy đủ thông tin
     * Lấy thông tin actor và recipe dựa vào type và relatedId
     */
    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse.NotificationResponseBuilder builder = NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt());

        // Xử lý dựa vào loại notification
        switch (notification.getType()) {
            case MENTION:
            case COMMENT:
                enrichCommentNotification(notification, builder);
                break;
            case LIKE:
                enrichLikeNotification(notification, builder);
                break;
            case FOLLOW:
                enrichFollowNotification(notification, builder);
                break;
            case RECIPE_PUBLISHED:
            case SYSTEM:
                enrichRecipeNotification(notification, builder);
                break;
        }

        return builder.build();
    }

    /**
     * Bổ sung thông tin cho notification COMMENT
     * relatedId = commentId
     */
    private void enrichCommentNotification(Notification notification, NotificationResponse.NotificationResponseBuilder builder) {
        if (notification.getRelatedId() == null) return;

        commentRepository.findById(notification.getRelatedId()).ifPresent(comment -> {
            // Lấy thông tin recipe từ comment
            builder.recipeId(comment.getRecipeId());
            recipeRepository.findById(comment.getRecipeId()).ifPresent(recipe -> {
                builder.recipeTitle(recipe.getTitle())
                        .recipeImage(fileStorageService.convertPathToFirebaseUrl(recipe.getFeaturedImage()));
            });

            // Lấy thông tin người comment
            userRepository.findById(comment.getUserId()).ifPresent(actor -> {
                builder.actorId(actor.getUserId())
                        .actorName(actor.getFullName())
                        .actorAvatar(fileStorageService.convertPathToFirebaseUrl(actor.getAvatarUrl()));
            });
        });
    }

    /**
     * Bổ sung thông tin cho notification LIKE
     * relatedId = recipeId
     */
    private void enrichLikeNotification(Notification notification, NotificationResponse.NotificationResponseBuilder builder) {
        if (notification.getRelatedId() == null) return;

        builder.recipeId(notification.getRelatedId());

        recipeRepository.findById(notification.getRelatedId()).ifPresent(recipe -> {
            builder.recipeTitle(recipe.getTitle())
                    .recipeImage(fileStorageService.convertPathToFirebaseUrl(recipe.getFeaturedImage()));
        });

        try {
            String[] parts = notification.getMessage().split("::", 2);

            if (parts.length == 2) {
                UUID actorId = UUID.fromString(parts[0]);
                String realText = parts[1];

                // Set lại message đẹp
                builder.message(realText);

                // Load actor
                userRepository.findById(actorId).ifPresent(actor -> {
                    builder.actorId(actor.getUserId())
                            .actorName(actor.getFullName())
                            .actorAvatar(
                                    actor.getAvatarUrl() != null
                                            ? fileStorageService.convertPathToFirebaseUrl(actor.getAvatarUrl())
                                            : null
                            );
                });
            } else {
                builder.message(notification.getMessage()); // fallback
            }

        } catch (Exception e) {
            builder.message(notification.getMessage()); // fallback
        }
    }

    /**
     * Bổ sung thông tin cho notification FOLLOW
     * relatedId = actorId (người theo dõi)
     */
    private void enrichFollowNotification(Notification notification, NotificationResponse.NotificationResponseBuilder builder) {
        if (notification.getRelatedId() == null) return;

        userRepository.findById(notification.getRelatedId()).ifPresent(actor -> {
            builder.actorId(actor.getUserId())
                    .actorName(actor.getFullName())
                    .actorAvatar(fileStorageService.convertPathToFirebaseUrl(actor.getAvatarUrl()));
        });
    }

    /**
     * Bổ sung thông tin cho notification RECIPE_PUBLISHED và SYSTEM
     * relatedId = recipeId
     */
    private void enrichRecipeNotification(Notification notification, NotificationResponse.NotificationResponseBuilder builder) {
        if (notification.getRelatedId() == null) return;

        builder.recipeId(notification.getRelatedId());

        recipeRepository.findById(notification.getRelatedId()).ifPresent(recipe -> {
            builder.recipeTitle(recipe.getTitle())
                    .recipeImage(fileStorageService.convertPathToFirebaseUrl(recipe.getFeaturedImage()));

            // Lấy thông tin tác giả recipe
            userRepository.findById(recipe.getUserId()).ifPresent(actor -> {
                builder.actorId(actor.getUserId())
                        .actorName(actor.getFullName())
                        .actorAvatar(fileStorageService.convertPathToFirebaseUrl(actor.getAvatarUrl()));
            });
        });
    }

    // ✅ Method cho NEW/READ - gửi đầy đủ thông tin
    private void sendNotificationWebSocketMessage(String action, Notification notification, UUID userId) {
        // Convert sang NotificationResponse để có đầy đủ thông tin
        NotificationResponse enrichedNotification = convertToResponse(notification);

        NotificationWebSocketMessage message = NotificationWebSocketMessage.builder()
                .action(action)
                .notification(enrichedNotification)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
        );
    }

    // ✅ Method riêng cho DELETE - chỉ cần notificationId
    private void sendDeleteNotificationWebSocketMessage(UUID notificationId, UUID userId) {
        NotificationDto dto = NotificationDto.builder()
                .notificationId(notificationId)
                .userId(userId)
                .build();

        NotificationWebSocketMessage message = NotificationWebSocketMessage.builder()
                .action("DELETE")
                .notification(dto)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
        );
    }
}