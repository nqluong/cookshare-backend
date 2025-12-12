package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO phản hồi chi tiết cho nhóm báo cáo theo Recipe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportGroupDetailResponse {
    // Thông tin Recipe
    UUID recipeId;
    String recipeTitle;
    String recipeThumbnail;
    
    // Thông tin tác giả
    UUID authorId;
    String authorUsername;
    String authorFullName;
    String authorAvatar;
    
    // Thống kê
    Long reportCount;
    Double weightedScore;
    ReportType mostSevereType;
    Map<ReportType, Long> reportTypeBreakdown;
    Boolean exceedsThreshold;
    Double threshold;
    
    // Danh sách báo cáo chi tiết
    List<ReportDetailInGroupResponse> reports;
}
