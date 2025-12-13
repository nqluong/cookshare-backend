package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.entity.ForgotPassword;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.ForgotPasswordRepository;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.EmailService;
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
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ForgotPasswordRepository forgotPasswordRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    private User user;
    private final String testUsername = "testuser";
    private final String testEmail = "testuser@example.com";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUsername(testUsername);
        user.setEmail(testEmail);
        user.setFullName("Test User");
        user.setEmailVerified(false);
    }

    @Test
    void sendVerificationOtp_ShouldSendOtpSuccessfully() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of(testUsername));

            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));
            when(forgotPasswordRepository.save(any(ForgotPassword.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(emailService).sendHtmlMessage(
                    anyString(), anyString(), anyString(), any(Context.class)
            );

            String result = emailVerificationService.sendVerificationOtp();

            assertNotNull(result);
            assertTrue(result.contains("Mã OTP"));
            verify(forgotPasswordRepository).deleteByUser(user);
            verify(forgotPasswordRepository).save(any(ForgotPassword.class));
            verify(emailService).sendHtmlMessage(anyString(), anyString(), anyString(), any(Context.class));
        }
    }

    @Test
    void sendVerificationOtp_WhenEmailAlreadyVerified_ShouldReturnMessage() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            user.setEmailVerified(true);

            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of(testUsername));
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            String result = emailVerificationService.sendVerificationOtp();

            assertEquals("Email đã được xác thực trước đó", result);
            verify(emailService, never()).sendHtmlMessage(anyString(), anyString(), anyString(), any(Context.class));
        }
    }

    @Test
    void sendVerificationOtp_WhenUserNotFound_ShouldThrowException() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("nonexistent"));
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> emailVerificationService.sendVerificationOtp());

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Test
    void verifyOtp_ShouldVerifyEmailSuccessfully() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of(testUsername));

            ForgotPassword fp = ForgotPassword.builder()
                    .otp(123456)
                    .expirationTime(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                    .user(user)
                    .build();

            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));
            when(forgotPasswordRepository.findByOtpAndUser(123456, user)).thenReturn(Optional.of(fp));
            when(userRepository.save(user)).thenReturn(user);

            String result = emailVerificationService.verifyOtp(123456);

            assertNotNull(result);
            assertEquals("Xác thực email thành công!", result);
            assertTrue(user.getEmailVerified());
            verify(userRepository).save(user);
            verify(forgotPasswordRepository).deleteByUser(user);
        }
    }

    @Test
    void verifyOtp_WhenOtpNotFound_ShouldThrowException() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of(testUsername));

            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));
            when(forgotPasswordRepository.findByOtpAndUser(999999, user)).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> emailVerificationService.verifyOtp(999999));

            assertEquals(ErrorCode.OTP_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Test
    void verifyOtp_WhenOtpExpired_ShouldThrowException() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of(testUsername));

            ForgotPassword fp = ForgotPassword.builder()
                    .otp(123456)
                    .expirationTime(new Date(System.currentTimeMillis() - 1000))
                    .user(user)
                    .build();

            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));
            when(forgotPasswordRepository.findByOtpAndUser(123456, user)).thenReturn(Optional.of(fp));

            CustomException ex = assertThrows(CustomException.class,
                    () -> emailVerificationService.verifyOtp(123456));

            assertEquals(ErrorCode.OTP_EXPIRED, ex.getErrorCode());
            verify(forgotPasswordRepository).deleteByUser(user);
        }
    }

    @Test
    void verifyOtp_WhenEmailAlreadyVerified_ShouldReturnMessage() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            user.setEmailVerified(true);

            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of(testUsername));
            when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(user));

            String result = emailVerificationService.verifyOtp(123456);

            assertEquals("Email đã được xác thực trước đó", result);
            verify(forgotPasswordRepository, never()).findByOtpAndUser(anyInt(), any());
        }
    }

    @Test
    void verifyOtp_WhenUserNotFound_ShouldThrowException() {
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentUserLogin).thenReturn(Optional.of("nonexistent"));
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> emailVerificationService.verifyOtp(123456));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }
}