package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.request.AdminUpdateUserRequest;
import com.backend.cookshare.authentication.dto.request.BanUserRequest;
import com.backend.cookshare.authentication.dto.response.AdminUserDetailResponseDTO;
import com.backend.cookshare.authentication.dto.response.AdminUserListResponseDTO;
import com.backend.cookshare.authentication.service.AdminUserService;
import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private UUID userId;
    private AdminUserListResponseDTO userListDTO;
    private AdminUserDetailResponseDTO userDetailDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userListDTO = new AdminUserListResponseDTO();
        userListDTO.setUserId(userId);
        userListDTO.setUsername("testuser");
        userListDTO.setEmail("test@example.com");

        userDetailDTO = new AdminUserDetailResponseDTO();
        userDetailDTO.setUserId(userId);
        userDetailDTO.setUsername("testuser");
        userDetailDTO.setEmail("test@example.com");
    }

    @Test
    void getAllUsers_ShouldReturnPaginatedUsers() {
        // Arrange
        List<AdminUserListResponseDTO> users = Arrays.asList(userListDTO);
        PageResponse<AdminUserListResponseDTO> pageResponse = new PageResponse<>();
        pageResponse.setContent(users);
        pageResponse.setTotalElements(1L);
        pageResponse.setTotalPages(1);

        when(adminUserService.getAllUsersWithPagination(any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AdminUserListResponseDTO>>> response =
                adminUserController.getAllUsers(null, 0, 10, "createdAt", "desc");

        // Assert
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().getTotalElements());
        verify(adminUserService).getAllUsersWithPagination(any(), any(Pageable.class));
    }

    @Test
    void getUserDetail_ShouldReturnUserDetail() {
        // Arrange
        when(adminUserService.getUserDetailById(userId)).thenReturn(userDetailDTO);

        // Act
        ResponseEntity<ApiResponse<AdminUserDetailResponseDTO>> response =
                adminUserController.getUserDetail(userId);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getData().getUserId());
        verify(adminUserService).getUserDetailById(userId);
    }

    @Test
    void banUser_ShouldCallServiceWithReason() {
        // Arrange
        BanUserRequest request = new BanUserRequest();
        request.setReason("Violation");

        doNothing().when(adminUserService).banUser(userId, "Violation");

        // Act
        ResponseEntity<ApiResponse<Void>> response = adminUserController.banUser(userId, request);

        // Assert
        assertTrue(response.getBody().isSuccess());
        verify(adminUserService).banUser(userId, "Violation");
    }

    @Test
    void banUser_ShouldUseDefaultReason_WhenRequestIsNull() {
        // Arrange
        doNothing().when(adminUserService).banUser(userId, "Không có lý do");

        // Act
        ResponseEntity<ApiResponse<Void>> response = adminUserController.banUser(userId, null);

        // Assert
        assertTrue(response.getBody().isSuccess());
        verify(adminUserService).banUser(userId, "Không có lý do");
    }

    @Test
    void unbanUser_ShouldCallService() {
        // Arrange
        doNothing().when(adminUserService).unbanUser(userId);

        // Act
        ResponseEntity<ApiResponse<Void>> response = adminUserController.unbanUser(userId);

        // Assert
        assertTrue(response.getBody().isSuccess());
        verify(adminUserService).unbanUser(userId);
    }

    @Test
    void deleteUser_ShouldCallService() {
        // Arrange
        doNothing().when(adminUserService).deleteUser(userId);

        // Act
        ResponseEntity<ApiResponse<Void>> response = adminUserController.deleteUser(userId);

        // Assert
        assertTrue(response.getBody().isSuccess());
        verify(adminUserService).deleteUser(userId);
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() {
        // Arrange
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername("updatedUser");

        when(adminUserService.updateUser(userId, request)).thenReturn(userDetailDTO);

        // Act
        ResponseEntity<ApiResponse<AdminUserDetailResponseDTO>> response =
                adminUserController.updateUser(userId, request);

        // Assert
        assertNotNull(response.getBody());
        assertEquals(userDetailDTO.getUserId(), response.getBody().getData().getUserId());
        verify(adminUserService).updateUser(userId, request);
    }
}
