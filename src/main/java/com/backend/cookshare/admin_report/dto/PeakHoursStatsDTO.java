package com.backend.cookshare.admin_report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
