package com.backend.cookshare.system.repository.projection;

import com.backend.cookshare.system.enums.ReportType;

public interface ReportTypeCount {
    ReportType getType();
    Long getCount();
}
