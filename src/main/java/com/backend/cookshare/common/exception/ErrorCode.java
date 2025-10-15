package com.backend.cookshare.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(9999, "Đã xảy ra lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    RECIPE_NOT_FOUND(1001, "Không tìm thấy công thức", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(1002, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1003, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(1004, "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    NOT_FOUND(1005, "Không tìm thấy dữ liệu yêu cầu", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
