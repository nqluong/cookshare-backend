package com.backend.cookshare.admin_report.repository;

import com.backend.cookshare.admin_report.repository.interaction_projection.*;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InteractionStatisticsRepository extends JpaRepository<Recipe, UUID> {
    /**
     * Đếm tổng số lượt like trong khoảng thời gian
     */
    @Query("""
            SELECT COUNT(rl) FROM RecipeLike rl
            WHERE rl.createdAt BETWEEN :startDate AND :endDate
            """)
    Long countTotalLikes(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm tổng số bình luận trong khoảng thời gian
     */
    @Query("""
            SELECT COUNT(c) FROM Comment c
            WHERE c.createdAt BETWEEN :startDate AND :endDate
            """)
    Long countTotalComments(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm tổng số lượt lưu trong khoảng thời gian
     */
    @Query("""
            SELECT COUNT(cr) FROM CollectionRecipe cr
            WHERE cr.addedAt BETWEEN :startDate AND :endDate
            """)
    Long countTotalSaves(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm tổng số công thức đã xuất bản trong khoảng thời gian
     */
    @Query("""
            SELECT COUNT(r) FROM Recipe r
            WHERE r.isPublished = true AND r.status = 'APPROVED'
            AND r.createdAt BETWEEN :startDate AND :endDate
            """)
    Long countPublishedRecipes(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm tổng số lượt xem trong khoảng thời gian
     * Dựa vào activity_logs với action type là VIEW_RECIPE
     */
    @Query("""
            SELECT COUNT(al) FROM ActivityLog al
            WHERE al.activityType = 'VIEW'
            AND al.createdAt BETWEEN :startDate AND :endDate
            """)
    Long countTotalViews(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    /**
     * Tính trung bình số likes mỗi công thức trong khoảng thời gian
     */
    @Query(value = """
            SELECT COALESCE(AVG(like_count), 0) FROM (
              SELECT r.recipe_id, COUNT(rl.like_id) as like_count
              FROM recipes r
              LEFT JOIN recipe_likes rl ON r.recipe_id = rl.recipe_id
                AND rl.created_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true AND r.status = 'APPROVED'
              GROUP BY r.recipe_id
            ) subquery
            """,
            nativeQuery = true)
    BigDecimal getAverageLikesPerRecipe(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Tính trung bình số comments mỗi công thức
     */
    @Query(value = """
            SELECT COALESCE(AVG(comment_count), 0) FROM (
              SELECT COUNT(c.comment_id) as comment_count
              FROM recipes r
              LEFT JOIN comments c ON r.recipe_id = c.recipe_id
              WHERE r.is_published = true
              AND r.created_at BETWEEN :startDate AND :endDate
              GROUP BY r.recipe_id
            ) subquery
            """,
            nativeQuery = true)
    BigDecimal getAverageCommentsPerRecipe(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Tính trung bình số saves mỗi công thức trong khoảng thời gian
     */
    @Query(value = """
            SELECT COALESCE(AVG(save_count), 0) FROM (
              SELECT r.recipe_id, COUNT(cr.collection_recipe_id) as save_count
              FROM recipes r
              LEFT JOIN collection_recipes cr ON r.recipe_id = cr.recipe_id
                AND cr.added_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true
              GROUP BY r.recipe_id
            ) subquery
            """,
            nativeQuery = true)
    BigDecimal getAverageSavesPerRecipe(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy trung vị likes trong khoảng thời gian
     */
    @Query(value = """
            WITH recipe_likes_count AS (
              SELECT r.recipe_id, COUNT(rl.like_id) as like_count
              FROM recipes r
              LEFT JOIN recipe_likes rl ON r.recipe_id = rl.recipe_id
                AND rl.created_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true
              GROUP BY r.recipe_id
            )
            SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY like_count), 0)
            FROM recipe_likes_count """,
            nativeQuery = true)
    BigDecimal getMedianLikesPerRecipe(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy trung vị comments
     */
    @Query(value = """
            WITH recipe_comments AS (
              SELECT r.recipe_id, COUNT(c.comment_id) as comment_count
              FROM recipes r
              LEFT JOIN comments c ON r.recipe_id = c.recipe_id
              WHERE r.is_published = true
              AND r.created_at BETWEEN :startDate AND :endDate
              GROUP BY r.recipe_id
            )
            SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY comment_count), 0)
            FROM recipe_comments
            """,
            nativeQuery = true)
    BigDecimal getMedianCommentsPerRecipe(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy trung vị saves trong khoảng thời gian
     */
    @Query(value = """
            WITH recipe_saves_count AS (
              SELECT r.recipe_id, COUNT(cr.collection_recipe_id) as save_count
              FROM recipes r
              LEFT JOIN collection_recipes cr ON r.recipe_id = cr.recipe_id
                AND cr.added_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true
              GROUP BY r.recipe_id
            )
            SELECT COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY save_count), 0)
            FROM recipe_saves_count """,
            nativeQuery = true)
    BigDecimal getMedianSavesPerRecipe(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy số likes lớn nhất trên một công thức trong khoảng thời gian
     */
    @Query(value = """
            SELECT COALESCE(MAX(like_count), 0) FROM (
              SELECT COUNT(rl.like_id) as like_count
              FROM recipes r
              LEFT JOIN recipe_likes rl ON r.recipe_id = rl.recipe_id
                AND rl.created_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true
              GROUP BY r.recipe_id
            ) subquery
            """,
            nativeQuery = true)
    Long getMaxLikesOnRecipe(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy số comments lớn nhất trên một công thức
     */
    @Query(value = """
            SELECT COALESCE(MAX(comment_count), 0) FROM (
              SELECT COUNT(*) as comment_count FROM comments c
              JOIN recipes r ON c.recipe_id = r.recipe_id
              WHERE r.is_published = true
              AND r.created_at BETWEEN :startDate AND :endDate
              GROUP BY r.recipe_id
            ) subquery
            """,
            nativeQuery = true)
    Long getMaxCommentsOnRecipe(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy số saves lớn nhất trên một công thức trong khoảng thời gian
     */
    @Query(value = """
            SELECT COALESCE(MAX(save_count), 0) FROM (
              SELECT COUNT(cr.collection_recipe_id) as save_count
              FROM recipes r
              LEFT JOIN collection_recipes cr ON r.recipe_id = cr.recipe_id
                AND cr.added_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true
              GROUP BY r.recipe_id
            ) subquery
            """,
            nativeQuery = true)
    Long getMaxSavesOnRecipe(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy phân phối tương tác theo khoảng
     */
    @Query(value = """
            SELECT
              COUNT(CASE WHEN interaction_count BETWEEN 0 AND 10 THEN 1 END) as range0To10,
              COUNT(CASE WHEN interaction_count BETWEEN 11 AND 50 THEN 1 END) as range11To50,
              COUNT(CASE WHEN interaction_count BETWEEN 51 AND 100 THEN 1 END) as range51To100,
              COUNT(CASE WHEN interaction_count BETWEEN 101 AND 500 THEN 1 END) as range101To500,
              COUNT(CASE WHEN interaction_count > 500 THEN 1 END) as rangeOver500
            FROM (
              SELECT r.recipe_id,
                CASE :type
                  WHEN 'LIKE' THEN COUNT(DISTINCT rl.like_id)
                  WHEN 'COMMENT' THEN COUNT(DISTINCT c.comment_id)
                  WHEN 'SAVE' THEN COUNT(DISTINCT cr.collection_recipe_id)
                END as interaction_count
              FROM recipes r
              LEFT JOIN recipe_likes rl ON r.recipe_id = rl.recipe_id
                AND rl.created_at BETWEEN :startDate AND :endDate
              LEFT JOIN comments c ON r.recipe_id = c.recipe_id
                AND c.created_at BETWEEN :startDate AND :endDate
              LEFT JOIN collection_recipes cr ON r.recipe_id = cr.recipe_id
                AND cr.added_at BETWEEN :startDate AND :endDate
              WHERE r.is_published = true
              GROUP BY r.recipe_id
            ) counts
            """,
            nativeQuery = true)
    InteractionDistributionProjection getInteractionDistribution(@Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate,
                                                                 @Param("type") String type);

    /**
     * Lấy tương tác theo giờ trong ngày
     */
    @Query(value = """
            SELECT
              EXTRACT(HOUR FROM created_at)::integer as hour,
              SUM(CASE WHEN activity_type = 'LIKE' THEN 1 ELSE 0 END) as likes,
              SUM(CASE WHEN activity_type = 'COMMENT' THEN 1 ELSE 0 END) as comments,
              SUM(CASE WHEN activity_type = 'SAVE' THEN 1 ELSE 0 END) as saves,
              COUNT(*) as total
            FROM (
              SELECT created_at, 'LIKE' as activity_type
              FROM recipe_likes
              WHERE created_at BETWEEN :startDate AND :endDate
              UNION ALL
              SELECT created_at, 'COMMENT' as activity_type
              FROM comments
              WHERE created_at BETWEEN :startDate AND :endDate
              UNION ALL
              SELECT added_at as created_at, 'SAVE' as activity_type
              FROM collection_recipes
              WHERE added_at BETWEEN :startDate AND :endDate
            ) all_interactions
            GROUP BY hour
            ORDER BY hour
            """,
            nativeQuery = true)
    List<HourlyInteractionProjection> getInteractionsByHour(@Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy tương tác theo ngày trong tuần
     */
    @Query(value = """
            SELECT
              EXTRACT(ISODOW FROM created_at)::integer as dayOfWeek,
              SUM(CASE WHEN activity_type = 'LIKE' THEN 1 ELSE 0 END) as likes,
              SUM(CASE WHEN activity_type = 'COMMENT' THEN 1 ELSE 0 END) as comments,
              SUM(CASE WHEN activity_type = 'SAVE' THEN 1 ELSE 0 END) as saves,
              COUNT(*) as total
            FROM (
              SELECT created_at, 'LIKE' as activity_type
              FROM recipe_likes
              WHERE created_at BETWEEN :startDate AND :endDate
              UNION ALL
              SELECT created_at, 'COMMENT' as activity_type
              FROM comments
              WHERE created_at BETWEEN :startDate AND :endDate
              UNION ALL
              SELECT added_at as created_at, 'SAVE' as activity_type
              FROM collection_recipes
              WHERE added_at BETWEEN :startDate AND :endDate
            ) all_interactions
            GROUP BY dayOfWeek
            ORDER BY dayOfWeek
            """,
            nativeQuery = true)
    List<DailyInteractionProjection> getInteractionsByDayOfWeek(@Param("startDate") LocalDateTime startDate,
                                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy top bình luận được like nhiều nhất
     */
    @Query(value = """
            SELECT
              c.comment_id as commentId,
              c.content as content,
              r.recipe_id as recipeId,
              r.title as recipeTitle,
              u.user_id as userId,
              u.username as username,
              u.avatar_url as avatarUrl,
              0 as likeCount,
              c.created_at as createdAt
            FROM comments c
            JOIN recipes r ON c.recipe_id = r.recipe_id
            JOIN users u ON c.user_id = u.user_id
            WHERE c.created_at BETWEEN :startDate AND :endDate
            ORDER BY LENGTH(c.content) DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<TopCommentProjection> getTopComments(@Param("limit") int limit,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy xu hướng follow theo thời gian
     */
    @Query(value = """
            WITH follow_by_period AS (
              SELECT
                CASE :groupBy
                  WHEN 'DAY' THEN DATE_TRUNC('day', created_at)
                  WHEN 'WEEK' THEN DATE_TRUNC('week', created_at)
                  WHEN 'MONTH' THEN DATE_TRUNC('month', created_at)
                  ELSE DATE_TRUNC('day', created_at)
                END as period_date,
                COUNT(*) as new_follows
              FROM follows
              WHERE created_at BETWEEN :startDate AND :endDate
              GROUP BY period_date
              ORDER BY period_date
            )
            SELECT
              period_date as periodDate,
              new_follows as newFollows,
              SUM(new_follows) OVER (ORDER BY period_date) as cumulativeFollows
            FROM follow_by_period
            """,
            nativeQuery = true)
    List<FollowTrendProjection> getFollowTrendsByPeriod(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate,
                                                        @Param("groupBy") String groupBy);

    /**
     * Lấy engagement theo danh mục
     */
    @Query(value = """
            SELECT
              c.category_id as categoryId,
              c.name as categoryName,
              COUNT(DISTINCT r.recipe_id) as recipeCount,
              COALESCE(COUNT(DISTINCT rv.view_id), 0) as totalViews,
              COALESCE(COUNT(DISTINCT rl.like_id), 0) as totalLikes,
              COALESCE(COUNT(DISTINCT cm.comment_id), 0) as totalComments,
              COALESCE(COUNT(DISTINCT cr.collection_recipe_id), 0) as totalSaves
            FROM categories c
            JOIN recipe_categories rc ON c.category_id = rc.category_id
            JOIN recipes r ON rc.recipe_id = r.recipe_id
            LEFT JOIN recipe_views rv ON r.recipe_id = rv.recipe_id
              AND rv.viewed_at BETWEEN :startDate AND :endDate
            LEFT JOIN recipe_likes rl ON r.recipe_id = rl.recipe_id
              AND rl.created_at BETWEEN :startDate AND :endDate
            LEFT JOIN comments cm ON r.recipe_id = cm.recipe_id
              AND cm.created_at BETWEEN :startDate AND :endDate
            LEFT JOIN collection_recipes cr ON r.recipe_id = cr.recipe_id
              AND cr.added_at BETWEEN :startDate AND :endDate
            WHERE r.is_published = true
            GROUP BY c.category_id, c.name
            ORDER BY totalViews DESC
            """,
            nativeQuery = true)
    List<CategoryEngagementProjection> getEngagementByCategory(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}
