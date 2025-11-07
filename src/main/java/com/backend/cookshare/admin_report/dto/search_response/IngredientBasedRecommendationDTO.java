package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngredientBasedRecommendationDTO {
     Long totalIngredientSearches;    // Tổng tìm kiếm theo nguyên liệu
     Long totalRecommendations;       // Tổng gợi ý được tạo
     BigDecimal averageAccuracy;      // Độ chính xác trung bình (%)
     BigDecimal usageRate;            // Tỷ lệ sử dụng gợi ý (%)
     List<IngredientRecommendationStatsDTO> topIngredients;
     LocalDateTime periodStart;
     LocalDateTime periodEnd;
}

