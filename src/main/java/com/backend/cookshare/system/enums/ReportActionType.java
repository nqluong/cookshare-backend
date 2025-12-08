package com.backend.cookshare.system.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportActionType {
    NO_ACTION("Không có hành động", "Báo cáo không vi phạm chính sách"),
    USER_WARNED("Cảnh cáo người dùng", "Người dùng bị cảnh báo về hành vi vi phạm"),
    USER_SUSPENDED("Tạm khóa tài khoản", "Tài khoản bị tạm khóa do vi phạm nghiêm trọng"),
    USER_BANNED("Vĩnh viễn cấm", "Tài khoản bị cấm vĩnh viễn do vi phạm nghiêm trọng nhiều lần"),
    RECIPE_UNPUBLISHED("Gỡ công thức", "Công thức đã bị gỡ xuống do vi phạm chính sách"),
    RECIPE_EDITED("Yêu cầu chỉnh sửa", "Yêu cầu tác giả chỉnh sửa nội dung vi phạm"),
    CONTENT_REMOVED("Xóa nội dung", "Nội dung vi phạm đã bị xóa"),
    OTHER("Hành động khác", "Xem mô tả chi tiết");

    private final String displayName;
    private final String description;
}
