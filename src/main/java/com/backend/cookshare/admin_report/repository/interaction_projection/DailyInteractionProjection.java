package com.backend.cookshare.admin_report.repository.interaction_projection;

public interface DailyInteractionProjection {
    Integer getDayOfWeek();
    Long getLikes();
    Long getComments();
    Long getSaves();
    Long getTotal();
}
