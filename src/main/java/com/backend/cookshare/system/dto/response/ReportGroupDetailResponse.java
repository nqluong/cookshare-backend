package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportGroupDetailResponse {
     String targetType;
     UUID targetId;
     String targetTitle;
     Long reportCount;
     Double weightedScore;
     ReportType mostSevereType;
     Map<ReportType, Long> reportTypeBreakdown;
     Boolean exceedsThreshold;
     Double threshold;
     List<ReportDetailInGroupResponse> reports;
}
