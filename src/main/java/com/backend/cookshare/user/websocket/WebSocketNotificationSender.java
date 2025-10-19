package com.backend.cookshare.user.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WebSocketNotificationSender {

    public void sendFollowNotification(UUID followerId, UUID followingId, String followerName) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("type", "FOLLOW");
        notificationData.put("title", "Người theo dõi mới");
        notificationData.put("message", followerName + " vừa theo dõi bạn");
        notificationData.put("relatedId", followerId);
        notificationData.put("relatedType", "user");
        notificationData.put("timestamp", System.currentTimeMillis());

        NotificationWebSocketHandler.sendNotificationToUser(
                followingId.toString(),
                notificationData
        );

        log.info("📢 Sent follow notification to {}", followingId);
    }

    public void sendCommentNotification(UUID authorId, UUID recipeId, String commentContent) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("type", "COMMENT");
        notificationData.put("title", "Bình luận mới");
        notificationData.put("message", "Ai đó vừa bình luận: " + commentContent);
        notificationData.put("relatedId", recipeId);
        notificationData.put("relatedType", "recipe");
        notificationData.put("timestamp", System.currentTimeMillis());

        NotificationWebSocketHandler.sendNotificationToUser(
                authorId.toString(),
                notificationData
        );

        log.info("💬 Sent comment notification to {}", authorId);
    }

    public void sendShareNotification(UUID recipeOwnerId, UUID recipeId, String sharerName, String collectionName, String recipeTitle) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("type", "SHARE");
        notificationData.put("title", "Công thức được chia sẻ");
        notificationData.put("message",
                sharerName + " đã thêm công thức \"" + recipeTitle + "\" của bạn vào bộ sưu tập \"" + collectionName + "\"");
        notificationData.put("relatedId", recipeId);
        notificationData.put("relatedType", "recipe");
        notificationData.put("timestamp", System.currentTimeMillis());

        NotificationWebSocketHandler.sendNotificationToUser(
                recipeOwnerId.toString(),
                notificationData
        );

        log.info("📢 Sent share notification to {}", recipeOwnerId);
    }


    // ... thêm các loại notification khác
}
