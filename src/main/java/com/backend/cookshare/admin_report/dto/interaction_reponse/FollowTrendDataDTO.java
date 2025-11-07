package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FollowTrendDataDTO {
    LocalDateTime date;
    Long newFollows;
    Long cumulativeFollows;
    BigDecimal growthRate;
}
