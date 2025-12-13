package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.LoginDTO;
import com.backend.cookshare.authentication.dto.request.ChangePasswordRequest;
import com.backend.cookshare.authentication.dto.request.UserRequest;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.service.AuthService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private UserRequest userRequest;
    private LoginDTO loginDTO;
    private LoginResponseDTO loginResponseDTO;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        userRequest = new UserRequest();
        userRequest.setUsername("testuser");
        userRequest.setEmail("test@example.com");
        userRequest.setPassword("password");

        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password");

        loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setAccessToken("access-token");
        loginResponseDTO.setRefreshToken("refresh-token");
        loginResponseDTO.setExpiresIn(3600L); // Lưu ý: Long

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("oldPass");
        changePasswordRequest.setNewPassword("newPass");
        changePasswordRequest.setConfirmPassword("newPass");
    }

    @Test
    void register_ShouldReturnCreatedMessage() {
        when(authService.register(userRequest)).thenReturn("User registered");

        ResponseEntity<String> response = authController.register(userRequest);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("User registered", response.getBody());
        verify(authService).register(userRequest);
    }

    @Test
    void login_ShouldReturnLoginResponseWithCookie() {
        when(authService.login(loginDTO)).thenReturn(loginResponseDTO);

        ResponseEntity<LoginResponseDTO> response = authController.login(loginDTO);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(loginResponseDTO.getAccessToken(), response.getBody().getAccessToken());
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
        verify(authService).login(loginDTO);
    }

    @Test
    void getAccount_ShouldReturnUserInfo() {
        LoginResponseDTO.UserInfo userInfo = new LoginResponseDTO.UserInfo();
        userInfo.setUsername("testuser");
        when(authService.getAccount()).thenReturn(userInfo);

        ResponseEntity<LoginResponseDTO.UserInfo> response = authController.getAccount();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("testuser", response.getBody().getUsername());
        verify(authService).getAccount();
    }

    @Test
    void getRefreshToken_ShouldReturnNewTokenWithCookie() {
        String cookieToken = "cookie-refresh-token";
        String headerToken = "header-refresh-token";
        when(authService.refreshToken(headerToken)).thenReturn(loginResponseDTO);

        ResponseEntity<LoginResponseDTO> response =
                authController.getRefreshToken(cookieToken, headerToken);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(loginResponseDTO.getAccessToken(), response.getBody().getAccessToken());
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
        verify(authService).refreshToken(headerToken);
    }

    @Test
    void logout_ShouldCallServiceAndReturnMessage() throws CustomException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        doNothing().when(authService).logout(request);

        ResponseEntity<String> response = authController.logout(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Đăng xuất thành công", response.getBody());
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
        verify(authService).logout(request);
    }

    @Test
    void changePassword_ShouldCallServiceAndReturnMessage() throws CustomException {
        doNothing().when(authService).changePassword(changePasswordRequest);

        ResponseEntity<String> response = authController.changePassword(changePasswordRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Đổi mật khẩu thành công", response.getBody());
        verify(authService).changePassword(changePasswordRequest);
    }

    @Test
    void login_WhenAuthServiceThrowsException_ShouldPropagate() {
        when(authService.login(loginDTO)).thenThrow(new RuntimeException("Invalid credentials"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.login(loginDTO);
        });

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void changePassword_WhenAuthServiceThrowsException_ShouldPropagate() throws CustomException {
        // Arrange: ném CustomException với ErrorCode cụ thể
        doThrow(new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD))
                .when(authService).changePassword(changePasswordRequest);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            authController.changePassword(changePasswordRequest);
        });

        // Kiểm tra đúng ErrorCode
        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
    }

}
