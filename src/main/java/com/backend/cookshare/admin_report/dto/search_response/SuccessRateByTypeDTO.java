package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuccessRateByTypeDTO {
    private String searchType;             // "RECIPE", "INGREDIENT", "USER", "CATEGORY"
    private Long totalSearches;
    private Long successfulSearches;
    private BigDecimal successRate;
}
