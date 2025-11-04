package com.backend.cookshare.admin_report.dto.search_response;

import com.backend.cookshare.admin_report.dto.interaction_reponse.CategoryEngagementDTO;
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
public class EngagementByCategoryDTO {
    List<CategoryEngagementDTO> categoryEngagements;
    BigDecimal overallEngagementRate;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
}
