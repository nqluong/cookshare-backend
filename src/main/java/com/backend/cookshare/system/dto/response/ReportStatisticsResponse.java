package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportStatisticsResponse {
    // Thống kê theo reports (đếm từng báo cáo riêng lẻ)
    Long totalReports;          // Tổng số báo cáo
    Long pendingReports;        // Số báo cáo đang chờ
    Long resolvedReports;       // Số báo cáo đã xử lý
    Long rejectedReports;       // Số báo cáo bị từ chối
    
    // Thống kê theo recipes (đếm số recipes bị báo cáo)
    Long totalReportedRecipes;  // Tổng số recipes bị báo cáo
    Long recipesWithPendingReports;  // Số recipes có báo cáo pending
    
    Map<ReportType, Long> reportsByType;
    List<TopReportedItem> topReportedUsers;
    List<TopReportedItem> topReportedRecipes;

}
