package com.backend.cookshare.system.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetStatisticsResponse {
    // Số lượng công thức bị báo cáo
    Long totalReportedRecipes;

    // Công thức vượt ngưỡng
    Long recipesExceedingThreshold;

    // Trung bình báo cáo mỗi công thức
    Double avgReportsPerRecipe;

    // Phân phối ưu tiên theo công thức
    Long criticalPriorityRecipes;
    Long highPriorityRecipes;
    Long mediumPriorityRecipes;
    Long lowPriorityRecipes;
}
