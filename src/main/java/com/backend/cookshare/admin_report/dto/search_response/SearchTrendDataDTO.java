package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchTrendDataDTO {
     LocalDateTime date;
     Long totalSearches;
     Long uniqueUsers;
     Long uniqueQueries;
     BigDecimal averageResultsPerSearch;
     BigDecimal successRate;
}
