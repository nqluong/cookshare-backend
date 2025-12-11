package com.backend.cookshare.system.service.notification.resolver;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.response.RecipeInfo;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
@RequiredArgsConstructor
public class ReportTargetResolver {

    private final ReportQueryRepository queryRepository;

    /**
     * Xác định target của report.
     * Ưu tiên: Recipe > User > Unknown
     */
    public ReportTarget resolve(Report report) {
        // Ưu tiên kiểm tra Recipe trước (vì logic chỉ có báo cáo Recipe)
        if (report.getRecipeId() != null) {
            RecipeInfo recipeInfo = queryRepository.findRecipeInfoById(report.getRecipeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

            return new ReportTarget("RECIPE", recipeInfo.getTitle());
        }

        // Fallback: Nếu có reportedId (legacy)
        if (report.getReportedId() != null) {
            String username = findUsernameById(report.getReportedId());
            return new ReportTarget("USER", username);
        }

        return new ReportTarget("UNKNOWN", "Unknown");
    }

    /**
     * Helper: Tìm username bằng batch query với 1 phần tử.
     */
    private String findUsernameById(java.util.UUID userId) {
        List<UsernameProjection> results = queryRepository.findUsernamesByIds(List.of(userId));
        if (results.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return results.getFirst().getUsername();
    }

    public record ReportTarget(String type, String name) {}
}