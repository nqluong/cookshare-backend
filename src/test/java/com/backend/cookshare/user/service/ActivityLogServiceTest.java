package com.backend.cookshare.user.service;

import com.backend.cookshare.user.entity.ActivityLog;
import com.backend.cookshare.user.enums.ActivityType;
import com.backend.cookshare.user.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @InjectMocks
    private ActivityLogService activityLogService;

    private UUID userId;
    private UUID targetId;
    private UUID recipeId;
    private UUID commentId;
    private UUID collectionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        recipeId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        collectionId = UUID.randomUUID();
    }

    private void mockRequestContext(String ipAddress, String userAgent, String referrer) {
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn(userAgent);
        when(httpServletRequest.getHeader("Referer")).thenReturn(referrer);
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(ipAddress);
    }

    @Test
    void logCommentActivity_CreateAction_ShouldLogWithCreateType() {
        // Arrange
        String action = "CREATE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCommentActivity(userId, commentId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertNotNull(savedLog);
            assertEquals(userId, savedLog.getUserId());
            assertEquals(commentId, savedLog.getTargetId());
            assertEquals(ActivityType.CREATE, savedLog.getActivityType());
            assertEquals("192.168.1.1", savedLog.getIpAddress());
            assertEquals("Mozilla/5.0", savedLog.getUserAgent());
            assertEquals("http://example.com", savedLog.getReferrer());
        }
    }

    @Test
    void logCommentActivity_UpdateAction_ShouldLogWithUpdateType() {
        // Arrange
        String action = "UPDATE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCommentActivity(userId, commentId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.UPDATE, savedLog.getActivityType());
        }
    }

    @Test
    void logCommentActivity_DeleteAction_ShouldLogWithDeleteType() {
        // Arrange
        String action = "DELETE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCommentActivity(userId, commentId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.DELETE, savedLog.getActivityType());
        }
    }

    @Test
    void logCommentActivity_UnknownAction_ShouldLogWithViewType() {
        // Arrange
        String action = "UNKNOWN";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCommentActivity(userId, commentId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.VIEW, savedLog.getActivityType());
        }
    }

    @Test
    void logLikeActivity_CreateAction_ShouldLogWithCreateType() {
        // Arrange
        String action = "CREATE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logLikeActivity(userId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.CREATE, savedLog.getActivityType());
            assertEquals(recipeId, savedLog.getTargetId());
        }
    }

    @Test
    void logLikeActivity_DeleteAction_ShouldLogWithDeleteType() {
        // Arrange
        String action = "DELETE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logLikeActivity(userId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.DELETE, savedLog.getActivityType());
        }
    }

    @Test
    void logFollowActivity_CreateAction_ShouldLogWithCreateType() {
        // Arrange
        UUID targetUserId = UUID.randomUUID();
        String action = "CREATE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logFollowActivity(userId, targetUserId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.CREATE, savedLog.getActivityType());
            assertEquals(targetUserId, savedLog.getTargetId());
        }
    }

    @Test
    void logFollowActivity_DeleteAction_ShouldLogWithDeleteType() {
        // Arrange
        UUID targetUserId = UUID.randomUUID();
        String action = "DELETE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logFollowActivity(userId, targetUserId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.DELETE, savedLog.getActivityType());
        }
    }

    @Test
    void logViewActivity_ShouldLogWithViewType() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logViewActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.VIEW, savedLog.getActivityType());
            assertEquals(recipeId, savedLog.getTargetId());
        }
    }

    @Test
    void logSearchActivity_ShouldLogWithSearchType() {
        // Arrange
        String searchQuery = "pizza recipe";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logSearchActivity(userId, searchQuery);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.SEARCH, savedLog.getActivityType());
            assertNull(savedLog.getTargetId());
        }
    }

    @Test
    void logAuthActivity_LoginType_ShouldLogSuccessfully() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logAuthActivity(userId, ActivityType.LOGIN);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.LOGIN, savedLog.getActivityType());
            assertNull(savedLog.getTargetId());
        }
    }

    @Test
    void logAuthActivity_LogoutType_ShouldLogSuccessfully() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logAuthActivity(userId, ActivityType.LOGOUT);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.LOGOUT, savedLog.getActivityType());
        }
    }

    @Test
    void logAuthActivity_InvalidType_ShouldNotLog() {
        // Arrange & Act
        activityLogService.logAuthActivity(userId, ActivityType.CREATE);

        // Assert
        verify(activityLogRepository, never()).save(any(ActivityLog.class));
    }

    @Test
    void logShareActivity_ShouldLogWithShareType() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logShareActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.SHARE, savedLog.getActivityType());
            assertEquals(recipeId, savedLog.getTargetId());
        }
    }

    @Test
    void logDownloadActivity_ShouldLogWithDownloadType() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logDownloadActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.DOWNLOAD, savedLog.getActivityType());
            assertEquals(recipeId, savedLog.getTargetId());
        }
    }

    @Test
    void logPrintActivity_ShouldLogWithPrintType() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logPrintActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.PRINT, savedLog.getActivityType());
            assertEquals(recipeId, savedLog.getTargetId());
        }
    }

    @Test
    void logCollectionActivity_CreateAction_ShouldLogWithCreateType() {
        // Arrange
        String action = "CREATE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCollectionActivity(userId, collectionId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.CREATE, savedLog.getActivityType());
            assertEquals(collectionId, savedLog.getTargetId());
        }
    }

    @Test
    void logCollectionActivity_AddRecipeAction_ShouldLogWithCreateType() {
        // Arrange
        String action = "ADD_RECIPE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCollectionActivity(userId, collectionId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.CREATE, savedLog.getActivityType());
        }
    }

    @Test
    void logCollectionActivity_RemoveRecipeAction_ShouldLogWithDeleteType() {
        // Arrange
        String action = "REMOVE_RECIPE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logCollectionActivity(userId, collectionId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.DELETE, savedLog.getActivityType());
        }
    }

    @Test
    void logRecipeActivity_CreateAction_ShouldLogWithCreateType() {
        // Arrange
        String action = "CREATE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logRecipeActivity(userId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.CREATE, savedLog.getActivityType());
            assertEquals(recipeId, savedLog.getTargetId());
        }
    }

    @Test
    void logRecipeActivity_ApproveAction_ShouldLogWithUpdateType() {
        // Arrange
        String action = "APPROVE";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logRecipeActivity(userId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.UPDATE, savedLog.getActivityType());
        }
    }

    @Test
    void logRecipeActivity_RejectAction_ShouldLogWithUpdateType() {
        // Arrange
        String action = "REJECT";
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logRecipeActivity(userId, recipeId, action);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals(ActivityType.UPDATE, savedLog.getActivityType());
        }
    }

    @Test
    void logActivity_WithProxyChainIpAddress_ShouldExtractFirstIp() {
        // Arrange
        mockRequestContext("203.0.113.1, 198.51.100.1, 192.0.2.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logViewActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals("203.0.113.1", savedLog.getIpAddress());
        }
    }

    @Test
    void logActivity_NoRequestContext_ShouldLogWithNullHttpInfo() {
        // Arrange
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(null);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logViewActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertNull(savedLog.getIpAddress());
            assertNull(savedLog.getUserAgent());
            assertNull(savedLog.getReferrer());
        }
    }

    @Test
    void logActivity_RepositoryThrowsException_ShouldNotPropagateException() {
        // Arrange
        mockRequestContext("192.168.1.1", "Mozilla/5.0", "http://example.com");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            when(activityLogRepository.save(any(ActivityLog.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> {
                activityLogService.logViewActivity(userId, recipeId);
            });

            verify(activityLogRepository).save(any(ActivityLog.class));
        }
    }

    @Test
    void logActivity_WithRemoteAddrFallback_ShouldUseRemoteAddr() {
        // Arrange
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        // Mock tất cả IP headers trả về null
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_X_FORWARDED")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_X_CLUSTER_CLIENT_IP")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_FORWARDED_FOR")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_FORWARDED")).thenReturn(null);
        when(httpServletRequest.getHeader("HTTP_VIA")).thenReturn(null);
        when(httpServletRequest.getHeader("REMOTE_ADDR")).thenReturn(null);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getHeader("Referer")).thenReturn("http://example.com");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");

        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(null);

            // Act
            activityLogService.logViewActivity(userId, recipeId);

            // Assert
            verify(activityLogRepository).save(captor.capture());
            ActivityLog savedLog = captor.getValue();

            assertEquals("192.168.1.100", savedLog.getIpAddress());
        }
    }
}