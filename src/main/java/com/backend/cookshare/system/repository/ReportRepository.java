package com.backend.cookshare.system.repository;

import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.projection.ReportCountProjection;
import com.backend.cookshare.system.repository.projection.ReportProjection;
import com.backend.cookshare.system.repository.projection.TopReportedProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("SELECT r FROM Report r WHERE r.recipeId = :recipeId AND r.status = 'PENDING'")
    List<Report> findPendingReportsByRecipeId(@Param("recipeId") UUID recipeId);

    /**
     * Lấy tất cả pending reports của user (để tính weighted score)
     */
    @Query("SELECT r FROM Report r WHERE r.reportedId = :userId AND r.status = 'PENDING'")
    List<Report> findPendingReportsByUserId(@Param("userId") UUID userId);

    @Query("SELECT r FROM Report r WHERE r.recipeId = :recipeId")
    List<Report> findAllByRecipeId(@Param("recipeId") UUID recipeId);

    @Query("SELECT r FROM Report r WHERE r.reportedId = :userId")
    List<Report> findAllByReportedUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(r) > 0 
        FROM Report r 
        WHERE r.reporterId = :reporterId 
        AND r.status = 'PENDING'
        AND (
            (r.reportedId = :reportedId AND :reportedId IS NOT NULL) 
            OR (r.recipeId = :recipeId AND :recipeId IS NOT NULL)
        )
    """)
    boolean existsPendingReportByReporter(
            @Param("reporterId") UUID reporterId,
            @Param("reportedId") UUID reportedId,
            @Param("recipeId") UUID recipeId
    );

    // Đếm số lượng báo cáo theo status
    long countByStatus(ReportStatus status);


    // Đếm số lượng báo cáo pending cho một recipe
    @Query("SELECT COUNT(r) FROM Report r WHERE r.recipeId = :recipeId AND r.status = 'PENDING'")
    long countPendingReportsByRecipeId(@Param("recipeId") UUID recipeId);

    // Lấy danh sách report với filter động - JOIN tất cả thông tin liên quan
    @Query(value = """
        SELECT r.report_id as reportId,
               r.reporter_id as reporterId,
               r.reported_id as reportedId,
               r.recipe_id as recipeId,
               r.report_type as reportType,
               r.reason as reason,
               r.description as description,
               r.status as status,
               r.action_taken as actionTaken,
               r.action_description as actionDescription,
               r.admin_note as adminNote,
               r.reviewed_by as reviewedBy,
               r.reviewed_at as reviewedAt,
               r.created_at as createdAt,
               r.reporters_notified as reportersNotified,
               
               reporter.user_id as reporterUserId,
               reporter.username as reporterUsername,
               reporter.full_name as reporterFullName,
               reporter.avatar_url as reporterAvatarUrl,
               
               reported_user.user_id as reportedUserId,
               reported_user.username as reportedUsername,
               reported_user.email as reportedEmail,
               reported_user.avatar_url as reportedAvatarUrl,
               reported_user.role as reportedRole,
               reported_user.is_active as reportedIsActive,
               
               recipe.recipe_id as reportedRecipeId,
               recipe.title as reportedRecipeTitle,
               recipe.slug as reportedRecipeSlug,
               recipe.featured_image as reportedRecipeFeaturedImage,
               recipe.status as reportedRecipeStatus,
               recipe.is_published as reportedRecipeIsPublished,
               recipe.view_count as reportedRecipeViewCount,
               recipe.user_id as reportedRecipeUserId,
               recipe_author.username as reportedRecipeAuthorUsername,
               
               reviewer.user_id as reviewerUserId,
               reviewer.username as reviewerUsername,
               reviewer.full_name as reviewerFullName,
               reviewer.avatar_url as reviewerAvatarUrl
               
        FROM reports r
        LEFT JOIN users reporter ON r.reporter_id = reporter.user_id
        LEFT JOIN users reported_user ON r.reported_id = reported_user.user_id
        LEFT JOIN recipes recipe ON r.recipe_id = recipe.recipe_id
        LEFT JOIN users recipe_author ON recipe.user_id = recipe_author.user_id
        LEFT JOIN users reviewer ON r.reviewed_by = reviewer.user_id
        
        WHERE (CAST(:reportType AS VARCHAR) IS NULL OR r.report_type = CAST(:reportType AS VARCHAR))
        AND (CAST(:status AS VARCHAR) IS NULL OR r.status = CAST(:status AS VARCHAR))
        AND (CAST(:reporterId AS UUID) IS NULL OR r.reporter_id = CAST(:reporterId AS UUID))
        AND (CAST(:reportedId AS UUID) IS NULL OR r.reported_id = CAST(:reportedId AS UUID))
        AND (CAST(:recipeId AS UUID) IS NULL OR r.recipe_id = CAST(:recipeId AS UUID))
        AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.created_at >= CAST(:fromDate AS TIMESTAMP))
        AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.created_at <= CAST(:toDate AS TIMESTAMP))
    """, nativeQuery = true)
    Page<ReportProjection> findByFilters(
            @Param("reportType") String reportType,
            @Param("status") String status,
            @Param("reporterId") String reporterId,
            @Param("reportedId") String reportedId,
            @Param("recipeId") String recipeId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            Pageable pageable
    );

    // Lấy report projection theo ID - JOIN tất cả thông tin liên quan
    @Query(value = """
        SELECT r.report_id as reportId,
               r.reporter_id as reporterId,
               r.reported_id as reportedId,
               r.recipe_id as recipeId,
               r.report_type as reportType,
               r.reason as reason,
               r.description as description,
               r.status as status,
               r.action_taken as actionTaken,
               r.action_description as actionDescription,
               r.admin_note as adminNote,
               r.reviewed_by as reviewedBy,
               r.reviewed_at as reviewedAt,
               r.created_at as createdAt,
               r.reporters_notified as reportersNotified,
               
               reporter.user_id as reporterUserId,
               reporter.username as reporterUsername,
               reporter.full_name as reporterFullName,
               reporter.avatar_url as reporterAvatarUrl,
               
               reported_user.user_id as reportedUserId,
               reported_user.username as reportedUsername,
               reported_user.email as reportedEmail,
               reported_user.avatar_url as reportedAvatarUrl,
               reported_user.role as reportedRole,
               reported_user.is_active as reportedIsActive,
               
               recipe.recipe_id as reportedRecipeId,
               recipe.title as reportedRecipeTitle,
               recipe.slug as reportedRecipeSlug,
               recipe.featured_image as reportedRecipeFeaturedImage,
               recipe.status as reportedRecipeStatus,
               recipe.is_published as reportedRecipeIsPublished,
               recipe.view_count as reportedRecipeViewCount,
               recipe.user_id as reportedRecipeUserId,
               recipe_author.username as reportedRecipeAuthorUsername,
               
               reviewer.user_id as reviewerUserId,
               reviewer.username as reviewerUsername,
               reviewer.avatar_url as reviewerAvatarUrl
               
        FROM reports r
        LEFT JOIN users reporter ON r.reporter_id = reporter.user_id
        LEFT JOIN users reported_user ON r.reported_id = reported_user.user_id
        LEFT JOIN recipes recipe ON r.recipe_id = recipe.recipe_id
        LEFT JOIN users recipe_author ON recipe.user_id = recipe_author.user_id
        LEFT JOIN users reviewer ON r.reviewed_by = reviewer.user_id
        
        WHERE r.report_id = :reportId
    """, nativeQuery = true)
    Optional<ReportProjection> findProjectionById(@Param("reportId") UUID reportId);


    // Thống kê báo cáo theo type
    @Query("""
        SELECT r.reportType as reportType, COUNT(r) as count
        FROM Report r
        GROUP BY r.reportType
    """)
    List<ReportCountProjection> countReportsByType();

    // Top users bị báo cáo nhiều nhất
    @Query("""
        SELECT r.reportedId as itemId, COUNT(r) as reportCount
        FROM Report r
        WHERE r.reportedId IS NOT NULL
        AND r.status = 'PENDING'
        GROUP BY r.reportedId
        ORDER BY COUNT(r) DESC
    """)
    List<TopReportedProjection> findTopReportedUsers(Pageable pageable);

    // Top recipes bị báo cáo nhiều nhất
    @Query("""
        SELECT r.recipeId as itemId, COUNT(r) as reportCount
        FROM Report r
        WHERE r.recipeId IS NOT NULL
        AND r.status = 'PENDING'
        GROUP BY r.recipeId 
        ORDER BY COUNT(r) DESC
    """)
    List<TopReportedProjection> findTopReportedRecipes(Pageable pageable);


    /**
     * Đếm số recipes bị report (distinct)
     */
    @Query("SELECT COUNT(DISTINCT r.recipeId) FROM Report r WHERE r.recipeId IS NOT NULL")
    long countDistinctRecipeIds();


    /**
     * Đếm recipes có >= N reports
     */
    @Query("""
        SELECT COUNT(DISTINCT r.recipeId)
        FROM Report r
        WHERE r.recipeId IS NOT NULL
        AND r.status = 'PENDING'
        GROUP BY r.recipeId
        HAVING COUNT(r) >= :threshold
    """)
    long countRecipesExceedingThreshold(@Param("threshold") Long threshold);


    /**
     * Đếm reports có recipe
     */
    long countByRecipeIdIsNotNull();


    /**
     * Đếm công thức có > N báo cáo (Ưu tiên Critical)
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT r.recipeId FROM Report r 
            WHERE r.recipeId IS NOT NULL AND r.status = 'PENDING'
            GROUP BY r.recipeId HAVING COUNT(r) > :count
        ) as recipes
    """, nativeQuery = true)
    long countRecipesWithReportCountGreaterThan(@Param("count") Long count);

    /**
     * Đếm công thức có số báo cáo trong khoảng [min, max]
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT r.recipeId FROM Report r 
            WHERE r.recipeId IS NOT NULL AND r.status = 'PENDING'
            GROUP BY r.recipeId HAVING COUNT(r) BETWEEN :min AND :max
        ) as recipes
    """, nativeQuery = true)
    long countRecipesWithReportCountBetween(@Param("min") Long min, @Param("max") Long max);

    /**
     * Đếm công thức có < N báo cáo
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT r.recipeId FROM Report r 
            WHERE r.recipeId IS NOT NULL AND r.status = 'PENDING'
            GROUP BY r.recipeId HAVING COUNT(r) < :count
        ) as recipes
    """, nativeQuery = true)
    long countRecipesWithReportCountLessThan(@Param("count") Long count);

    /**
     * Đếm số recipes bị báo cáo (distinct) theo status
     */
    @Query("SELECT COUNT(DISTINCT r.recipeId) FROM Report r WHERE r.recipeId IS NOT NULL AND r.status = :status")
    long countDistinctRecipesByStatus(@Param("status") ReportStatus status);
}
