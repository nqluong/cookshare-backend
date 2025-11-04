package com.backend.cookshare.admin_report.repository;

import com.backend.cookshare.admin_report.repository.search_projection.*;
import com.backend.cookshare.interaction.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SearchStatisticsRepository extends JpaRepository<SearchHistory, UUID> {
    /**
     * Đếm tổng số lượt tìm kiếm trong khoảng thời gian
     */
    @Query("SELECT COUNT(sh) FROM SearchHistory sh " +
            "WHERE sh.createdAt BETWEEN :startDate AND :endDate")
    Long countTotalSearches(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số từ khóa unique
     */
    @Query("SELECT COUNT(DISTINCT sh.searchQuery) FROM SearchHistory sh " +
            "WHERE sh.createdAt BETWEEN :startDate AND :endDate")
    Long countUniqueQueries(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số tìm kiếm thành công (có kết quả > 0)
     */
    @Query("SELECT COUNT(sh) FROM SearchHistory sh " +
            "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
            "AND sh.resultCount > 0")
    Long countSuccessfulSearches(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số tìm kiếm không có kết quả
     */
    @Query("SELECT COUNT(sh) FROM SearchHistory sh " +
            "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
            "AND sh.resultCount = 0")
    Long countZeroResultSearches(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số người dùng đã tìm kiếm
     */
    @Query("SELECT COUNT(DISTINCT sh.userId) FROM SearchHistory sh " +
            "WHERE sh.createdAt BETWEEN :startDate AND :endDate " +
            "AND sh.userId IS NOT NULL")
    Long countUniqueSearchUsers(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Tính trung bình số kết quả mỗi lần tìm kiếm
     */
    @Query("SELECT AVG(sh.resultCount) FROM SearchHistory sh " +
            "WHERE sh.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getAverageResultsPerSearch(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy top từ khóa được tìm kiếm nhiều nhất
     */
    @Query(value = """
            SELECT
              search_query as searchQuery,
              COUNT(*) as searchCount, 
              COUNT(DISTINCT user_id) as uniqueUsers, 
              AVG(result_count) as avgResults, 
              MAX(created_at) as lastSearched 
            FROM search_history 
            WHERE created_at BETWEEN :startDate AND :endDate 
            AND search_query IS NOT NULL 
            AND TRIM(search_query) != '' 
            GROUP BY search_query 
            ORDER BY searchCount DESC 
            LIMIT :limit
            """,
            nativeQuery = true)
    List<PopularKeywordProjection> getPopularKeywords(
            @Param("limit") int limit,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy từ khóa không có kết quả
     */
    @Query(value = "SELECT " +
            "  search_query as searchQuery, " +
            "  COUNT(*) as searchCount, " +
            "  COUNT(DISTINCT user_id) as uniqueUsers, " +
            "  MIN(created_at) as firstSearched, " +
            "  MAX(created_at) as lastSearched " +
            "FROM search_history " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "AND result_count = 0 " +
            "AND search_query IS NOT NULL " +
            "AND TRIM(search_query) != '' " +
            "GROUP BY search_query " +
            "ORDER BY searchCount DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<ZeroResultKeywordProjection> getZeroResultKeywords(@Param("limit") int limit,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy nguyên liệu được tìm kiếm nhiều nhất
     */
    @Query(value = "WITH ingredient_searches AS (" +
            "  SELECT " +
            "    i.ingredient_id, " +
            "    i.name, " +
            "    COUNT(sh.search_id) as search_count, " +
            "    COUNT(CASE WHEN sh.search_type = 'INGREDIENT' THEN 1 END) as direct_searches " +
            "  FROM ingredients i " +
            "  LEFT JOIN search_history sh ON " +
            "    LOWER(sh.search_query) LIKE '%' || LOWER(i.name) || '%' " +
            "    AND sh.created_at BETWEEN :startDate AND :endDate " +
            "  GROUP BY i.ingredient_id, i.name " +
            "  HAVING COUNT(sh.search_id) > 0" +
            ") " +
            "SELECT " +
            "  s.ingredient_id as ingredientId, " +
            "  s.name as ingredientName, " +
            "  s.search_count as searchCount, " +
            "  s.direct_searches as directSearches, " +
            "  COUNT(DISTINCT ri.recipe_id) as recipeCount " +
            "FROM ingredient_searches s " +
            "LEFT JOIN recipe_ingredients ri ON s.ingredient_id = ri.ingredient_id " +
            "GROUP BY s.ingredient_id, s.name, s.search_count, s.direct_searches " +
            "ORDER BY s.search_count DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<PopularIngredientProjection> getPopularIngredients(@Param("limit") int limit,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);


    /**
     * Lấy thống kê view theo danh mục
     */
    @Query(value = "SELECT " +
            "  c.category_id as categoryId, " +
            "  c.name as categoryName, " +
            "  COUNT(DISTINCT al.log_id) as viewCount, " +
            "  COUNT(DISTINCT al.user_id) as uniqueUsers, " +
            "  COUNT(DISTINCT r.recipe_id) as recipeCount " +
            "FROM categories c " +
            "JOIN recipe_categories rc ON c.category_id = rc.category_id " +
            "JOIN recipes r ON rc.recipe_id = r.recipe_id " +
            "LEFT JOIN activity_logs al ON " +
            "  r.recipe_id = al.target_id " +
            "  AND al.activity_type = 'VIEW' " +
            "  AND al.created_at BETWEEN :startDate AND :endDate " +
            "WHERE r.is_published = true " +
            "AND c.is_active = true " +
            "GROUP BY c.category_id, c.name " +
            "ORDER BY viewCount DESC",
            nativeQuery = true)
    List<CategoryViewProjection> getCategoryViewStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);


    /**
     * Lấy tỷ lệ thành công theo loại tìm kiếm
     */
    @Query(value = "SELECT " +
            "  COALESCE(search_type, 'GENERAL') as searchType, " +
            "  COUNT(*) as totalSearches, " +
            "  COUNT(CASE WHEN result_count > 0 THEN 1 END) as successfulSearches " +
            "FROM search_history " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY search_type " +
            "ORDER BY totalSearches DESC",
            nativeQuery = true)
    List<SuccessRateByTypeProjection> getSuccessRateByType(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy xu hướng tỷ lệ thành công theo thời gian
     */
    @Query(value = "SELECT " +
            "  CASE :groupBy " +
            "    WHEN 'DAY' THEN DATE_TRUNC('day', created_at) " +
            "    WHEN 'WEEK' THEN DATE_TRUNC('week', created_at) " +
            "    WHEN 'MONTH' THEN DATE_TRUNC('month', created_at) " +
            "    ELSE DATE_TRUNC('day', created_at) " +
            "  END as period, " +
            "  COUNT(*) as totalSearches, " +
            "  COUNT(CASE WHEN result_count > 0 THEN 1 END) as successfulSearches " +
            "FROM search_history " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY period " +
            "ORDER BY period",
            nativeQuery = true)
    List<SuccessRateTrendProjection> getSuccessRateTrend(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("groupBy") String groupBy);

    /**
     * Lấy xu hướng tìm kiếm theo thời gian
     */
    @Query(value = "SELECT " +
            "  CASE :groupBy " +
            "    WHEN 'DAY' THEN DATE_TRUNC('day', created_at) " +
            "    WHEN 'WEEK' THEN DATE_TRUNC('week', created_at) " +
            "    WHEN 'MONTH' THEN DATE_TRUNC('month', created_at) " +
            "    ELSE DATE_TRUNC('day', created_at) " +
            "  END as period, " +
            "  COUNT(*) as totalSearches, " +
            "  COUNT(DISTINCT user_id) as uniqueUsers, " +
            "  COUNT(DISTINCT search_query) as uniqueQueries, " +
            "  COUNT(CASE WHEN result_count > 0 THEN 1 END) as successfulSearches, " +
            "  AVG(result_count) as avgResults " +
            "FROM search_history " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY period " +
            "ORDER BY period",
            nativeQuery = true)
    List<SearchTrendProjection> getSearchTrends(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("groupBy") String groupBy);
}
