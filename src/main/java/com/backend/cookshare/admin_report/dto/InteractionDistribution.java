package com.backend.cookshare.admin_report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionDistribution {
    private Long count0to10;       // Số công thức có 0-10 tương tác
    private Long count11to50;      // Số công thức có 11-50 tương tác
    private Long count51to100;     // Số công thức có 51-100 tương tác
    private Long count101to500;    // Số công thức có 101-500 tương tác
    private Long countOver500;     // Số công thức có >500 tương tác
}