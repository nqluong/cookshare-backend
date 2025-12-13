package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.GoogleOAuthService;
import com.backend.cookshare.authentication.service.FacebookOAuthService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.authentication.util.SecurityUtil;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private FacebookOAuthService facebookOAuthService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private OAuthServiceImpl oAuthService;

    private String code;
    private LoginResponseDTO loginResponse;
    private User activeUser;
    private User bannedUser;
    private User bannedNoDateUser;

    @BeforeEach
    void setUp() {
        code = "test_code";

        activeUser = new User();
        activeUser.setUserId(java.util.UUID.randomUUID());
        activeUser.setUsername("activeUser");
        activeUser.setIsActive(true);
        activeUser.setLastActive(LocalDateTime.now());

        bannedUser = new User();
        bannedUser.setUserId(java.util.UUID.randomUUID());
        bannedUser.setUsername("bannedUser");
        bannedUser.setIsActive(false);
        bannedUser.setBannedAt(LocalDateTime.now().minusDays(31));

        bannedNoDateUser = new User();
        bannedNoDateUser.setUserId(java.util.UUID.randomUUID());
        bannedNoDateUser.setUsername("bannedNoDate");
        bannedNoDateUser.setIsActive(false);
        bannedNoDateUser.setBannedAt(null);

        loginResponse = LoginResponseDTO.builder()
                .accessToken("jwt_access")
                .refreshToken("jwt_refresh")
                .user(LoginResponseDTO.UserInfo.builder()
                        .userId(activeUser.getUserId())
                        .username(activeUser.getUsername())
                        .isActive(activeUser.getIsActive())
                        .build())
                .build();
    }

    @Test
    void authenticateWithOAuth_GoogleActiveUser_ShouldReturnResponse() {
        when(googleOAuthService.authenticateGoogleUser(code)).thenReturn(loginResponse);
        when(userService.getUserById(activeUser.getUserId())).thenReturn(Optional.of(activeUser));

        LoginResponseDTO result = oAuthService.authenticateWithOAuth(code, "google");

        assertNotNull(result);
        assertEquals(activeUser.getUsername(), result.getUser().getUsername());
        verify(userService).updateUser(any(User.class));
    }

    @Test
    void authenticateWithOAuth_FacebookActiveUser_ShouldReturnResponse() {
        when(facebookOAuthService.authenticateFacebookUser(code)).thenReturn(loginResponse);
        when(userService.getUserById(activeUser.getUserId())).thenReturn(Optional.of(activeUser));

        LoginResponseDTO result = oAuthService.authenticateWithOAuth(code, "facebook");

        assertNotNull(result);
        assertEquals(activeUser.getUsername(), result.getUser().getUsername());
        verify(userService).updateUser(any(User.class));
    }

    @Test
    void authenticateWithOAuth_InvalidProvider_ShouldThrowException() {
        CustomException exception = assertThrows(CustomException.class,
                () -> oAuthService.authenticateWithOAuth(code, "invalid"));

        assertEquals(ErrorCode.INVALID_OAUTH_PROVIDER, exception.getErrorCode());
    }

    @Test
    void authenticateWithOAuth_BannedUserMoreThan30Days_ShouldAutoUnban() {
        LoginResponseDTO bannedResponse = LoginResponseDTO.builder()
                .accessToken("jwt_access")
                .refreshToken("jwt_refresh")
                .user(LoginResponseDTO.UserInfo.builder()
                        .userId(bannedUser.getUserId())
                        .username(bannedUser.getUsername())
                        .isActive(false)
                        .build())
                .build();

        when(googleOAuthService.authenticateGoogleUser(code)).thenReturn(bannedResponse);
        when(userService.getUserById(bannedUser.getUserId())).thenReturn(Optional.of(bannedUser));

        LoginResponseDTO result = oAuthService.authenticateWithOAuth(code, "google");

        assertTrue(result.getUser().getIsActive());
        verify(userService).updateUser(any(User.class));
    }

    @Test
    void authenticateWithOAuth_BannedUserLessThan30Days_ShouldThrowException() {
        bannedUser.setBannedAt(LocalDateTime.now().minusDays(10));
        LoginResponseDTO bannedResponse = LoginResponseDTO.builder()
                .accessToken("jwt_access")
                .refreshToken("jwt_refresh")
                .user(LoginResponseDTO.UserInfo.builder()
                        .userId(bannedUser.getUserId())
                        .username(bannedUser.getUsername())
                        .isActive(false)
                        .build())
                .build();

        when(googleOAuthService.authenticateGoogleUser(code)).thenReturn(bannedResponse);
        when(userService.getUserById(bannedUser.getUserId())).thenReturn(Optional.of(bannedUser));

        CustomException exception = assertThrows(CustomException.class,
                () -> oAuthService.authenticateWithOAuth(code, "google"));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    void authenticateWithOAuth_BannedUserNoBannedAt_ShouldThrowException() {
        LoginResponseDTO bannedResponse = LoginResponseDTO.builder()
                .accessToken("jwt_access")
                .refreshToken("jwt_refresh")
                .user(LoginResponseDTO.UserInfo.builder()
                        .userId(bannedNoDateUser.getUserId())
                        .username(bannedNoDateUser.getUsername())
                        .isActive(false)
                        .build())
                .build();

        when(googleOAuthService.authenticateGoogleUser(code)).thenReturn(bannedResponse);
        when(userService.getUserById(bannedNoDateUser.getUserId())).thenReturn(Optional.of(bannedNoDateUser));

        CustomException exception = assertThrows(CustomException.class,
                () -> oAuthService.authenticateWithOAuth(code, "google"));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    void saveAndGetAuthResult_ShouldReturnSavedResult() {
        String state = "state123";
        oAuthService.saveAuthResult(state, loginResponse);

        LoginResponseDTO result = oAuthService.getAuthResult(state);
        assertNotNull(result);
        assertEquals("jwt_access", result.getAccessToken());
    }

    @Test
    void saveAuthResult_NullOrEmptyState_ShouldNotSave() {
        oAuthService.saveAuthResult(null, loginResponse);
        oAuthService.saveAuthResult("", loginResponse);
    }

    @Test
    void saveAndGetAuthError_ShouldReturnSavedError() {
        String state = "state123";
        oAuthService.saveAuthError(state, "ERROR_CODE", "Error message");

        var error = oAuthService.getAuthError(state);
        assertNotNull(error);
        assertEquals("ERROR_CODE", error.get("code"));
        assertEquals("Error message", error.get("message"));
    }

    @Test
    void saveAuthError_NullOrEmptyState_ShouldNotSave() {
        oAuthService.saveAuthError(null, "ERR", "msg");
        oAuthService.saveAuthError("", "ERR", "msg");
    }

    @Test
    void getAuthResult_NonExistingState_ShouldReturnNull() {
        LoginResponseDTO result = oAuthService.getAuthResult("unknown");
        assertNull(result);
    }

    @Test
    void getAuthError_NonExistingState_ShouldReturnNull() {
        var error = oAuthService.getAuthError("unknown");
        assertNull(error);
    }
}
