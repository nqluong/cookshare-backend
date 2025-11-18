package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
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

    // Tìm thông báo theo relatedId và type
    @Query("SELECT n FROM Notification n WHERE n.relatedId = :relatedId AND n.type = :type")
    List<Notification> findByRelatedIdAndType(
            @Param("relatedId") UUID relatedId,
            @Param("type") NotificationType type
    );

    // Tìm thông báo theo userId, relatedId và type (dùng cho unlike, unfollow)
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.relatedId = :relatedId AND n.type = :type")
    List<Notification> findByUserIdAndRelatedIdAndType(
            @Param("userId") UUID userId,
            @Param("relatedId") UUID relatedId,
            @Param("type") NotificationType type
    );

    // Xóa tất cả thông báo liên quan đến một comment
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.relatedId = :commentId AND n.type = :type")
    void deleteByRelatedIdAndType(
            @Param("commentId") UUID commentId,
            @Param("type") NotificationType type
    );

    // Xóa thông báo theo danh sách commentIds (dùng cho cascade delete replies)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.relatedId IN :commentIds AND n.type = :type")
    void deleteByRelatedIdInAndType(
            @Param("commentIds") List<UUID> commentIds,
            @Param("type") NotificationType type
    );

    // Tìm thông báo theo relatedId và nhiều types (dùng cho xóa recipe)
    @Query("SELECT n FROM Notification n WHERE n.relatedId = :relatedId AND n.type IN :types")
    List<Notification> findByRelatedIdAndTypes(
            @Param("relatedId") UUID relatedId,
            @Param("types") List<NotificationType> types
    );
}
