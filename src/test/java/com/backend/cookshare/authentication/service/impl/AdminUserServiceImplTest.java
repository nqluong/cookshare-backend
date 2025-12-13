package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.AdminUpdateUserRequest;
import com.backend.cookshare.authentication.dto.response.AdminUserDetailResponseDTO;
import com.backend.cookshare.authentication.dto.response.AdminUserListResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.mapper.PageMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PageMapper pageMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private UUID userId;
    private User user;
    private Pageable pageable;
    private Page<User> userPage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);

        user = new User();
        user.setUserId(userId);
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");
        user.setFullName("Test User");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setBio("Test bio");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setGoogleId("google123");
        user.setFacebookId("fb123");
        user.setFollowerCount(10);
        user.setFollowingCount(20);
        user.setRecipeCount(5);
        user.setLastActive(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userPage = new PageImpl<>(List.of(user), pageable, 1);
    }

    @Test
    void getAllUsersWithPagination_ShouldReturnPageResponse() {
        String search = "test";
        PageResponse<AdminUserListResponseDTO> expectedResponse = new PageResponse<>();

        when(userService.getAllUsersWithPagination(search, pageable)).thenReturn(userPage);
        when(pageMapper.toPageResponse(eq(userPage), any(Function.class))).thenReturn(expectedResponse);

        PageResponse<AdminUserListResponseDTO> result = adminUserService.getAllUsersWithPagination(search, pageable);

        assertNotNull(result);
        verify(userService).getAllUsersWithPagination(search, pageable);
        verify(pageMapper).toPageResponse(eq(userPage), any(Function.class));
    }

    @Test
    void getAllUsersWithPagination_WithNullSearch_ShouldWork() {
        PageResponse<AdminUserListResponseDTO> expectedResponse = new PageResponse<>();

        when(userService.getAllUsersWithPagination(null, pageable)).thenReturn(userPage);
        when(pageMapper.toPageResponse(eq(userPage), any(Function.class))).thenReturn(expectedResponse);

        PageResponse<AdminUserListResponseDTO> result = adminUserService.getAllUsersWithPagination(null, pageable);

        assertNotNull(result);
        verify(userService).getAllUsersWithPagination(null, pageable);
    }

    @Test
    void getUserDetailById_ShouldReturnUserDetail() {
        when(userService.getUserDetailById(userId)).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.getUserDetailById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@gmail.com", result.getEmail());
        assertEquals("Test User", result.getFullName());
        assertEquals("https://example.com/avatar.jpg", result.getAvatarUrl());
        assertEquals("Test bio", result.getBio());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getIsActive());
        assertTrue(result.getEmailVerified());
        assertEquals("google123", result.getGoogleId());
        assertEquals("fb123", result.getFacebookId());
        assertEquals(10, result.getFollowerCount());
        assertEquals(20, result.getFollowingCount());
        assertEquals(5, result.getRecipeCount());
        assertNotNull(result.getLastActive());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(userService).getUserDetailById(userId);
    }

    @Test
    void banUser_ShouldCallUserService() {
        String reason = "Spam";

        adminUserService.banUser(userId, reason);

        verify(userService).banUser(userId, reason);
    }

    @Test
    void banUser_WithNullReason_ShouldWork() {
        adminUserService.banUser(userId, null);

        verify(userService).banUser(userId, null);
    }

    @Test
    void unbanUser_ShouldCallUserService() {
        adminUserService.unbanUser(userId);

        verify(userService).unbanUser(userId);
    }

    @Test
    void deleteUser_ShouldCallUserService() {
        adminUserService.deleteUser(userId);

        verify(userService).deleteUserByAdmin(userId);
    }

    @Test
    void updateUser_WithAllFields_ShouldUpdateAndReturn() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername("newusername");
        request.setEmail("newemail@gmail.com");
        request.setFullName("New Full Name");
        request.setAvatarUrl("https://new-avatar.jpg");
        request.setBio("New bio");
        request.setRole(UserRole.ADMIN);
        request.setIsActive(false);
        request.setEmailVerified(false);

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.existsByUsername("newusername")).thenReturn(false);
        when(userService.existsByEmail("newemail@gmail.com")).thenReturn(false);
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals("newusername", user.getUsername());
        assertEquals("newemail@gmail.com", user.getEmail());
        assertEquals("New Full Name", user.getFullName());
        assertEquals("https://new-avatar.jpg", user.getAvatarUrl());
        assertEquals("New bio", user.getBio());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertFalse(user.getIsActive());
        assertFalse(user.getEmailVerified());

        verify(userService).getUserById(userId);
        verify(userService).existsByUsername("newusername");
        verify(userService).existsByEmail("newemail@gmail.com");
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithSameUsername_ShouldNotCheckExistence() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername("testuser"); // Same as current username

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        verify(userService).getUserById(userId);
        verify(userService, never()).existsByUsername(anyString());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithSameEmail_ShouldNotCheckExistence() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("test@gmail.com"); // Same as current email

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        verify(userService).getUserById(userId);
        verify(userService, never()).existsByEmail(anyString());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithDuplicateUsername_ShouldThrowException() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername("existinguser");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.existsByUsername("existinguser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                adminUserService.updateUser(userId, request));

        assertEquals("Username đã tồn tại", exception.getMessage());
        verify(userService).getUserById(userId);
        verify(userService).existsByUsername("existinguser");
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void updateUser_WithDuplicateEmail_ShouldThrowException() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("existing@gmail.com");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.existsByEmail("existing@gmail.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                adminUserService.updateUser(userId, request));

        assertEquals("Email đã tồn tại", exception.getMessage());
        verify(userService).getUserById(userId);
        verify(userService).existsByEmail("existing@gmail.com");
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void updateUser_WithNonExistentUser_ShouldThrowException() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();

        when(userService.getUserById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                adminUserService.updateUser(userId, request));

        assertEquals("Không tìm thấy người dùng với ID: " + userId, exception.getMessage());
        verify(userService).getUserById(userId);
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void updateUser_WithNullFields_ShouldNotUpdate() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        // All fields are null

        String originalUsername = user.getUsername();
        String originalEmail = user.getEmail();
        String originalFullName = user.getFullName();
        String originalAvatarUrl = user.getAvatarUrl();
        String originalBio = user.getBio();
        UserRole originalRole = user.getRole();
        Boolean originalIsActive = user.getIsActive();
        Boolean originalEmailVerified = user.getEmailVerified();

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals(originalUsername, user.getUsername());
        assertEquals(originalEmail, user.getEmail());
        assertEquals(originalFullName, user.getFullName());
        assertEquals(originalAvatarUrl, user.getAvatarUrl());
        assertEquals(originalBio, user.getBio());
        assertEquals(originalRole, user.getRole());
        assertEquals(originalIsActive, user.getIsActive());
        assertEquals(originalEmailVerified, user.getEmailVerified());

        verify(userService).getUserById(userId);
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyUsername_ShouldUpdateOnlyUsername() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setUsername("newusername");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.existsByUsername("newusername")).thenReturn(false);
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals("newusername", user.getUsername());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyEmail_ShouldUpdateOnlyEmail() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("newemail@gmail.com");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.existsByEmail("newemail@gmail.com")).thenReturn(false);
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals("newemail@gmail.com", user.getEmail());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyFullName_ShouldUpdateOnlyFullName() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("New Name");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals("New Name", user.getFullName());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyAvatarUrl_ShouldUpdateOnlyAvatarUrl() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setAvatarUrl("https://new-avatar.jpg");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals("https://new-avatar.jpg", user.getAvatarUrl());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyBio_ShouldUpdateOnlyBio() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setBio("New bio");

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals("New bio", user.getBio());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyRole_ShouldUpdateOnlyRole() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(UserRole.ADMIN);

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertEquals(UserRole.ADMIN, user.getRole());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyIsActive_ShouldUpdateOnlyIsActive() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setIsActive(false);

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertFalse(user.getIsActive());
        verify(userService).updateUser(user);
    }

    @Test
    void updateUser_WithOnlyEmailVerified_ShouldUpdateOnlyEmailVerified() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmailVerified(false);

        when(userService.getUserById(userId)).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        AdminUserDetailResponseDTO result = adminUserService.updateUser(userId, request);

        assertNotNull(result);
        assertFalse(user.getEmailVerified());
        verify(userService).updateUser(user);
    }
}