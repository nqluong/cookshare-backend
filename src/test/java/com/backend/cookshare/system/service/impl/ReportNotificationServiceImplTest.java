package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.request.NotificationMessage;
import com.backend.cookshare.system.dto.response.RecipeInfo;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
import com.backend.cookshare.system.service.notification.builder.NotificationMessageBuilder;
import com.backend.cookshare.system.service.notification.persistence.NotificationPersistenceService;
import com.backend.cookshare.system.service.notification.resolver.ReportTargetResolver;
import com.backend.cookshare.user.enums.NotificationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportNotificationServiceImplTest {

    @Mock
    private ReportQueryRepository queryRepository;

    @Mock
    private NotificationMessageBuilder messageBuilder;

    @Mock
    private NotificationPersistenceService persistenceService;

    @Mock
    private ReportTargetResolver targetResolver;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ReportNotificationServiceImpl notificationService;

    private Report mockReport;
    private UUID recipeId;
    private UUID userId;
    private String reporterUsername = "reporter";
    private String adminUsername = "admin1";

    @BeforeEach
    void setUp() {
        recipeId = UUID.randomUUID();
        userId = UUID.randomUUID();

        mockReport = new Report();
        mockReport.setReportId(UUID.randomUUID());
        mockReport.setRecipeId(recipeId);
        mockReport.setReportedId(userId);
        mockReport.setReportType(ReportType.SPAM);
        mockReport.setStatus(ReportStatus.PENDING);
        mockReport.setActionTaken(ReportActionType.USER_WARNED);
        mockReport.setActionDescription("Please fix content");
    }

    @Test
    @DisplayName("notifyAdminsNewReport - success with multiple admins")
    void notifyAdminsNewReport_success() {
        List<String> admins = List.of("admin1", "admin2");
        when(queryRepository.findAdminUsernames()).thenReturn(admins);
        ReportTargetResolver.ReportTarget mockTarget = mock(ReportTargetResolver.ReportTarget.class);
        when(mockTarget.type()).thenReturn(ReportType.SPAM.name());
        when(mockTarget.name()).thenReturn("Bad Recipe");
        when(targetResolver.resolve(eq(mockReport))).thenReturn(mockTarget);
        NotificationMessage expectedMsg = NotificationMessage.builder().build();
        when(messageBuilder.buildNewReportMessage(eq(mockReport), eq(reporterUsername), any(), any()))
                .thenReturn(expectedMsg);

        notificationService.notifyAdminsNewReport(mockReport, reporterUsername);

        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/notifications"), eq(expectedMsg));
    }

    @Test
    @DisplayName("notifyAdminsNewReport - no admins - early return")
    void notifyAdminsNewReport_noAdmins() {
        when(queryRepository.findAdminUsernames()).thenReturn(List.of());

        notificationService.notifyAdminsNewReport(mockReport, reporterUsername);

        verifyNoInteractions(messageBuilder, messagingTemplate);
    }

    @Test
    @DisplayName("notifyAdminsNewReport - exception - logs error")
    void notifyAdminsNewReport_exception() {
        when(queryRepository.findAdminUsernames()).thenThrow(new RuntimeException("DB error"));

        notificationService.notifyAdminsNewReport(mockReport, reporterUsername);

        verifyNoInteractions(messageBuilder, messagingTemplate);
        // log.error được gọi nhưng không verify trực tiếp (cần logger appender nếu muốn)
    }

    @ParameterizedTest
    @MethodSource("provideReportStatusForReview")
    @DisplayName("notifyReporterReviewComplete - success for different status")
    void notifyReporterReviewComplete_success(ReportStatus status, String expectedPrefix) {
        mockReport.setStatus(status);
        UUID reporterId = UUID.randomUUID();

        NotificationMessage wsMsg = NotificationMessage.builder().title("Reviewed").build();
        when(messageBuilder.buildReportReviewedMessage(eq(mockReport), eq(reporterUsername)))
                .thenReturn(wsMsg);

        notificationService.notifyReporterReviewComplete(mockReport, reporterUsername, reporterId);

        // Verify persistence
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(persistenceService).saveNotification(
                eq(reporterId),
                titleCaptor.capture(),
                messageCaptor.capture(),
                eq(NotificationType.REPORT_REVIEW),
                eq(mockReport.getReportId())
        );
        assertEquals("Kết quả xử lý báo cáo", titleCaptor.getValue());
        assertTrue(messageCaptor.getValue().startsWith(expectedPrefix));

        // Verify WS
        verify(messagingTemplate).convertAndSendToUser(eq(reporterUsername), eq("/queue/notifications"), eq(wsMsg));
    }

    static Stream<Arguments> provideReportStatusForReview() {
        return Stream.of(
                Arguments.of(ReportStatus.REJECTED, "Báo cáo không đủ cơ sở xử lý"),
                Arguments.of(ReportStatus.RESOLVED, "Báo cáo đã được giải quyết"),
                Arguments.of(ReportStatus.PENDING, "Báo cáo đã được xử lý") // default case
        );
    }

    @Test
    @DisplayName("broadcastPendingCountUpdate - success")
    void broadcastPendingCountUpdate_success() {
        List<String> admins = List.of("admin1");
        when(queryRepository.findAdminUsernames()).thenReturn(admins);

        NotificationMessage msg = NotificationMessage.builder().build();
        when(messageBuilder.buildPendingCountUpdateMessage(5L)).thenReturn(msg);

        notificationService.broadcastPendingCountUpdate(5L);

        verify(messagingTemplate).convertAndSendToUser(eq("admin1"), eq("/queue/notifications"), eq(msg));
    }

    @Test
    @DisplayName("notifyAdminsActionCompleted - success with recipe target")
    void notifyAdminsActionCompleted_recipeTarget() {
        List<String> admins = List.of("admin1");
        when(queryRepository.findAdminUsernames()).thenReturn(admins);

        RecipeInfo recipeInfo = new RecipeInfo(recipeId, "Bad Recipe", UUID.randomUUID(), "author", "Author Name");
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(java.util.Optional.of(recipeInfo));

        notificationService.notifyAdminsActionCompleted(mockReport);

        ArgumentCaptor<NotificationMessage> msgCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(messagingTemplate).convertAndSendToUser(eq("admin1"), eq("/queue/notifications"), msgCaptor.capture());

        NotificationMessage sent = msgCaptor.getValue();
        assertEquals("REPORT_ACTION_COMPLETED", sent.getType());
        assertTrue(sent.getMessage().contains("công thức 'Bad Recipe'"));
    }

    @Test
    @DisplayName("notifyUserWarned/Suspended/Banned - success")
    void notifyUserWarned_success() {
        UsernameProjection proj = mock(UsernameProjection.class);
        when(proj.getUsername()).thenReturn("targetUser");
        when(queryRepository.findUsernamesByIds(List.of(userId))).thenReturn(List.of(proj));

        // Sửa ở đây: thêm message không null
        NotificationMessage msg = NotificationMessage.builder()
                .title("Cảnh báo")
                .message("Bạn đã bị cảnh báo vì nội dung vi phạm")  // ← THÊM DÒNG NÀY
                .build();

        when(messageBuilder.buildUserWarningMessage(any(), anyString())).thenReturn(msg);

        notificationService.notifyUserWarned(mockReport, userId);

        verify(persistenceService).saveNotification(
                eq(userId),
                eq("Cảnh báo"),                                   // title
                anyString(),                                      // message (bây giờ không null)
                eq(NotificationType.WARNING),
                eq(mockReport.getReportId())
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("targetUser"),
                eq("/queue/notifications"),
                eq(msg)
        );
    }

    @Test
    @DisplayName("notifyRecipeEditRequired - success")
    void notifyRecipeEditRequired_success() {
        RecipeInfo info = new RecipeInfo(recipeId, "Recipe Title", userId, "authorUser", "Author");
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(Optional.of(info));

        NotificationMessage msg = NotificationMessage.builder()
                .title("Chỉnh sửa công thức yêu cầu")
                .message("Vui lòng chỉnh sửa nội dung: Please fix content")
                .build();

        when(messageBuilder.buildRecipeEditRequiredMessage(
                eq(recipeId),
                eq("Recipe Title"),
                eq("Please fix content")  // vì actionDescription = "Please fix content"
        )).thenReturn(msg);

        notificationService.notifyRecipeEditRequired(mockReport, recipeId);

        verify(persistenceService).saveNotification(
                eq(userId),
                eq("Chỉnh sửa công thức yêu cầu"),     // msg.getTitle()
                contains("Please fix content"),        // msg.getMessage()
                eq(NotificationType.RECIPE_STATUS),
                eq(mockReport.getReportId())
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("authorUser"),
                eq("/queue/notifications"),
                eq(msg)
        );
    }

    @Test
    @DisplayName("notifyContentRemoved - success")
    void notifyContentRemoved_success() {
        RecipeInfo info = new RecipeInfo(recipeId, "Bad Recipe", userId, "author", "Author");
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(Optional.of(info));

        // Mock msg với message không null
        NotificationMessage msg = NotificationMessage.builder()
                .title("Anything")  // title không dùng ở đây, có thể để bất kỳ
                .message("Nội dung vi phạm chính sách: Please fix content")  // ← QUAN TRỌNG: phải có message
                .build();

        when(messageBuilder.buildRecipeUnpublishedMessage(
                eq(recipeId),
                eq("Bad Recipe"),
                eq("Please fix content")  // vì actionDescription = "Please fix content"
        )).thenReturn(msg);

        notificationService.notifyContentRemoved(mockReport, recipeId);

        verify(persistenceService).saveNotification(
                eq(userId),
                eq("Nội dung đã bị xóa"),                     // hard-coded title
                eq("Nội dung vi phạm chính sách: Please fix content"),  // msg.getMessage()
                eq(NotificationType.RECIPE_STATUS),
                eq(mockReport.getReportId())
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("author"),
                eq("/queue/notifications"),
                eq(msg)
        );
    }

    @Test
    @DisplayName("notifyAutoDisableUser & notifyAutoUnpublishRecipe - success")
    void notifyAutoDisableUser_success() {
        // Mock msg với title và message không null
        NotificationMessage msg = NotificationMessage.builder()
                .title("Tài khoản bị vô hiệu hóa tự động")
                .message("Tài khoản của bạn bị vô hiệu hóa do nhận 10 báo cáo vi phạm")
                .build();

        when(messageBuilder.buildAutoDisableUserMessage(10L)).thenReturn(msg);

        notificationService.notifyAutoDisableUser(userId, "disabledUser", 10L);

        verify(persistenceService).saveNotification(
                eq(userId),
                eq("Tài khoản bị vô hiệu hóa tự động"),     // msg.getTitle()
                contains("10 báo cáo"),                     // msg.getMessage()
                eq(NotificationType.ACCOUNT_STATUS),
                isNull()                                    // reportId = null
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("disabledUser"),
                eq("/queue/notifications"),
                eq(msg)
        );
    }

    @Test
    @DisplayName("findUsernameById - user not found throws CustomException")
    void findUsernameById_notFound() throws Exception {
        when(queryRepository.findUsernamesByIds(anyList())).thenReturn(List.of());

        Method method = ReportNotificationServiceImpl.class
                .getDeclaredMethod("findUsernameById", UUID.class);
        method.setAccessible(true);

        // Bắt InvocationTargetException và kiểm tra cause
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(notificationService, userId));

        Throwable cause = ex.getCause();
        assertTrue(cause instanceof CustomException);
        assertEquals(ErrorCode.USER_NOT_FOUND, ((CustomException) cause).getErrorCode());
    }

    @ParameterizedTest
    @MethodSource("provideBuildReviewCompleteMessageCases")
    @DisplayName("buildReviewCompleteMessage - all status cases")
    void buildReviewCompleteMessage_allCases(ReportStatus status, String expectedPrefix) throws Exception {
        mockReport.setStatus(status);
        mockReport.setActionDescription("Custom note");

        Method method = ReportNotificationServiceImpl.class
                .getDeclaredMethod("buildReviewCompleteMessage", Report.class);
        method.setAccessible(true);

        String result = (String) method.invoke(notificationService, mockReport);

        assertTrue(result.startsWith(expectedPrefix));
        assertTrue(result.contains("Custom note"));
    }

    static Stream<Arguments> provideBuildReviewCompleteMessageCases() {
        return Stream.of(
                Arguments.of(ReportStatus.REJECTED, "Báo cáo không đủ cơ sở xử lý. "),
                Arguments.of(ReportStatus.RESOLVED, "Báo cáo đã được giải quyết. "),
                Arguments.of(ReportStatus.PENDING, "Báo cáo đã được xử lý. ")
        );
    }

    @Test
    @DisplayName("buildActionSummary - recipe and user target")
    void buildActionSummary_bothTargets() throws Exception {
        // Recipe target
        RecipeInfo recipeInfo = new RecipeInfo(recipeId, "Test Recipe", userId, "author", "Author");
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(java.util.Optional.of(recipeInfo));

        Method method = ReportNotificationServiceImpl.class
                .getDeclaredMethod("buildActionSummary", Report.class);
        method.setAccessible(true);

        String result1 = (String) method.invoke(notificationService, mockReport);
        assertTrue(result1.contains("công thức 'Test Recipe'"));

        // User target
        mockReport.setRecipeId(null);
        UsernameProjection proj = mock(UsernameProjection.class);
        when(proj.getUsername()).thenReturn("baduser");
        when(queryRepository.findUsernamesByIds(List.of(userId))).thenReturn(List.of(proj));

        String result2 = (String) method.invoke(notificationService, mockReport);
        assertTrue(result2.contains("người dùng 'baduser'"));
    }
    @Test
    void notifyAdminsActionCompleted_recipeNotFound_shouldSendEmptyTarget() {
        when(queryRepository.findAdminUsernames()).thenReturn(List.of("admin"));
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(Optional.empty());

        notificationService.notifyAdminsActionCompleted(mockReport);

        ArgumentCaptor<NotificationMessage> captor =
                ArgumentCaptor.forClass(NotificationMessage.class);

        verify(messagingTemplate).convertAndSendToUser(
                eq("admin"),
                eq("/queue/notifications"),
                captor.capture()
        );

        NotificationMessage msg = captor.getValue();
        assertEquals("REPORT_ACTION_COMPLETED", msg.getType());
        assertTrue(msg.getMessage().contains("Đã thực hiện")); // target rỗng
    }
    @Test
    void notifyAdminsActionCompleted_noRecipeNoUser() {
        mockReport.setRecipeId(null);
        mockReport.setReportedId(null);

        when(queryRepository.findAdminUsernames()).thenReturn(List.of("admin"));

        notificationService.notifyAdminsActionCompleted(mockReport);

        verify(messagingTemplate).convertAndSendToUser(
                eq("admin"),
                eq("/queue/notifications"),
                any(NotificationMessage.class)
        );
    }
    @Test
    void notifyUserSuspended_success() {
        UsernameProjection proj = mock(UsernameProjection.class);
        when(proj.getUsername()).thenReturn("user1");
        when(queryRepository.findUsernamesByIds(List.of(userId))).thenReturn(List.of(proj));

        NotificationMessage msg = NotificationMessage.builder()
                .title("Tạm khóa")
                .message("Bị khóa 7 ngày")
                .build();

        when(messageBuilder.buildUserSuspendedMessage(7)).thenReturn(msg);

        notificationService.notifyUserSuspended(mockReport, userId, 7);

        verify(persistenceService).saveNotification(
                eq(userId),
                eq("Tạm khóa"),
                eq("Bị khóa 7 ngày"),
                eq(NotificationType.ACCOUNT_STATUS),
                eq(mockReport.getReportId())
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/queue/notifications"),
                eq(msg)
        );
    }
    @Test
    void notifyRecipeEditRequired_recipeNotFound_shouldReturn() {
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(Optional.empty());

        notificationService.notifyRecipeEditRequired(mockReport, recipeId);

        verifyNoInteractions(persistenceService, messagingTemplate);
    }
    @Test
    void notifyContentRemoved_recipeNotFound_shouldReturn() {
        when(queryRepository.findRecipeInfoById(recipeId)).thenReturn(Optional.empty());

        notificationService.notifyContentRemoved(mockReport, recipeId);

        verifyNoInteractions(persistenceService, messagingTemplate);
    }
    @Test
    void notifyAutoUnpublishRecipe_success() {
        NotificationMessage msg = NotificationMessage.builder()
                .title("Công thức bị ẩn")
                .message("Bị ẩn do quá nhiều báo cáo")
                .build();

        when(messageBuilder.buildAutoUnpublishRecipeMessage(
                eq(recipeId), eq("Recipe"), eq(5L)
        )).thenReturn(msg);

        notificationService.notifyAutoUnpublishRecipe(
                recipeId, userId, "author", "Recipe", 5L
        );

        verify(persistenceService).saveNotification(
                eq(userId),
                eq("Công thức bị ẩn"),
                eq("Bị ẩn do quá nhiều báo cáo"),
                eq(NotificationType.RECIPE_STATUS),
                isNull()
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq("author"),
                eq("/queue/notifications"),
                eq(msg)
        );
    }
    @Test
    void broadcastPendingCountUpdate_exception() {
        when(queryRepository.findAdminUsernames())
                .thenThrow(new RuntimeException("DB down"));

        notificationService.broadcastPendingCountUpdate(3);

        verifyNoInteractions(messagingTemplate);
    }
    @Test
    void notifyReporterReviewComplete_exception_shouldNotThrow() {
        when(messageBuilder.buildReportReviewedMessage(any(), any()))
                .thenThrow(new RuntimeException("WS error"));

        notificationService.notifyReporterReviewComplete(
                mockReport, reporterUsername, UUID.randomUUID()
        );

        verify(persistenceService, atLeastOnce()).saveNotification(
                any(), any(), any(), any(), any()
        );
    }
}