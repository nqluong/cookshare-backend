package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO phản hồi cho nhóm báo cáo theo Recipe.
 * Chỉ hỗ trợ báo cáo Recipe, User bị xử lý thông qua tác giả của Recipe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportGroupResponse {
    // Thông tin Recipe bị báo cáo
    UUID recipeId;
    String recipeTitle;
    String recipeFeaturedImage;
    
    // Thông tin tác giả (để xử lý khi cần warn/suspend/ban user)
    UUID authorId;
    String authorUsername;
    String authorFullName;
    String authorAvatarUrl;

    // Thống kê tổng hợp
    Long reportCount;
    Double weightedScore;
    ReportType mostSevereType;
    LocalDateTime latestReportTime;
    LocalDateTime oldestReportTime;

    // Phân loại theo loại báo cáo
    Map<ReportType, Long> reportTypeBreakdown;

    // Cờ trạng thái
    Boolean autoActioned;
    Boolean exceedsThreshold;
    String priority;            // "CRITICAL", "HIGH", "MEDIUM", "LOW"

    // Xem trước người báo cáo (top 3)
    List<String> topReporters;

    // Danh sách báo cáo chi tiết (tải theo yêu cầu)
    List<ReportDetailInGroupResponse> reports;
}
