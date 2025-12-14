package com.backend.cookshare.system.service.moderation;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.response.ModerationScore;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.ReportRepository;
import com.backend.cookshare.system.repository.projection.ReportedRecipeInfoProjection;
import com.backend.cookshare.system.service.ReportNotificationService;
import com.backend.cookshare.system.service.notification.ReportNotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    private final ReportNotificationOrchestrator notificationOrchestrator;

    // Ngưỡng tự động gỡ công thức
    private static final int AUTO_UNPUBLISH_RECIPE_THRESHOLD = 5;
    private static final double AUTO_UNPUBLISH_RECIPE_SCORE_THRESHOLD = 6.0;

    // Trọng số độ nghiêm trọng theo loại báo cáo
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

    /**
     * Kiểm tra ngưỡng tự động xử lý cho công thức
     */
    public boolean checkAutoModeration(UUID recipeId) {
        if (recipeId == null) {
            return false;
        }

        return checkRecipeThreshold(recipeId);
    }


    /**
     * Kiểm tra ngưỡng công thức với tính điểm trọng số
     * @return true nếu có hành động được thực hiện
     */
    private boolean checkRecipeThreshold(UUID recipeId) {
        // Lấy tất cả báo cáo đang chờ của công thức
        List<Report> pendingReports = reportRepository.findPendingReportsByRecipeId(recipeId);

        if (pendingReports.isEmpty()) {
            return false;
        }

        // Tính điểm theo độ nghiêm trọng
        ModerationScore score = calculateModerationScore(pendingReports);

        log.debug("Recipe {} - Điểm moderation: {}/{} báo cáo, trọng số: {}/{}",
                recipeId,
                score.getTotalCount(),
                AUTO_UNPUBLISH_RECIPE_THRESHOLD,
                score.getWeightedScore(),
                AUTO_UNPUBLISH_RECIPE_SCORE_THRESHOLD);

        // Kiểm tra theo cả 2 tiêu chí: số lượng HOẶC điểm số
        boolean exceedsCountThreshold = score.getTotalCount() >= AUTO_UNPUBLISH_RECIPE_THRESHOLD;
        boolean exceedsScoreThreshold = score.getWeightedScore() >= AUTO_UNPUBLISH_RECIPE_SCORE_THRESHOLD;

        if (exceedsCountThreshold || exceedsScoreThreshold) {
            autoUnpublishRecipe(recipeId, score);
            return true;
        }

        return false;
    }

    /**
     * Tính điểm moderation dựa trên loại và số lượng báo cáo
     */
    private ModerationScore calculateModerationScore(List<Report> reports) {
        Map<ReportType, Long> reportTypeCount = new EnumMap<>(ReportType.class);

        // Đếm số lượng từng loại báo cáo
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

    /**
     * Tìm loại báo cáo nghiêm trọng nhất dựa trên điểm trọng số
     */
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
     * Tự động xử lý công thức dựa trên loại báo cáo nghiêm trọng nhất
     */
    private void autoUnpublishRecipe(UUID recipeId, ModerationScore score) {
        // Kiểm tra xem đã gỡ chưa để tránh xử lý trùng
        if (reportQueryRepository.isRecipeAlreadyUnpublished(recipeId)) {
            log.debug("Công thức {} đã được gỡ trước đó, bỏ qua auto-moderation", recipeId);
            return;
        }

        // Lấy thông tin công thức
        List<ReportedRecipeInfoProjection> recipeInfoList = reportQueryRepository
                .findReportedRecipeInfoByIds(List.of(recipeId));
        
        if (recipeInfoList.isEmpty()) {
            throw new CustomException(ErrorCode.REPORTED_RECIPE_NOT_FOUND);
        }
        
        ReportedRecipeInfoProjection recipeInfo = recipeInfoList.get(0);

        // Xác định hành động dựa trên loại báo cáo nghiêm trọng nhất
        ReportActionType actionType = determineActionFromReportType(score.getMostSevereType());

        // Thực hiện hành động tương ứng
        executeAutoAction(recipeId, actionType, recipeInfo, score);

        // Cập nhật tất cả báo cáo đang chờ liên quan thành RESOLVED
        List<Report> pendingReports = reportRepository.findPendingReportsByRecipeId(recipeId);
        updateReportsAsAutoResolved(pendingReports, actionType);

        // Gửi thông báo cho tất cả người báo cáo (bất đồng bộ)
        if (!pendingReports.isEmpty()) {
            Report representativeReport = pendingReports.get(0);
            notificationOrchestrator.notifyAllReportersAsync(representativeReport);
        }

        log.warn("Tự động xử lý công thức '{}' với hành động {} - {} báo cáo (điểm trọng số: {:.2f}), nghiêm trọng nhất: {}",
                recipeInfo.getTitle(),
                actionType.getDisplayName(),
                score.getTotalCount(),
                score.getWeightedScore(),
                score.getMostSevereType());

        logReportBreakdown(score.getReportTypeCount());
    }

    /**
     * Xác định hành động dựa trên loại báo cáo
     */
    private ReportActionType determineActionFromReportType(ReportType reportType) {
        return switch (reportType) {
            case HARASSMENT -> ReportActionType.CONTENT_REMOVED;        // Xóa ngay nội dung quấy rối
            case COPYRIGHT -> ReportActionType.CONTENT_REMOVED;         // Xóa nội dung vi phạm bản quyền
            case INAPPROPRIATE -> ReportActionType.RECIPE_UNPUBLISHED;  // Gỡ nội dung không phù hợp
            case FAKE -> ReportActionType.RECIPE_UNPUBLISHED;           // Gỡ thông tin giả mạo
            case MISLEADING -> ReportActionType.RECIPE_EDITED;          // Yêu cầu chỉnh sửa thông tin sai lệch
            case SPAM -> ReportActionType.RECIPE_EDITED;                // Yêu cầu chỉnh sửa spam
            case OTHER -> ReportActionType.RECIPE_UNPUBLISHED;          // Mặc định gỡ xuống
        };
    }

    /**
     * Thực thi hành động tự động
     */
    private void executeAutoAction(UUID recipeId, ReportActionType actionType, 
                                   ReportedRecipeInfoProjection recipeInfo, ModerationScore score) {
        switch (actionType) {
            case CONTENT_REMOVED -> {
                // Xóa hoàn toàn công thức
                reportQueryRepository.deleteRecipe(recipeId);
                notificationService.notifyAutoContentRemoved(
                        recipeId,
                        recipeInfo.getUserId(),
                        recipeInfo.getAuthorUsername(),
                        recipeInfo.getTitle(),
                        score.getTotalCount(),
                        score.getMostSevereType()
                );
            }
            case RECIPE_UNPUBLISHED -> {
                // Gỡ công thức (unpublish)
                reportQueryRepository.unpublishRecipe(recipeId);
                notificationService.notifyAutoUnpublishRecipe(
                        recipeId,
                        recipeInfo.getUserId(),
                        recipeInfo.getAuthorUsername(),
                        recipeInfo.getTitle(),
                        score.getTotalCount()
                );
            }
            case RECIPE_EDITED -> {
                // Đánh dấu cần chỉnh sửa (có thể unpublish tạm thời)
                reportQueryRepository.markRecipeNeedsEdit(recipeId);
                notificationService.notifyAutoRecipeEditRequired(
                        recipeId,
                        recipeInfo.getUserId(),
                        recipeInfo.getAuthorUsername(),
                        recipeInfo.getTitle(),
                        score.getTotalCount(),
                        score.getMostSevereType()
                );
            }
            default -> {
                // Mặc định gỡ xuống
                reportQueryRepository.unpublishRecipe(recipeId);
                notificationService.notifyAutoUnpublishRecipe(
                        recipeId,
                        recipeInfo.getUserId(),
                        recipeInfo.getAuthorUsername(),
                        recipeInfo.getTitle(),
                        score.getTotalCount()
                );
            }
        }
    }

    /**
     * Ghi log chi tiết phân loại báo cáo
     */
    private void logReportBreakdown(Map<ReportType, Long> reportTypeCount) {
        StringBuilder breakdown = new StringBuilder("Phân loại báo cáo: ");
        reportTypeCount.forEach((type, count) ->
                breakdown.append(String.format("%s(%d), ", type, count))
        );
        log.info(breakdown.toString());
    }

    /**
     * Cập nhật tất cả báo cáo thành RESOLVED với hành động tự động
     */
    private void updateReportsAsAutoResolved(List<Report> reports, ReportActionType actionType) {
        if (reports.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String autoDescription = "Tự động xử lý do vượt ngưỡng báo cáo";

        for (Report report : reports) {
            report.setStatus(ReportStatus.RESOLVED);
            report.setActionTaken(actionType);
            report.setActionDescription(autoDescription);
            report.setReviewedAt(now);
        }

        reportRepository.saveAll(reports);

        log.info("Đã cập nhật {} báo cáo thành RESOLVED với hành động {}", reports.size(), actionType);
    }

}
