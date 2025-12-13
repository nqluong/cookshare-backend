package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.service.OAuthService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthControllerTest {

    @InjectMocks
    private GoogleAuthController googleAuthController;

    @Mock
    private OAuthService oAuthService;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private Model model;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loginWithGoogle_NoState_ShouldGenerateStateAndReturn302() {
        ResponseEntity<Void> response = googleAuthController.loginWithGoogle(null);

        assertEquals(302, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("https://accounts.google.com/o/oauth2/v2/auth"));
    }

    @Test
    void loginWithGoogle_WithState_ShouldReturn302WithSameState() {
        String state = "testState";
        ResponseEntity<Void> response = googleAuthController.loginWithGoogle(state);

        assertEquals(302, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("state=" + state));
    }

    @Test
    void googleCallback_ShouldReturnAuthLoadingOnSuccess() throws Exception {
        String code = "authCode";
        String state = "state123";
        LoginResponseDTO mockResponse = new LoginResponseDTO();
        mockResponse.setRefreshToken("refreshToken");

        when(oAuthService.authenticateWithOAuth(code, "google")).thenReturn(mockResponse);

        Object result = googleAuthController.googleCallback(code, state, null, servletResponse, model);

        assertEquals("auth-loading", result);
        verify(oAuthService).saveAuthResult(state, mockResponse);
    }

    @Test
    void googleCallback_ShouldReturnErrorPageOnCustomException() throws Exception {
        String code = "authCode";
        String state = "state123";

        when(oAuthService.authenticateWithOAuth(code, "google"))
                .thenThrow(new CustomException(ErrorCode.GOOGLE_AUTH_ERROR));

        Object result = googleAuthController.googleCallback(code, state, null, servletResponse, model);

        assertEquals("auth-error", result);
        verify(oAuthService).saveAuthError(eq(state), anyString(), anyString());
    }

    @Test
    void getAuthResult_ShouldReturnUnauthorizedIfErrorExists() {
        String state = "state123";
        Map<String, Object> errorMap = Map.of("status", "error", "message", "Invalid token");
        when(oAuthService.getAuthError(state)).thenReturn(errorMap);

        ResponseEntity<?> response = googleAuthController.getAuthResult(state);

        assertEquals(401, response.getStatusCodeValue());
        assertEquals(errorMap, response.getBody());
    }

    @Test
    void getAuthResult_ShouldReturnOkIfResultExists() {
        String state = "state123";
        LoginResponseDTO mockResult = new LoginResponseDTO();
        when(oAuthService.getAuthError(state)).thenReturn(null);
        when(oAuthService.getAuthResult(state)).thenReturn(mockResult);

        ResponseEntity<?> response = googleAuthController.getAuthResult(state);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockResult, response.getBody());
    }

    @Test
    void getAuthResult_ShouldReturnNotFoundIfNoResult() {
        String state = "state123";
        when(oAuthService.getAuthError(state)).thenReturn(null);
        when(oAuthService.getAuthResult(state)).thenReturn(null);

        ResponseEntity<?> response = googleAuthController.getAuthResult(state);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void authenticateWithCode_ShouldReturnOkWithCookie() {
        String code = "authCode";
        LoginResponseDTO mockResponse = new LoginResponseDTO();
        mockResponse.setRefreshToken("refreshToken");

        when(oAuthService.authenticateWithOAuth(code, "google")).thenReturn(mockResponse);

        ResponseEntity<LoginResponseDTO> response = googleAuthController.authenticateWithCode(code);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockResponse, response.getBody());
        assertTrue(response.getHeaders().containsKey("Set-Cookie"));
    }

    @Test
    void authenticateWithCode_ShouldThrowCustomExceptionOnError() {
        String code = "authCode";
        when(oAuthService.authenticateWithOAuth(code, "google")).thenThrow(new RuntimeException("fail"));

        CustomException exception = assertThrows(CustomException.class, () ->
                googleAuthController.authenticateWithCode(code));

        assertEquals(ErrorCode.GOOGLE_AUTH_ERROR, exception.getErrorCode());
    }
}
