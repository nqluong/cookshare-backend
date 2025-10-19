package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ==================== READ QUERIES ====================

    /**
     * Lấy danh sách thông báo của user (mới nhất trước)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Đếm số thông báo chưa đọc của user
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Đếm tất cả thông báo của user
     */
    long countByUserId(UUID userId);

    /**
     * Lấy danh sách thông báo theo kiểu (FOLLOW, COMMENT, SHARE, ...)
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            UUID userId,
            NotificationType type,
            Pageable pageable
    );

    /**
     * Lấy thông báo chưa đọc theo kiểu
     */
    List<Notification> findByUserIdAndTypeAndIsReadFalse(UUID userId, NotificationType type);

    /**
     * Kiểm tra thông báo có thuộc về user hay không
     */
    @Query("""
        SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
        FROM Notification n
        WHERE n.notificationId = :notificationId AND n.userId = :userId
    """)
    boolean existsByIdAndUserId(@Param("notificationId") UUID notificationId, @Param("userId") UUID userId);

    /**
     * Lấy 10 thông báo gần nhất của user
     */
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Lấy tất cả thông báo liên quan đến một đối tượng (recipe, user, comment, ...)
     */
    List<Notification> findByRelatedIdOrderByCreatedAtDesc(UUID relatedId);

    // ==================== UPDATE QUERIES ====================

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     * @return số bản ghi được cập nhật
     */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.userId = :userId AND n.isRead = false
    """)
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    /**
     * Đánh dấu tất cả thông báo của một kiểu là đã đọc
     */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP
        WHERE n.userId = :userId AND n.type = :type AND n.isRead = false
    """)
    int markAllAsReadByUserIdAndType(@Param("userId") UUID userId, @Param("type") NotificationType type);

    /**
     * Cập nhật trạng thái đã gửi (isSent)
     */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isSent = :isSent
        WHERE n.notificationId = :notificationId
    """)
    void updateSentStatus(@Param("notificationId") UUID notificationId, @Param("isSent") Boolean isSent);

    // ==================== DELETE QUERIES ====================

    /**
     * Xóa tất cả thông báo của user
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Xóa tất cả thông báo đã đọc của user
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.isRead = true")
    int deleteReadNotificationsByUserId(@Param("userId") UUID userId);

    /**
     * Xóa thông báo cũ hơn N ngày
     * 👉 Dùng LocalDateTime để tránh lỗi JPQL khi dùng DATE_SUB
     */
    @Modifying
    @Query("""
        DELETE FROM Notification n
        WHERE n.userId = :userId AND n.createdAt < :limitDate
    """)
    int deleteOldNotificationsByUserId(@Param("userId") UUID userId,
                                       @Param("limitDate") LocalDateTime limitDate);

    /**
     * Xóa tất cả thông báo liên quan đến một đối tượng (recipe, user, comment, ...)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.relatedId = :relatedId")
    int deleteByRelatedId(@Param("relatedId") UUID relatedId);
}
