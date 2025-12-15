package com.backend.cookshare.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor

public enum ErrorCode {
    INTERNAL_SERVER_ERROR(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    RECIPE_NOT_FOUND(1001, "Không tìm thấy công thức", HttpStatus.NOT_FOUND),
    RECIPE_NOT_APPROVED(1002, "Công thức chưa được duyệt", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(1003, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1004, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(1005, "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    NOT_FOUND(1006, "Không tìm thấy dữ liệu yêu cầu", HttpStatus.NOT_FOUND),
    REQUEST_TIMEOUT(1007, "Yêu cầu timeout", HttpStatus.REQUEST_TIMEOUT),
    RECIPE_APPROVAL(1008, "Công thức đã được duyệt bạn không thể chỉnh sửa", HttpStatus.BAD_REQUEST),

    TAG_NOT_FOUND(1101, "Không tìm thấy thẻ (Tag)", HttpStatus.NOT_FOUND),
    TAG_ALREADY_EXISTS(1102, "Thẻ (Tag) đã tồn tại", HttpStatus.CONFLICT),

    // 🔹 Category errors
    CATEGORY_NOT_FOUND(1301, "Không tìm thấy danh mục (Category)", HttpStatus.NOT_FOUND),
    CATEGORY_ALREADY_EXISTS(1302, "Danh mục (Category) đã tồn tại", HttpStatus.CONFLICT),

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
    OTP_NOT_VERIFIED(4012, "Bạn chưa xác thực OTP. Vui lòng xác thực OTP trước khi đặt lại mật khẩu",
            HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED(4013, "Gửi email thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_EXISTED(4014, "Username đã tồn tại", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(4015, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_ALREADY_BANNED(4016, "Người dùng đã bị cấm trước đó", HttpStatus.BAD_REQUEST),
    USER_ALREADY_ACTIVE(4017, "Người dùng đã đang hoạt động", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_UPDATE(4020, "Bạn không có quyền cập nhật thông tin người dùng này", HttpStatus.FORBIDDEN),

    // Google OAuth errors (5xxx)
    GOOGLE_AUTH_ERROR(4018, "Lỗi xác thực với Google", HttpStatus.UNAUTHORIZED),
    // Facebook OAuth errors (5xxx)
    FACEBOOK_AUTH_ERROR(4019, "Lỗi xác thực với Facebook", HttpStatus.UNAUTHORIZED),
    INVALID_OAUTH_PROVIDER(4021, "OAuth provider không hợp lệ", HttpStatus.BAD_REQUEST),

    // Collection errors (4xxx)
    COLLECTION_NOT_FOUND(4001, "Không tìm thấy bộ sưu tập", HttpStatus.NOT_FOUND),
    RECIPE_ALREADY_IN_COLLECTION(4002, "Công thức đã có trong bộ sưu tập", HttpStatus.BAD_REQUEST),
    RECIPE_NOT_IN_COLLECTION(4003, "Công thức không có trong bộ sưu tập", HttpStatus.NOT_FOUND),
    COLLECTION_NAME_DUPLICATE(4004, "Tên bộ sưu tập đã tồn tại", HttpStatus.BAD_REQUEST),
    RECIPE_NOT_PUBLISHED(4006, "Công thức chưa được publish", HttpStatus.BAD_REQUEST),

    // 🔹 Notification errors (✨ thêm mới)
    NOTIFICATION_NOT_FOUND(5001, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    NOTIFICATION_FORBIDDEN(5002, "Không có quyền truy cập thông báo này", HttpStatus.FORBIDDEN),
    RECIPE_ALREADY_LIKED(6003, "Công thức đã được thích", HttpStatus.CONFLICT),
    RECIPE_NOT_LIKED(6004, "Công thức chưa được thích", HttpStatus.BAD_REQUEST),

    REPORT_NOT_FOUND(7001, "Không tìm thấy báo cáo", HttpStatus.NOT_FOUND),
    REPORT_ALREADY_EXISTS(7002, "Bạn đã báo cáo công thức này rồi", HttpStatus.CONFLICT),
    REPORT_ALREADY_REVIEWED(7003, "Báo cáo này đã được xem xét", HttpStatus.BAD_REQUEST),
    CANNOT_REPORT_YOURSELF(7004, "Không thể tự báo cáo chính mình", HttpStatus.BAD_REQUEST),
    REPORT_TARGET_REQUIRED(7005, "Phải chỉ định người dùng hoặc công thức để báo cáo", HttpStatus.BAD_REQUEST),
    REPORTED_USER_NOT_FOUND(7006, "Người dùng bị báo cáo không tồn tại", HttpStatus.NOT_FOUND),
    REPORTED_RECIPE_NOT_FOUND(7007, "Công thức bị báo cáo không tồn tại", HttpStatus.NOT_FOUND),
    REPORT_FORBIDDEN(7008, "Bạn không có quyền truy cập báo cáo này", HttpStatus.FORBIDDEN),
    INVALID_REPORT_STATUS(7009, "Trạng thái báo cáo không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_NOT_AUTHENTICATED(7010, "Người dùng chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    NO_PENDING_REPORTS(7011, "Không có báo cáo chờ xử lý", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
