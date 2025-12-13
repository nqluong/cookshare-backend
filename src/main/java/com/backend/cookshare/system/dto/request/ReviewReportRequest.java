package com.backend.cookshare.system.dto.request;

import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewReportRequest {
    @NotNull(message = "Status không được để trống")
    ReportStatus status;

    ReportActionType actionType;

    String actionDescription;

    String adminNote;

    Boolean notifyAllReporters = true;
}
