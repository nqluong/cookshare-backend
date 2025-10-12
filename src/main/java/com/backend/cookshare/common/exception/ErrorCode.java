package com.backend.cookshare.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor

public enum ErrorCode {
    INTERNAL_SERVER_ERROR(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    RECIPE_NOT_FOUND(1001, "Không tìm thấy công thức", HttpStatus.NOT_FOUND),
    SEARCH_QUERY_TOO_SHORT(3002, "Từ khóa tìm kiếm quá ngắn (tối thiểu 2 ký tự)", HttpStatus.BAD_REQUEST),
    SEARCH_QUERY_TOO_LONG(3003, "Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)", HttpStatus.BAD_REQUEST),
    SEARCH_QUERY_EMPTY(3004, "Từ khóa tìm kiếm không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_CHARACTERS(31005, "Từ khóa tìm kiếm chứa ký tự không hợp lệ", HttpStatus.BAD_REQUEST),

    VALIDATION_ERROR(1002, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1003, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST);
    INTERNAL_SERVER_ERROR(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND(1001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(1002, "Tài khoản người dùng không hoạt động", HttpStatus.BAD_REQUEST),
    CANNOT_FOLLOW_YOURSELF(2001, "Không thể follow chính mình", HttpStatus.BAD_REQUEST),
    ALREADY_FOLLOWING(2002, "Bạn đã follow người dùng này rồi", HttpStatus.BAD_REQUEST),
    NOT_FOLLOWING(2003, "Bạn chưa follow người dùng này", HttpStatus.BAD_REQUEST),
    FOLLOW_NOT_FOUND(2004, "Không tìm thấy quan hệ follow", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
