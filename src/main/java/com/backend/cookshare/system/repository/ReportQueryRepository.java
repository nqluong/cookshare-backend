package com.backend.cookshare.system.repository;

import com.backend.cookshare.system.dto.response.RecipeInfo;
import com.backend.cookshare.system.dto.response.ReporterInfo;
import com.backend.cookshare.system.dto.response.ReviewerInfo;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.projection.RecipeAuthorProjection;
import com.backend.cookshare.system.repository.projection.ReportedRecipeInfoProjection;
import com.backend.cookshare.system.repository.projection.ReportedUserInfoProjection;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
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
     * BATCH: Lấy thông tin cơ bản của nhiều users (userId, username)
     * Dùng cho: reporter usernames, reviewer names, etc.
     */
    @Query(value = """
        SELECT u.user_id as userId, u.username, u.full_name as fullName
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<UsernameProjection> findUsernamesByIds(@Param("ids") List<UUID> ids);

    /**
     * BATCH: Lấy thông tin reporter (userId, username, avatar)
     */
    @Query(value = """
        SELECT u.user_id as userId, u.username, u.avatar_url as avatarUrl, u.full_name as fullName
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<ReporterInfo> findReporterInfoByIds(@Param("ids") List<UUID> ids);

    /**
     * BATCH: Lấy thông tin chi tiết của reported users
     */
    @Query(value = """
        SELECT u.user_id as userId, u.username, u.full_name as fullName, u.email, u.avatar_url as avatarUrl,
               u.role, u.is_active as isActive
        FROM users u
        WHERE u.user_id IN :ids
        """, nativeQuery = true)
    List<ReportedUserInfoProjection> findReportedUserInfoByIds(@Param("ids") List<UUID> ids);

    /**
     * BATCH: Lấy thông tin chi tiết của nhiều recipes + author info
     */
    @Query(value = """
        SELECT r.recipe_id as recipeId, r.title, r.slug, r.featured_image as featuredImage,
               r.status, r.is_published as isPublished, r.view_count as viewCount, 
               r.user_id as userId, u.username as authorUsername
        FROM recipes r
        LEFT JOIN users u ON r.user_id = u.user_id
        WHERE r.recipe_id IN :ids
        """, nativeQuery = true)
    List<ReportedRecipeInfoProjection> findReportedRecipeInfoByIds(@Param("ids") List<UUID> ids);

    /**
     * Lấy thông tin recipe + author trong 1 query (dùng DTO constructor)
     */
    @Query("""
        SELECT new com.backend.cookshare.system.dto.response.RecipeInfo(
            r.recipeId,
            r.title,
            r.user.userId, 
            r.user.username,
            r.user.fullName
        )
        FROM Recipe r
        WHERE r.recipeId = :recipeId
    """)
    Optional<RecipeInfo> findRecipeInfoById(@Param("recipeId") UUID recipeId);

    /**
     * Lấy thông tin tác giả của Recipe
     */
    @Query(value = """
        SELECT u.user_id as authorId, u.username as authorUsername, u.full_name as authorFullName
        FROM recipes r
        JOIN users u ON r.user_id = u.user_id
        WHERE r.recipe_id = :recipeId
        """, nativeQuery = true)
    Optional<RecipeAuthorProjection> findRecipeAuthorInfo(@Param("recipeId") UUID recipeId);

    /**
     * Lấy ID tác giả của Recipe
     */
    @Query(value = "SELECT user_id FROM recipes WHERE recipe_id = :recipeId", nativeQuery = true)
    Optional<UUID> findAuthorIdByRecipeId(@Param("recipeId") UUID recipeId);


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


    /**
     * Tìm userId theo username
     */
    @Query(value = "SELECT user_id FROM users WHERE username = :username", nativeQuery = true)
    Optional<UUID> findUserIdByUsername(@Param("username") String username);
}
