package com.backend.cookshare.system.repository.projection;

import com.backend.cookshare.system.enums.ReportType;

public interface ReportCountProjection {
    ReportType getReportType();
    Long getCount();
}
