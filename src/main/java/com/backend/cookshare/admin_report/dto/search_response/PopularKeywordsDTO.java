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
public class PopularKeywordsDTO {
     List<KeywordStatsDTO> keywords;
     Integer totalUniqueKeywords;
     LocalDateTime periodStart;
     LocalDateTime periodEnd;
}
