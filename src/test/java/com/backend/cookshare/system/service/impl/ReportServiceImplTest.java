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
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.*;
import com.backend.cookshare.system.service.ReportGroupService;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.action.ReportActionExecutor;
import com.backend.cookshare.system.service.mapper.ReportMapper;
import com.backend.cookshare.system.service.moderation.ReportAutoModerator;
import com.backend.cookshare.system.service.notification.ReportNotificationOrchestrator;
import com.backend.cookshare.system.service.status.ReportStatusManager;
import com.backend.cookshare.system.service.sync.ReportSynchronizer;
import com.backend.cookshare.system.service.validation.ReportValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock ReportRepository reportRepository;
    @Mock ReportQueryRepository reportQueryRepository;
    @Mock ReportGroupRepository groupRepository;
    @Mock SecurityUtil securityUtil;
    @Mock ReportValidator validator;
    @Mock ReportMapper mapper;
    @Mock ReportStatusManager statusManager;
    @Mock ReportActionExecutor actionExecutor;
    @Mock ReportSynchronizer synchronizer;
    @Mock ReportNotificationOrchestrator notificationOrchestrator;
    @Mock ReportAutoModerator autoModerator;
    @Mock ReportNotificationService notificationService;
    @Mock ReportGroupService reportGroupService;

    @InjectMocks
    ReportServiceImpl service;

    UUID userId = UUID.randomUUID();
    UUID reportId = UUID.randomUUID();
    UUID recipeId = UUID.randomUUID();
    String username = "admin";

    Report report;

    @BeforeEach
    void setup() {
        report = Report.builder()
                .reportId(reportId)
                .reporterId(userId)
                .recipeId(recipeId)
                .reportType(ReportType.SPAM)
                .status(ReportStatus.PENDING)
                .build();
    }

    /* ======================= createReport ======================= */

    @Test
    void createReport_success() {
        CreateReportRequest req = new CreateReportRequest();
        req.setReportType(ReportType.SPAM);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {

            mocked.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(reportQueryRepository.findUserIdByUsername(username))
                    .thenReturn(Optional.of(userId));

            when(reportRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            doReturn(new ReportResponse()) .when(mapper) .toResponse(any(Report.class));

            ReportResponse res = service.createReport(req);

            assertNotNull(res);

            verify(validator).validateCreateRequest(eq(req), eq(userId));
            verify(notificationService).notifyAdminsNewReport(any(), eq(username));
            verify(autoModerator).checkAutoModeration(any(), any());
            verify(notificationService).broadcastPendingCountUpdate(anyLong());
        }
    }

    @Test
    void createReport_notAuthenticated() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {

            mocked.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.empty());

            CustomException ex = assertThrows(
                    CustomException.class,
                    () -> service.createReport(new CreateReportRequest())
            );

            assertEquals(ErrorCode.USER_NOT_AUTHENTICATED, ex.getErrorCode());
        }
    }

    /* ======================= getReports ======================= */

    @Test
    void getReports_success() {
        ReportFilterRequest filter = new ReportFilterRequest();
        Page<ReportProjection> page =
                new PageImpl<>(List.of(mock(ReportProjection.class)));

        when(reportRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        when(mapper.toPageResponse(page)).thenReturn(PageResponse.<ReportResponse>builder().build());

        PageResponse<ReportResponse> res = service.getReports(filter);

        assertNotNull(res);
    }

    /* ======================= getReportById ======================= */

    @Test
    void getReportById_success() {
        ReportProjection projection = mock(ReportProjection.class);

        when(reportRepository.findProjectionById(reportId)).thenReturn(Optional.of(projection));
        when(mapper.toResponse(projection)).thenReturn(new ReportResponse());

        assertNotNull(service.getReportById(reportId));
    }

    @Test
    void getReportById_notFound() {
        when(reportRepository.findProjectionById(reportId)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.getReportById(reportId));

        assertEquals(ErrorCode.REPORT_NOT_FOUND, ex.getErrorCode());
    }

    /* ======================= reviewReport ======================= */

    @Test
    void reviewReport_success() {
        ReviewReportRequest req = new ReviewReportRequest();
        req.setActionType(ReportActionType.USER_WARNED);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {

            // mock static security
            mocked.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(reportQueryRepository.findUserIdByUsername(username))
                    .thenReturn(Optional.of(userId));

            when(reportRepository.findById(reportId))
                    .thenReturn(Optional.of(report));

            when(statusManager.determineStatusFromAction(any()))
                    .thenReturn(ReportStatus.RESOLVED);

            when(reportRepository.save(any(Report.class)))
                    .thenReturn(report);

            doReturn(new ReportResponse())
                    .when(mapper)
                    .toResponse(any(Report.class));

            ReportResponse res = service.reviewReport(reportId, req);

            assertNotNull(res);

            verify(actionExecutor).execute(report);
            verify(synchronizer).syncRelatedReports(report);
            verify(notificationOrchestrator).notifyAllReportersAsync(report);
            verify(notificationService).broadcastPendingCountUpdate(anyLong());
        }
    }


    @Test
    void reviewReport_alreadyReviewed() {
        report.setStatus(ReportStatus.RESOLVED);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {

            mocked.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(reportQueryRepository.findUserIdByUsername(username))
                    .thenReturn(Optional.of(userId));

            when(reportRepository.findById(reportId))
                    .thenReturn(Optional.of(report));

            CustomException ex = assertThrows(
                    CustomException.class,
                    () -> service.reviewReport(reportId, new ReviewReportRequest())
            );

            assertEquals(ErrorCode.REPORT_ALREADY_REVIEWED, ex.getErrorCode());
        }
    }

    /* ======================= deleteReport ======================= */

    @Test
    void deleteReport_success() {
        when(reportRepository.existsById(reportId)).thenReturn(true);

        service.deleteReport(reportId);

        verify(reportRepository).deleteById(reportId);
        verify(notificationService).broadcastPendingCountUpdate(anyLong());
    }

    @Test
    void deleteReport_notFound() {
        when(reportRepository.existsById(reportId)).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.deleteReport(reportId));

        assertEquals(ErrorCode.REPORT_NOT_FOUND, ex.getErrorCode());
    }

    /* ======================= statistics ======================= */

    @Test
    void getStatistics_success() {
        when(reportRepository.count()).thenReturn(10L);
        when(reportRepository.countByStatus(any())).thenReturn(5L);
        when(reportRepository.countReportsByType()).thenReturn(List.of());

        ReportStatisticsResponse res = service.getStatistics();

        assertEquals(10L, res.getTotalReports());
    }

    @Test
    void getTargetStatistics_success() {
        when(reportRepository.countDistinctRecipeIds()).thenReturn(5L);
        when(reportRepository.countByRecipeIdIsNotNull()).thenReturn(20L);
        when(reportRepository.countRecipesExceedingThreshold(anyLong())).thenReturn(2L);
        when(reportRepository.countRecipesWithReportCountGreaterThan(anyLong())).thenReturn(1L);
        when(reportRepository.countRecipesWithReportCountBetween(anyLong(), anyLong())).thenReturn(1L);
        when(reportRepository.countRecipesWithReportCountLessThan(anyLong())).thenReturn(2L);

        TargetStatisticsResponse res = service.getTargetStatistics();

        assertEquals(5L, res.getTotalReportedRecipes());
    }

    /* ======================= batchReview ======================= */

    @Test
    void batchReview_success() {
        ReviewReportRequest req = new ReviewReportRequest();
        req.setActionType(ReportActionType.RECIPE_UNPUBLISHED);

        report.setStatus(ReportStatus.PENDING);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {

            mocked.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(reportQueryRepository.findUserIdByUsername(username))
                    .thenReturn(Optional.of(userId));

            when(groupRepository.findReportsByRecipe(recipeId))
                    .thenReturn(List.of(report));

            when(statusManager.determineStatusFromAction(any()))
                    .thenReturn(ReportStatus.RESOLVED);

            when(reportRepository.saveAll(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            BatchReviewResponse res = service.batchReviewByRecipe(recipeId, req);

            assertNotNull(res);
            assertEquals(1, res.getTotalReportsAffected());

            verify(actionExecutor).execute(any());
            verify(synchronizer).syncRelatedReports(any());
            verify(notificationOrchestrator).notifyAllReportersAsync(any());
            verify(notificationService).broadcastPendingCountUpdate(anyLong());
        }
    }



    @Test
    void batchReview_noPending() {
        report.setStatus(ReportStatus.RESOLVED);

        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {

            mocked.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(reportQueryRepository.findUserIdByUsername(username))
                    .thenReturn(Optional.of(userId));

            when(groupRepository.findReportsByRecipe(recipeId))
                    .thenReturn(List.of(report));

            CustomException ex = assertThrows(
                    CustomException.class,
                    () -> service.batchReviewByRecipe(recipeId, new ReviewReportRequest())
            );

            assertEquals(ErrorCode.NO_PENDING_REPORTS, ex.getErrorCode());
        }
    }


}
