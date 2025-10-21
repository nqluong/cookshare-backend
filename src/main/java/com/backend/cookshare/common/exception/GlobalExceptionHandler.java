package com.backend.cookshare.common.exception;

import com.backend.cookshare.common.dto.ErrorResponse;
import com.backend.cookshare.common.dto.ValidationResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================
    // ⚙️ CUSTOM EXCEPTION
    // ==========================
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, WebRequest request) {
        ErrorCode code = ex.getErrorCode();

        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(code.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : code.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    // ==========================
    // ⚙️ VALIDATION ERRORS (@Valid)
    // ==========================
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
                .succes(false)
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message("Dữ liệu nhập vào không hợp lệ. Vui lòng kiểm tra lại.")
                .path(extractPath(request))
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==========================
    // ⚙️ BIND & CONSTRAINT ERRORS
    // ==========================
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    // ==========================
    // ⚙️ JSON PARSE / UUID / FORMAT ERRORS
    // ==========================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException ex, WebRequest request) {
        String msg = ex.getMessage();
        String viMessage;

        if (msg != null && msg.contains("UUID")) {
            viMessage = "Định dạng UUID không hợp lệ. Vui lòng kiểm tra lại ID được gửi lên.";
        } else if (msg != null && msg.contains("JSON parse error")) {
            viMessage = "Dữ liệu JSON không hợp lệ. Vui lòng kiểm tra lại nội dung gửi lên.";
        } else {
            viMessage = "Không thể đọc dữ liệu gửi lên. Kiểm tra lại định dạng JSON.";
        }

        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message(viMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==========================
    // ⚙️ DATABASE CONSTRAINT (FK, UNIQUE...)
    // ==========================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String viMessage = "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc. Vui lòng kiểm tra lại.";

        if (ex.getMessage() != null && ex.getMessage().contains("recipes_slug_key")) {
            viMessage = "Slug công thức đã tồn tại. Vui lòng chọn tiêu đề khác.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("recipe_ingredients_ingredient_id_fkey")) {
            viMessage = "Nguyên liệu không tồn tại trong hệ thống. Vui lòng kiểm tra lại danh sách nguyên liệu.";
        }

        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(ErrorCode.BAD_REQUEST.getCode())
                .message(viMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==========================
    // ⚙️ SECURITY / AUTH ERRORS
    // ==========================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Tên đăng nhập hoặc mật khẩu không đúng");
        error.put("status", "401");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UsernameNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "User không tồn tại");
        error.put("status", "404");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("status", "400");
        return ResponseEntity.badRequest().body(error);
    }

    // ==========================
    // ⚙️ WRONG HTTP METHOD
    // ==========================
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(ErrorCode.METHOD_NOT_ALLOWED.getCode())
                .message("Phương thức HTTP này không được hỗ trợ cho endpoint hiện tại.")
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // ==========================
    // ⚙️ GENERIC (UNCATEGORIZED) ERRORS
    // ==========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("Đã xảy ra lỗi hệ thống: " + ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(response);
    }

    // ==========================
    // ⚙️ Utility
    // ==========================
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
