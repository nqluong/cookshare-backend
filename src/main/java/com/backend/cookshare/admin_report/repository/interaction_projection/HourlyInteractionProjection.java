package com.backend.cookshare.admin_report.repository.interaction_projection;

public interface HourlyInteractionProjection {
    Integer getHour();
    Long getLikes();
    Long getComments();
    Long getSaves();
    Long getTotal();
}
