package com.backend.cookshare.system.service.notification.resolver;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportTargetResolver {

    private final ReportQueryRepository queryRepository;

    public ReportTarget resolve(Report report) {
        if (report.getReportedId() != null) {
            String username = queryRepository.findUsernameById(report.getReporterId()).orElseThrow(
                    () -> new CustomException(ErrorCode.USER_NOT_FOUND)
            );

            return new ReportTarget("USER", username);
        }

        if (report.getRecipeId() != null) {
            String title = queryRepository.findRecipeTitleById(report.getRecipeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

            return new ReportTarget("RECIPE", title);
        }

        return new ReportTarget("UNKNOWN", "Unknown");
    }

    public record ReportTarget(String type, String name) {}
}