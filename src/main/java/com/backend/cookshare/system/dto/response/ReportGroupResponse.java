package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.checkerframework.checker.units.qual.N;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportGroupResponse {
    // Target information
     String targetType;          // "USER" or "RECIPE"
     UUID targetId;
     String targetTitle;         // Recipe title hoặc username
     String authorUsername;      // Cho recipes
     String avatarUrl;           // Avatar của user hoặc thumbnail của recipe

    // Aggregate statistics
     Long reportCount;
     Double weightedScore;
     ReportType mostSevereType;
     LocalDateTime latestReportTime;
     LocalDateTime oldestReportTime;

    // Report breakdown
     Map<ReportType, Long> reportTypeBreakdown;

    // Status flags
     Boolean autoActioned;
     Boolean exceedsThreshold;
     String priority;            // "CRITICAL", "HIGH", "MEDIUM", "LOW"

    // Preview of reporters (first 3)
     List<String> topReporters;

    // Related reports (optional, loaded on demand)
     List<ReportDetailInGroupResponse> reports;
}
