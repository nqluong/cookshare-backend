package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.response.ReportGroupDetailResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.service.ReportGroupService;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader.GroupEnrichmentData;
import com.backend.cookshare.system.service.mapper.ReportGroupMapper;
import com.backend.cookshare.system.service.score.ReportGroupScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ReportGroupServiceImpl implements ReportGroupService {

    private final ReportGroupRepository groupRepository;
    private final ReportGroupScoreCalculator scoreCalculator;
    private final ReportGroupDataLoader dataLoader;
    private final ReportGroupMapper groupMapper;
    private final Executor asyncExecutor;

    public ReportGroupServiceImpl(ReportGroupRepository groupRepository,
                                  ReportGroupScoreCalculator scoreCalculator,
                                  ReportGroupDataLoader dataLoader,
                                  ReportGroupMapper groupMapper,
                                  @Qualifier("reportAsyncExecutor") Executor asyncExecutor) {
        this.groupRepository = groupRepository;
        this.scoreCalculator = scoreCalculator;
        this.dataLoader = dataLoader;
        this.groupMapper = groupMapper;
        this.asyncExecutor = asyncExecutor;

    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportGroupResponse> getGroupedReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportGroupResponse> groupPage = groupRepository.findGroupedReports(pageable);

        List<ReportGroupResponse> content = groupPage.getContent();

        if (content.isEmpty()) {
            return groupMapper.buildEmptyPageResponse(page, size, groupPage);
        }

        // Tải tất cả dữ liệu bổ sung song song
        GroupEnrichmentData enrichmentData = dataLoader.loadEnrichmentData(content);

        // Làm giàu dữ liệu và sắp xếp các nhóm
        List<ReportGroupResponse> enrichedGroups = groupMapper.enrichAndSortGroups(content, enrichmentData);

        return groupMapper.buildPageResponse(page, size, groupPage, enrichedGroups);
    }


    @Override
    @Transactional(readOnly = true)
    public ReportGroupDetailResponse getGroupDetail(String targetType, UUID targetId) {
        // Lấy tất cả báo cáo cho mục tiêu này
        List<Report> reports = dataLoader.loadReportsByTarget(targetType, targetId);

        if (reports.isEmpty()) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }

        // Tải dữ liệu bổ sung song song
        CompletableFuture<Map<ReportType, Long>> breakdownFuture =
                CompletableFuture.supplyAsync(() ->
                                reports.stream()
                                        .collect(Collectors.groupingBy(Report::getReportType, Collectors.counting())),
                        asyncExecutor
                );

        CompletableFuture<Map<UUID, String>> reporterUsernamesFuture =
                CompletableFuture.supplyAsync(() -> {
                            Set<UUID> reporterIds = reports.stream()
                                    .map(Report::getReporterId)
                                    .collect(Collectors.toSet());
                            return dataLoader.loadReporterUsernames(reporterIds);
                        },
                        asyncExecutor
                );

        CompletableFuture<String> targetInfoFuture =
                CompletableFuture.supplyAsync(() ->
                                dataLoader.loadTargetInfo(targetType, targetId),
                        asyncExecutor
                );

        // Chờ tất cả các thao tác bất đồng bộ hoàn thành
        CompletableFuture.allOf(breakdownFuture, reporterUsernamesFuture, targetInfoFuture).join();

        Map<ReportType, Long> typeBreakdown = breakdownFuture.join();
        Map<UUID, String> reporterUsernames = reporterUsernamesFuture.join();
        String targetTitle = targetInfoFuture.join();

        return groupMapper.buildGroupDetailResponse(
                targetType,
                targetId,
                targetTitle,
                reports,
                typeBreakdown,
                reporterUsernames
        );
    }
}
