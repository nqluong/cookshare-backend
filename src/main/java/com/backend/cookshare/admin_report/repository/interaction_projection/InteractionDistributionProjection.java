package com.backend.cookshare.admin_report.repository.interaction_projection;

public interface InteractionDistributionProjection {
    Long getRange0To10();
    Long getRange11To50();
    Long getRange51To100();
    Long getRange101To500();
    Long getRangeOver500();
}
