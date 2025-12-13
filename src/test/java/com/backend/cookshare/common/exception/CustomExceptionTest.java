package com.backend.cookshare.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void constructor_ShouldSetErrorCodeAndMessage_FromErrorCode() {
        // Arrange
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

        // Act
        CustomException ex = new CustomException(errorCode);

        // Assert
        assertEquals(errorCode, ex.getErrorCode());
        assertEquals(errorCode.getMessage(), ex.getMessage());
    }

    @Test
    void constructor_ShouldSetErrorCodeAndCustomMessage() {
        // Arrange
        ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
        String customMessage = "Người dùng không tồn tại trong hệ thống";

        // Act
        CustomException ex = new CustomException(errorCode, customMessage);

        // Assert
        assertEquals(errorCode, ex.getErrorCode());
        assertEquals(customMessage, ex.getMessage());
    }
}
