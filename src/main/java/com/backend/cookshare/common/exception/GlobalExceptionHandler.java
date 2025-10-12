package com.backend.cookshare.common.exception;

import com.backend.cookshare.common.dto.ErrorResponse;
import com.backend.cookshare.common.dto.ValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * Xử lý ngoại lệ tùy chỉnh (CustomException) được ném trong ứng dụng.
     * Trả về phản hồi với mã lỗi, thông điệp, và trạng thái HTTP tương ứng từ ErrorCode.
     *
     * @param ex Ngoại lệ tùy chỉnh chứa thông tin ErrorCode.
     * @param request Đối tượng WebRequest chứa thông tin về yêu cầu HTTP.
     * @return ResponseEntity chứa ErrorResponse với trạng thái HTTP từ ErrorCode.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getErrorCode().getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(errorResponse);
    }

    /**
     * Xử lý ngoại lệ validation khi dữ liệu đầu vào không hợp lệ (MethodArgumentNotValidException).
     * Thu thập các lỗi validation từ các trường và trả về danh sách lỗi chi tiết.
     *
     * @param ex Ngoại lệ validation chứa thông tin về các lỗi trường.
     * @param request Đối tượng WebRequest chứa thông tin về yêu cầu HTTP.
     * @return ResponseEntity chứa ErrorResponse với trạng thái 400 và danh sách lỗi validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ValidationResponse.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(400)
                .message("Lỗi validation")
                .validationErrors(errors)
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
