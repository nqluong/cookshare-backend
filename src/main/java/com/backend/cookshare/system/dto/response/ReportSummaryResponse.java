package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportSummaryResponse {
    private UUID reportId;
    private String reporterUsername;
    private ReportType reportType;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
