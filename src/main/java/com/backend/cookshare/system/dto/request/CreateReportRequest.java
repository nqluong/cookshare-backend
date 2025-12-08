package com.backend.cookshare.system.dto.request;

import com.backend.cookshare.system.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateReportRequest {

    @NotNull(message = "Report type is required")
    ReportType reportType;

    UUID reportedId; // ID của user bị báo cáo (nullable)

    UUID recipeId; // ID của recipe bị báo cáo (nullable)

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    String reason;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description;
}
