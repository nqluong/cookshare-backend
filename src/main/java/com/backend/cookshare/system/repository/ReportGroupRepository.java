package com.backend.cookshare.system.repository;

import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.projection.ReportDetailWithContextProjection;
import com.backend.cookshare.system.repository.projection.ReportTypeCount;
import com.backend.cookshare.system.repository.projection.TopReporterProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportGroupRepository extends JpaRepository<Report, UUID> {

    /**
     * Query nhóm reports theo Recipe với thông tin tác giả
     */
    @Query("""
        SELECT new com.backend.cookshare.system.dto.response.ReportGroupResponse(
            r.recipeId,
            rec.title,
            rec.featuredImage,
            recAuthor.userId,
            recAuthor.username,
            recAuthor.fullName,
            recAuthor.avatarUrl,
            COUNT(r.reportId),
            0.0,
            NULL,
            MAX(r.createdAt),
            MIN(r.createdAt),
            NULL,
            false,
            false,
            'MEDIUM',
            NULL,
            NULL
        )
        FROM Report r
        JOIN Recipe rec ON r.recipeId = rec.recipeId
        JOIN User recAuthor ON rec.user.userId = recAuthor.userId
        WHERE r.status = 'PENDING'
        AND r.recipeId IS NOT NULL
        GROUP BY 
            r.recipeId,
            rec.title,
            rec.featuredImage,
            recAuthor.userId,
            recAuthor.username,
            recAuthor.avatarUrl
        ORDER BY COUNT(r.reportId) DESC, MAX(r.createdAt) DESC
    """)
    Page<ReportGroupResponse> findGroupedReports(Pageable pageable);

    /**
     * Lấy tất cả báo cáo của một Recipe cụ thể
     */
    @Query("""
        SELECT r
        FROM Report r
        WHERE r.status = 'PENDING'
        AND r.recipeId = :recipeId
        ORDER BY r.createdAt DESC
    """)
    List<Report> findReportsByRecipe(@Param("recipeId") UUID recipeId);

    /**
     * BATCH: Đếm báo cáo theo loại cho NHIỀU Recipe cùng lúc
     */
    @Query("""
        SELECT r.recipeId as recipeId, r.reportType as type, COUNT(r) as count
        FROM Report r
        WHERE r.status = 'PENDING'
        AND r.recipeId IN :recipeIds
        GROUP BY r.recipeId, r.reportType
    """)
    List<BatchReportTypeCount> batchCountReportTypesByRecipes(@Param("recipeIds") List<UUID> recipeIds);

    /**
     Lấy top reporters cho Recipe cùng lúc (với username đã join sẵn)
     */
    @Query(value = """
        SELECT ranked.recipe_id as recipeId, ranked.reporter_id as reporterId, u.full_name as reporterFullname
        FROM (
            SELECT r.recipe_id, r.reporter_id, r.created_at,
                   ROW_NUMBER() OVER (PARTITION BY r.recipe_id ORDER BY r.created_at DESC) as rn
            FROM reports r
            WHERE r.status = 'PENDING'
            AND r.recipe_id IN :recipeIds
        ) ranked
        JOIN users u ON ranked.reporter_id = u.user_id
        WHERE ranked.rn <= 3
        ORDER BY ranked.recipe_id, ranked.rn
        """, nativeQuery = true)
    List<TopReporterProjection> batchFindTopReportersByRecipes(@Param("recipeIds") List<UUID> recipeIds);

    /**
     * Đếm báo cáo theo loại cho một Recipe (giữ lại cho getGroupDetail)
     */
    @Query("""
        SELECT r.reportType as type, COUNT(r) as count
        FROM Report r
        WHERE r.status = 'PENDING'
        AND r.recipeId = :recipeId
        GROUP BY r.reportType
    """)
    List<ReportTypeCount> countReportTypesByRecipe(@Param("recipeId") UUID recipeId);

    /**
     * Lấy ID tác giả của Recipe
     */
    @Query("""
        SELECT rec.user.userId
        FROM Recipe rec
        WHERE rec.recipeId = :recipeId
    """)
    UUID findAuthorIdByRecipeId(@Param("recipeId") UUID recipeId);

    /**
     * Đếm tổng số Recipe bị báo cáo của một User
     */
    @Query("""
        SELECT COUNT(DISTINCT r.recipeId)
        FROM Report r
        JOIN Recipe rec ON r.recipeId = rec.recipeId
        WHERE rec.user.userId = :userId
        AND r.status = 'PENDING'
    """)
    long countReportedRecipesByAuthor(@Param("userId") UUID userId);

    /**
     * Tính tổng số báo cáo của tất cả Recipe thuộc một User
     */
    @Query("""
        SELECT COUNT(r)
        FROM Report r
        JOIN Recipe rec ON r.recipeId = rec.recipeId
        WHERE rec.user.userId = :userId
        AND r.status = 'PENDING'
    """)
    long countTotalReportsByAuthor(@Param("userId") UUID userId);

    /**
     * Lấy chi tiết đầy đủ cho một Recipe: reports + author info + reporter usernames
     * Một query duy nhất thay vì 3 queries riêng biệt
     */
    @Query(value = """
        SELECT 
            r.report_id as reportId,
            r.reporter_id as reporterId,
            u_reporter.username as reporterUsername,
            u_reporter.full_name as reporterFullName,
            u_reporter.avatar_url as reporterAvatarUrl,
            r.report_type as reportType,
            r.reason as reason,
            r.description as description,
            r.created_at as createdAt,
            rec.title as recipeTitle,
            rec.featured_image as recipeFeaturedImage,
            rec.user_id as authorId,
            u_author.username as authorUsername,
            u_author.full_name as authorFullName,
            u_author.avatar_url as authorAvatarUrl
        FROM reports r
        JOIN recipes rec ON r.recipe_id = rec.recipe_id
        JOIN users u_author ON rec.user_id = u_author.user_id
        JOIN users u_reporter ON r.reporter_id = u_reporter.user_id
        WHERE r.status = 'PENDING'
        AND r.recipe_id = :recipeId
        ORDER BY r.created_at DESC
        """, nativeQuery = true)
    List<ReportDetailWithContextProjection> findReportDetailsWithContext(@Param("recipeId") UUID recipeId);

    /**
     * Projection cho batch report type count
     */
    interface BatchReportTypeCount {
        UUID getRecipeId();
        ReportType getType();
        Long getCount();
    }

}
