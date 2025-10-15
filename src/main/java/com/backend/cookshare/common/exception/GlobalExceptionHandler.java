package com.backend.cookshare.common.exception;

import com.backend.cookshare.common.dto.ErrorResponse;
import com.backend.cookshare.common.dto.ValidationResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ⚙️ Xử lý các lỗi tùy chỉnh do developer ném ra
     */
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

    /**
     * ⚙️ Xử lý lỗi validate (ví dụ @NotBlank, @Positive, ...)
     */
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

    /**
     * ⚙️ Lỗi JSON hoặc UUID không hợp lệ
     */
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

    /**
     * ⚙️ Lỗi vi phạm ràng buộc CSDL (ví dụ slug trùng, FK không tồn tại)
     */
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

    /**
     * ⚙️ Lỗi khi client gọi sai method (GET/POST/PUT...)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String viMessage = "Phương thức HTTP này không được hỗ trợ cho endpoint hiện tại.";

        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(ErrorCode.METHOD_NOT_ALLOWED.getCode())
                .message(viMessage)
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * ⚙️ Lỗi hệ thống chung
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .succes(false)
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("Đã xảy ra lỗi hệ thống: " + ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
