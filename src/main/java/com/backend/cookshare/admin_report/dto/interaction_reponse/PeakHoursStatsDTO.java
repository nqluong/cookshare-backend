package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeakHoursStatsDTO {
    private List<HourlyInteractionDTO> hourlyStats;
    private List<DailyInteractionDTO> dailyStats;
    private Integer peakHour;           // Giờ có nhiều tương tác nhất
    private String peakDayOfWeek;       // Thứ có nhiều tương tác nhất
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
