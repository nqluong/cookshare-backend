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
public class ZeroResultKeywordsDTO {
    List<ZeroResultKeywordDTO> keywords;
    Integer totalCount;
    BigDecimal percentageOfTotal;   // % trên tổng tìm kiếm
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
}