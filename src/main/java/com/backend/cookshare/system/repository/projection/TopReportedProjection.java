package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

public interface TopReportedProjection {
    UUID getItemId();
    Long getReportCount();
}
