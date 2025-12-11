package com.backend.cookshare.system.repository;

import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.projection.ReportTypeCount;
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
     * Query nhóm reports theo target với đầy đủ thông tin
     */
    @Query("""
        SELECT new com.backend.cookshare.system.dto.response.ReportGroupResponse(
            CASE 
                WHEN r.recipeId IS NOT NULL THEN 'RECIPE'
                ELSE 'USER'
            END as targetType,
            COALESCE(r.recipeId, r.reportedId) as targetId,
            CASE 
                WHEN r.recipeId IS NOT NULL THEN rec.title
                ELSE u.username
            END as targetTitle,
            CASE 
                WHEN r.recipeId IS NOT NULL THEN recAuthor.username
                ELSE NULL
            END as authorUsername,
            CASE 
                WHEN r.recipeId IS NOT NULL THEN rec.featuredImage
                ELSE u.avatarUrl
            END as avatarUrl,
            COUNT(r.reportId) as reportCount,
            0.0 as weightedScore,
            NULL as mostSevereType,
            MAX(r.createdAt) as latestReportTime,
            MIN(r.createdAt) as oldestReportTime,
            NULL as reportTypeBreakdown,
            false as autoActioned,
            false as exceedsThreshold,
            'MEDIUM' as priority,
            NULL as topReporters,
            NULL as reports
        )
        FROM Report r
        LEFT JOIN Recipe rec ON r.recipeId = rec.recipeId
        LEFT JOIN User recAuthor ON rec.user.userId = recAuthor.userId
        LEFT JOIN User u ON r.reportedId = u.userId
        WHERE r.status = 'PENDING'
        GROUP BY 
            CASE WHEN r.recipeId IS NOT NULL THEN 'RECIPE' ELSE 'USER' END,
            COALESCE(r.recipeId, r.reportedId),
            CASE WHEN r.recipeId IS NOT NULL THEN rec.title ELSE u.username END,
            CASE WHEN r.recipeId IS NOT NULL THEN recAuthor.username ELSE NULL END,
            CASE WHEN r.recipeId IS NOT NULL THEN rec.featuredImage ELSE u.avatarUrl END
        ORDER BY COUNT(r.reportId) DESC, MAX(r.createdAt) DESC
    """)
    Page<ReportGroupResponse> findGroupedReports(Pageable pageable);

    /**
     * Query chi tiết các reports của 1 target cụ thể
     */
    @Query("""
        SELECT r
        FROM Report r
        WHERE r.status = 'PENDING'
        AND (
            (r.recipeId = :targetId AND :targetType = 'RECIPE')
            OR
            (r.reportedId = :targetId AND :targetType = 'USER')
        )
        ORDER BY r.createdAt DESC
    """)
    List<Report> findReportsByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") UUID targetId
    );

    /**
     * Đếm reports theo type cho 1 target
     */
    @Query("""
        SELECT r.reportType as type, COUNT(r) as count
        FROM Report r
        WHERE r.status = 'PENDING'
        AND (
            (r.recipeId = :targetId AND :targetType = 'RECIPE')
            OR
            (r.reportedId = :targetId AND :targetType = 'USER')
        )
        GROUP BY r.reportType
    """)
    List<ReportTypeCount> countReportTypesByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") UUID targetId
    );

}
