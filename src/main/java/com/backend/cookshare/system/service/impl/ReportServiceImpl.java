package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.CreateReportRequest;
import com.backend.cookshare.system.dto.request.ReportFilterRequest;
import com.backend.cookshare.system.dto.request.ReviewReportRequest;
import com.backend.cookshare.system.dto.response.*;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.*;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.ReportGroupService;
import com.backend.cookshare.system.service.ReportService;
import com.backend.cookshare.system.service.mapper.ReportMapper;
import com.backend.cookshare.system.service.moderation.ReportAutoModerator;
import com.backend.cookshare.system.service.notification.ReportNotificationOrchestrator;
import com.backend.cookshare.system.service.status.ReportStatusManager;
import com.backend.cookshare.system.service.sync.ReportSynchronizer;
import com.backend.cookshare.system.service.validation.ReportValidator;
import com.backend.cookshare.system.service.action.ReportActionExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportServiceImpl implements ReportService {

    ReportRepository reportRepository;
    ReportQueryRepository reportQueryRepository;
    ReportGroupRepository groupRepository;
    SecurityUtil securityUtil;

    ReportValidator validator;
    ReportMapper mapper;
    ReportStatusManager statusManager;
    ReportActionExecutor actionExecutor;
    ReportSynchronizer synchronizer;
    ReportNotificationOrchestrator notificationOrchestrator;
    ReportAutoModerator autoModerator;
    ReportNotificationService notificationService;
    ReportGroupService reportGroupService;


    /**
     * Tạo báo cáo mới
     */
    @Transactional
    @Override
    public ReportResponse createReport(CreateReportRequest request) {
        String username = getCurrentUsername();
        UUID reporterId = getCurrentUserId();

        // Validate through dedicated validator
        validator.validateCreateRequest(request, reporterId);

        // Create report entity
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedId(request.getReportedId())
                .recipeId(request.getRecipeId())
                .reportType(request.getReportType())
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .reportersNotified(false)
                .build();

        report = reportRepository.save(report);

        // Notify admins
        notificationService.notifyAdminsNewReport(report, username);

        // Check auto-moderation thresholds
        autoModerator.checkAutoModeration(request.getReportedId(), request.getRecipeId());

        // Update pending count
        broadcastPendingCountUpdate();

        log.info("Báo cáo {} đã được tạo bởi người dùng {}", report.getReportId(), username);

        return mapper.toResponse(report);
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

        return mapper.toPageResponse(projections);
    }

    /**
     * Admin xem chi tiết 1 báo cáo
     */
    @Transactional(readOnly = true)
    @Override
    public ReportResponse getReportById(UUID reportId) {
        ReportProjection projection = reportRepository.findProjectionById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        return mapper.toResponse(projection);
    }

    /**
     * Admin phê duyệt/từ chối báo cáo
     */
    @Transactional
    @Override
    public ReportResponse reviewReport(UUID reportId, ReviewReportRequest request) {
        String reviewerUsername = getCurrentUsername();
        UUID adminId = getCurrentUserId();

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_REVIEWED);
        }

        // Update report with action and auto-determined status
        report.setActionTaken(request.getActionType());
        report.setStatus(statusManager.determineStatusFromAction(request.getActionType()));
        report.setAdminNote(request.getAdminNote());
        report.setActionDescription(request.getActionDescription());
        report.setReviewedBy(adminId);
        report.setReviewedAt(LocalDateTime.now());

        report = reportRepository.save(report);

        // Execute the action
        actionExecutor.execute(report);

        // Sync all related reports
        synchronizer.syncRelatedReports(report);

        // Notify all reporters (async)
        notificationOrchestrator.notifyAllReportersAsync(report);

        // Update pending count
        broadcastPendingCountUpdate();

        log.info("Báo cáo {} đã được xem xét bởi quản trị viên {} với hành động {}: {}",
                reportId, reviewerUsername, request.getActionType(), report.getStatus());

        return mapper.toResponse(report);
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
        broadcastPendingCountUpdate();

        log.info("Báo cáo đã bị xóa: {}", reportId);
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
        long resolved = reportRepository.countByStatus(ReportStatus.RESOLVED);

        List<ReportCountProjection> typeProjections = reportRepository.countReportsByType();
        Map<ReportType, Long> reportsByType = typeProjections.stream()
                .collect(Collectors.toMap(
                        ReportCountProjection::getReportType,
                        ReportCountProjection::getCount
                ));

        List<TopReportedProjection> topUsersProjections =
                reportRepository.findTopReportedUsers(PageRequest.of(0, 10));
        List<TopReportedItem> topUsers = mapper.toTopReportedUsers(topUsersProjections);

        List<TopReportedProjection> topRecipesProjections =
                reportRepository.findTopReportedRecipes(PageRequest.of(0, 10));
        List<TopReportedItem> topRecipes = mapper.toTopReportedRecipes(topRecipesProjections);

        return ReportStatisticsResponse.builder()
                .totalReports(total)
                .pendingReports(pending)
                .approvedReports(approved)
                .rejectedReports(rejected)
                .resolvedReports(resolved)
                .reportsByType(reportsByType)
                .topReportedUsers(topUsers)
                .topReportedRecipes(topRecipes)
                .build();
    }

    @Override
    public PageResponse<ReportGroupResponse> getGroupedReports(int page, int size) {
        return reportGroupService.getGroupedReports(page, size);
    }

    @Override
    public ReportGroupDetailResponse getGroupDetail(String targetType, UUID targetId) {
        return reportGroupService.getGroupDetail(targetType, targetId);
    }

    @Override
    public BatchReviewResponse batchReviewByTarget(String targetType, UUID targetId, ReviewReportRequest request) {
        String reviewerUsername = getCurrentUsername();
        UUID adminId = getCurrentUserId();

        // Get all pending reports for this target
        List<Report> reports = groupRepository.findReportsByTarget(targetType, targetId)
                .stream()
                .filter(r -> r.getStatus() == ReportStatus.PENDING)
                .collect(Collectors.toList());

        if (reports.isEmpty()) {
            throw new CustomException(ErrorCode.NO_PENDING_REPORTS);
        }

        // If selectedReportIds is provided, filter only those
//        if (request.getSelectedReportIds() != null && !request.getSelectedReportIds().isEmpty()) {
//            reports = reports.stream()
//                    .filter(r -> request.getSelectedReportIds().contains(r.getReportId()))
//                    .collect(Collectors.toList());
//        }

        // Update all reports
        LocalDateTime reviewTime = LocalDateTime.now();
        ReportStatus determinedStatus = statusManager.determineStatusFromAction(request.getActionType());

        List<UUID> reviewedIds = new ArrayList<>();

        for (Report report : reports) {
            report.setActionTaken(request.getActionType());
            report.setStatus(determinedStatus);
            report.setAdminNote(request.getAdminNote());
            report.setActionDescription(request.getActionDescription());
            report.setReviewedBy(adminId);
            report.setReviewedAt(reviewTime);
            reviewedIds.add(report.getReportId());
        }

        // Save all
        reportRepository.saveAll(reports);

        // Execute action ONCE (not for each report)
        // Use the first report as representative
        Report representativeReport = reports.get(0);
        actionExecutor.execute(representativeReport);

        // Sync all related reports (if target has more reports from other actions)
        synchronizer.syncRelatedReports(representativeReport);

        // Notify all reporters asynchronously
        notificationOrchestrator.notifyAllReportersAsync(representativeReport);

        // Update pending count
        broadcastPendingCountUpdate();

        log.info("Batch reviewed {} reports for {} {} by admin {}",
                reports.size(), targetType, targetId, reviewerUsername);

        return BatchReviewResponse.builder()
                .targetType(targetType)
                .targetId(targetId)
                .totalReportsAffected(reports.size())
                .actionTaken(request.getActionType().toString())
                .status(determinedStatus.toString())
                .reviewedReportIds(reviewedIds)
                .message(String.format("Successfully reviewed %d reports and applied action: %s",
                        reports.size(), request.getActionType()))
                .build();
    }

    @Override
    public TargetStatisticsResponse getTargetStatistics() {
        // Count unique reported recipes
        long totalReportedRecipes = reportRepository.countDistinctRecipeIds();

        // Count unique reported users
        long totalReportedUsers = reportRepository.countDistinctReportedUserIds();

        // Count targets exceeding threshold (need to calculate weighted scores)
        // This is expensive, so we'll use a simplified version
        long recipesExceedingThreshold = reportRepository.countRecipesExceedingThreshold(5L); // Count >= 5 reports
        long usersExceedingThreshold = reportRepository.countUsersExceedingThreshold(10L); // Count >= 10 reports

//        // Count auto-actioned targets
//        long autoUnpublishedRecipes = reportQueryRepository.countAutoUnpublishedRecipes();
//        long autoDisabledUsers = reportQueryRepository.countAutoDisabledUsers();

        // Calculate averages
        double avgReportsPerRecipe = totalReportedRecipes > 0
                ? (double) reportRepository.countByRecipeIdIsNotNull() / totalReportedRecipes
                : 0.0;

        double avgReportsPerUser = totalReportedUsers > 0
                ? (double) reportRepository.countByReportedIdIsNotNull() / totalReportedUsers
                : 0.0;

        // Priority distribution (simplified - based on report count)
        long criticalPriorityTargets = reportRepository.countTargetsWithReportCountGreaterThan(10L);
        long highPriorityTargets = reportRepository.countTargetsWithReportCountBetween(5L, 9L);
        long mediumPriorityTargets = reportRepository.countTargetsWithReportCountBetween(3L, 4L);
        long lowPriorityTargets = reportRepository.countTargetsWithReportCountLessThan(3L);

        return TargetStatisticsResponse.builder()
                .totalReportedRecipes(totalReportedRecipes)
                .totalReportedUsers(totalReportedUsers)
                .recipesExceedingThreshold(recipesExceedingThreshold)
                .usersExceedingThreshold(usersExceedingThreshold)
//                .autoUnpublishedRecipes(autoUnpublishedRecipes)
//                .autoDisabledUsers(autoDisabledUsers)
                .avgReportsPerRecipe(avgReportsPerRecipe)
                .avgReportsPerUser(avgReportsPerUser)
                .criticalPriorityTargets(criticalPriorityTargets)
                .highPriorityTargets(highPriorityTargets)
                .mediumPriorityTargets(mediumPriorityTargets)
                .lowPriorityTargets(lowPriorityTargets)
                .build();
    }

    private void broadcastPendingCountUpdate() {
        long pendingCount = reportRepository.countByStatus(ReportStatus.PENDING);
        notificationService.broadcastPendingCountUpdate(pendingCount);
    }

    private UUID getCurrentUserId() {
        String username = getCurrentUsername();
        return reportQueryRepository.findUserIdByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private String getCurrentUsername() {
        return securityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_AUTHENTICATED));
    }

}