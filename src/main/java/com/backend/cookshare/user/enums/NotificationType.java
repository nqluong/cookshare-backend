package com.backend.cookshare.user.enums;

public enum NotificationType {
    // Social notifications
    FOLLOW,           // Có người follow
    LIKE,            // Có người like recipe/comment
    COMMENT,         // Có người comment
    MENTION,         // Được mention trong comment
    SHARE,           // Recipe được share
    RATING,          // Recipe được rating

    // Recipe notifications
    RECIPE_PUBLISHED,    // Recipe được publish thành công
    RECIPE_STATUS,       // Trạng thái recipe thay đổi (unpublished, edit required)

    // Report & Moderation notifications
    REPORT_REVIEW,       // Kết quả review báo cáo (cho người báo cáo)
    REPORT_ACTION,       // Admin thực hiện action với báo cáo

    // Account & Security notifications
    WARNING,             // Cảnh báo vi phạm
    ACCOUNT_STATUS,      // Trạng thái tài khoản (suspended, banned)
    SECURITY,            // Bảo mật (đổi password, login mới...)

    // System notifications
    SYSTEM,              // Thông báo hệ thống chung
    ANNOUNCEMENT         // Thông báo quan trọng từ admin
}
