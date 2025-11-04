package com.backend.cookshare.admin_report.dto.recipe_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TimeSeriesStatDTO {
    String period; // Ngày/Tuần/Tháng
    Long recipeCount;
    Long viewCount;
    Long likeCount;
    Long commentCount;
    LocalDateTime timestamp;
}