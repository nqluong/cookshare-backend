package com.backend.cookshare.admin_report.repository.interaction_projection;

import java.time.LocalDateTime;

public interface FollowTrendProjection {
    LocalDateTime getPeriodDate();
    Long getNewFollows();
    Long getCumulativeFollows();
}
