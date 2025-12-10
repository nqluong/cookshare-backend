package com.backend.cookshare.system.repository;

import com.backend.cookshare.system.dto.response.ReportedRecipeInfo;
import com.backend.cookshare.system.dto.response.ReporterInfo;
import com.backend.cookshare.system.dto.response.ReviewerInfo;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.projection.RecipeTitleProjection;
import com.backend.cookshare.system.repository.projection.ReportedRecipeInfoProjection;
import com.backend.cookshare.system.repository.projection.ReportedUserInfoProjection;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
import com.backend.cookshare.system.service.impl.ReportNotificationServiceImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportQueryRepository extends JpaRepository<Report, UUID> {

    /**
     * Tìm tất cả username của admins
     */
    @Query("SELECT u.username FROM User u WHERE u.role = 'ADMIN' AND u.isActive = true")
    List<String> findAdminUsernames();

    /**
     * Tìm username theo userId
     */
    @Query("SELECT u.username FROM User u WHERE u.userId = :userId")
    Optional<String> findUsernameById(@Param("userId") UUID userId);

    /**
     * Kiểm tra recipe đã bị unpublish chưa
     */
    @Query("SELECT CASE WHEN r.isPublished = false THEN true ELSE false END FROM Recipe r WHERE r.recipeId = :recipeId")
    boolean isRecipeAlreadyUnpublished(@Param("recipeId") UUID recipeId);

    /**
     * Kiểm tra user đã bị disable chưa
     */
    @Query("SELECT CASE WHEN u.isActive = false THEN true ELSE false END FROM User u WHERE u.userId = :userId")
    boolean isUserAlreadyDisabled(@Param("userId") UUID userId);

    /**
     * Tìm thông tin recipe và author
     */
    @Query("""
        SELECT new com.backend.cookshare.system.service.impl.ReportNotificationServiceImpl.RecipeInfo(
            r.recipeId, 
            r.title, 
            r.user.userId, 
            r.user.username
        )
        FROM Recipe r
        WHERE r.recipeId = :recipeId
    """)
    ReportNotificationServiceImpl.RecipeInfo findRecipeInfoById(@Param("recipeId") UUID recipeId);

    /**
     * Tạm khóa user trong số ngày nhất định
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.isActive = false,
            u.suspendedUntil = CURRENT_TIMESTAMP + :days DAY,
            u.updatedAt = CURRENT_TIMESTAMP
        WHERE u.userId = :userId
    """)
    void suspendUser(@Param("userId") UUID userId, @Param("days") int days);

    /**
     * Vô hiệu hóa user vĩnh viễn
     */
    @Modifying
    @Query("""
        UPDATE User u 
        SET u.isActive = false,
            u.bannedAt = CURRENT_TIMESTAMP,
            u.updatedAt = CURRENT_TIMESTAMP
        WHERE u.userId = :userId
    """)
    void disableUser(@Param("userId") UUID userId);

    /**
     * Gỡ xuống công thức (unpublish)
     */
    @Modifying
    @Query("""
        UPDATE Recipe r 
        SET r.isPublished = false,
            r.unpublishedAt = CURRENT_TIMESTAMP,
            r.updatedAt = CURRENT_TIMESTAMP
        WHERE r.recipeId = :recipeId
    """)
    void unpublishRecipe(@Param("recipeId") UUID recipeId);

    @Query(value = """
        SELECT u.user_id as userId, u.username, u.avatar_url as avatarUrl
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<ReporterInfo> findReporterInfoByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
        SELECT u.user_id as userId, u.username, u.email, u.avatar_url as avatarUrl, 
               u.role, u.is_active as isActive
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<ReportedUserInfoProjection> findReportedUserInfoByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
        SELECT r.recipe_id as recipeId, r.title, r.slug, r.featured_image as featuredImage,
               r.status, r.is_published as isPublished, r.view_count as viewCount, 
               r.user_id as userId, u.username as authorUsername
        FROM recipes r
        LEFT JOIN users u ON r.user_id = u.user_id
        WHERE r.recipe_id IN :ids
        """, nativeQuery = true)
    List<ReportedRecipeInfoProjection> findReportedRecipeInfoByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
        SELECT u.user_id as userId, u.username, u.avatar_url as avatarUrl
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<ReviewerInfo> findReviewerInfoByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
        SELECT u.user_id as userId, u.username, u.avatar_url as avatarUrl
        FROM users u
        WHERE u.user_id = :id
        """, nativeQuery = true)
    Optional<ReporterInfo> findReporterInfoById(@Param("id") UUID id);

    @Query(value = """
        SELECT u.user_id as userId, u.username, u.email, u.avatar_url as avatarUrl,
               u.role, u.is_active as isActive
        FROM users u
        WHERE u.user_id = :id
        """, nativeQuery = true)
    Optional<ReportedUserInfoProjection> findReportedUserInfoById(@Param("id") UUID id);

    @Query(value = """
        SELECT r.recipe_id as recipeId, r.title, r.slug, r.featured_image as featuredImage,
               r.status, r.is_published as isPublished, r.view_count as viewCount,
               r.user_id as userId, u.username as authorUsername
        FROM recipes r
        LEFT JOIN users u ON r.user_id = u.user_id
        WHERE r.recipe_id = :id
        """, nativeQuery = true)
    Optional<ReportedRecipeInfoProjection> findReportedRecipeInfoById(@Param("id") UUID id);

    @Query(value = """
        SELECT u.user_id as userId, u.username, u.avatar_url as avatarUrl
        FROM users u
        WHERE u.user_id = :id
        """, nativeQuery = true)
    Optional<ReviewerInfo> findReviewerInfoById(@Param("id") UUID id);

    @Query(value = "SELECT user_id FROM users WHERE username = :username", nativeQuery = true)
    Optional<UUID> findUserIdByUsername(@Param("username") String username);

    @Query(value = "SELECT title FROM recipes WHERE recipe_id = :recipeId", nativeQuery = true)
    Optional<String> findRecipeTitleById(@Param("recipeId") UUID recipeId);

    @Query(value = """
        SELECT u.user_id as userId, u.username
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<UsernameProjection> findUsernamesByIds(@Param("ids") List<UUID> ids);

    @Query(value = """
        SELECT r.recipe_id as recipeId, r.title
        FROM recipes r
        WHERE r.recipe_id IN :ids
        """, nativeQuery = true)
    List<RecipeTitleProjection> findRecipeTitlesByIds(@Param("ids") List<UUID> ids);


}
