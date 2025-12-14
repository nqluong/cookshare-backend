package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportDetailInGroupResponse {
     // Reporter info
     UUID reportId;
     UUID reporterId;
     String reporterUsername;
     String reporterFullName;
     String reporterAvatar;
     
     // Report info
     ReportType reportType;
     String reason;
     String description;
     String status;
     LocalDateTime createdAt;
     
     // Review info (nếu đã xử lý)
     String actionTaken;
     String actionDescription;
     String adminNote;
     UUID reviewedBy;
     String reviewerUsername;
     String reviewerFullName;
     LocalDateTime reviewedAt;
}
