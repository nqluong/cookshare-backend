package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.dto.request.ResetPasswordRequest;
import com.backend.cookshare.authentication.service.ForgotPasswordService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForgotPasswordControllerTest {

    @InjectMocks
    private ForgotPasswordController forgotPasswordController;

    @Mock
    private ForgotPasswordService forgotPasswordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void verifyEmail_ShouldReturnOkMessage() {
        String email = "test@example.com";
        String expectedMessage = "OTP sent";
        when(forgotPasswordService.sendOtpForPasswordReset(email)).thenReturn(expectedMessage);

        ResponseEntity<String> response = forgotPasswordController.verifyEmail(email);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedMessage, response.getBody());
    }

    @Test
    void verifyOtp_ShouldReturnOkMessage() {
        String email = "test@example.com";
        Integer otp = 123456;
        String expectedMessage = "OTP verified";
        when(forgotPasswordService.verifyOtpForPasswordReset(email, otp)).thenReturn(expectedMessage);

        ResponseEntity<String> response = forgotPasswordController.verifyOtp(email, otp);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedMessage, response.getBody());
    }

    @Test
    void resetPassword_ShouldReturnOkMessage() {
        String email = "test@example.com";
        ResetPasswordRequest request = new ResetPasswordRequest("newPass123", "newPass123");
        String expectedMessage = "Password reset successfully";

        when(forgotPasswordService.resetPassword(email, request)).thenReturn(expectedMessage);

        ResponseEntity<String> response = forgotPasswordController.resetPassword(request, email);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedMessage, response.getBody());
    }

    @Test
    void verifyEmail_WhenServiceThrowsCustomException_ShouldPropagate() {
        String email = "test@example.com";
        doThrow(new CustomException(ErrorCode.USER_NOT_FOUND)).when(forgotPasswordService)
                .sendOtpForPasswordReset(email);

        CustomException exception = assertThrows(CustomException.class, () ->
                forgotPasswordController.verifyEmail(email));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void verifyOtp_WhenServiceThrowsCustomException_ShouldPropagate() {
        String email = "test@example.com";
        Integer otp = 123456;
        doThrow(new CustomException(ErrorCode.OTP_EXPIRED)).when(forgotPasswordService)
                .verifyOtpForPasswordReset(email, otp);

        CustomException exception = assertThrows(CustomException.class, () ->
                forgotPasswordController.verifyOtp(email, otp));

        assertEquals(ErrorCode.OTP_EXPIRED, exception.getErrorCode());
    }

    @Test
    void resetPassword_WhenServiceThrowsCustomException_ShouldPropagate() {
        String email = "test@example.com";
        ResetPasswordRequest request = new ResetPasswordRequest("newPass123", "newPass123");
        doThrow(new CustomException(ErrorCode.PASSWORD_MISMATCH)).when(forgotPasswordService)
                .resetPassword(email, request);

        CustomException exception = assertThrows(CustomException.class, () ->
                forgotPasswordController.resetPassword(request, email));

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
    }
}
