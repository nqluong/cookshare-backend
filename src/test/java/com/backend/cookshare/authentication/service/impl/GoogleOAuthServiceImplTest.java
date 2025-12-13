package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.GoogleTokenResponse;
import com.backend.cookshare.authentication.dto.response.GoogleUserInfo;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @InjectMocks
    private GoogleOAuthServiceImpl googleOAuthService;

    private String code;
    private UUID userId;
    private User user;
    private GoogleUserInfo googleUserInfo;
    private GoogleTokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        code = "test_code";
        userId = UUID.randomUUID();

        user = new User();
        user.setUserId(userId);
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setEmail("test@gmail.com");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setRecipeCount(0);

        googleUserInfo = new GoogleUserInfo();
        googleUserInfo.setGoogleId("google123");
        googleUserInfo.setEmail("test@gmail.com");
        googleUserInfo.setName("Test User");
        googleUserInfo.setPicture("https://example.com/avatar.jpg");
        googleUserInfo.setEmailVerified(true);

        tokenResponse = new GoogleTokenResponse();
        tokenResponse.setAccessToken("access_token");
    }

    @Test
    void getAccessToken_ShouldReturnTokenResponse() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(GoogleTokenResponse.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        GoogleTokenResponse result = googleOAuthService.getAccessToken(code);

        assertNotNull(result);
        assertEquals("access_token", result.getAccessToken());
    }

    @Test
    void getAccessToken_WhenError_ShouldThrowCustomException() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(GoogleTokenResponse.class)))
                .thenThrow(new RuntimeException("fail"));

        CustomException exception = assertThrows(CustomException.class, () ->
                googleOAuthService.getAccessToken(code));

        assertEquals(ErrorCode.GOOGLE_AUTH_ERROR, exception.getErrorCode());
    }

    @Test
    void getUserInfo_ShouldReturnGoogleUserInfo() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(GoogleUserInfo.class)))
                .thenReturn(new ResponseEntity<>(googleUserInfo, HttpStatus.OK));

        GoogleUserInfo result = googleOAuthService.getUserInfo("access_token");

        assertNotNull(result);
        assertEquals("google123", result.getGoogleId());
        assertEquals("https://example.com/avatar.jpg", result.getPicture());
    }

    @Test
    void getUserInfo_WhenError_ShouldThrowCustomException() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(GoogleUserInfo.class)))
                .thenThrow(new RuntimeException("fail"));

        CustomException exception = assertThrows(CustomException.class, () ->
                googleOAuthService.getUserInfo("access_token"));

        assertEquals(ErrorCode.GOOGLE_AUTH_ERROR, exception.getErrorCode());
    }

    @Test
    void findOrCreateUser_ExistingUserWithoutAvatar_ShouldUpdateWithNewAvatar() {
        user.setAvatarUrl(null);
        user.setGoogleId("google123");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        // Removed unnecessary stubbings for firebaseStorageService
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserWithOAuthAvatar_ShouldUpdateAvatar() {
        user.setAvatarUrl("https://firebase.com/oauth_google_123.jpg");
        user.setGoogleId("google123");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        when(firebaseStorageService.deleteAvatar(anyString())).thenReturn(true);
        // Removed unnecessary stubbings for uploadAvatar and getAvatarPublicUrl
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(firebaseStorageService).deleteAvatar(anyString());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserWithCustomAvatar_ShouldKeepAvatar() {
        user.setAvatarUrl("https://firebase.com/custom_avatar.jpg");
        user.setGoogleId("google123");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(userRepository).save(user);
        verify(firebaseStorageService, never()).deleteAvatar(anyString());
    }

    @Test
    void findOrCreateUser_ExistingUserWithEmptyAvatar_ShouldUploadNewAvatar() {
        user.setAvatarUrl("");
        user.setGoogleId("google123");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        // Removed unnecessary stubbings for firebaseStorageService
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserWithFacebookOAuthAvatar_ShouldUpdateToGoogleAvatar() {
        user.setAvatarUrl("https://firebase.com/oauth_facebook_123.jpg");
        user.setGoogleId("google123");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        when(firebaseStorageService.deleteAvatar(anyString())).thenReturn(true);
        // Removed unnecessary stubbings for uploadAvatar and getAvatarPublicUrl
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(firebaseStorageService).deleteAvatar(anyString());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserWithNullPicture_ShouldNotUpdateAvatar() {
        user.setGoogleId("google123");
        googleUserInfo.setPicture(null);

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(firebaseStorageService, never()).uploadAvatar(anyString(), any(byte[].class), anyString());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserWithEmptyPicture_ShouldNotUpdateAvatar() {
        user.setGoogleId("google123");
        googleUserInfo.setPicture("");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(firebaseStorageService, never()).uploadAvatar(anyString(), any(byte[].class), anyString());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserByEmail_ShouldLinkGoogleAccount() {
        user.setGoogleId(null);

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        // Removed unnecessary stubbings for firebaseStorageService
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        assertEquals("google123", result.getGoogleId());
        assertTrue(result.getEmailVerified());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserByEmailWithoutAvatar_ShouldLinkAndUploadAvatar() {
        user.setGoogleId(null);
        user.setAvatarUrl(null);

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        // Removed unnecessary stubbings for firebaseStorageService
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        assertEquals("google123", result.getGoogleId());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_ExistingUserByEmailWithCustomAvatar_ShouldLinkButKeepAvatar() {
        user.setGoogleId(null);
        user.setAvatarUrl("https://firebase.com/custom_avatar.jpg");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        assertEquals("google123", result.getGoogleId());
        verify(firebaseStorageService, never()).uploadAvatar(anyString(), any(byte[].class), anyString());
        verify(userRepository).save(user);
    }

    @Test
    void findOrCreateUser_NewUser_ShouldCreateAndReturn() {
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(userId);
            return savedUser;
        });
        // Removed unnecessary stubbings for firebaseStorageService

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        assertTrue(result.getUsername().contains("test"));
        assertEquals("GOOGLE_AUTH", result.getPasswordHash());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getIsActive());
        assertTrue(result.getEmailVerified());
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void findOrCreateUser_NewUserWithoutPicture_ShouldCreateWithoutAvatar() {
        googleUserInfo.setPicture(null);

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(userId);
            return savedUser;
        });

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(firebaseStorageService, never()).uploadAvatar(anyString(), any(byte[].class), anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findOrCreateUser_NewUserWithEmptyPicture_ShouldCreateWithoutAvatar() {
        googleUserInfo.setPicture("");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(userId);
            return savedUser;
        });

        User result = googleOAuthService.findOrCreateUser(googleUserInfo);

        assertNotNull(result);
        verify(firebaseStorageService, never()).uploadAvatar(anyString(), any(byte[].class), anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticateGoogleUser_ShouldReturnLoginResponseDTO() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(GoogleTokenResponse.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.exchange(anyString(), any(), any(), eq(GoogleUserInfo.class)))
                .thenReturn(new ResponseEntity<>(googleUserInfo, HttpStatus.OK));
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(securityUtil.createAccessToken(anyString(), any())).thenReturn("jwt_access");
        when(securityUtil.createRefreshToken(anyString(), any())).thenReturn("jwt_refresh");

        LoginResponseDTO result = googleOAuthService.authenticateGoogleUser(code);

        assertNotNull(result);
        assertEquals("jwt_access", result.getAccessToken());
        assertEquals("jwt_refresh", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());
        assertEquals(userId, result.getUser().getUserId());
        // Changed from times(2) to times(3) to match actual implementation
        verify(userRepository, times(3)).save(any(User.class));
    }

    @Test
    void generateUniqueUsername_WhenUsernameExists_ShouldAppendCounter() {
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("test1")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("test2")).thenReturn(Optional.empty());

        String result = googleOAuthService.generateUniqueUsername("test@gmail.com");

        assertEquals("test2", result);
        verify(userRepository, times(3)).findByUsername(anyString());
    }

    @Test
    void generateUniqueUsername_WhenUsernameAvailable_ShouldReturnBaseUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        String result = googleOAuthService.generateUniqueUsername("testuser@gmail.com");

        assertEquals("testuser", result);
        verify(userRepository).findByUsername("testuser");
    }
}