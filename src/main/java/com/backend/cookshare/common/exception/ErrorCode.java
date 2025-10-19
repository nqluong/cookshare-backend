package com.backend.cookshare.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor

public enum ErrorCode {
    INTERNAL_SERVER_ERROR(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    RECIPE_NOT_FOUND(1001, "Không tìm thấy công thức", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(1002, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1003, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),

    CANNOT_FOLLOW_YOURSELF(2001, "Không thể follow chính mình", HttpStatus.BAD_REQUEST),
    ALREADY_FOLLOWING(2002, "Bạn đã follow người dùng này rồi", HttpStatus.BAD_REQUEST),
    NOT_FOLLOWING(2003, "Bạn chưa follow người dùng này", HttpStatus.BAD_REQUEST),
    FOLLOW_NOT_FOUND(2004, "Không tìm thấy quan hệ follow", HttpStatus.NOT_FOUND),

    SEARCH_QUERY_TOO_SHORT(3002, "Từ khóa tìm kiếm quá ngắn (tối thiểu 2 ký tự)", HttpStatus.BAD_REQUEST),
    SEARCH_QUERY_TOO_LONG(3003, "Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)", HttpStatus.BAD_REQUEST),
    SEARCH_QUERY_EMPTY(3004, "Từ khóa tìm kiếm không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_CHARACTERS(3105, "Từ khóa tìm kiếm chứa ký tự không hợp lệ", HttpStatus.BAD_REQUEST),


    USER_NOT_FOUND(4001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(4002, "Tài khoản người dùng không hoạt động", HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_TOKEN(4003, "refresh token không hợp lệ", HttpStatus.UNAUTHORIZED),
    INVALID_ACCESS_TOKEN(4004, "access token không hợp lệ", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(4005, "Bạn không có quyền truy cập tài nguyên này", HttpStatus.FORBIDDEN),
    INVALID_CURRENT_PASSWORD(4006, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(4007, "Mật khẩu mới và xác nhận mật khẩu không khớp", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD(4008, "Mật khẩu mới không được trùng với mật khẩu hiện tại", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(4009, "Địa chỉ email không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(4010, "Mã OTP không hợp lệ", HttpStatus.NOT_FOUND),
    OTP_EXPIRED(4011, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_NOT_VERIFIED(4012, "Bạn chưa xác thực OTP. Vui lòng xác thực OTP trước khi đặt lại mật khẩu", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(4013, "Gửi email thất bại", HttpStatus.INTERNAL_SERVER_ERROR)
    ;


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
