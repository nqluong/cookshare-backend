package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // Lấy danh sách notification của user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Đếm số notification chưa đọc
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") UUID userId);

    // Đánh dấu tất cả notification là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId);

    // Lấy notification chưa gửi qua WebSocket
    @Query("SELECT n FROM Notification n WHERE n.isSent = false ORDER BY n.createdAt ASC")
    List<Notification> findUnsentNotifications();

    // Đánh dấu notification đã gửi
    @Modifying
    @Query("UPDATE Notification n SET n.isSent = true WHERE n.notificationId = :notificationId")
    void markAsSent(@Param("notificationId") UUID notificationId);
}
