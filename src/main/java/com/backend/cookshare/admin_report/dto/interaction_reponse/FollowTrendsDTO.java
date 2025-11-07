package com.backend.cookshare.admin_report.dto.interaction_reponse;

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
public class FollowTrendsDTO {
     List<FollowTrendDataDTO> trendData;
     Long totalNewFollows;
     Long totalUnfollows;
     Long netFollowGrowth;
     BigDecimal followGrowthRate;
     LocalDateTime periodStart;
     LocalDateTime periodEnd;
}