package com.backend.cookshare.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * Khởi tạo ngoại lệ với mã lỗi (ErrorCode) được cung cấp.
     * Thông báo lỗi sẽ được lấy từ ErrorCode.getMessage().
     *
     * @param errorCode Mã lỗi xác định loại ngoại lệ (ví dụ: USER_NOT_FOUND).
     */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Khởi tạo ngoại lệ với mã lỗi (ErrorCode) và thông báo tùy chỉnh.
     * Thông báo tùy chỉnh sẽ ghi đè thông báo mặc định từ ErrorCode.
     *
     * @param errorCode Mã lỗi xác định loại ngoại lệ.
     * @param message Thông báo lỗi tùy chỉnh để cung cấp chi tiết hơn.
     */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
