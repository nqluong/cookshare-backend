package com.backend.cookshare.system.dto.request;

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
public class ReportFilterRequest {
    ReportType reportType;
    ReportStatus status;
    UUID reporterId;
    UUID reportedId;
    UUID recipeId;
    LocalDateTime fromDate;
    LocalDateTime toDate;
    int page = 0;
    int size = 20;
    String sortBy = "createdAt";
    String sortDirection = "DESC";
}
