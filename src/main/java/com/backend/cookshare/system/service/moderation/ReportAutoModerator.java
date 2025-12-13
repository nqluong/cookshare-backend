package com.backend.cookshare.system.service.moderation;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.response.ModerationScore;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.ReportedRecipeInfoProjection;
import com.backend.cookshare.system.repository.projection.UsernameProjection;
import com.backend.cookshare.system.service.ReportNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    private static final Map<ReportType, Double> SEVERITY_WEIGHTS = new EnumMap<>(ReportType.class);

    static {
        SEVERITY_WEIGHTS.put(ReportType.HARASSMENT, 2.0);     // Nghiêm trọng nhất
        SEVERITY_WEIGHTS.put(ReportType.COPYRIGHT, 1.5);      // Rất nghiêm trọng
        SEVERITY_WEIGHTS.put(ReportType.INAPPROPRIATE, 1.5);  // Rất nghiêm trọng
        SEVERITY_WEIGHTS.put(ReportType.FAKE, 1.2);           // Nghiêm trọng
        SEVERITY_WEIGHTS.put(ReportType.MISLEADING, 1.0);     // Trung bình
        SEVERITY_WEIGHTS.put(ReportType.SPAM, 0.8);           // Ít nghiêm trọng hơn
        SEVERITY_WEIGHTS.put(ReportType.OTHER, 0.5);          // Ít nghiêm trọng nhất
    }

    private static final double AUTO_DISABLE_USER_SCORE_THRESHOLD = 12.0;
    private static final double AUTO_UNPUBLISH_RECIPE_SCORE_THRESHOLD = 6.0;

    public void checkAutoModeration(UUID reportedId, UUID recipeId) {
        if (reportedId != null) {
            checkUserThreshold(reportedId);
        }

        if (recipeId != null) {
            checkRecipeThreshold(recipeId);
        }
    }


    /**
     * Kiểm tra ngưỡng user với weighted scoring
     */
    private void checkUserThreshold(UUID userId) {
        // Lấy tất cả pending reports của user
        List<Report> pendingReports = reportRepository.findPendingReportsByUserId(userId);

        if (pendingReports.isEmpty()) {
            return;
        }

        // Tính điểm theo độ nghiêm trọng
        ModerationScore score = calculateModerationScore(pendingReports);

        log.debug("User {} moderation score: {}/{} reports, weighted: {}/{}",
                userId,
                score.getTotalCount(),
                AUTO_DISABLE_USER_THRESHOLD,
                score.getWeightedScore(),
                AUTO_DISABLE_USER_SCORE_THRESHOLD);

        // Kiểm tra theo cả 2 tiêu chí: số lượng HOẶC điểm số
        boolean exceedsCountThreshold = score.getTotalCount() >= AUTO_DISABLE_USER_THRESHOLD;
        boolean exceedsScoreThreshold = score.getWeightedScore() >= AUTO_DISABLE_USER_SCORE_THRESHOLD;

        if (exceedsCountThreshold || exceedsScoreThreshold) {
            autoDisableUser(userId, score);
        }
    }

    /**
     * Kiểm tra ngưỡng recipe với weighted scoring
     */
    private void checkRecipeThreshold(UUID recipeId) {
        // Lấy tất cả pending reports của recipe
        List<Report> pendingReports = reportRepository.findPendingReportsByRecipeId(recipeId);

        if (pendingReports.isEmpty()) {
            return;
        }

        // Tính điểm theo độ nghiêm trọng
        ModerationScore score = calculateModerationScore(pendingReports);

        // Kiểm tra theo cả 2 tiêu chí
        boolean exceedsCountThreshold = score.getTotalCount() >= AUTO_UNPUBLISH_RECIPE_THRESHOLD;
        boolean exceedsScoreThreshold = score.getWeightedScore() >= AUTO_UNPUBLISH_RECIPE_SCORE_THRESHOLD;

        if (exceedsCountThreshold || exceedsScoreThreshold) {
            autoUnpublishRecipe(recipeId, score);
        }
    }

    /**
     * Tính điểm moderation dựa trên loại và số lượng reports
     */
    private ModerationScore calculateModerationScore(List<Report> reports) {
        Map<ReportType, Long> reportTypeCount = new EnumMap<>(ReportType.class);

        // Đếm số lượng từng loại report
        for (Report report : reports) {
            reportTypeCount.merge(report.getReportType(), 1L, Long::sum);
        }

        double weightedScore = 0.0;

        // Tính điểm có trọng số
        for (Map.Entry<ReportType, Long> entry : reportTypeCount.entrySet()) {
            ReportType type = entry.getKey();
            long count = entry.getValue();
            double weight = SEVERITY_WEIGHTS.getOrDefault(type, 1.0);

            // Điểm = số lượng × trọng số
            weightedScore += count * weight;
        }

        return new ModerationScore(
                reports.size(),
                weightedScore,
                reportTypeCount,
                findMostSevereType(reportTypeCount)
        );
    }

    private ReportType findMostSevereType(Map<ReportType, Long> reportTypeCount) {
        return reportTypeCount.entrySet().stream()
                .max((e1, e2) -> {
                    double score1 = e1.getValue() * SEVERITY_WEIGHTS.getOrDefault(e1.getKey(), 1.0);
                    double score2 = e2.getValue() * SEVERITY_WEIGHTS.getOrDefault(e2.getKey(), 1.0);
                    return Double.compare(score1, score2);
                })
                .map(Map.Entry::getKey)
                .orElse(ReportType.OTHER);
    }

    /**
     * Tự động vô hiệu hóa user
     */
    private void autoDisableUser(UUID userId, ModerationScore score) {
        // Kiểm tra xem đã bị disable chưa để tránh duplicate
        if (reportQueryRepository.isUserAlreadyDisabled(userId)) {
            log.debug("User {} already disabled, skipping auto-moderation", userId);
            return;
        }

        reportQueryRepository.disableUser(userId);

        String username = findUsernameById(userId);

        notificationService.notifyAutoDisableUser(userId, username, score.getTotalCount());

        log.warn("Auto-disabled user {} - {} reports (weighted score: {:.2f}), most severe: {}",
                username,
                score.getTotalCount(),
                score.getWeightedScore(),
                score.getMostSevereType());

        logReportBreakdown(score.getReportTypeCount());
    }

    /**
     * Tự động gỡ recipe
     */
    private void autoUnpublishRecipe(UUID recipeId, ModerationScore score) {
        // Kiểm tra xem đã unpublish chưa
        if (reportQueryRepository.isRecipeAlreadyUnpublished(recipeId)) {
            log.debug("Recipe {} already unpublished, skipping auto-moderation", recipeId);
            return;
        }

        reportQueryRepository.unpublishRecipe(recipeId);

        // Sử dụng batch query với 1 phần tử
        List<ReportedRecipeInfoProjection> recipeInfoList = reportQueryRepository
                .findReportedRecipeInfoByIds(List.of(recipeId));
        
        if (recipeInfoList.isEmpty()) {
            throw new CustomException(ErrorCode.REPORTED_RECIPE_NOT_FOUND);
        }
        
        ReportedRecipeInfoProjection recipeInfo = recipeInfoList.get(0);

        notificationService.notifyAutoUnpublishRecipe(
                recipeId,
                recipeInfo.getUserId(),
                recipeInfo.getAuthorUsername(),
                recipeInfo.getTitle(),
                score.getTotalCount()
        );


        logReportBreakdown(score.getReportTypeCount());
    }

    /**
     * Log chi tiết phân loại reports
     */
    private void logReportBreakdown(Map<ReportType, Long> reportTypeCount) {
        StringBuilder breakdown = new StringBuilder("Report breakdown: ");
        reportTypeCount.forEach((type, count) ->
                breakdown.append(String.format("%s(%d), ", type, count))
        );
        log.info(breakdown.toString());
    }

    private String findUsernameById(UUID userId) {
        List<UsernameProjection> results = reportQueryRepository.findUsernamesByIds(List.of(userId));
        if (results.isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return results.get(0).getUsername();
    }
}
