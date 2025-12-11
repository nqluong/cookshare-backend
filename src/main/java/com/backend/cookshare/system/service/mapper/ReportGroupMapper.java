package com.backend.cookshare.system.service.mapper;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.system.dto.response.ReportDetailInGroupResponse;
import com.backend.cookshare.system.dto.response.ReportGroupDetailResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader.GroupEnrichmentData;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader.RecipeAuthorInfo;
import com.backend.cookshare.system.service.score.ReportGroupScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportGroupMapper {

    private final ReportGroupScoreCalculator scoreCalculator;


    public PageResponse<ReportGroupResponse> buildEmptyPageResponse(
            int page, int size, Page<ReportGroupResponse> groupPage) {

        return PageResponse.<ReportGroupResponse>builder()
                .page(page)
                .size(size)
                .totalPages(groupPage.getTotalPages())
                .totalElements(groupPage.getTotalElements())
                .content(Collections.emptyList())
                .build();
    }

    public PageResponse<ReportGroupResponse> buildPageResponse(
            int page, int size,
            Page<ReportGroupResponse> groupPage,
            List<ReportGroupResponse> enrichedContent) {

        return PageResponse.<ReportGroupResponse>builder()
                .page(page)
                .size(size)
                .totalPages(groupPage.getTotalPages())
                .totalElements(groupPage.getTotalElements())
                .content(enrichedContent)
                .build();
    }

    /**
     * Làm giàu dữ liệu cho một nhóm báo cáo công thức.
     */
    public ReportGroupResponse enrichGroupData(
            ReportGroupResponse group,
            GroupEnrichmentData enrichmentData) {

        UUID recipeId = group.getRecipeId();

        // Lấy phân loại theo loại báo cáo
        Map<ReportType, Long> breakdown = enrichmentData.breakdowns()
                .getOrDefault(recipeId, Collections.emptyMap());

        // Tính điểm trọng số
        double weightedScore = scoreCalculator.calculateWeightedScore(breakdown);

        // Tìm loại nghiêm trọng nhất
        ReportType mostSevere = scoreCalculator.findMostSevereType(breakdown);

        // Xác định xem có vượt ngưỡng không
        boolean exceedsThreshold = scoreCalculator.exceedsThreshold(weightedScore);

        // Xác định độ ưu tiên
        String priority = scoreCalculator.determinePriority(weightedScore, group.getReportCount());

        // Lấy người báo cáo hàng đầu
        List<String> topReporters = enrichmentData.reporters()
                .getOrDefault(recipeId, Collections.emptyList());

        // Chuyển đổi URL thumbnail nếu cần
        String thumbnailUrl = group.getRecipeFeaturedImage();
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("http")) {
            thumbnailUrl = enrichmentData.thumbnailUrls().getOrDefault(thumbnailUrl, thumbnailUrl);
        }

        // Cập nhật dữ liệu nhóm
        group.setWeightedScore(weightedScore);
        group.setMostSevereType(mostSevere);
        group.setReportTypeBreakdown(breakdown);
        group.setExceedsThreshold(exceedsThreshold);
        group.setPriority(priority);
        group.setTopReporters(topReporters);
        group.setRecipeFeaturedImage(thumbnailUrl);

        return group;
    }

    public List<ReportGroupResponse> enrichAndSortGroups(
            List<ReportGroupResponse> groups,
            GroupEnrichmentData enrichmentData) {

        return groups.stream()
                .map(group -> enrichGroupData(group, enrichmentData))
                .sorted(this::compareByPriority)
                .collect(Collectors.toList());
    }


    private int compareByPriority(ReportGroupResponse g1, ReportGroupResponse g2) {
        int p1 = scoreCalculator.getPriorityOrder(g1.getPriority());
        int p2 = scoreCalculator.getPriorityOrder(g2.getPriority());

        if (p1 != p2) {
            return Integer.compare(p2, p1); // Giảm dần
        }

        // Cùng độ ưu tiên, sắp xếp theo điểm trọng số
        return Double.compare(g2.getWeightedScore(), g1.getWeightedScore());
    }
}
