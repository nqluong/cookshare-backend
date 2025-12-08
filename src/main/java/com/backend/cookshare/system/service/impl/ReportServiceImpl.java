package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.system.dto.request.CreateReportRequest;
import com.backend.cookshare.system.dto.request.ReportFilterRequest;
import com.backend.cookshare.system.dto.request.ReviewReportRequest;
import com.backend.cookshare.system.dto.response.*;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.*;
import com.backend.cookshare.system.repository.projection.ReportedUserInfoProjection;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final ReportNotificationService notificationService;
    private final SecurityUtil securityUtil;
    private final PageMapper pageMapper;
    private final ReportQueryRepository reportQueryRepository;

    private static final int AUTO_DISABLE_USER_THRESHOLD = 10;
    private static final int AUTO_UNPUBLISH_RECIPE_THRESHOLD = 5;

    private final Executor asyncExecutor;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             RecipeRepository recipeRepository,
                             ReportNotificationService notificationService,
                             SecurityUtil securityUtil,
                             PageMapper pageMapper,
                             ReportQueryRepository reportQueryRepository,
                             @Qualifier("reportAsyncExecutor") Executor asyncExecutor) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.notificationService = notificationService;
        this.securityUtil = securityUtil;
        this.pageMapper = pageMapper;
        this.reportQueryRepository = reportQueryRepository;
        this.asyncExecutor = asyncExecutor;

    }

    /**
     * Tạo báo cáo mới
     */
    @Transactional
    @Override
    public ReportResponse createReport(CreateReportRequest request) {
        String username = getCurrentUsername();
        UUID reporterId = getCurrentUserId();

        if (request.getReportedId() == null && request.getRecipeId() == null) {
            throw new CustomException(ErrorCode.REPORT_TARGET_REQUIRED);
        }

        if (request.getReportedId() != null && request.getReportedId().equals(reporterId)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_YOURSELF);
        }

        // Kiểm tra đã báo cáo chưa (tránh spam)
        boolean alreadyReported = reportRepository.existsPendingReportByReporter(
                reporterId,
                request.getReportedId(),
                request.getRecipeId()
        );

        if (alreadyReported) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        // Validate reported user exists
        if (request.getReportedId() != null) {
            userRepository.findById(request.getReportedId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORTED_USER_NOT_FOUND));
        }

        // Validate recipe exists
        if (request.getRecipeId() != null) {
            recipeRepository.findById(request.getRecipeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REPORTED_RECIPE_NOT_FOUND));
        }

        // Tạo report
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedId(request.getReportedId())
                .recipeId(request.getRecipeId())
                .reportType(request.getReportType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);

        // Gửi thông báo cho admin qua WebSocket
        notificationService.notifyAdminsNewReport(report, username);

        // Kiểm tra ngưỡng tự động xử lý
        checkAutoModeration(request.getReportedId(), request.getRecipeId());

        // Broadcast pending count update
        long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        notificationService.broadcastPendingCountUpdate(pendingCount);

        log.info("Báo cáo được tạo: {} bởi người dùng {}", report.getReportId(), username);

        return mapEntityToResponse(report);
    }

    /**
     * Admin xem danh sách báo cáo với filter
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getReports(ReportFilterRequest filter) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        Page<ReportProjection> projections = reportRepository.findByFilters(
                filter.getReportType() != null ? filter.getReportType().name() : null,
                filter.getStatus() != null ? filter.getStatus().name() : null,
                filter.getReporterId() != null ? filter.getReporterId().toString() : null,
                filter.getReportedId() != null ? filter.getReportedId().toString() : null,
                filter.getRecipeId() != null ? filter.getRecipeId().toString() : null,
                filter.getFromDate() != null ? filter.getFromDate().toString() : null,
                filter.getToDate() != null ? filter.getToDate().toString() : null,
                pageable
        );

        return batchMapProjectionsToResponses(projections);
    }

    /**
     * Admin xem chi tiết 1 báo cáo
     */
    @Transactional(readOnly = true)
    @Override
    public ReportResponse getReportById(UUID reportId) {
        ReportProjection projection = reportRepository.findProjectionById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        return mapProjectionToResponse(projection);
    }

    /**
     * Admin phê duyệt/từ chối báo cáo
     */
    @Transactional
    @Override
    public ReportResponse reviewReport(UUID reportId, ReviewReportRequest request) {
        // Lấy admin hiện tại từ SecurityUtil
        String reviewerUsername = getCurrentUsername();
        UUID adminId = getCurrentUserId();

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_REVIEWED);
        }

        // Cập nhật report
        report.setStatus(request.getStatus());
        report.setAdminNote(request.getAdminNote());
        report.setReviewedBy(adminId);
        report.setReviewedAt(LocalDateTime.now());

        report = reportRepository.save(report);

        if (request.getStatus() == ReportStatus.APPROVED) {
            executeReportAction(report);
        }
        if (request.getNotifyAllReporters()) {
            notifyAllReportersAsync(report);
        }

        long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        notificationService.broadcastPendingCountUpdate(pendingCount);

        log.info("Báo cáo {} được xem xét bởi admin {}: {}", reportId, reviewerUsername, request.getStatus());

        return mapEntityToResponse(report);
    }



    /**
     * Xóa báo cáo (admin only)
     */
    @Override
    @Transactional
    public void deleteReport(UUID reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }
        reportRepository.deleteById(reportId);

        long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        notificationService.broadcastPendingCountUpdate(pendingCount);

        log.info("Báo cáo đã xóa: {}", reportId);
    }

    /**
     * Lấy thống kê báo cáo
     */
    @Override
    @Transactional(readOnly = true)
    public ReportStatisticsResponse getStatistics() {
        long total = reportRepository.count();
        long pending = reportRepository.countByStatus(ReportStatus.PENDING);
        long approved = reportRepository.countByStatus(ReportStatus.APPROVED);
        long rejected = reportRepository.countByStatus(ReportStatus.REJECTED);

        List<ReportCountProjection> typeProjections = reportRepository.countReportsByType();
        Map<ReportType, Long> reportsByType = typeProjections.stream()
                .collect(Collectors.toMap(
                        ReportCountProjection::getReportType,
                        ReportCountProjection::getCount
                ));

        // Top reported users từ projection
        List<TopReportedProjection> topUsersProjections =
                reportRepository.findTopReportedUsers(PageRequest.of(0, 10));
        List<TopReportedItem> topUsers = buildTopReportedUsers(topUsersProjections);


        // Top reported recipes từ projection
        List<TopReportedProjection> topRecipesProjections =
                reportRepository.findTopReportedRecipes(PageRequest.of(0, 10));
        List<TopReportedItem> topRecipes = buildTopReportedRecipes(topRecipesProjections);

        return ReportStatisticsResponse.builder()
                .totalReports(total)
                .pendingReports(pending)
                .approvedReports(approved)
                .rejectedReports(rejected)
                .reportsByType(reportsByType)
                .topReportedUsers(topUsers)
                .topReportedRecipes(topRecipes)
                .build();
    }

    private void notifyAllReportersAsync(Report reviewedReport) {
        CompletableFuture.runAsync(() -> {
            try {
                notifyAllReporters(reviewedReport);
            } catch (Exception e) {
                log.error("Lỗi khi thông báo cho reporters: {}", e.getMessage(), e);
            }
        }, asyncExecutor);
    }

    private void notifyAllReporters(Report reviewedReport) {
        List<Report> relatedReports;

        // Lấy TẤT CẢ reports cùng target (recipe hoặc user)
        if (reviewedReport.getRecipeId() != null) {
            relatedReports = reportRepository.findAllByRecipeId(reviewedReport.getRecipeId());
        } else if (reviewedReport.getReportedId() != null) {
            relatedReports = reportRepository.findAllByReportedUserId(reviewedReport.getReportedId());
        } else {
            log.warn("Report {} không có target để thông báo", reviewedReport.getReportId());
            return;
        }

        // Lấy unique reporter IDs
        List<UUID> reporterIds = relatedReports.stream()
                .map(Report::getReporterId)
                .distinct()
                .collect(Collectors.toList());

        log.info("Đang thông báo kết quả xử lý report {} tới {} reporters",
                reviewedReport.getReportId(), reporterIds.size());

        // Batch load reporter usernames
        List<UsernameProjection> reporters = reportQueryRepository.findUsernamesByIds(reporterIds);

        // Gửi thông báo cho từng reporter
        for (UsernameProjection reporter : reporters) {
            try {
                notificationService.notifyReporterReviewComplete(
                        reviewedReport,
                        reporter.getUsername(),
                        reporter.getUserId()
                );
            } catch (Exception e) {
                log.error("Không thể gửi thông báo cho reporter {}: {}",
                        reporter.getUserId(), e.getMessage());
            }
        }

        // Mark tất cả related reports là đã thông báo
        relatedReports.forEach(r -> {
            r.setReportersNotified(true);
            r.setStatus(reviewedReport.getStatus());
            r.setActionTaken(reviewedReport.getActionTaken());
            r.setActionDescription(reviewedReport.getActionDescription());
        });

        reportRepository.saveAll(relatedReports);

        log.info("Đã thông báo thành công tới {} reporters", reporters.size());
    }

    private void checkAutoModeration(UUID reportedId, UUID recipeId) {
        if (reportedId != null) {
            long count = reportRepository.countPendingReportsByUserId(reportedId);
            if (count >= AUTO_DISABLE_USER_THRESHOLD) {
                autoDisableUser(reportedId, count);
            }
        }

        if (recipeId != null) {
            long count = reportRepository.countPendingReportsByRecipeId(recipeId);
            if (count >= AUTO_UNPUBLISH_RECIPE_THRESHOLD) {
                autoUnpublishRecipe(recipeId, count);
            }
        }
    }

    /**
     * Tự động disable user khi quá nhiều báo cáo
     */
    private void autoDisableUser(UUID userId, long reportCount) {
        reportQueryRepository.disableUser(userId);

        // Thông báo cho user bị suspend
        String username = reportQueryRepository.findUsernameById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );
        if (username != null) {
            notificationService.notifyAutoDisableUser(userId, username, reportCount);
        }

        log.warn("Tự động vô hiệu hóa người dùng {} do có {} báo cáo đang chờ xử lý", userId, reportCount);
    }

    /**
     * Tự động unpublish recipe khi quá nhiều báo cáo
     */
    private void autoUnpublishRecipe(UUID recipeId, long reportCount) {
        reportQueryRepository.unpublishRecipe(recipeId);
        reportQueryRepository.unpublishRecipe(recipeId);

        // Lấy thông tin recipe và author để thông báo
        ReportedRecipeInfoProjection recipeInfo = reportQueryRepository.findReportedRecipeInfoById(recipeId)
                .orElseThrow(()-> new CustomException(ErrorCode.REPORTED_RECIPE_NOT_FOUND));
        if (recipeInfo != null) {
            notificationService.notifyAutoUnpublishRecipe(
                    recipeId,
                    recipeInfo.getAuthorUsername(),
                    recipeInfo.getTitle(),
                    reportCount
            );
        }

        log.warn("Tự động hủy xuất bản công thức {} do có {} báo cáo đang chờ xử lý", recipeId, reportCount);
    }

    /**
     * Thực hiện hành động khi báo cáo được approved
     */
    private void executeReportAction(Report report) {
        if (report.getReportedId() != null) {
            // Xử lý user bị báo cáo
            String reportedUsername = reportQueryRepository.findUsernameById(report.getReportedId())
                    .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

            // Thông báo cho user bị báo cáo
            if (reportedUsername != null) {
                notificationService.notifyReportedUser(
                        report.getReportedId(),
                        reportedUsername,
                        report.getReportType(),
                        "Đã gửi cảnh báo"
                );
            }

            log.info("Người dùng {} bị báo cáo vì {}", report.getReportedId(), report.getReportType());
        }

        if (report.getRecipeId() != null) {
            // Unpublish recipe
            reportQueryRepository.unpublishRecipe(report.getRecipeId());

            // Thông báo cho recipe author
            reportQueryRepository.findReportedRecipeInfoById(report.getRecipeId()).ifPresent(
                    recipeInfo -> notificationService.notifyRecipeAuthorUnpublished(
                            report.getRecipeId(),
                            recipeInfo.getAuthorUsername(),
                            recipeInfo.getTitle()
            ));

            log.info("Công thức {} đã hủy xuất bản do báo cáo được phê duyệt", report.getRecipeId());
        }
    }



    /**
     * Map Projection sang Response DTO (cho ReportProjection interface)
     */
    private ReportResponse mapProjectionToResponse(ReportProjection projection) {
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

        // Load thông tin từ JDBC helper
        populateReportDetails(response, projection.getReporterId(), projection.getReportedId(),
                projection.getRecipeId(), projection.getReviewedBy());

        return response;
    }

    /**
     * Batch map projections to responses - tối ưu hiệu suất
     */
    private PageResponse<ReportResponse> batchMapProjectionsToResponses(Page<ReportProjection> projections) {
        List<ReportProjection> content = projections.getContent();

        if (content.isEmpty()) {
            return pageMapper.toPageResponse(Collections.emptyList(), projections);
        }

        // Collect all IDs cần load
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

        // Batch load tất cả data song song
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

        // Get results
        Map<UUID, ReporterInfo> reporters = reportersFuture.join();
        Map<UUID, ReportedUserInfoProjection> reportedUsers = reportedUsersFuture.join();
        Map<UUID, ReportedRecipeInfoProjection> recipes = recipesFuture.join();
        Map<UUID, ReviewerInfo> reviewers = reviewersFuture.join();

        List<ReportResponse> responses = content.stream()
                .map(proj -> mapProjectionWithCache(proj, reporters, reportedUsers, recipes, reviewers))
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(responses, projections);
    }

    /**
     * Map projection với cached data
     */
    private ReportResponse mapProjectionWithCache(
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

        // Set cached data
        if (projection.getReporterId() != null) {
            response.setReporter(reporters.get(projection.getReporterId()));
        }
        if (projection.getReportedId() != null) {
            response.setReportedUser(mapToReportedUserInfo(reportedUsers.get(projection.getReportedId())));
        }
        if (projection.getRecipeId() != null) {
            response.setReportedRecipe(mapToReportedRecipeInfo(recipes.get(projection.getRecipeId())));
        }
        if (projection.getReviewedBy() != null) {
            response.setReviewer(reviewers.get(projection.getReviewedBy()));
        }

        return response;
    }

    private List<TopReportedItem> buildTopReportedUsers(List<TopReportedProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch load usernames
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

    private ReportResponse mapEntityToResponse(Report report) {
        ReportResponse response = ReportResponse.builder()
                .reportId(report.getReportId())
                .reportType(report.getReportType())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();

        // Load song song các related info
        populateReportDetails(response, report.getReporterId(), report.getReportedId(),
                report.getRecipeId(), report.getReviewedBy());

        return response;
    }

    /**
     * Build danh sách top reported recipes
     */
    private List<TopReportedItem> buildTopReportedRecipes(List<TopReportedProjection> projections) {
        if (projections == null || projections.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch load recipe titles
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

    private void populateReportDetails(ReportResponse response, UUID reporterId,
                                       UUID reportedId, UUID recipeId, UUID reviewedBy) {

        // Load song song bằng CompletableFuture
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

        // Wait for all
        CompletableFuture.allOf(reporterFuture, reportedUserFuture, recipeFuture, reviewerFuture).join();

        // Set results
        response.setReporter(reporterFuture.join());
        response.setReportedUser(reportedUserFuture.join() != null ?
                mapToReportedUserInfo(reportedUserFuture.join()) : null);
        response.setReportedRecipe(recipeFuture.join() != null ?
                mapToReportedRecipeInfo(recipeFuture.join()) : null);
        response.setReviewer(reviewerFuture.join());
    }

    private UUID getCurrentUserId() {
        String username = securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_AUTHENTICATED));

        return reportQueryRepository.findUserIdByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String getCurrentUsername() {
        return securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_AUTHENTICATED));
    }

    private ReportedRecipeInfo mapToReportedRecipeInfo(ReportedRecipeInfoProjection proj) {
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

    private ReportedUserInfo mapToReportedUserInfo(ReportedUserInfoProjection proj) {
        return ReportedUserInfo.builder()
                .userId(proj.getUserId())
                .username(proj.getUsername())
                .email(proj.getEmail())
                .avatarUrl(proj.getAvatarUrl())
                .role(UserRole.valueOf(proj.getRole()))
                .isActive(proj.getIsActive())
                .build();
    }
}