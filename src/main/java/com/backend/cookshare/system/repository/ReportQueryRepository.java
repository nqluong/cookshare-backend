package com.backend.cookshare.system.repository;

import com.backend.cookshare.system.dto.response.ReportedRecipeInfo;
import com.backend.cookshare.system.dto.response.ReporterInfo;
import com.backend.cookshare.system.dto.response.ReviewerInfo;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.projection.RecipeTitleProjection;
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

    @Query(value = "SELECT username FROM users WHERE user_id = :userId", nativeQuery = true)
    Optional<String> findUsernameById(@Param("userId") UUID userId);

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

    @Query(value = """
        SELECT user_id 
        FROM users 
        WHERE role = 'ADMIN' AND is_active = true
        """, nativeQuery = true)
    List<UUID> findAdminUserIds();

    @Query(value = """
        SELECT username 
        FROM users 
        WHERE role = 'ADMIN' AND is_active = true
        """, nativeQuery = true)
    List<String> findAdminUsernames();

    @Modifying
    @Query(value = "UPDATE users SET is_active = false WHERE user_id = :userId", nativeQuery = true)
    void disableUser(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
        UPDATE recipes 
        SET is_published = false, status = 'DRAFT' 
        WHERE recipe_id = :recipeId
        """, nativeQuery = true)
    void unpublishRecipeToDraft(@Param("recipeId") UUID recipeId);

    @Modifying
    @Query(value = "UPDATE recipes SET is_published = false WHERE recipe_id = :recipeId", nativeQuery = true)
    void unpublishRecipe(@Param("recipeId") UUID recipeId);


}
