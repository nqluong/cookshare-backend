package com.backend.cookshare.common.exception;

import com.backend.cookshare.common.dto.ErrorResponse;
import com.backend.cookshare.common.dto.ValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, WebRequest request) {
        ErrorCode code = ex.getErrorCode();
        ErrorResponse response = ErrorResponse.builder()
                .code(code.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : code.getMessage())
                .path(extractPath(request))
                .build();
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        List<ValidationResponse> validationErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fieldError -> new ValidationResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue()
                ))
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message(ErrorCode.VALIDATION_ERROR.getMessage())
                .path(extractPath(request))
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
