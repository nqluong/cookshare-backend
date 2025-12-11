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
     UUID reportId;
     String reporterUsername;
     String reporterFullName;
     String reporterAvatar;
     ReportType reportType;
     String reason;
     String description;
     LocalDateTime createdAt;
}
