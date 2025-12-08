package com.backend.cookshare.admin_report.repository;

import com.backend.cookshare.admin_report.repository.recipe_projection.*;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeStatisticsRepository extends JpaRepository<Recipe, UUID> {

    // Tổng số công thức
    @Query("""
            SELECT COUNT(r) FROM Recipe r 
            WHERE r.isPublished = true AND r.status = 'APPROVED'
            """)
    Long countTotalRecipes();

    // Công thức mới theo khoảng thời gian
    @Query("""
            SELECT COUNT(r) FROM Recipe r 
            WHERE r.createdAt >= :startDate 
            AND r.isPublished = true 
            AND r.status = 'APPROVED'
            """)
    Long countNewRecipes(@Param("startDate") LocalDateTime startDate);

    // Phân bố theo danh mục
    @Query(value = """
        SELECT c.name as name, COUNT(rc.recipe_id) as count
        FROM categories c
        LEFT JOIN recipe_categories rc ON c.category_id = rc.category_id
        LEFT JOIN recipes r ON rc.recipe_id = r.recipe_id 
          AND r.is_published = true 
          AND r.status = 'APPROVED'
        WHERE c.is_active = true
        GROUP BY c.category_id, c.name
        ORDER BY count DESC
        """, nativeQuery = true)
    List<CategoryDistribution> countRecipesByCategory();

    // Phân bố theo độ khó
    @Query(value = """
        SELECT difficulty, COUNT(*) as count
        FROM recipes
        WHERE is_published = true 
        AND status = 'APPROVED'
        AND difficulty IS NOT NULL
        GROUP BY difficulty
        ORDER BY count DESC
        """, nativeQuery = true)
    List<DifficultyDistribution> countRecipesByDifficulty();

    // Top công thức theo lượt xem
    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.slug as slug,
            r.view_count as viewCount,
            r.like_count as likeCount,
            r.save_count as saveCount,
            r.average_rating as averageRating,
            r.rating_count as ratingCount,
            u.full_name as authorName,
            r.created_at as createdAt
        FROM recipes r
        INNER JOIN users u ON r.user_id = u.user_id
        WHERE r.is_published = true AND r.status = 'APPROVED'
        ORDER BY r.view_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecipeProjection> findTopViewedRecipes(@Param("limit") int limit);

    // Top công thức được like
    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.slug as slug,
            r.view_count as viewCount,
            r.like_count as likeCount,
            r.save_count as saveCount,
            r.average_rating as averageRating,
            r.rating_count as ratingCount,
            u.full_name as authorName,
            r.created_at as createdAt
        FROM recipes r
        INNER JOIN users u ON r.user_id = u.user_id
        WHERE r.is_published = true AND r.status = 'APPROVED'
        ORDER BY r.like_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecipeProjection> findTopLikedRecipes(@Param("limit") int limit);


    // Top công thức được lưu
    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.slug as slug,
            r.view_count as viewCount,
            r.like_count as likeCount,
            r.save_count as saveCount,
            r.average_rating as averageRating,
            r.rating_count as ratingCount,
            u.full_name as authorName,
            r.created_at as createdAt
        FROM recipes r
        INNER JOIN users u ON r.user_id = u.user_id
        WHERE r.is_published = true AND r.status = 'APPROVED'
        ORDER BY r.save_count DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecipeProjection> findTopSavedRecipes(@Param("limit") int limit);


    // Top công thức có nhiều bình luận
    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.slug as slug,
            r.view_count as viewCount,
            r.like_count as likeCount,
            r.save_count as saveCount,
            r.average_rating as averageRating,
            r.rating_count as ratingCount,
            u.full_name as authorName,
            r.created_at as createdAt,
            COUNT(c.comment_id) as commentCount
        FROM recipes r
        INNER JOIN users u ON r.user_id = u.user_id
        LEFT JOIN comments c ON r.recipe_id = c.recipe_id
        WHERE r.is_published = true AND r.status = 'APPROVED'
        GROUP BY r.recipe_id, r.title, r.slug, r.view_count, r.like_count,
                 r.save_count, r.average_rating, r.rating_count, 
                 u.full_name, r.created_at
        ORDER BY commentCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecipeWithCommentProjection> findTopCommentedRecipes(@Param("limit") int limit);


    // Công thức trending (tính điểm dựa trên tương tác gần đây)
    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.slug as slug,
            r.view_count as viewCount,
            r.like_count as likeCount,
            (r.view_count + r.like_count * 3.0 + r.rating_count * 2.0) as trendingScore,
            r.created_at as createdAt
        FROM recipes r
        WHERE r.is_published = true
        AND r.status = 'APPROVED'
        AND r.created_at >= :startDate
        ORDER BY trendingScore DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TrendingRecipeProjection> findTrendingRecipes(
            @Param("startDate") LocalDateTime startDate,
            @Param("limit") int limit
    );

    // Công thức kém hiệu suất
    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.slug as slug,
            r.view_count as viewCount,
            r.like_count as likeCount,
            r.save_count as saveCount,
            r.average_rating as averageRating,
            r.rating_count as ratingCount,
            u.full_name as authorName,
            r.created_at as createdAt
        FROM recipes r
        INNER JOIN users u ON r.user_id = u.user_id
        WHERE r.is_published = true
        AND r.status = 'APPROVED'
        AND r.created_at <= :thresholdDate
        ORDER BY (r.view_count + r.like_count + r.save_count) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecipeProjection> findLowPerformanceRecipes(
            @Param("thresholdDate") LocalDateTime thresholdDate,
            @Param("limit") int limit
    );

    // Phân tích thời gian nấu trung bình
    @Query(value = """
        SELECT 
            AVG(cook_time) as avgCookTime,
            AVG(prep_time) as avgPrepTime,
            AVG(cook_time + prep_time) as avgTotalTime
        FROM recipes
        WHERE is_published = true
        AND status = 'APPROVED'
        AND cook_time IS NOT NULL 
        AND prep_time IS NOT NULL
        """, nativeQuery = true)
    CookingTimeStats getAverageCookingTimes();

    // Số lượng nguyên liệu trung bình
    @Query(value = """
        SELECT AVG(ingredient_count) as avgIngredientCount
        FROM (
            SELECT r.recipe_id, COUNT(ri.ingredient_id) as ingredient_count
            FROM recipes r
            LEFT JOIN recipe_ingredients ri ON r.recipe_id = ri.recipe_id
            WHERE r.is_published = true AND r.status = 'APPROVED'
            GROUP BY r.recipe_id
        ) subquery
        """, nativeQuery = true)
    Double getAverageIngredientCount();

    // Số bước nấu trung bình
    @Query(value = """
        SELECT AVG(step_count) as avgStepCount
        FROM (
            SELECT r.recipe_id, COUNT(rs.step_id) as step_count
            FROM recipes r
            LEFT JOIN recipe_steps rs ON r.recipe_id = rs.recipe_id
            WHERE r.is_published = true AND r.status = 'APPROVED'
            GROUP BY r.recipe_id
        ) subquery
        """, nativeQuery = true)
    Double getAverageStepCount();

    // Công thức có hình ảnh/video
    @Query(value = """
        SELECT 
            COUNT(CASE WHEN r.featured_image IS NOT NULL THEN 1 END) as recipesWithImage,
            COUNT(CASE WHEN EXISTS (
                SELECT 1 FROM recipe_steps rs 
                WHERE rs.recipe_id = r.recipe_id 
                AND rs.video_url IS NOT NULL
            ) THEN 1 END) as recipesWithVideo,
            COUNT(*) as totalRecipes
        FROM recipes r
        WHERE r.is_published = true AND r.status = 'APPROVED'
        """, nativeQuery = true)
    MediaStats getMediaStatistics();


    // Độ dài mô tả và hướng dẫn trung bình
    @Query(value = """
        SELECT 
            AVG(LENGTH(description)) as avgDescriptionLength,
            AVG(LENGTH(instructions)) as avgInstructionLength
        FROM recipes
        WHERE is_published = true
        AND status = 'APPROVED'
        AND description IS NOT NULL 
        AND instructions IS NOT NULL
        """, nativeQuery = true)
    ContentLengthStats getAverageContentLength();

    // Thống kê theo thời gian (time series)
    @Query(value = """
        SELECT 
            DATE(r.created_at) as date,
            COUNT(*) as recipeCount,
            SUM(r.view_count) as totalViews,
            SUM(r.like_count) as totalLikes
        FROM recipes r
        WHERE r.is_published = true
        AND r.status = 'APPROVED'
        AND r.created_at >= :startDate 
        AND r.created_at <= :endDate
        GROUP BY DATE(r.created_at)
        ORDER BY date
        """, nativeQuery = true)
    List<TimeSeriesProjection> getTimeSeriesData(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
        SELECT 
            u.user_id as userId,
            u.full_name as authorName,
            u.username as username,
            COUNT(r.recipe_id) as recipeCount,
            SUM(r.view_count) as totalViews,
            SUM(r.like_count) as totalLikes,
            AVG(r.average_rating) as avgRating
        FROM users u
        INNER JOIN recipes r ON u.user_id = r.user_id
        WHERE r.is_published = true AND r.status = 'APPROVED'
        GROUP BY u.user_id, u.full_name, u.username
        ORDER BY recipeCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TopAuthorProjection> findTopAuthors(@Param("limit") int limit);

    @Query(value = """
        SELECT 
            r.recipe_id as recipeId,
            r.title as title,
            r.view_count as viewCount,
            (r.like_count + r.save_count + r.rating_count) as engagementCount,
            CASE 
                WHEN r.view_count > 0 
                THEN (r.like_count + r.save_count + r.rating_count) * 100.0 / r.view_count
                ELSE 0 
            END as engagementRate
        FROM recipes r
        WHERE r.is_published = true
        AND r.status = 'APPROVED'
        AND r.view_count > 0
        ORDER BY engagementRate DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<EngagementRateProjection> findHighEngagementRecipes(@Param("limit") int limit);



    @Query(value = """
        SELECT 
            c.name as categoryName,
            COUNT(DISTINCT r.recipe_id) as recipeCount,
            SUM(r.view_count) as totalViews,
            SUM(r.like_count) as totalLikes,
            AVG(r.average_rating) as avgRating,
            AVG(CASE 
                WHEN r.view_count > 0 
                THEN (r.like_count + r.save_count) * 100.0 / r.view_count
                ELSE 0 
            END) as avgEngagementRate
        FROM categories c
        INNER JOIN recipe_categories rc ON c.category_id = rc.category_id
        INNER JOIN recipes r ON rc.recipe_id = r.recipe_id
        WHERE r.is_published = true AND r.status = 'APPROVED'
        GROUP BY c.category_id, c.name
        ORDER BY totalViews DESC
        """, nativeQuery = true)
    List<CategoryPerformanceProjection> getCategoryPerformance();

    @Query(value = """
        SELECT 
            COUNT(*) as totalRecipes,
            COUNT(CASE WHEN description IS NOT NULL AND LENGTH(TRIM(description)) > 0 THEN 1 END) as withDescription,
            COUNT(CASE WHEN featured_image IS NOT NULL THEN 1 END) as withImage,
            COUNT(CASE WHEN EXISTS (
                SELECT 1 FROM recipe_steps rs 
                WHERE rs.recipe_id = r.recipe_id AND rs.video_url IS NOT NULL
            ) THEN 1 END) as withVideo,
            COUNT(CASE WHEN EXISTS (
                SELECT 1 FROM recipe_ingredients ri 
                WHERE ri.recipe_id = r.recipe_id
            ) THEN 1 END) as withIngredients,
            COUNT(CASE WHEN EXISTS (
                SELECT 1 FROM recipe_steps rs 
                WHERE rs.recipe_id = r.recipe_id
            ) THEN 1 END) as withSteps,
            COUNT(CASE 
                WHEN description IS NOT NULL 
                AND featured_image IS NOT NULL
                AND EXISTS (SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id)
                AND EXISTS (SELECT 1 FROM recipe_steps rs WHERE rs.recipe_id = r.recipe_id)
                THEN 1 
            END) as completeRecipes,
            COUNT(CASE 
                WHEN description IS NOT NULL 
                AND featured_image IS NOT NULL
                AND EXISTS (SELECT 1 FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id)
                AND EXISTS (SELECT 1 FROM recipe_steps rs WHERE rs.recipe_id = r.recipe_id)
                THEN 1 
            END) * 100.0 / COUNT(*) as completionRate
        FROM recipes r
        WHERE is_published = true AND status = 'APPROVED'
        """, nativeQuery = true)
    RecipeCompletionStats getRecipeCompletionStats();
}
