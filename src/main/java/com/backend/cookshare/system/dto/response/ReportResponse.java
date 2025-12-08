package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
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
public class ReportResponse {
    UUID reportId;
    ReporterInfo reporter;
    ReportedUserInfo reportedUser;
    ReportedRecipeInfo reportedRecipe;
    ReportType reportType;
    String reason;
    String description;
    ReportStatus status;
    ReportActionType actionTaken;
    String actionDescription;
    String adminNote;
    ReviewerInfo reviewer;
    LocalDateTime reviewedAt;
    LocalDateTime createdAt;
}
