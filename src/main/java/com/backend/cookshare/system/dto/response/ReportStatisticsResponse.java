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
    Long totalReports;
    Long pendingReports;
    Long approvedReports;
    Long rejectedReports;
    Map<ReportType, Long> reportsByType;
    List<TopReportedItem> topReportedUsers;
    List<TopReportedItem> topReportedRecipes;
}
