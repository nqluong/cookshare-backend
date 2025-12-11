package com.backend.cookshare.system.service.score;

import com.backend.cookshare.system.enums.ReportType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ReportGroupScoreCalculator {

    // Trọng số mức độ nghiêm trọng cho các loại báo cáo khác nhau
    private static final Map<ReportType, Double> SEVERITY_WEIGHTS = new EnumMap<>(ReportType.class);

    static {
        SEVERITY_WEIGHTS.put(ReportType.HARASSMENT, 2.0);
        SEVERITY_WEIGHTS.put(ReportType.COPYRIGHT, 1.5);
        SEVERITY_WEIGHTS.put(ReportType.INAPPROPRIATE, 1.5);
        SEVERITY_WEIGHTS.put(ReportType.FAKE, 1.2);
        SEVERITY_WEIGHTS.put(ReportType.MISLEADING, 1.0);
        SEVERITY_WEIGHTS.put(ReportType.SPAM, 0.8);
        SEVERITY_WEIGHTS.put(ReportType.OTHER, 0.5);
    }

    // Ngưỡng cho các loại mục tiêu khác nhau
    public static final double USER_THRESHOLD = 12.0;
    public static final double RECIPE_THRESHOLD = 6.0;

    /**
     * Tính điểm trọng số dựa trên phân loại loại báo cáo.
     */
    public double calculateWeightedScore(Map<ReportType, Long> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return 0.0;
        }

        return breakdown.entrySet().stream()
                .mapToDouble(e -> e.getValue() * SEVERITY_WEIGHTS.getOrDefault(e.getKey(), 1.0))
                .sum();
    }

    /**
     * Tìm loại báo cáo nghiêm trọng nhất dựa trên tác động trọng số.
     */
    public ReportType findMostSevereType(Map<ReportType, Long> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return ReportType.OTHER;
        }

        return breakdown.entrySet().stream()
                .max((e1, e2) -> {
                    double score1 = e1.getValue() * SEVERITY_WEIGHTS.getOrDefault(e1.getKey(), 1.0);
                    double score2 = e2.getValue() * SEVERITY_WEIGHTS.getOrDefault(e2.getKey(), 1.0);
                    return Double.compare(score1, score2);
                })
                .map(Map.Entry::getKey)
                .orElse(ReportType.OTHER);
    }

    /**
     * Lấy ngưỡng cho một loại mục tiêu.
     */
    public double getThreshold(String targetType) {
        return "USER".equals(targetType) ? USER_THRESHOLD : RECIPE_THRESHOLD;
    }

    /**
     * Kiểm tra xem điểm có vượt ngưỡng cho loại mục tiêu không.
     */
    public boolean exceedsThreshold(double score, String targetType) {
        return score >= getThreshold(targetType);
    }

    /**
     * Xác định mức độ ưu tiên dựa trên điểm và số lượng báo cáo.

     */
    public String determinePriority(double score, String targetType, long count) {
        double threshold = getThreshold(targetType);
        double ratio = score / threshold;

        if (ratio >= 1.5 || count >= 10) return "CRITICAL";
        if (ratio >= 1.0 || count >= 5) return "HIGH";
        if (ratio >= 0.7 || count >= 3) return "MEDIUM";
        return "LOW";
    }

    /**
     * Lấy giá trị thứ tự ưu tiên để sắp xếp.
     */
    public int getPriorityOrder(String priority) {
        return switch (priority) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    /**
     * Lấy trọng số mức độ nghiêm trọng cho một loại báo cáo.
     */
    public double getSeverityWeight(ReportType type) {
        return SEVERITY_WEIGHTS.getOrDefault(type, 1.0);
    }
}
