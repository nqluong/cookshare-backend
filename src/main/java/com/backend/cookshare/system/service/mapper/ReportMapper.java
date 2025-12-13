package com.backend.cookshare.system.service.mapper;

import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.system.dto.response.*;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
public class ReportMapper {
    private final ReportQueryRepository reportQueryRepository;
    private final PageMapper pageMapper;
    private final Executor asyncExecutor;

    public ReportMapper(ReportQueryRepository reportQueryRepository,
                        PageMapper pageMapper,
                        @Qualifier("reportAsyncExecutor") Executor asyncExecutor) {
        this.reportQueryRepository = reportQueryRepository;
        this.pageMapper = pageMapper;
        this.asyncExecutor = asyncExecutor;
    }



    /**
     * Chuyển đổi Report entity sang ReportResponse.
     * Sử dụng batch queries với 1 phần tử thay vì single-item queries.
     */
    public ReportResponse toResponse(Report report) {
        ReportResponse response = buildBaseResponse(report);
        
        // Sử dụng batch load với single items
        populateDetailsWithBatch(response, report.getReporterId(), report.getReportedId(),
                report.getRecipeId(), report.getReviewedBy());

        return response;
    }

    /**
     * Chuyển đổi ReportProjection sang ReportResponse.
     */
    public ReportResponse toResponse(ReportProjection projection) {
        ReportResponse response = buildBaseResponse(projection);
        
        populateDetailsWithBatch(response, projection.getReporterId(), projection.getReportedId(),
                projection.getRecipeId(), projection.getReviewedBy());

        return response;
    }


    public PageResponse<ReportResponse> toPageResponse(Page<ReportProjection> projections) {
        List<ReportProjection> content = projections.getContent();

        if (content.isEmpty()) {
            return pageMapper.toPageResponse(Collections.emptyList(), projections);
        }

        // Thu thập tất cả IDs cần thiết
        IdCollector ids = collectIds(content);

        // Batch load song song tất cả thông tin liên quan
        BatchData batchData = loadBatchDataParallel(ids);

        // Map từng projection sang response với dữ liệu đã cache
        List<ReportResponse> responses = content.stream()
                .map(proj -> toResponseWithCache(proj, batchData))
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(responses, projections);
    }

    // ==================== TOP REPORTED ITEMS ====================

