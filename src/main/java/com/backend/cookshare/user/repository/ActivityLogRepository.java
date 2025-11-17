package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.ActivityLog;
import com.backend.cookshare.user.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    // Lấy activity logs của user
    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Lấy activity logs theo type
    Page<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType, Pageable pageable);

    // Lấy activity logs của user theo type
    Page<ActivityLog> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
            UUID userId,
            ActivityType activityType,
            Pageable pageable
    );

    // Đếm số lượng hoạt động theo type trong khoảng thời gian
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.activityType = :activityType " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    Long countByActivityTypeAndDateRange(
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Thống kê hoạt động của user theo ngày
    @Query("SELECT DATE(a.createdAt) as date, COUNT(a) as count " +
            "FROM ActivityLog a WHERE a.userId = :userId " +
            "AND a.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(a.createdAt) ORDER BY DATE(a.createdAt)")
    List<Object[]> getUserActivityStatsByDate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Thống kê recipe được xem nhiều nhất
    @Query("SELECT a.targetId, COUNT(a) as viewCount " +
            "FROM ActivityLog a WHERE a.activityType = :activityType " +
            "AND a.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY a.targetId ORDER BY viewCount DESC")
    List<Object[]> getMostViewedRecipes(
            @Param("activityType") ActivityType activityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Thống kê user hoạt động nhiều nhất
    @Query("SELECT a.userId, COUNT(a) as activityCount " +
            "FROM ActivityLog a WHERE a.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY a.userId ORDER BY activityCount DESC")
    List<Object[]> getMostActiveUsers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Xóa logs cũ hơn một khoảng thời gian (để cleanup)
    void deleteByCreatedAtBefore(LocalDateTime date);
}