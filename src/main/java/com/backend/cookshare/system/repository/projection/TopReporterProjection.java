package com.backend.cookshare.system.repository.projection;

import java.util.UUID;


public interface TopReporterProjection {
    UUID getRecipeId();
    UUID getReporterId();
    String getReporterUsername();
    String getReporterFullName();
}
