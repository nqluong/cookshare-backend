package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngredientSearchStatsDTO {
     UUID ingredientId;
     String ingredientName;
     Long searchCount;              // Số lần tìm kiếm
     Long directSearches;           // Tìm kiếm trực tiếp nguyên liệu
     Long recipeSearches;           // Tìm kiếm công thức chứa nguyên liệu
     Long recipeCount;              // Số công thức chứa nguyên liệu
     BigDecimal searchToRecipeRatio; // Tỷ lệ tìm kiếm / số công thức
}
