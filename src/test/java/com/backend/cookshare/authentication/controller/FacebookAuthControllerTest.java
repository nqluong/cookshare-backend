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
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FacebookAuthControllerTest {

    @InjectMocks
    private FacebookAuthController facebookAuthController;

    @Mock
    private OAuthService oAuthService;

    @Mock
    private Model model;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Sử dụng reflection để set private fields
        setField(facebookAuthController, "clientId", "test-client-id");
        setField(facebookAuthController, "redirectUri", "http://localhost/callback");
        setField(facebookAuthController, "authUri", "http://facebook.com/oauth");
        setField(facebookAuthController, "scope", "email,public_profile");
        setField(facebookAuthController, "refreshTokenExpiration", 3600L);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void loginWithFacebook_NoState_ShouldGenerateStateAndReturn302() {
        ResponseEntity<Void> response = facebookAuthController.loginWithFacebook(null);
        assertEquals(302, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("state="));
        assertTrue(response.getHeaders().getLocation().toString().contains("client_id=test-client-id"));
    }

    @Test
    void loginWithFacebook_WithState_ShouldReturn302() {
        String state = UUID.randomUUID().toString();
        ResponseEntity<Void> response = facebookAuthController.loginWithFacebook(state);
        assertEquals(302, response.getStatusCodeValue());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("state=" + state));
    }

    @Test
    void facebookCallback_WithError_ShouldReturnBadRequest() {
        String code = "dummyCode";
        String state = "state123";
        String error = "access_denied";
        String errorDescription = "User denied permission";

        Object result = facebookAuthController.facebookCallback(code, state, error, errorDescription,
                new MockHttpServletResponse(), model);

        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(400, response.getStatusCodeValue());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("error", body.get("status"));
        assertEquals(error, body.get("message"));
    }

    @Test
    void facebookCallback_WithSuccess_ShouldReturnAuthLoadingView() throws Exception {
        String code = "validCode";
        String state = "state123";

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setRefreshToken("refresh-token");

        when(oAuthService.authenticateWithOAuth(code, "facebook")).thenReturn(loginResponse);

        Object result = facebookAuthController.facebookCallback(code, state, null, null,
                new MockHttpServletResponse(), model);

        assertTrue(result instanceof String);
        assertEquals("auth-loading", result);

        verify(oAuthService).saveAuthResult(state, loginResponse);
    }

    @Test
    void facebookCallback_WithCustomException_ShouldReturnAuthErrorView() throws Exception {
        String code = "validCode";
        String state = "state123";

        doThrow(new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR))
                .when(oAuthService).authenticateWithOAuth(code, "facebook");

        Object result = facebookAuthController.facebookCallback(code, state, null, null,
                new MockHttpServletResponse(), model);

        assertTrue(result instanceof String);
        assertEquals("auth-error", result);

        verify(oAuthService).saveAuthError(eq(state), anyString(), anyString());
    }

    @Test
    void getAuthResult_WithSuccess_ShouldReturnLoginResponse() {
        String state = "state123";
        LoginResponseDTO loginResponse = new LoginResponseDTO();
        when(oAuthService.getAuthError(state)).thenReturn(null);
        when(oAuthService.getAuthResult(state)).thenReturn(loginResponse);

        ResponseEntity<?> response = (ResponseEntity<?>) facebookAuthController.getAuthResult(state);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(loginResponse, response.getBody());
    }

    @Test
    void getAuthResult_WithError_ShouldReturnUnauthorized() {
        String state = "state123";
        Map<String, Object> errorMap = Map.of("status", "error", "message", "failed");
        when(oAuthService.getAuthError(state)).thenReturn(errorMap);

        ResponseEntity<?> response = (ResponseEntity<?>) facebookAuthController.getAuthResult(state);
        assertEquals(401, response.getStatusCodeValue());
        assertEquals(errorMap, response.getBody());
    }

    @Test
    void getAuthResult_NoResult_ShouldReturnNotFound() {
        String state = "state123";
        when(oAuthService.getAuthError(state)).thenReturn(null);
        when(oAuthService.getAuthResult(state)).thenReturn(null);

        ResponseEntity<?> response = (ResponseEntity<?>) facebookAuthController.getAuthResult(state);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void authenticateWithCode_ShouldReturnLoginResponse() throws Exception {
        String code = "code123";
        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setRefreshToken("refresh-token");

        when(oAuthService.authenticateWithOAuth(code, "facebook")).thenReturn(loginResponse);

        ResponseEntity<LoginResponseDTO> response = facebookAuthController.authenticateWithCode(code);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(loginResponse, response.getBody());
    }

    @Test
    void authenticateWithCode_WithCustomException_ShouldThrow() throws Exception {
        String code = "code123";
        doThrow(new CustomException(ErrorCode.FACEBOOK_AUTH_ERROR))
                .when(oAuthService).authenticateWithOAuth(code, "facebook");

        CustomException exception = assertThrows(CustomException.class, () ->
                facebookAuthController.authenticateWithCode(code));

        assertEquals(ErrorCode.FACEBOOK_AUTH_ERROR, exception.getErrorCode());
    }
}
