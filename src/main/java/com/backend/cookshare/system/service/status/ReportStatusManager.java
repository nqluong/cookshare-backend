package com.backend.cookshare.system.service.status;

import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import org.springframework.stereotype.Component;

@Component
public class ReportStatusManager {
    public ReportStatus determineStatusFromAction(ReportActionType actionType) {
        return switch (actionType) {
            case NO_ACTION -> ReportStatus.REJECTED;
            case USER_WARNED, RECIPE_EDITED, OTHER -> ReportStatus.RESOLVED;
            case USER_SUSPENDED, USER_BANNED, RECIPE_UNPUBLISHED, CONTENT_REMOVED ->
                    ReportStatus.APPROVED;
        };
    }
}
