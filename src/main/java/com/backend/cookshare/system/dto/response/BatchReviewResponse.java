package com.backend.cookshare.system.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchReviewResponse {
     String targetType;
     UUID targetId;
     Integer totalReportsAffected;
     String actionTaken;
     String status;

    // Danh sách reports đã được review
     List<UUID> reviewedReportIds;

    // Thông báo
     String message;
}
