package com.backend.cookshare.system.service.mapper;

import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.system.dto.response.*;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.*;
import lombok.RequiredArgsConstructor;
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

    public ReportResponse toResponse(Report report) {
        ReportResponse response = ReportResponse.builder()
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

        populateDetails(response, report.getReporterId(), report.getReportedId(),
                report.getRecipeId(), report.getReviewedBy());

        return response;
    }

    public ReportResponse toResponse(ReportProjection projection) {
        ReportResponse response = ReportResponse.builder()
                .reportId(projection.getReportId())
                .reportType(projection.getReportType())
                .reason(projection.getReason())
                .description(projection.getDescription())
                .status(projection.getStatus())
                .adminNote(projection.getAdminNote())
                .reviewedAt(projection.getReviewedAt())
                .createdAt(projection.getCreatedAt())
                .build();

        populateDetails(response, projection.getReporterId(), projection.getReportedId(),
                projection.getRecipeId(), projection.getReviewedBy());

        return response;
    }

    public PageResponse<ReportResponse> toPageResponse(Page<ReportProjection> projections) {
        List<ReportProjection> content = projections.getContent();

        if (content.isEmpty()) {
            return pageMapper.toPageResponse(Collections.emptyList(), projections);
        }

        // Collect IDs
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

        // Batch load parallel
        CompletableFuture<Map<UUID, ReporterInfo>> reportersFuture =
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReporterInfoByIds(new ArrayList<>(reporterIds))
                                        .stream()
                                        .collect(Collectors.toMap(ReporterInfo::getUserId, info -> info)),
                        asyncExecutor
                );

        CompletableFuture<Map<UUID, ReportedUserInfoProjection>> reportedUsersFuture =
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReportedUserInfoByIds(new ArrayList<>(reportedIds))
                                        .stream()
                                        .collect(Collectors.toMap(ReportedUserInfoProjection::getUserId, info -> info)),
                        asyncExecutor
                );

        CompletableFuture<Map<UUID, ReportedRecipeInfoProjection>> recipesFuture =
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReportedRecipeInfoByIds(new ArrayList<>(recipeIds))
                                        .stream()
                                        .collect(Collectors.toMap(ReportedRecipeInfoProjection::getRecipeId, info -> info)),
                        asyncExecutor
                );

        CompletableFuture<Map<UUID, ReviewerInfo>> reviewersFuture =
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReviewerInfoByIds(new ArrayList<>(reviewerIds))
                                        .stream()
                                        .collect(Collectors.toMap(ReviewerInfo::getUserId, info -> info)),
                        asyncExecutor
                );

        CompletableFuture.allOf(reportersFuture, reportedUsersFuture, recipesFuture, reviewersFuture).join();

        Map<UUID, ReporterInfo> reporters = reportersFuture.join();
        Map<UUID, ReportedUserInfoProjection> reportedUsers = reportedUsersFuture.join();
        Map<UUID, ReportedRecipeInfoProjection> recipes = recipesFuture.join();
        Map<UUID, ReviewerInfo> reviewers = reviewersFuture.join();

        List<ReportResponse> responses = content.stream()
                .map(proj -> toResponseWithCache(proj, reporters, reportedUsers, recipes, reviewers))
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(responses, projections);
    }

    private ReportResponse toResponseWithCache(
            ReportProjection projection,
            Map<UUID, ReporterInfo> reporters,
            Map<UUID, ReportedUserInfoProjection> reportedUsers,
            Map<UUID, ReportedRecipeInfoProjection> recipes,
            Map<UUID, ReviewerInfo> reviewers) {

        ReportResponse response = ReportResponse.builder()
                .reportId(projection.getReportId())
                .reportType(projection.getReportType())
                .reason(projection.getReason())
                .description(projection.getDescription())
                .status(projection.getStatus())
                .adminNote(projection.getAdminNote())
                .reviewedAt(projection.getReviewedAt())
                .createdAt(projection.getCreatedAt())
                .build();

        if (projection.getReporterId() != null) {
            response.setReporter(reporters.get(projection.getReporterId()));
        }
        if (projection.getReportedId() != null) {
            response.setReportedUser(toReportedUserInfo(reportedUsers.get(projection.getReportedId())));
        }
        if (projection.getRecipeId() != null) {
            response.setReportedRecipe(toReportedRecipeInfo(recipes.get(projection.getRecipeId())));
        }
        if (projection.getReviewedBy() != null) {
            response.setReviewer(reviewers.get(projection.getReviewedBy()));
        }

        return response;
    }

    private void populateDetails(ReportResponse response, UUID reporterId,
                                 UUID reportedId, UUID recipeId, UUID reviewedBy) {
        CompletableFuture<ReporterInfo> reporterFuture = reporterId != null ?
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReporterInfoById(reporterId).orElse(null),
                        asyncExecutor
                ) : CompletableFuture.completedFuture(null);

        CompletableFuture<ReportedUserInfoProjection> reportedUserFuture = reportedId != null ?
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReportedUserInfoById(reportedId).orElse(null),
                        asyncExecutor
                ) : CompletableFuture.completedFuture(null);

        CompletableFuture<ReportedRecipeInfoProjection> recipeFuture = recipeId != null ?
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReportedRecipeInfoById(recipeId).orElse(null),
                        asyncExecutor
                ) : CompletableFuture.completedFuture(null);

        CompletableFuture<ReviewerInfo> reviewerFuture = reviewedBy != null ?
                CompletableFuture.supplyAsync(() ->
                                reportQueryRepository.findReviewerInfoById(reviewedBy).orElse(null),
                        asyncExecutor
                ) : CompletableFuture.completedFuture(null);

        CompletableFuture.allOf(reporterFuture, reportedUserFuture, recipeFuture, reviewerFuture).join();

        response.setReporter(reporterFuture.join());
        response.setReportedUser(reportedUserFuture.join() != null ?
                toReportedUserInfo(reportedUserFuture.join()) : null);
        response.setReportedRecipe(recipeFuture.join() != null ?
                toReportedRecipeInfo(recipeFuture.join()) : null);
        response.setReviewer(reviewerFuture.join());
    }

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

    public List<TopReportedItem> toTopReportedUsers(List<TopReportedProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> userIds = projections.stream()
                .map(TopReportedProjection::getItemId)
                .collect(Collectors.toSet());

        Map<UUID, String> usernameMap = reportQueryRepository.findUsernamesByIds(new ArrayList<>(userIds))
                .stream()
                .collect(Collectors.toMap(
                        UsernameProjection::getUserId,
                        UsernameProjection::getUsername
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

        Set<UUID> recipeIds = projections.stream()
                .map(TopReportedProjection::getItemId)
                .collect(Collectors.toSet());

        Map<UUID, String> titleMap = reportQueryRepository.findRecipeTitlesByIds(new ArrayList<>(recipeIds))
                .stream()
                .collect(Collectors.toMap(
                        RecipeTitleProjection::getRecipeId,
                        RecipeTitleProjection::getTitle
                ));

        return projections.stream()
                .map(proj -> TopReportedItem.builder()
                        .itemId(proj.getItemId())
                        .itemName(titleMap.getOrDefault(proj.getItemId(), "Unknown"))
                        .reportCount(proj.getReportCount())
                        .build())
                .collect(Collectors.toList());
    }

}
