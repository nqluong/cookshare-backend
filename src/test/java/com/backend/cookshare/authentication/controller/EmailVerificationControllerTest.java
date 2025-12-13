package com.backend.cookshare.authentication.controller;

import com.backend.cookshare.authentication.service.EmailVerificationService;
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
class EmailVerificationControllerTest {

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private EmailVerificationController emailVerificationController;

    private final Integer otp = 123456;

    @BeforeEach
    void setUp() {
        // Không cần setUp nhiều vì các service mock sẽ được inject
    }

    @Test
    void sendVerificationOtp_ShouldReturnSuccessMessage() {
        // Arrange
        when(emailVerificationService.sendVerificationOtp()).thenReturn("OTP sent successfully");

        // Act
        ResponseEntity<String> response = emailVerificationController.sendVerificationOtp();

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("OTP sent successfully", response.getBody());
        verify(emailVerificationService).sendVerificationOtp();
    }

    @Test
    void verifyOtp_ShouldReturnSuccessMessage() {
        // Arrange
        when(emailVerificationService.verifyOtp(otp)).thenReturn("OTP verified successfully");

        // Act
        ResponseEntity<String> response = emailVerificationController.verifyOtp(otp);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("OTP verified successfully", response.getBody());
        verify(emailVerificationService).verifyOtp(otp);
    }

    @Test
    void verifyOtp_WhenServiceThrowsException_ShouldPropagate() {
        // Arrange
        when(emailVerificationService.verifyOtp(otp)).thenThrow(new RuntimeException("Invalid OTP"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailVerificationController.verifyOtp(otp);
        });

        assertEquals("Invalid OTP", exception.getMessage());
        verify(emailVerificationService).verifyOtp(otp);
    }
}
