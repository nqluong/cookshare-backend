package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {
    // Featured recipes
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true AND r.isFeatured = true ORDER BY r.updatedAt DESC")
    Page<Recipe> findFeaturedRecipes(Pageable pageable);

    // Popular recipes - using custom scoring
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY " +
            "(r.likeCount * 2.0 + r.viewCount * 0.5 + r.saveCount * 1.5) DESC")
    Page<Recipe> findPopularRecipes(Pageable pageable);

    // Newest recipes
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY r.createdAt DESC")
    Page<Recipe> findNewestRecipes(Pageable pageable);

    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true AND r.ratingCount >= :minRatingCount " +
            "ORDER BY r.averageRating DESC, r.ratingCount DESC")
    Page<Recipe> findTopRatedRecipes(@Param("minRatingCount") int minRatingCount, Pageable pageable);

    // Trending recipes - using custom scoring
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY " +
            "(r.viewCount + r.likeCount * 3.0 + r.ratingCount * 2.0) DESC, r.createdAt DESC")
    Page<Recipe> findTrendingRecipes(Pageable pageable);

    // Get recipes by user IDs for batch user name lookup
    @Query("SELECT r FROM Recipe r WHERE r.userId IN :userIds")
    List<Recipe> findByUserIdIn(@Param("userIds") List<UUID> userIds);

    // Count total published recipes for pagination
    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true")
    long countPublishedRecipes();

    // Count featured recipes for pagination
    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true AND r.isFeatured = true")
    long countFeaturedRecipes();

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = true AND r.ratingCount >= :minRatingCount")
    long countTopRatedRecipes(@Param("minRatingCount") int minRatingCount);
    List<Recipe> findByUserId(UUID userId);

    //Tong so luot thich cua tat ca cac cong thuc
    @Query("SELECT COALESCE(SUM(r.likeCount), 0) FROM Recipe r WHERE r.userId = :userId")
    Integer getTotalLikeCountByUserId(@Param("userId") UUID userId);

    /**
     * Tìm kiếm công thức với bộ lọc admin
     */
    @Query("SELECT r FROM Recipe r WHERE " +
            "(:search IS NULL OR r.title LIKE %:search% OR " +
            "r.description LIKE %:search% OR " +
            "r.instructions LIKE %:search%) AND " +
            "(:isPublished IS NULL OR r.isPublished = :isPublished) AND " +
            "(:isFeatured IS NULL OR r.isFeatured = :isFeatured) AND " +
            "(:status IS NULL OR r.status = :status)")
    Page<Recipe> findAllWithAdminFilters(
            @Param("search") String search,
            @Param("isPublished") Boolean isPublished,
            @Param("isFeatured") Boolean isFeatured,
            @Param("status") RecipeStatus status,
            Pageable pageable);

    /**
     * Lấy danh sách công thức theo trạng thái
     */
    Page<Recipe> findByStatusOrderByCreatedAtDesc(RecipeStatus status, Pageable pageable);

    /**
     * Lấy danh sách công thức chờ phê duyệt
     */
    Page<Recipe> findByStatusAndIsPublishedFalseOrderByCreatedAtDesc(RecipeStatus status, Pageable pageable);

    /**
     * Lấy danh sách công thức đã được phê duyệt
     */
    Page<Recipe> findByStatusAndIsPublishedTrueOrderByCreatedAtDesc(RecipeStatus status, Pageable pageable);

    /**
     * Lấy danh sách công thức nổi bật
     */
    Page<Recipe> findByIsFeaturedTrueOrderByCreatedAtDesc(Pageable pageable);
    @Query("SELECT r FROM Recipe r WHERE r.isPublished = true ORDER BY r.recipeId")
    List<Recipe> findAllPublishedRecipes();
    /**
     * Đếm số lượng công thức theo trạng thái
     */
    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.status = :status")
    long countByStatus(@Param("status") RecipeStatus status);


    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isPublished = :isPublished")
    long countByIsPublished(@Param("isPublished") Boolean isPublished);

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.isFeatured = :isFeatured")
    long countByIsFeatured(@Param("isFeatured") Boolean isFeatured);

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.status = 'PENDING'")
    long countPendingRecipes();

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.status = 'APPROVED'")
    long countApprovedRecipes();

    @Query("SELECT COUNT(r) FROM Recipe r WHERE r.status = 'REJECTED'")
    long countRejectedRecipes();

    @Query("SELECT r FROM Recipe r WHERE r.user.userId IN :followingIds ORDER BY r.createdAt DESC")
    Page<Recipe> findRecipesByFollowingIds(List<UUID> followingIds, Pageable pageable);
}
