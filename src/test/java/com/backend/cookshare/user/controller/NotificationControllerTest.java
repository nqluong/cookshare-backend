package com.backend.cookshare.user.controller;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.user.dto.NotificationResponse;
import com.backend.cookshare.user.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationController notificationController;

    @Mock
    private Authentication authentication;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();

    // Tạo User giả lập
    private User createUser() {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("testuser");
        return user;
    }

    // Tạo NotificationResponse giả lập
    private NotificationResponse createNotificationResponse() {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notificationId);
        response.setTitle("Thông báo mới");
        response.setMessage("Nội dung thông báo");
        response.setIsRead(false);
        return response;
    }

    // Tạo PageResponse<NotificationResponse> đúng kiểu
    private PageResponse<NotificationResponse> createNotificationPageResponse() {
        PageResponse<NotificationResponse> page = new PageResponse<>();
        page.setContent(Collections.singletonList(createNotificationResponse()));
        page.setPage(0);
        page.setSize(20);
        page.setTotalPages(1);
        return page;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(createUser()));
    }

//    @Test
//    void getNotifications_Success() throws Exception {
//        Page<NotificationResponse> page = new PageImpl<>(Collections.singletonList(createNotificationResponse()));
//
//        when(notificationService.getUserNotifications(eq(userId), anyInt(), anyInt()))
//                .thenReturn(page);
//
//        mockMvc.perform(get("/notifications")
//                        .param("page", "0")
//                        .param("size", "20")
//                        .principal(authentication))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content[0].notificationId").value(notificationId.toString()));
//
//        verify(notificationService).getUserNotifications(eq(userId), eq(0), eq(20));
//    }

    @Test
    void getUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread-count").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));

        verify(notificationService).getUnreadCount(userId);
    }

    @Test
    void markAsRead_Success() throws Exception {
        NotificationResponse response = createNotificationResponse();
        when(notificationService.markAsRead(notificationId, userId)).thenReturn(response);

        mockMvc.perform(put("/notifications/{notificationId}/read", notificationId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notificationId.toString()));

        verify(notificationService).markAsRead(notificationId, userId);
    }

    @Test
    void markAllAsRead_Success() throws Exception {
        doNothing().when(notificationService).markAllAsRead(userId);

        mockMvc.perform(put("/notifications/read-all").principal(authentication))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(userId);
    }

    @Test
    void deleteNotification_Success() throws Exception {
        doNothing().when(notificationService).deleteNotification(notificationId, userId);

        mockMvc.perform(delete("/notifications/{notificationId}", notificationId)
                        .principal(authentication))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(notificationId, userId);
    }
}
