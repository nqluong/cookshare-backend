package com.backend.cookshare.system.dto.request;

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
public class NewReportNotification {
    UUID reportId;
    String reporterUsername;
    ReportType reportType;
    String targetType; // USER or RECIPE
    String targetName;
    LocalDateTime createdAt;
}
