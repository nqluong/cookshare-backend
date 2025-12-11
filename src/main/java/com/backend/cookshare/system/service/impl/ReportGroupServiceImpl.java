package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.response.ReportDetailInGroupResponse;
import com.backend.cookshare.system.dto.response.ReportGroupDetailResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.repository.projection.ReportDetailWithContextProjection;
import com.backend.cookshare.system.service.ReportGroupService;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader.GroupEnrichmentData;
import com.backend.cookshare.system.service.mapper.ReportGroupMapper;
import com.backend.cookshare.system.service.score.ReportGroupScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGroupServiceImpl implements ReportGroupService {

    private final ReportGroupRepository groupRepository;
    private final ReportGroupDataLoader dataLoader;
    private final ReportGroupMapper groupMapper;
    private final ReportGroupScoreCalculator scoreCalculator;


    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportGroupResponse> getGroupedReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportGroupResponse> groupPage = groupRepository.findGroupedReports(pageable);

        List<ReportGroupResponse> content = groupPage.getContent();

        if (content.isEmpty()) {
            return groupMapper.buildEmptyPageResponse(page, size, groupPage);
        }

        // Tải tất cả dữ liệu bổ sung song song (sử dụng batch queries)
        GroupEnrichmentData enrichmentData = dataLoader.loadEnrichmentData(content);

        // Làm giàu dữ liệu và sắp xếp các nhóm
        List<ReportGroupResponse> enrichedGroups = groupMapper.enrichAndSortGroups(content, enrichmentData);

        return groupMapper.buildPageResponse(page, size, groupPage, enrichedGroups);
    }


    @Override
    @Transactional(readOnly = true)
    public ReportGroupDetailResponse getGroupDetail(UUID recipeId) {
        // MỘT query duy nhất lấy tất cả: reports + author info + reporter usernames
        List<ReportDetailWithContextProjection> reportDetails =
                groupRepository.findReportDetailsWithContext(recipeId);

        if (reportDetails.isEmpty()) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }

        // Lấy thông tin chung từ row đầu tiên (giống nhau cho tất cả rows)
        ReportDetailWithContextProjection first = reportDetails.get(0);
        String recipeTitle = first.getRecipeTitle();
        String recipeThumbnail = first.getRecipeFeaturedImage();
        UUID authorId = first.getAuthorId();
        String authorUsername = first.getAuthorUsername();
        String authorFullName = first.getAuthorFullName();

        // Tính phân loại theo loại báo cáo từ dữ liệu đã có
        Map<ReportType, Long> typeBreakdown = reportDetails.stream()
                .collect(Collectors.groupingBy(
                        p -> ReportType.valueOf(p.getReportType()),
                        Collectors.counting()
                ));

        // Tính điểm và các thống kê
        double weightedScore = scoreCalculator.calculateWeightedScore(typeBreakdown);
        ReportType mostSevere = scoreCalculator.findMostSevereType(typeBreakdown);
        double threshold = scoreCalculator.getThreshold();
        boolean exceedsThreshold = weightedScore >= threshold;

        // Chuyển đổi sang DTO
        List<ReportDetailInGroupResponse> reports = reportDetails.stream()
                .map(p -> ReportDetailInGroupResponse.builder()
                        .reportId(p.getReportId())
                        .reporterUsername(p.getReporterUsername())
                        .reporterFullName(p.getReporterFullName())
                        .reportType(ReportType.valueOf(p.getReportType()))
                        .reason(p.getReason())
                        .description(p.getDescription())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ReportGroupDetailResponse.builder()
                .recipeId(recipeId)
                .recipeTitle(recipeTitle)
                .recipeThumbnail(recipeThumbnail)
                .authorId(authorId)
                .authorUsername(authorUsername)
                .authorFullName(authorFullName)
                .reportCount((long) reports.size())
                .weightedScore(weightedScore)
                .mostSevereType(mostSevere)
                .reportTypeBreakdown(typeBreakdown)
                .exceedsThreshold(exceedsThreshold)
                .threshold(threshold)
                .reports(reports)
                .build();
    }
}
