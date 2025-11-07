package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PopularIngredientsDTO {
     List<IngredientSearchStatsDTO> ingredients;
     Integer totalCount;
     LocalDateTime periodStart;
     LocalDateTime periodEnd;
}