    /**
     * Chuyển đổi danh sách top reported users.
     * Batch load usernames trong 1 query.
     */
    public List<TopReportedItem> toTopReportedUsers(List<TopReportedProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> userIds = projections.stream()
                .map(TopReportedProjection::getItemId)
                .collect(Collectors.toList());

        Map<UUID, String> usernameMap = reportQueryRepository.findUsernamesByIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UsernameProjection::getUserId,
                        UsernameProjection::getUsername,
                        (existing, replacement) -> existing
                ));

        return projections.stream()
                .map(proj -> TopReportedItem.builder()
                        .itemId(proj.getItemId())
                        .itemName(usernameMap.getOrDefault(proj.getItemId(), "Unknown"))
                        .reportCount(proj.getReportCount())
                        .build())
                .collect(Collectors.toList());
    }

    public List<TopReportedItem> toTopReportedRecipes(List<TopReportedProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> recipeIds = projections.stream()
                .map(TopReportedProjection::getItemId)
                .collect(Collectors.toList());

        // Sử dụng findReportedRecipeInfoByIds thay vì findRecipeTitlesByIds (đã xóa)
        Map<UUID, String> titleMap = reportQueryRepository.findReportedRecipeInfoByIds(recipeIds)
                .stream()
                .collect(Collectors.toMap(
                        ReportedRecipeInfoProjection::getRecipeId,
                        ReportedRecipeInfoProjection::getTitle,
                        (existing, replacement) -> existing
                ));

        return projections.stream()
                .map(proj -> TopReportedItem.builder()
                        .itemId(proj.getItemId())
                        .itemName(titleMap.getOrDefault(proj.getItemId(), "Unknown"))
                        .reportCount(proj.getReportCount())
                        .build())
                .collect(Collectors.toList());
    }

    private ReportResponse buildBaseResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .actionTaken(report.getActionTaken())
                .actionDescription(report.getActionDescription())
                .adminNote(report.getAdminNote())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .reportersNotified(report.getReportersNotified())
                .build();
    }

    /**
     * Build base response từ ReportProjection.
     */
    private ReportResponse buildBaseResponse(ReportProjection projection) {
        return ReportResponse.builder()
                .reportId(projection.getReportId())
                .reportType(projection.getReportType())
                .reason(projection.getReason())
                .description(projection.getDescription())
                .status(projection.getStatus())
                .adminNote(projection.getAdminNote())
                .reviewedAt(projection.getReviewedAt())
                .createdAt(projection.getCreatedAt())
                .build();
    }

    /**
     * Populate details sử dụng batch queries (với danh sách 1 phần tử).
     * Tối ưu hơn so với single-item queries vì dùng chung logic.
     */
    private void populateDetailsWithBatch(ReportResponse response, UUID reporterId,
                                          UUID reportedId, UUID recipeId, UUID reviewedBy) {
        // Tạo lists chứa single item (nếu có)
        List<UUID> reporterIds = reporterId != null ? List.of(reporterId) : Collections.emptyList();
        List<UUID> reportedIds = reportedId != null ? List.of(reportedId) : Collections.emptyList();
        List<UUID> recipeIds = recipeId != null ? List.of(recipeId) : Collections.emptyList();
        List<UUID> reviewerIds = reviewedBy != null ? List.of(reviewedBy) : Collections.emptyList();

        // Batch load song song
        CompletableFuture<Map<UUID, ReporterInfo>> reportersFuture = loadReportersAsync(reporterIds);
        CompletableFuture<Map<UUID, ReportedUserInfoProjection>> reportedUsersFuture = loadReportedUsersAsync(reportedIds);
        CompletableFuture<Map<UUID, ReportedRecipeInfoProjection>> recipesFuture = loadRecipesAsync(recipeIds);
        CompletableFuture<Map<UUID, ReporterInfo>> reviewersFuture = loadReportersAsync(reviewerIds); // Reviewer dùng chung ReporterInfo

        CompletableFuture.allOf(reportersFuture, reportedUsersFuture, recipesFuture, reviewersFuture).join();

        // Set values
        if (reporterId != null) {
            response.setReporter(reportersFuture.join().get(reporterId));
        }
        if (reportedId != null) {
            response.setReportedUser(toReportedUserInfo(reportedUsersFuture.join().get(reportedId)));
        }
        if (recipeId != null) {
            response.setReportedRecipe(toReportedRecipeInfo(recipesFuture.join().get(recipeId)));
        }
        if (reviewedBy != null) {
            ReporterInfo reviewerInfo = reviewersFuture.join().get(reviewedBy);
            if (reviewerInfo != null) {
                response.setReviewer(ReviewerInfo.builder()
                        .userId(reviewerInfo.getUserId())
                        .username(reviewerInfo.getUsername())
                        .avatarUrl(reviewerInfo.getAvatarUrl())
                        .build());
            }
        }
    }

    /**
     * Thu thập tất cả IDs từ danh sách projections.
     */
    private IdCollector collectIds(List<ReportProjection> content) {
        Set<UUID> reporterIds = new HashSet<>();
        Set<UUID> reportedIds = new HashSet<>();
        Set<UUID> recipeIds = new HashSet<>();
        Set<UUID> reviewerIds = new HashSet<>();

        for (ReportProjection proj : content) {
            if (proj.getReporterId() != null) reporterIds.add(proj.getReporterId());
            if (proj.getReportedId() != null) reportedIds.add(proj.getReportedId());
            if (proj.getRecipeId() != null) recipeIds.add(proj.getRecipeId());
            if (proj.getReviewedBy() != null) reviewerIds.add(proj.getReviewedBy());
        }

        return new IdCollector(reporterIds, reportedIds, recipeIds, reviewerIds);
    }

    /**
     * Load tất cả batch data song song.
     */
    private BatchData loadBatchDataParallel(IdCollector ids) {
        CompletableFuture<Map<UUID, ReporterInfo>> reportersFuture = 
                loadReportersAsync(new ArrayList<>(ids.reporterIds));
        CompletableFuture<Map<UUID, ReportedUserInfoProjection>> reportedUsersFuture = 
                loadReportedUsersAsync(new ArrayList<>(ids.reportedIds));
        CompletableFuture<Map<UUID, ReportedRecipeInfoProjection>> recipesFuture = 
                loadRecipesAsync(new ArrayList<>(ids.recipeIds));
        CompletableFuture<Map<UUID, ReporterInfo>> reviewersFuture = 
                loadReportersAsync(new ArrayList<>(ids.reviewerIds));

        CompletableFuture.allOf(reportersFuture, reportedUsersFuture, recipesFuture, reviewersFuture).join();

        return new BatchData(
                reportersFuture.join(),
                reportedUsersFuture.join(),
                recipesFuture.join(),
                reviewersFuture.join()
        );
    }

    /**
     * Async load reporters info.
     */
    private CompletableFuture<Map<UUID, ReporterInfo>> loadReportersAsync(List<UUID> ids) {
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        return CompletableFuture.supplyAsync(() ->
                        reportQueryRepository.findReporterInfoByIds(ids)
                                .stream()
                                .collect(Collectors.toMap(ReporterInfo::getUserId, info -> info, (a, b) -> a)),
                asyncExecutor
        );
    }

    /**
     * Async load reported users info.
     */
    private CompletableFuture<Map<UUID, ReportedUserInfoProjection>> loadReportedUsersAsync(List<UUID> ids) {
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        return CompletableFuture.supplyAsync(() ->
                        reportQueryRepository.findReportedUserInfoByIds(ids)
                                .stream()
                                .collect(Collectors.toMap(ReportedUserInfoProjection::getUserId, info -> info, (a, b) -> a)),
                asyncExecutor
        );
    }

    /**
     * Async load recipes info.
     */
    private CompletableFuture<Map<UUID, ReportedRecipeInfoProjection>> loadRecipesAsync(List<UUID> ids) {
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        return CompletableFuture.supplyAsync(() ->
                        reportQueryRepository.findReportedRecipeInfoByIds(ids)
                                .stream()
                                .collect(Collectors.toMap(ReportedRecipeInfoProjection::getRecipeId, info -> info, (a, b) -> a)),
                asyncExecutor
        );
    }

    /**
     * Map projection sang response với cached data.
     */
    private ReportResponse toResponseWithCache(ReportProjection projection, BatchData batchData) {
        ReportResponse response = buildBaseResponse(projection);

        if (projection.getReporterId() != null) {
            response.setReporter(batchData.reporters.get(projection.getReporterId()));
        }
        if (projection.getReportedId() != null) {
            response.setReportedUser(toReportedUserInfo(batchData.reportedUsers.get(projection.getReportedId())));
        }
        if (projection.getRecipeId() != null) {
            response.setReportedRecipe(toReportedRecipeInfo(batchData.recipes.get(projection.getRecipeId())));
        }
        if (projection.getReviewedBy() != null) {
            ReporterInfo reviewerInfo = batchData.reviewers.get(projection.getReviewedBy());
            if (reviewerInfo != null) {
                response.setReviewer(ReviewerInfo.builder()
                        .userId(reviewerInfo.getUserId())
                        .username(reviewerInfo.getUsername())
                        .avatarUrl(reviewerInfo.getAvatarUrl())
                        .build());
            }
        }

        return response;
    }

    /**
     * Chuyển đổi projection sang ReportedUserInfo DTO.
     */
    private ReportedUserInfo toReportedUserInfo(ReportedUserInfoProjection proj) {
        if (proj == null) return null;
        return ReportedUserInfo.builder()
                .userId(proj.getUserId())
                .username(proj.getUsername())
                .email(proj.getEmail())
                .avatarUrl(proj.getAvatarUrl())
                .role(UserRole.valueOf(proj.getRole()))
                .isActive(proj.getIsActive())
                .build();
    }

    /**
     * Chuyển đổi projection sang ReportedRecipeInfo DTO.
     */
    private ReportedRecipeInfo toReportedRecipeInfo(ReportedRecipeInfoProjection proj) {
        if (proj == null) return null;
        return ReportedRecipeInfo.builder()
                .recipeId(proj.getRecipeId())
                .title(proj.getTitle())
                .slug(proj.getSlug())
                .featuredImage(proj.getFeaturedImage())
                .status(RecipeStatus.valueOf(proj.getStatus()))
                .isPublished(proj.getIsPublished())
                .viewCount(proj.getViewCount())
                .userId(proj.getUserId())
                .authorUsername(proj.getAuthorUsername())
                .build();
    }

    // ==================== INNER CLASSES ====================

    /**
     * Helper class để thu thập IDs.
     */
    private record IdCollector(
            Set<UUID> reporterIds,
            Set<UUID> reportedIds,
            Set<UUID> recipeIds,
            Set<UUID> reviewerIds
    ) {}

    /**
     * Helper class để chứa batch loaded data.
     */
    private record BatchData(
            Map<UUID, ReporterInfo> reporters,
            Map<UUID, ReportedUserInfoProjection> reportedUsers,
            Map<UUID, ReportedRecipeInfoProjection> recipes,
            Map<UUID, ReporterInfo> reviewers
    ) {}
}
