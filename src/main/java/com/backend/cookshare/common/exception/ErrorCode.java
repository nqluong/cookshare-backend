package com.backend.cookshare.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor

public enum ErrorCode {
    INTERNAL_SERVER_ERROR(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
