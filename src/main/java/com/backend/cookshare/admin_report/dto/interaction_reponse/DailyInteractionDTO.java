package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyInteractionDTO {
    private String dayOfWeek;          // Thứ trong tuần
    private Integer dayNumber;         // Số thứ tự (1=Monday, 7=Sunday)
    private Long totalInteractions;
    private Long likes;
    private Long comments;
    private Long saves;
}