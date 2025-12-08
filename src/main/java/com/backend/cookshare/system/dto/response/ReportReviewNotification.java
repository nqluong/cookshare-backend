package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportReviewNotification {
    private UUID reportId;
    private ReportStatus status;
    private ReportActionType actionTaken;
    private String actionDescription;
    private LocalDateTime reviewedAt;
    private String message;
}
