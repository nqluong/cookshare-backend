package com.backend.cookshare.system.enums;

public enum ReportStatus {
    PENDING,    // Đang chờ xử lý
    RESOLVED,   // Đã xử lý (báo cáo hợp lệ)
    REJECTED    // Đã từ chối (báo cáo không hợp lệ)
}
