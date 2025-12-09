package com.backend.cookshare.system.service;

import com.backend.cookshare.system.entity.Report;

import java.util.UUID;

public interface ReportNotificationService {

    void notifyAdminsNewReport(Report report, String reporterUsername);

    void notifyReporterReviewComplete(Report report, String reporterUsername, UUID reporterId);

    void broadcastPendingCountUpdate(long pendingCount);
}
