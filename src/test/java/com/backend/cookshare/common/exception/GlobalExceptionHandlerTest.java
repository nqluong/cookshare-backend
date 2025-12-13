package com.backend.cookshare.common.exception;

import com.backend.cookshare.common.dto.ErrorResponse;
import com.backend.cookshare.common.dto.ValidationResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        lenient().when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    void handleCustomException_ShouldReturnErrorResponse() {
        // Arrange
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus(), response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSucces());
        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), response.getBody().getCode());
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleCustomException_WithCustomMessage_ShouldReturnCustomMessage() {
        // Arrange
        String customMessage = "Custom error message";
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND, customMessage);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(customMessage, response.getBody().getMessage());
    }

    @Test
    void handleValidationException_ShouldReturnValidationErrors() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("object", "field1", "rejectedValue1", false, null, null, "Error message 1");
        FieldError fieldError2 = new FieldError("object", "field2", "rejectedValue2", false, null, null, "Error message 2");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSucces());
        assertEquals(ErrorCode.VALIDATION_ERROR.getCode(), response.getBody().getCode());
        assertEquals("Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra lại.", response.getBody().getMessage());
        assertEquals(2, response.getBody().getValidationErrors().size());
        assertEquals("field1", response.getBody().getValidationErrors().get(0).getField());
        assertEquals("Error message 1", response.getBody().getValidationErrors().get(0).getMessage());
    }

    @Test
    void handleBindException_ShouldReturnFieldErrors() {
        // Arrange
        BindException exception = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("object", "testField", "Test error");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleBindException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test error", response.getBody().get("testField"));
    }

    @Test
    void handleConstraintViolationException_ShouldReturnConstraintErrors() {
        // Arrange
        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("testProperty");
        when(violation.getMessage()).thenReturn("Constraint violation message");
        when(exception.getConstraintViolations()).thenReturn(Collections.singleton(violation));

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleConstraintViolationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Constraint violation message", response.getBody().get("testProperty"));
    }

    @Test
    void handleJsonParseError_WithUUIDError_ShouldReturnUUIDMessage() {
        // Arrange
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("Error parsing UUID");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJsonParseError(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Định dạng UUID không hợp lệ. Vui lòng kiểm tra lại ID được gửi lên.", response.getBody().getMessage());
    }

    @Test
    void handleJsonParseError_WithJSONParseError_ShouldReturnJSONMessage() {
        // Arrange
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error: unexpected character");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJsonParseError(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Dữ liệu JSON không hợp lệ. Vui lòng kiểm tra lại nội dung gửi lên.", response.getBody().getMessage());
    }

    @Test
    void handleJsonParseError_WithOtherError_ShouldReturnGenericMessage() {
        // Arrange
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("Some other error");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJsonParseError(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Không thể đọc dữ liệu gửi lên. Kiểm tra lại định dạng JSON.", response.getBody().getMessage());
    }

    @Test
    void handleJsonParseError_WithNullMessage_ShouldReturnGenericMessage() {
        // Arrange
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn(null);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleJsonParseError(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Không thể đọc dữ liệu gửi lên. Kiểm tra lại định dạng JSON.", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_WithRecipeSlugError_ShouldReturnSlugMessage() {
        // Arrange
        DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
        when(exception.getMessage()).thenReturn("Duplicate entry for recipes_slug_key");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Slug công thức đã tồn tại. Vui lòng chọn tiêu đề khác.", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_WithIngredientFKError_ShouldReturnIngredientMessage() {
        // Arrange
        DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
        when(exception.getMessage()).thenReturn("Foreign key violation recipe_ingredients_ingredient_id_fkey");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Nguyên liệu không tồn tại trong hệ thống. Vui lòng kiểm tra lại danh sách nguyên liệu.", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_WithGenericError_ShouldReturnGenericMessage() {
        // Arrange
        DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
        when(exception.getMessage()).thenReturn("Some other constraint violation");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc. Vui lòng kiểm tra lại.", response.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_WithNullMessage_ShouldReturnGenericMessage() {
        // Arrange
        DataIntegrityViolationException exception = mock(DataIntegrityViolationException.class);
        when(exception.getMessage()).thenReturn(null);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc. Vui lòng kiểm tra lại.", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentials_ShouldReturnUnauthorizedError() {
        // Arrange
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleBadCredentials(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng", response.getBody().get("error"));
        assertEquals("401", response.getBody().get("status"));
    }

    @Test
    void handleUserNotFound_ShouldReturnNotFoundError() {
        // Arrange
        UsernameNotFoundException exception = new UsernameNotFoundException("User not found");

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleUserNotFound(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User không tồn tại", response.getBody().get("error"));
        assertEquals("404", response.getBody().get("status"));
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequestError() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid argument", response.getBody().get("error"));
        assertEquals("400", response.getBody().get("status"));
    }

    @Test
    void handleMethodNotAllowed_ShouldReturnMethodNotAllowedError() {
        // Arrange
        HttpRequestMethodNotSupportedException exception = mock(HttpRequestMethodNotSupportedException.class);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodNotAllowed(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSucces());
        assertEquals(ErrorCode.METHOD_NOT_ALLOWED.getCode(), response.getBody().getCode());
        assertEquals("Phương thức HTTP này không được hỗ trợ cho endpoint hiện tại.", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Arrange
        Exception exception = new Exception("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSucces());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("Đã xảy ra lỗi hệ thống"));
        assertTrue(response.getBody().getMessage().contains("Unexpected error"));
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleGenericException_WithNullMessage_ShouldStillReturnError() {
        // Arrange
        Exception exception = new Exception();

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void extractPath_ShouldRemoveUriPrefix() {
        // Arrange
        when(webRequest.getDescription(false)).thenReturn("uri=/api/users/123");
        CustomException exception = new CustomException(ErrorCode.USER_NOT_FOUND);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomException(exception, webRequest);

        // Assert
        assertEquals("/api/users/123", response.getBody().getPath());
    }

    @Test
    void handleValidationException_WithEmptyFieldErrors_ShouldReturnEmptyValidationList() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, webRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.getBody().getValidationErrors().isEmpty());
    }

    @Test
    void handleBindException_WithMultipleErrors_ShouldReturnAllErrors() {
        // Arrange
        BindException exception = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError error1 = new FieldError("object", "field1", "Error 1");
        FieldError error2 = new FieldError("object", "field2", "Error 2");
        FieldError error3 = new FieldError("object", "field3", "Error 3");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(error1, error2, error3));

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleBindException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getBody().size());
        assertEquals("Error 1", response.getBody().get("field1"));
        assertEquals("Error 2", response.getBody().get("field2"));
        assertEquals("Error 3", response.getBody().get("field3"));
    }

    @Test
    void handleConstraintViolationException_WithMultipleViolations_ShouldReturnAllViolations() {
        // Arrange
        ConstraintViolationException exception = mock(ConstraintViolationException.class);

        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(path1.toString()).thenReturn("property1");
        when(violation1.getMessage()).thenReturn("Message 1");

        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(path2.toString()).thenReturn("property2");
        when(violation2.getMessage()).thenReturn("Message 2");

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation1);
        violations.add(violation2);

        when(exception.getConstraintViolations()).thenReturn(violations);

        // Act
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleConstraintViolationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().containsKey("property1"));
        assertTrue(response.getBody().containsKey("property2"));
    }
}