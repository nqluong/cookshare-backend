package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyInteractionDTO {
    private Integer hour;              // Giờ trong ngày (0-23)
    private Long totalInteractions;    // Tổng số tương tác
    private Long likes;
    private Long comments;
    private Long saves;
    private BigDecimal averageInteractionsPerHour;
}

