package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.user.dto.NotificationDto;
import com.backend.cookshare.user.entity.Comment;
import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.enums.RelatedType;
import com.backend.cookshare.user.repository.CommentRepository;
import com.backend.cookshare.user.repository.NotificationRepository;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final CommentRepository commentRepository;

    // ==================== CREATE NOTIFICATIONS ====================

    /**
     * Tạo thông báo FOLLOW
     * Khi: User A theo dõi User B
     * Message: "Tên A bắt đầu theo dõi bạn"
     */
    @Transactional
    public void createFollowNotification(UUID followerId, UUID followingId) {
        try {
            log.info("Creating FOLLOW notification from {} to {}", followerId, followingId);

            User follower = userRepository.findById(followerId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Kiểm tra user đó có tồn tại không
            userRepository.findById(followingId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String message = follower.getUsername() + " bắt đầu theo dõi bạn";

            Notification notification = Notification.builder()
                    .userId(followingId)
                    .type(NotificationType.FOLLOW)
                    .title("Người theo dõi mới")
                    .message(message)
                    .relatedId(followerId)
                    .relatedType(RelatedType.user)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ FOLLOW notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating FOLLOW notification", e);
        }
    }

    /**
     * Tạo thông báo LIKE
     * Khi: User A thích Recipe của User B
     * Message: "Tên A thích công thức 'Tên công thức' của bạn"
     */
    @Transactional
    public void createLikeNotification(UUID recipeId, UUID likerId) {
        try {
            log.info("Creating LIKE notification for recipe {} from user {}", recipeId, likerId);

            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RECIPE_NOT_FOUND
                    ));

            User liker = userRepository.findById(likerId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Không gửi thông báo cho chính mình
            if (recipe.getUserId().equals(likerId)) {
                log.info("⏭️ Skipping: User liked their own recipe");
                return;
            }

            String message = liker.getUsername() + " thích công thức \"" + recipe.getTitle() + "\" của bạn";

            Notification notification = Notification.builder()
                    .userId(recipe.getUserId())
                    .type(NotificationType.LIKE)
                    .title("Công thức được thích")
                    .message(message)
                    .relatedId(recipeId)
                    .relatedType(RelatedType.record)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ LIKE notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating LIKE notification", e);
        }
    }

    /**
     * Tạo thông báo COMMENT
     * Khi: User A bình luận vào Recipe của User B
     * Message: "Tên A đã bình luận: 'Nội dung bình luận' vào công thức 'Tên công thức'"
     */
    @Transactional
    public void createCommentNotification(UUID recipeId, UUID commenterId, String commentContent) {
        try {
            log.info("Creating COMMENT notification for recipe {} from user {}", recipeId, commenterId);

            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RECIPE_NOT_FOUND
                    ));

            User commenter = userRepository.findById(commenterId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Không gửi thông báo cho chính mình
            if (recipe.getUserId().equals(commenterId)) {
                log.info("⏭️ Skipping: User commented on their own recipe");
                return;
            }

            String truncatedComment = commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent;
            String message = commenter.getUsername() + " đã bình luận: \"" + truncatedComment + "\" vào \"" + recipe.getTitle() + "\"";

            Notification notification = Notification.builder()
                    .userId(recipe.getUserId())
                    .type(NotificationType.COMMENT)
                    .title("Bình luận mới")
                    .message(message)
                    .relatedId(recipeId)
                    .relatedType(RelatedType.record)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ COMMENT notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating COMMENT notification", e);
        }
    }

    /**
     * Tạo thông báo RATING
     * Khi: User A đánh giá sao Recipe của User B
     * Message: "Ai đó đã đánh giá ⭐⭐⭐⭐⭐ cho 'Tên công thức' của bạn"
     */
    @Transactional
    public void createRatingNotification(UUID recipeId, UUID raterId, int rating) {
        try {
            log.info("Creating RATING notification for recipe {} - rating: {}", recipeId, rating);

            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RECIPE_NOT_FOUND
                    ));

            User rater = userRepository.findById(raterId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Không gửi thông báo cho chính mình
            if (recipe.getUserId().equals(raterId)) {
                log.info("⏭️ Skipping: User rated their own recipe");
                return;
            }

            String stars = "⭐".repeat(rating);
            String message = rater.getUsername() + " đã đánh giá " + stars + " cho \"" + recipe.getTitle() + "\" của bạn";

            Notification notification = Notification.builder()
                    .userId(recipe.getUserId())
                    .type(NotificationType.RATING)
                    .title("Công thức được đánh giá")
                    .message(message)
                    .relatedId(recipeId)
                    .relatedType(RelatedType.record)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ RATING notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating RATING notification", e);
        }
    }

    /**
     * Tạo thông báo MENTION
     * Khi: User A nhắc đến User B trong bình luận
     * Message: "Tên A đã nhắc đến bạn trong bình luận"
     */
    @Transactional
    public void createMentionNotification(UUID mentionedUserId, UUID mentionerId, UUID recipeId, String commentContent) {
        try {
            log.info("Creating MENTION notification for user {} by {}", mentionedUserId, mentionerId);

            User mentioner = userRepository.findById(mentionerId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            userRepository.findById(mentionedUserId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Không gửi thông báo cho chính mình
            if (mentionedUserId.equals(mentionerId)) {
                return;
            }

            String truncatedComment = commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent;
            String message = mentioner.getUsername() + " đã nhắc đến bạn: \"" + truncatedComment + "\"";

            Notification notification = Notification.builder()
                    .userId(mentionedUserId)
                    .type(NotificationType.MENTION)
                    .title("Bạn được nhắc đến")
                    .message(message)
                    .relatedId(recipeId)
                    .relatedType(RelatedType.record)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ MENTION notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating MENTION notification", e);
        }
    }

    /**
     * Tạo thông báo SHARE
     * Khi: Công thức của User A được chia sẻ hoặc thêm vào collection
     * Message: "Ai đó đã thêm 'Tên công thức' vào bộ sưu tập"
     */
    @Transactional
    public void createShareNotification(UUID recipeId, UUID sharerId, String collectionName) {
        try {
            log.info("Creating SHARE notification for recipe {} shared by {}", recipeId, sharerId);

            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RECIPE_NOT_FOUND
                    ));

            // Không gửi thông báo cho chính mình
            if (recipe.getUserId().equals(sharerId)) {
                log.info("⏭️ Skipping: User shared their own recipe");
                return;
            }

            String message = "Công thức \"" + recipe.getTitle() + "\" của bạn được thêm vào bộ sưu tập \"" + collectionName + "\"";

            Notification notification = Notification.builder()
                    .userId(recipe.getUserId())
                    .type(NotificationType.SHARE)
                    .title("Công thức được chia sẻ")
                    .message(message)
                    .relatedId(recipeId)
                    .relatedType(RelatedType.record)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ SHARE notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating SHARE notification", e);
        }
    }

    /**
     * Tạo thông báo RECIPE_PUBLISHED
     * Khi: User A đăng công thức mới (có thể gửi cho followers)
     * Message: "Tên A đã đăng công thức mới 'Tên công thức'"
     */
    @Transactional
    public void createRecipePublishedNotification(UUID recipeId, UUID publisherId, List<UUID> followerIds) {
        try {
            log.info("Creating RECIPE_PUBLISHED notification for recipe {} to {} followers", recipeId, followerIds.size());

            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RECIPE_NOT_FOUND
                    ));

            User publisher = userRepository.findById(publisherId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            String message = publisher.getUsername() + " đã đăng công thức mới \"" + recipe.getTitle() + "\"";

            List<Notification> notifications = followerIds.stream()
                    .map(followerId -> Notification.builder()
                            .userId(followerId)
                            .type(NotificationType.RECIPE_PUBLISHED)
                            .title("Công thức mới từ người bạn theo dõi")
                            .message(message)
                            .relatedId(recipeId)
                            .relatedType(RelatedType.record)
                            .isRead(false)
                            .isSent(true)
                            .build())
                    .collect(Collectors.toList());

            notificationRepository.saveAll(notifications);
            log.info("✅ RECIPE_PUBLISHED notification created for {} followers", notifications.size());
        } catch (Exception e) {
            log.error("❌ Error creating RECIPE_PUBLISHED notification", e);
        }
    }

    /**
     * Tạo thông báo SYSTEM
     * Dùng cho các thông báo hệ thống chung
     * Message: Tùy chỉnh
     */
    @Transactional
    public void createSystemNotification(UUID userId, String title, String message) {
        try {
            log.info("Creating SYSTEM notification for user {}", userId);

            userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(NotificationType.SYSTEM)
                    .title(title)
                    .message(message)
                    .isRead(false)
                    .isSent(true)
                    .build();

            notificationRepository.save(notification);
            log.info("✅ SYSTEM notification created successfully");
        } catch (Exception e) {
            log.error("❌ Error creating SYSTEM notification", e);
        }
    }

    // ==================== READ NOTIFICATIONS ====================

    /**
     * Lấy danh sách thông báo của user (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(UUID userId, Pageable pageable) {
        log.info("Getting notifications for user: {} - Page: {}", userId, pageable.getPageNumber());

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    /**
     * Lấy số lượng thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        log.info("Getting unread count for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        log.info("Unread count: {}", count);
        return count;
    }

    /**
     * Lấy chi tiết một thông báo
     */
    @Transactional(readOnly = true)
    public NotificationDto getNotificationDetail(UUID notificationId, UUID userId) {
        log.info("Getting notification detail: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOTIFICATION_NOT_FOUND
                ));

        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(
                    ErrorCode.NOTIFICATION_FORBIDDEN
            );
        }

        return mapToDto(notification);
    }

    // ==================== UPDATE NOTIFICATIONS ====================

    /**
     * Đánh dấu một thông báo là đã đọc
     */
    @Transactional
    public NotificationDto markAsRead(UUID notificationId, UUID userId) {
        log.info("Marking notification as read: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOTIFICATION_NOT_FOUND
                ));

        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(
                    ErrorCode.NOTIFICATION_FORBIDDEN
            );
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        Notification updated = notificationRepository.save(notification);

        log.info("✅ Notification marked as read");
        return mapToDto(updated);
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     */
    @Transactional
    public int markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.markAllAsReadByUserId(userId);
    }

    // ==================== DELETE NOTIFICATIONS ====================

    /**
     * Xóa một thông báo
     */
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        log.info("Deleting notification: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOTIFICATION_NOT_FOUND
                ));

        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(
                    ErrorCode.NOTIFICATION_FORBIDDEN
            );
        }

        notificationRepository.delete(notification);
        log.info("✅ Notification deleted");
    }

    /**
     * Xóa tất cả thông báo của user
     */
    @Transactional
    public int deleteAllNotifications(UUID userId) {
        log.info("Deleting all notifications for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.deleteByUserId(userId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Map Notification Entity to DTO
     */
    private NotificationDto mapToDto(Notification notification) {
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
}