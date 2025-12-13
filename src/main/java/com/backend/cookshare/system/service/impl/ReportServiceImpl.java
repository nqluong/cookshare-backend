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
    public BatchReviewResponse batchReviewByRecipe(UUID recipeId, ReviewReportRequest request) {
        String reviewerUsername = getCurrentUsername();
        UUID adminId = getCurrentUserId();

        // Lấy tất cả báo cáo đang chờ xử lý cho công thức này
        List<Report> reports = groupRepository.findReportsByRecipe(recipeId)
                .stream()
                .filter(r -> r.getStatus() == ReportStatus.PENDING)
                .collect(Collectors.toList());

        if (reports.isEmpty()) {
            throw new CustomException(ErrorCode.NO_PENDING_REPORTS);
        }

        // Cập nhật tất cả báo cáo
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

        // Lưu tất cả báo cáo
        reportRepository.saveAll(reports);

        // Thực thi hành động MỘT LẦN (không phải cho mỗi báo cáo)
        // Sử dụng báo cáo đầu tiên làm đại diện
        Report representativeReport = reports.get(0);
        actionExecutor.execute(representativeReport);

        // Đồng bộ tất cả báo cáo liên quan
        synchronizer.syncRelatedReports(representativeReport);

        // Thông báo cho tất cả người báo cáo bất đồng bộ
        notificationOrchestrator.notifyAllReportersAsync(representativeReport);

        // Cập nhật số lượng đang chờ
        broadcastPendingCountUpdate();

        log.info("Đã xem xét hàng loạt {} báo cáo cho công thức {} bởi admin {}",
                reports.size(), recipeId, reviewerUsername);

        return BatchReviewResponse.builder()
                .recipeId(recipeId)
                .totalReportsAffected(reports.size())
                .actionTaken(request.getActionType().toString())
                .status(determinedStatus.toString())
                .reviewedReportIds(reviewedIds)
                .message(String.format("Đã xem xét thành công %d báo cáo và áp dụng hành động: %s",
                        reports.size(), request.getActionType()))
                .build();
    }

    @Override
    public TargetStatisticsResponse getTargetStatistics() {
        // Đếm số công thức bị báo cáo riêng biệt
        long totalReportedRecipes = reportRepository.countDistinctRecipeIds();

        // Đếm số công thức vượt ngưỡng (cần tính điểm trọng số)
        // Đây là phiên bản đơn giản, sử dụng số lượng báo cáo
        long recipesExceedingThreshold = reportRepository.countRecipesExceedingThreshold(5L); // Đếm >= 5 báo cáo

        // Tính trung bình
        double avgReportsPerRecipe = totalReportedRecipes > 0
                ? (double) reportRepository.countByRecipeIdIsNotNull() / totalReportedRecipes
                : 0.0;

        // Phân phối ưu tiên (đơn giản - dựa trên số lượng báo cáo)
        long criticalPriorityTargets = reportRepository.countRecipesWithReportCountGreaterThan(10L);
        long highPriorityTargets = reportRepository.countRecipesWithReportCountBetween(5L, 9L);
        long mediumPriorityTargets = reportRepository.countRecipesWithReportCountBetween(3L, 4L);
        long lowPriorityTargets = reportRepository.countRecipesWithReportCountLessThan(3L);

        return TargetStatisticsResponse.builder()
                .totalReportedRecipes(totalReportedRecipes)
                .recipesExceedingThreshold(recipesExceedingThreshold)
                .avgReportsPerRecipe(avgReportsPerRecipe)
                .criticalPriorityRecipes(criticalPriorityTargets)
                .highPriorityRecipes(highPriorityTargets)
                .mediumPriorityRecipes(mediumPriorityTargets)
                .lowPriorityRecipes(lowPriorityTargets)
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