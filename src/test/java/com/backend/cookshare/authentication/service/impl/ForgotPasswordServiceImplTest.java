package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.dto.request.ResetPasswordRequest;
import com.backend.cookshare.authentication.entity.ForgotPassword;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.ForgotPasswordRepository;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.EmailService;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ForgotPasswordRepository forgotPasswordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    private String email;
    private UUID userId;
    private User user;
    private ForgotPassword forgotPassword;
    private Integer otp;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        userId = UUID.randomUUID();
        otp = 123456;

        user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setUsername("testuser");
        user.setFullName("Test User");

        forgotPassword = ForgotPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 60 * 1000 * 5))
                .user(user)
                .isVerified(false)
                .build();
    }

    @Test
    void sendOtpForPasswordReset_ShouldSendEmailSuccessfully() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenReturn(forgotPassword);
        doNothing().when(emailService).sendHtmlMessage(eq(email), anyString(), anyString(), any(Context.class));

        // Act
        String result = forgotPasswordService.sendOtpForPasswordReset(email);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(email));
        verify(userRepository).findByEmail(email);
        verify(forgotPasswordRepository).deleteByUser(user);
        verify(forgotPasswordRepository).save(any(ForgotPassword.class));
        verify(emailService).sendHtmlMessage(eq(email), anyString(), eq("otp-email"), any(Context.class));
    }

    @Test
    void sendOtpForPasswordReset_UserWithFullName_ShouldUseFullNameInEmail() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenReturn(forgotPassword);
        doNothing().when(emailService).sendHtmlMessage(eq(email), anyString(), anyString(), any(Context.class));

        // Act
        String result = forgotPasswordService.sendOtpForPasswordReset(email);

        // Assert
        assertNotNull(result);
        verify(emailService).sendHtmlMessage(eq(email), anyString(), anyString(), any(Context.class));
    }

    @Test
    void sendOtpForPasswordReset_UserWithoutFullName_ShouldUseUsernameInEmail() {
        // Arrange
        user.setFullName(null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenReturn(forgotPassword);
        doNothing().when(emailService).sendHtmlMessage(eq(email), anyString(), anyString(), any(Context.class));

        // Act
        String result = forgotPasswordService.sendOtpForPasswordReset(email);

        // Assert
        assertNotNull(result);
        verify(emailService).sendHtmlMessage(eq(email), anyString(), anyString(), any(Context.class));
    }

    @Test
    void sendOtpForPasswordReset_InvalidEmail_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.sendOtpForPasswordReset(email);
        });

        assertEquals(ErrorCode.INVALID_EMAIL, exception.getErrorCode());
        verify(userRepository).findByEmail(email);
        verify(forgotPasswordRepository, never()).save(any());
        verify(emailService, never()).sendHtmlMessage(anyString(), anyString(), anyString(), any(Context.class));
    }

    @Test
    void sendOtpForPasswordReset_EmailSendFailed_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);
        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendHtmlMessage(anyString(), anyString(), anyString(), any(Context.class));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.sendOtpForPasswordReset(email);
        });

        assertEquals(ErrorCode.EMAIL_SEND_FAILED, exception.getErrorCode());
        verify(forgotPasswordRepository).deleteByUser(user);
        verify(forgotPasswordRepository, never()).save(any());
    }

    @Test
    void verifyOtpForPasswordReset_ShouldVerifySuccessfully() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(otp, user)).thenReturn(Optional.of(forgotPassword));
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenReturn(forgotPassword);

        // Act
        String result = forgotPasswordService.verifyOtpForPasswordReset(email, otp);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("OTP hợp lệ"));
        verify(userRepository).findByEmail(email);
        verify(forgotPasswordRepository).findByOtpAndUser(otp, user);
        verify(forgotPasswordRepository).save(any(ForgotPassword.class));
    }

    @Test
    void verifyOtpForPasswordReset_InvalidEmail_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.verifyOtpForPasswordReset(email, otp);
        });

        assertEquals(ErrorCode.INVALID_EMAIL, exception.getErrorCode());
        verify(forgotPasswordRepository, never()).findByOtpAndUser(any(), any());
    }

    @Test
    void verifyOtpForPasswordReset_OtpNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(otp, user)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.verifyOtpForPasswordReset(email, otp);
        });

        assertEquals(ErrorCode.OTP_NOT_FOUND, exception.getErrorCode());
        verify(forgotPasswordRepository, never()).save(any());
    }

    @Test
    void verifyOtpForPasswordReset_ExpiredOtp_ShouldThrowException() {
        // Arrange
        forgotPassword.setExpirationTime(new Date(System.currentTimeMillis() - 1000));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(otp, user)).thenReturn(Optional.of(forgotPassword));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.verifyOtpForPasswordReset(email, otp);
        });

        assertEquals(ErrorCode.OTP_EXPIRED, exception.getErrorCode());
        verify(forgotPasswordRepository).deleteByUser(user);
        verify(forgotPasswordRepository, never()).save(any());
    }

    @Test
    void resetPassword_ShouldResetSuccessfully() {
        // Arrange
        forgotPassword.setIsVerified(true);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByUserAndIsVerified(user, true)).thenReturn(Optional.of(forgotPassword));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(forgotPasswordRepository).deleteByUser(user);

        // Act
        String result = forgotPasswordService.resetPassword(email, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("thành công"));
        verify(userRepository).findByEmail(email);
        verify(forgotPasswordRepository).findByUserAndIsVerified(user, true);
        verify(passwordEncoder).encode(request.getNewPassword());
        verify(userRepository).save(any(User.class));
        verify(forgotPasswordRepository).deleteByUser(user);
    }

    @Test
    void resetPassword_InvalidEmail_ShouldThrowException() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.resetPassword(email, request);
        });

        assertEquals(ErrorCode.INVALID_EMAIL, exception.getErrorCode());
        verify(forgotPasswordRepository, never()).findByUserAndIsVerified(any(), anyBoolean());
    }

    @Test
    void resetPassword_OtpNotVerified_ShouldThrowException() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByUserAndIsVerified(user, true)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.resetPassword(email, request);
        });

        assertEquals(ErrorCode.OTP_NOT_VERIFIED, exception.getErrorCode());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ExpiredOtp_ShouldThrowException() {
        // Arrange
        forgotPassword.setIsVerified(true);
        forgotPassword.setExpirationTime(new Date(System.currentTimeMillis() - 1000));
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByUserAndIsVerified(user, true)).thenReturn(Optional.of(forgotPassword));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.resetPassword(email, request);
        });

        assertEquals(ErrorCode.OTP_EXPIRED, exception.getErrorCode());
        verify(forgotPasswordRepository).deleteByUser(user);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_PasswordMismatch_ShouldThrowException() {
        // Arrange
        forgotPassword.setIsVerified(true);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByUserAndIsVerified(user, true)).thenReturn(Optional.of(forgotPassword));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            forgotPasswordService.resetPassword(email, request);
        });

        assertEquals(ErrorCode.PASSWORD_MISMATCH, exception.getErrorCode());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(forgotPasswordRepository, never()).deleteByUser(user);
    }

    @Test
    void sendOtpForPasswordReset_ShouldDeleteOldOtpBeforeCreatingNew() {
        // Arrange
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(forgotPasswordRepository).deleteByUser(user);
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenReturn(forgotPassword);
        doNothing().when(emailService).sendHtmlMessage(anyString(), anyString(), anyString(), any(Context.class));

        // Act
        forgotPasswordService.sendOtpForPasswordReset(email);

        // Assert
        verify(forgotPasswordRepository).deleteByUser(user);
        verify(forgotPasswordRepository).save(any(ForgotPassword.class));
    }

    @Test
    void verifyOtpForPasswordReset_ShouldSetIsVerifiedToTrue() {
        // Arrange
        assertFalse(forgotPassword.getIsVerified());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByOtpAndUser(otp, user)).thenReturn(Optional.of(forgotPassword));
        when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenAnswer(invocation -> {
            ForgotPassword fp = invocation.getArgument(0);
            assertTrue(fp.getIsVerified());
            return fp;
        });

        // Act
        forgotPasswordService.verifyOtpForPasswordReset(email, otp);

        // Assert
        verify(forgotPasswordRepository).save(any(ForgotPassword.class));
    }

    @Test
    void resetPassword_ShouldDeleteForgotPasswordRecordAfterSuccess() {
        // Arrange
        forgotPassword.setIsVerified(true);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(forgotPasswordRepository.findByUserAndIsVerified(user, true)).thenReturn(Optional.of(forgotPassword));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(forgotPasswordRepository).deleteByUser(user);

        // Act
        forgotPasswordService.resetPassword(email, request);

        // Assert
        verify(forgotPasswordRepository).deleteByUser(user);
    }
}