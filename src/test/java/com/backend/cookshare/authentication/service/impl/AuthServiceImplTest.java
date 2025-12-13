package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.request.ChangePasswordRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.enums.UserRole;
import com.backend.cookshare.authentication.service.TokenBlacklistService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private UserService userService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private LoginDTO loginDTO;
    private UserRequest userRequest;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setUserId(userId);
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");
        user.setFullName("Test User");
        user.setAvatarUrl("https://avatar.url");
        user.setBio("Test bio");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setFollowingCount(10);
        user.setFollowerCount(20);
        user.setRecipeCount(5);
        user.setRefreshToken("refresh-token");

        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        userRequest = new UserRequest();
        userRequest.setUsername("newuser");
        userRequest.setEmail("newuser@gmail.com");
        userRequest.setPassword("password123");
        userRequest.setFullname("New User");
    }

    // ==================== REGISTER TESTS ====================

    @Test
    void register_ShouldReturnSuccessMessage() {
        String expectedMessage = "User registered successfully";
        when(userService.createUser(userRequest)).thenReturn(expectedMessage);

        String result = authService.register(userRequest);

        assertEquals(expectedMessage, result);
        verify(userService).createUser(userRequest);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Mock authentication
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

        // Mock user service
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);

        // Mock security util
        LoginResponseDTO.UserInfo userInfo = buildUserInfo(user);
        when(securityUtil.createAccessToken(eq("testuser"), any())).thenReturn("access-token");
        when(securityUtil.createRefreshToken(eq("testuser"), any())).thenReturn("refresh-token");

        LoginResponseDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());
        assertEquals("testuser", result.getUser().getUsername());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByUsernameOrEmail("testuser");
        verify(userService).updateUser(any(User.class));
        verify(userService).updateUserToken("refresh-token", "testuser");
        verify(securityUtil).createAccessToken(eq("testuser"), any());
        verify(securityUtil).createRefreshToken(eq("testuser"), any());
    }

    @Test
    void login_WithNonExistentUser_ShouldThrowException() {
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.login(loginDTO));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userService).getUserByUsernameOrEmail("testuser");
    }

    @Test
    void login_WithInactiveUserNoBannedAt_ShouldThrowException() {
        user.setIsActive(false);
        user.setBannedAt(null);

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.login(loginDTO));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());
        verify(userService).getUserByUsernameOrEmail("testuser");
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void login_WithBannedUserLessThan30Days_ShouldThrowException() {
        user.setIsActive(false);
        user.setBannedAt(LocalDateTime.now().minusDays(15)); // Banned 15 days ago

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.login(loginDTO));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("15 ngày nữa"));
        verify(userService).getUserByUsernameOrEmail("testuser");
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    void login_WithBannedUserExactly30Days_ShouldAutoUnbanAndLogin() {
        user.setIsActive(false);
        user.setBannedAt(LocalDateTime.now().minusDays(30)); // Banned exactly 30 days ago

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);
        when(securityUtil.createAccessToken(anyString(), any())).thenReturn("access-token");
        when(securityUtil.createRefreshToken(anyString(), any())).thenReturn("refresh-token");

        LoginResponseDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertTrue(user.getIsActive());
        assertNull(user.getBannedAt());
        verify(userService, times(2)).updateUser(any(User.class)); // Once for unban, once for last active
    }

    @Test
    void login_WithBannedUserMoreThan30Days_ShouldAutoUnbanAndLogin() {
        user.setIsActive(false);
        user.setBannedAt(LocalDateTime.now().minusDays(45)); // Banned 45 days ago

        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));
        when(userService.updateUser(any(User.class))).thenReturn(user);
        when(securityUtil.createAccessToken(anyString(), any())).thenReturn("access-token");
        when(securityUtil.createRefreshToken(anyString(), any())).thenReturn("refresh-token");

        LoginResponseDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertTrue(user.getIsActive());
        assertNull(user.getBannedAt());
        verify(userService, times(2)).updateUser(any(User.class));
    }

    // ==================== GET ACCOUNT TESTS ====================

    @Test
    void getAccount_WithValidToken_ShouldReturnUserInfo() {
        try (MockedStatic<SecurityUtil> mockedStatic = mockStatic(SecurityUtil.class)) {
            mockedStatic.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

            LoginResponseDTO.UserInfo result = authService.getAccount();

            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals("testuser", result.getUsername());
            assertEquals("test@gmail.com", result.getEmail());
            assertEquals("Test User", result.getFullName());
            assertEquals("https://avatar.url", result.getAvatarUrl());
            assertEquals("Test bio", result.getBio());
            assertEquals(UserRole.USER, result.getRole());
            assertTrue(result.getIsActive());
            assertTrue(result.getEmailVerified());
            assertEquals(10, result.getFollowingCount());
            assertEquals(20, result.getFollowerCount());
            assertEquals(5, result.getRecipeCount());

            verify(userService).getUserByUsernameOrEmail("testuser");
        }
    }

    @Test
    void getAccount_WithInvalidToken_ShouldThrowException() {
        try (MockedStatic<SecurityUtil> mockedStatic = mockStatic(SecurityUtil.class)) {
            mockedStatic.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () ->
                    authService.getAccount());

            assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
            verify(userService, never()).getUserByUsernameOrEmail(anyString());
        }
    }

    @Test
    void getAccount_WithNonExistentUser_ShouldThrowException() {
        try (MockedStatic<SecurityUtil> mockedStatic = mockStatic(SecurityUtil.class)) {
            mockedStatic.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () ->
                    authService.getAccount());

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verify(userService).getUserByUsernameOrEmail("testuser");
        }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewTokens() {
        String refreshToken = "valid-refresh-token";
        Jwt jwt = createMockJwt("testuser");

        when(securityUtil.checkValidRefreshToken(refreshToken)).thenReturn(jwt);
        when(userService.getUserByRefreshTokenAndUsername(refreshToken, "testuser")).thenReturn(user);
        when(securityUtil.createAccessToken(eq("testuser"), any())).thenReturn("new-access-token");
        when(securityUtil.createRefreshToken(eq("testuser"), any())).thenReturn("new-refresh-token");

        LoginResponseDTO result = authService.refreshToken(refreshToken);

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getUser());

        verify(securityUtil).checkValidRefreshToken(refreshToken);
        verify(userService).getUserByRefreshTokenAndUsername(refreshToken, "testuser");
        verify(userService).updateUserToken("new-refresh-token", "testuser");
    }

    @Test
    void refreshToken_WithNullToken_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class, () ->
                authService.refreshToken(null));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        verify(securityUtil, never()).checkValidRefreshToken(anyString());
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldThrowException() {
        String refreshToken = "invalid-refresh-token";
        Jwt jwt = createMockJwt("testuser");

        when(securityUtil.checkValidRefreshToken(refreshToken)).thenReturn(jwt);
        when(userService.getUserByRefreshTokenAndUsername(refreshToken, "testuser")).thenReturn(null);

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.refreshToken(refreshToken));

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
        verify(userService).getUserByRefreshTokenAndUsername(refreshToken, "testuser");
        verify(userService, never()).updateUserToken(anyString(), anyString());
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    void logout_WithValidToken_ShouldBlacklistTokenAndClearRefreshToken() {
        String accessToken = "Bearer valid-access-token";
        Jwt jwt = createMockJwt("testuser");

        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(accessToken);
        when(securityUtil.checkValidAccessToken("valid-access-token")).thenReturn(jwt);
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        authService.logout(httpServletRequest);

        verify(httpServletRequest).getHeader(HttpHeaders.AUTHORIZATION);
        verify(securityUtil).checkValidAccessToken("valid-access-token");
        verify(tokenBlacklistService).blacklistToken("valid-access-token");
        verify(userService).updateUserToken(null, "testuser");
    }

    @Test
    void logout_WithNullAuthorizationHeader_ShouldThrowException() {
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.logout(httpServletRequest));

        assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
        verify(httpServletRequest).getHeader(HttpHeaders.AUTHORIZATION);
        verify(securityUtil, never()).checkValidAccessToken(anyString());
    }

    @Test
    void logout_WithInvalidTokenFormat_ShouldThrowException() {
        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("InvalidFormat token");

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.logout(httpServletRequest));

        assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
        verify(httpServletRequest).getHeader(HttpHeaders.AUTHORIZATION);
        verify(securityUtil, never()).checkValidAccessToken(anyString());
    }

    @Test
    void logout_WithEmptyUsername_ShouldThrowException() {
        String accessToken = "Bearer valid-access-token";
        Jwt jwt = createMockJwt("");

        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(accessToken);
        when(securityUtil.checkValidAccessToken("valid-access-token")).thenReturn(jwt);

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.logout(httpServletRequest));

        assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
        verify(securityUtil).checkValidAccessToken("valid-access-token");
        verify(userService, never()).getUserByUsernameOrEmail(anyString());
    }

    @Test
    void logout_WithNullUsername_ShouldThrowException() {
        String accessToken = "Bearer valid-access-token";
        Jwt jwt = createMockJwt(null);

        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(accessToken);
        when(securityUtil.checkValidAccessToken("valid-access-token")).thenReturn(jwt);

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.logout(httpServletRequest));

        assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
        verify(securityUtil).checkValidAccessToken("valid-access-token");
        verify(userService, never()).getUserByUsernameOrEmail(anyString());
    }

    @Test
    void logout_WithNonExistentUser_ShouldThrowException() {
        String accessToken = "Bearer valid-access-token";
        Jwt jwt = createMockJwt("testuser");

        when(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(accessToken);
        when(securityUtil.checkValidAccessToken("valid-access-token")).thenReturn(jwt);
        when(userService.getUserByUsernameOrEmail("testuser")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () ->
                authService.logout(httpServletRequest));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userService).getUserByUsernameOrEmail("testuser");
        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    void changePassword_WithValidRequest_ShouldChangePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        try (MockedStatic<SecurityUtil> mockedStatic = mockStatic(SecurityUtil.class)) {
            mockedStatic.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            doNothing().when(userService).changePassword("testuser", "oldPassword", "newPassword");

            authService.changePassword(request);

            verify(userService).changePassword("testuser", "oldPassword", "newPassword");
        }
    }

    @Test
    void changePassword_WithInvalidToken_ShouldThrowException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        try (MockedStatic<SecurityUtil> mockedStatic = mockStatic(SecurityUtil.class)) {
            mockedStatic.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.empty());

            CustomException exception = assertThrows(CustomException.class, () ->
                    authService.changePassword(request));

            assertEquals(ErrorCode.INVALID_ACCESS_TOKEN, exception.getErrorCode());
            verify(userService, never()).changePassword(anyString(), anyString(), anyString());
        }
    }

    @Test
    void changePassword_WithMismatchedPasswords_ShouldThrowException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("differentPassword");

        try (MockedStatic<SecurityUtil> mockedStatic = mockStatic(SecurityUtil.class)) {
            mockedStatic.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("testuser"));

            CustomException exception = assertThrows(CustomException.class, () ->
                    authService.changePassword(request));

            assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
            verify(userService, never()).changePassword(anyString(), anyString(), anyString());
        }
    }

    // ==================== HELPER METHODS ====================

    private LoginResponseDTO.UserInfo buildUserInfo(User user) {
        return LoginResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .followingCount(user.getFollowingCount())
                .followerCount(user.getFollowerCount())
                .recipeCount(user.getRecipeCount())
                .build();
    }

    private Jwt createMockJwt(String subject) {
        Map<String, Object> claims = new HashMap<>();
        if (subject != null) {
            claims.put("sub", subject);
        } else {
            // JWT requires at least one claim, so add a dummy claim when subject is null
            claims.put("dummy", "value");
        }

        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                claims
        );
    }
}