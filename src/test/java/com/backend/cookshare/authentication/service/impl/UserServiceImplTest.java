package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.AvatarUploadUrlRequest;
import com.backend.cookshare.authentication.dto.request.UpdateUserProfileRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.AvatarUploadUrlResponse;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;
    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        username = "testuser";
        email = "test@example.com";

        user = User.builder()
                .userId(userId)
                .username(username)
                .passwordHash("encodedPassword")
                .email(email)
                .fullName("Test User")
                .avatarUrl("avatar.jpg")
                .bio("Test bio")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastActive(LocalDateTime.now())
                .followerCount(0)
                .followingCount(0)
                .recipeCount(0)
                .build();
    }

    @Test
    void createUser_ShouldCreateSuccessfully() {
        // Arrange
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setPassword("password123");
        request.setEmail(email);
        request.setFullname("Test User");

        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        String result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("Ban da dang ky thanh cong!", result);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(request.getPassword());
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserByUsername_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserByUsername(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserByEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserByUsernameOrEmail_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserByUsernameOrEmail(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsernameOrEmail(username);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(user);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void updateUser_ShouldUpdateSuccessfully() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userService.updateUser(user);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_ShouldDeleteSuccessfully() {
        // Arrange
        doNothing().when(userRepository).deleteById(userId);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).deleteById(userId);
    }

    @Test
    void existsByUsername_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act
        boolean result = userService.existsByUsername(username);

        // Assert
        assertTrue(result);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertTrue(result);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void updateLastActive_ShouldUpdateSuccessfully() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.updateLastActive(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserToken_ShouldUpdateSuccessfully() {
        // Arrange
        String token = "refreshToken123";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.updateUserToken(token, username);

        // Assert
        verify(userRepository).findByUsername(username);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserToken_UserNotFound_ShouldThrowException() {
        // Arrange
        String token = "refreshToken123";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserToken(token, username);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByRefreshTokenAndUsername_ShouldReturnUser() {
        // Arrange
        String token = "refreshToken123";
        when(userRepository.findByRefreshTokenAndUsername(token, username)).thenReturn(user);

        // Act
        User result = userService.getUserByRefreshTokenAndUsername(token, username);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(userRepository).findByRefreshTokenAndUsername(token, username);
    }

    @Test
    void changePassword_ShouldChangeSuccessfully() {
        // Arrange
        String currentPassword = "oldPassword";
        String newPassword = "newPassword";

        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, user.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.changePassword(username, currentPassword, newPassword);

        // Assert
        verify(userRepository).save(user);
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void changePassword_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.changePassword(username, "oldPass", "newPass");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void changePassword_InvalidCurrentPassword_ShouldThrowException() {
        // Arrange
        String currentPassword = "wrongPassword";
        String newPassword = "newPassword";

        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, user.getPasswordHash())).thenReturn(false);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.changePassword(username, currentPassword, newPassword);
        });

        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_SamePassword_ShouldThrowException() {
        // Arrange
        String currentPassword = "password";
        String newPassword = "password";

        when(userRepository.findByUsernameOrEmail(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(currentPassword, user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, user.getPasswordHash())).thenReturn(true);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.changePassword(username, currentPassword, newPassword);
        });

        assertEquals(ErrorCode.SAME_PASSWORD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void generateAvatarUploadUrl_ShouldGenerateSuccessfully() {
        // Arrange
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest();
        request.setFileName("avatar.jpg");
        request.setContentType("image/jpeg");

        String uploadUrl = "https://storage.googleapis.com/upload-url";
        String publicUrl = "https://storage.googleapis.com/public-url";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(firebaseStorageService.isInitialized()).thenReturn(true);
        when(firebaseStorageService.generateAvatarUploadUrl(request.getFileName(), request.getContentType()))
                .thenReturn(uploadUrl);
        when(firebaseStorageService.getAvatarPublicUrl(request.getFileName())).thenReturn(publicUrl);

        // Act
        AvatarUploadUrlResponse result = userService.generateAvatarUploadUrl(userId, request);

        // Assert
        assertNotNull(result);
        assertEquals(uploadUrl, result.getUploadUrl());
        assertEquals(publicUrl, result.getPublicUrl());
        verify(firebaseStorageService).generateAvatarUploadUrl(request.getFileName(), request.getContentType());
    }

    @Test
    void generateAvatarUploadUrl_UserNotFound_ShouldThrowException() {
        // Arrange
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest();
        request.setFileName("avatar.jpg");
        request.setContentType("image/jpeg");

        when(firebaseStorageService.isInitialized()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.generateAvatarUploadUrl(userId, request);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void generateAvatarUploadUrl_InvalidContentType_ShouldThrowException() {
        // Arrange
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest();
        request.setFileName("document.pdf");
        request.setContentType("application/pdf");

        when(firebaseStorageService.isInitialized()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.generateAvatarUploadUrl(userId, request);
        });

        assertEquals("Chỉ chấp nhận file ảnh", exception.getMessage());
    }

    @Test
    void generateAvatarUploadUrl_InvalidFileExtension_ShouldThrowException() {
        // Arrange
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest();
        request.setFileName("avatar.txt");
        request.setContentType("image/jpeg");

        when(firebaseStorageService.isInitialized()).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.generateAvatarUploadUrl(userId, request);
        });

        assertTrue(exception.getMessage().contains("Phần mở rộng file không hợp lệ"));
    }

    @Test
    void generateAvatarUploadUrl_FirebaseNotInitialized_ShouldThrowException() {
        // Arrange
        AvatarUploadUrlRequest request = new AvatarUploadUrlRequest();
        request.setFileName("avatar.jpg");
        request.setContentType("image/jpeg");

        when(firebaseStorageService.isInitialized()).thenReturn(false);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.generateAvatarUploadUrl(userId, request);
        });

        assertTrue(exception.getMessage().contains("Firebase Storage chưa được cấu hình"));
    }

    @Test
    void getAllUsersWithPagination_ShouldReturnPageOfUsers() {
        // Arrange
        String search = "test";
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAllWithSearch(search, pageable)).thenReturn(userPage);

        // Act
        Page<User> result = userService.getAllUsersWithPagination(search, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAllWithSearch(search, pageable);
    }

    @Test
    void getUserDetailById_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserDetailById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserDetailById_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.getUserDetailById(userId);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void banUser_ShouldBanSuccessfully() {
        // Arrange
        String reason = "Violation of terms";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // Act
        userService.banUser(userId, reason);

        // Assert
        verify(userRepository).save(user);
        verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/account-status"),
                any(Map.class)
        );
    }

    @Test
    void banUser_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.banUser(userId, "Reason");
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void banUser_AlreadyBanned_ShouldThrowException() {
        // Arrange
        user.setIsActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.banUser(userId, "Reason");
        });

        assertEquals(ErrorCode.USER_ALREADY_BANNED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void unbanUser_ShouldUnbanSuccessfully() {
        // Arrange
        user.setIsActive(false);
        user.setBannedAt(LocalDateTime.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.unbanUser(userId);

        // Assert
        verify(userRepository).save(user);
    }

    @Test
    void unbanUser_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.unbanUser(userId);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void unbanUser_AlreadyActive_ShouldThrowException() {
        // Arrange
        user.setIsActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.unbanUser(userId);
        });

        assertEquals(ErrorCode.USER_ALREADY_ACTIVE, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUserByAdmin_ShouldDeleteSuccessfully() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        // Act
        userService.deleteUserByAdmin(userId);

        // Assert
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserByAdmin_UserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.deleteUserByAdmin(userId);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateUserProfile_ShouldUpdateSuccessfully() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFullName("Updated Name");
        request.setBio("Updated bio");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_UnauthorizedUser_ShouldThrowException() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFullName("Updated Name");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.updateUserProfile(differentUserId, request);
            });

            assertEquals(ErrorCode.UNAUTHORIZED_UPDATE, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updateUserProfile_UpdateUsername_ShouldCheckExisting() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setUsername("newusername");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("newusername")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository).existsByUsername("newusername");
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_UsernameExists_ShouldThrowException() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setUsername("existinguser");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.updateUserProfile(userId, request);
            });

            assertEquals(ErrorCode.USERNAME_EXISTED, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updateUserProfile_UpdateEmail_ShouldCheckExisting() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail("newemail@example.com");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository).existsByEmail("newemail@example.com");
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_EmailExists_ShouldThrowException() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail("existing@example.com");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.updateUserProfile(userId, request);
            });

            assertEquals(ErrorCode.EMAIL_EXISTED, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void updateUserProfile_WithNewAvatar_ShouldDeleteOldAvatar() {
        // Arrange
        String oldAvatarUrl = "https://firebase.storage/old-avatar.jpg";
        String newAvatarUrl = "https://firebase.storage/new-avatar.jpg";
        user.setAvatarUrl(oldAvatarUrl);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl(newAvatarUrl);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            // deleteAvatar returns boolean but we don't need to capture it
            when(firebaseStorageService.deleteAvatar(oldAvatarUrl)).thenReturn(true);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(firebaseStorageService).deleteAvatar(oldAvatarUrl);
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_UserNotFoundInSecurity_ShouldThrowException() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFullName("Updated Name");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.empty());

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.updateUserProfile(userId, request);
            });

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Test
    void updateUserProfile_UserNotFoundInRepository_ShouldThrowException() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFullName("Updated Name");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.updateUserProfile(userId, request);
            });

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }
    }

    @Test
    void updateLastActive_UserNotFound_ShouldNotUpdate() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        userService.updateLastActive(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserProfile_SameUsername_ShouldNotCheckExisting() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setUsername(username); // Same username

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_SameEmail_ShouldNotCheckExisting() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail(email); // Same email

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_NullAvatarUrl_ShouldNotDeleteOldAvatar() {
        // Arrange
        String oldAvatarUrl = "https://firebase.storage/old-avatar.jpg";
        user.setAvatarUrl(oldAvatarUrl);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl(null); // Null avatar

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(firebaseStorageService, never()).deleteAvatar(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_EmptyOldAvatarUrl_ShouldNotDelete() {
        // Arrange
        user.setAvatarUrl(""); // Empty avatar

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("https://firebase.storage/new-avatar.jpg");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(firebaseStorageService, never()).deleteAvatar(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_NullOldAvatarUrl_ShouldNotDelete() {
        // Arrange
        user.setAvatarUrl(null); // Null avatar

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl("https://firebase.storage/new-avatar.jpg");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(firebaseStorageService, never()).deleteAvatar(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_SameAvatarUrl_ShouldNotDelete() {
        // Arrange
        String avatarUrl = "https://firebase.storage/avatar.jpg";
        user.setAvatarUrl(avatarUrl);

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setAvatarUrl(avatarUrl); // Same avatar

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(firebaseStorageService, never()).deleteAvatar(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void updateUserProfile_AllFieldsNull_ShouldOnlyUpdateTimestamp() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        // All fields are null

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository).save(any(User.class));
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository, never()).existsByEmail(anyString());
            verify(firebaseStorageService, never()).deleteAvatar(anyString());
        }
    }

    @Test
    void updateUserProfile_AllFieldsProvided_ShouldUpdateAll() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setUsername("newusername");
        request.setEmail("newemail@example.com");
        request.setFullName("New Full Name");
        request.setAvatarUrl("https://firebase.storage/new-avatar.jpg");
        request.setBio("New bio");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("newusername")).thenReturn(false);
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(firebaseStorageService.deleteAvatar(anyString())).thenReturn(true);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository).existsByUsername("newusername");
            verify(userRepository).existsByEmail("newemail@example.com");
            verify(firebaseStorageService).deleteAvatar(anyString());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void banUser_WebSocketFails_ShouldStillBanUser() {
        // Arrange
        String reason = "Violation of terms";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // Act - Should not throw exception
        assertDoesNotThrow(() -> userService.banUser(userId, reason));

        // Assert
        verify(userRepository).save(user);
        verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/account-status"),
                any(Map.class)
        );
    }

    @Test
    void getCurrentUser_FromSecurityContext_ShouldReturnUser() {
        // This tests the private getCurrentUser() method indirectly through updateUserProfile
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFullName("Updated Name");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            User result = userService.updateUserProfile(userId, request);

            // Assert
            assertNotNull(result);
            verify(userRepository).findByUsername(username);
        }
    }

    @Test
    void getCurrentUser_UsernameNotFoundInRepository_ShouldThrowException() {
        // Arrange
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFullName("Updated Name");

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin)
                    .thenReturn(Optional.of(username));

            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            CustomException exception = assertThrows(CustomException.class, () -> {
                userService.updateUserProfile(userId, request);
            });

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }
    }
}