package com.backend.cookshare.system.service.moderation;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.ReportedRecipeInfoProjection;
import com.backend.cookshare.system.service.ReportNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportAutoModerator {

    private final ReportRepository reportRepository;
    private final ReportQueryRepository reportQueryRepository;
    private final ReportNotificationService notificationService;

    private static final int AUTO_DISABLE_USER_THRESHOLD = 10;
    private static final int AUTO_UNPUBLISH_RECIPE_THRESHOLD = 5;

    public void checkAutoModeration(UUID reportedId, UUID recipeId) {
        if (reportedId != null) {
            checkUserThreshold(reportedId);
        }

        if (recipeId != null) {
            checkRecipeThreshold(recipeId);
        }
    }

    private void checkUserThreshold(UUID userId) {
        long count = reportRepository.countPendingReportsByUserId(userId);

        if (count >= AUTO_DISABLE_USER_THRESHOLD) {
            autoDisableUser(userId, count);
        }
    }

    private void checkRecipeThreshold(UUID recipeId) {
        long count = reportRepository.countPendingReportsByRecipeId(recipeId);

        if (count >= AUTO_UNPUBLISH_RECIPE_THRESHOLD) {
            autoUnpublishRecipe(recipeId, count);
        }
    }

    private void autoDisableUser(UUID userId, long reportCount) {
        reportQueryRepository.disableUser(userId);

        String username = reportQueryRepository.findUsernameById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

//        notificationService.notifyAutoDisableUser(userId, username, reportCount);

        log.warn("Auto-disabled user {} due to {} pending reports", username, reportCount);
    }

    private void autoUnpublishRecipe(UUID recipeId, long reportCount) {
        reportQueryRepository.unpublishRecipe(recipeId);

        ReportedRecipeInfoProjection recipeInfo = reportQueryRepository
                .findReportedRecipeInfoById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORTED_RECIPE_NOT_FOUND));

//        notificationService.notifyAutoUnpublishRecipe(
//                recipeId,
//                recipeInfo.getAuthorUsername(),
//                recipeInfo.getTitle(),
//                reportCount
//        );

        log.warn("Auto-unpublished recipe {} due to {} pending reports", recipeId, reportCount);
    }
}
