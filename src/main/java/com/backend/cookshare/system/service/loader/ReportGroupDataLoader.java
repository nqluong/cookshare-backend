package com.backend.cookshare.system.service.loader;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.repository.ReportGroupRepository.BatchReportTypeCount;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.TopReporterProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReportGroupDataLoader {

    private final ReportGroupRepository groupRepository;
    private final ReportQueryRepository reportQueryRepository;
    private final FirebaseStorageService firebaseStorageService;
    private final Executor asyncExecutor;

    public ReportGroupDataLoader(
            ReportGroupRepository groupRepository,
            ReportQueryRepository reportQueryRepository,
            FirebaseStorageService firebaseStorageService,
            @Qualifier("reportAsyncExecutor") Executor asyncExecutor) {
        this.groupRepository = groupRepository;
        this.reportQueryRepository = reportQueryRepository;
        this.firebaseStorageService = firebaseStorageService;
        this.asyncExecutor = asyncExecutor;
    }

    public GroupEnrichmentData loadEnrichmentData(List<ReportGroupResponse> groups) {
        if (groups.isEmpty()) {
            return GroupEnrichmentData.empty();
        }

        List<UUID> recipeIds = groups.stream()
                .map(ReportGroupResponse::getRecipeId)
                .collect(Collectors.toList());

        CompletableFuture<Map<UUID, Map<ReportType, Long>>> breakdownsFuture =
                CompletableFuture.supplyAsync(() -> batchLoadReportTypeBreakdowns(recipeIds), asyncExecutor);

        CompletableFuture<Map<UUID, List<String>>> reportersFuture =
                CompletableFuture.supplyAsync(() -> batchLoadTopReporters(recipeIds), asyncExecutor);

        CompletableFuture<Map<String, String>> thumbnailUrlsFuture =
                CompletableFuture.supplyAsync(() -> batchLoadThumbnailUrls(groups), asyncExecutor);

        CompletableFuture.allOf(breakdownsFuture, reportersFuture, thumbnailUrlsFuture).join();

        return new GroupEnrichmentData(
                breakdownsFuture.join(),
                reportersFuture.join(),
                thumbnailUrlsFuture.join()
        );
    }


    public Map<UUID, Map<ReportType, Long>> batchLoadReportTypeBreakdowns(List<UUID> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Một query duy nhất lấy tất cả report type counts cho tất cả recipes
        List<BatchReportTypeCount> batchCounts = groupRepository.batchCountReportTypesByRecipes(recipeIds);

        // Chuyển đổi kết quả thành Map<RecipeId, Map<ReportType, Count>>
        return batchCounts.stream()
                .collect(Collectors.groupingBy(
                        BatchReportTypeCount::getRecipeId,
                        Collectors.toMap(
                                BatchReportTypeCount::getType,
                                BatchReportTypeCount::getCount
                        )
                ));
    }

    public Map<UUID, List<String>> batchLoadTopReporters(List<UUID> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Một query duy nhất lấy top 3 reporters cho tất cả recipes (đã có username)
        List<TopReporterProjection> topReporters = groupRepository.batchFindTopReportersByRecipes(recipeIds);

        // Chuyển đổi kết quả thành Map<RecipeId, List<@username>>
        return topReporters.stream()
                .collect(Collectors.groupingBy(
                        TopReporterProjection::getRecipeId,
                        Collectors.mapping(
                                p -> "@" + p.getReporterFullName(),
                                Collectors.toList()
                        )
                ));
    }


    public Map<String, String> batchLoadThumbnailUrls(List<ReportGroupResponse> groups) {
        Map<String, String> result = new HashMap<>();

        if (!firebaseStorageService.isInitialized()) {
            log.warn("Firebase chưa khởi tạo, bỏ qua việc tải thumbnail");
            return result;
        }

        // Thu thập tất cả các đường dẫn thumbnail cần chuyển đổi
        List<String> pathsToConvert = groups.stream()
                .map(ReportGroupResponse::getRecipeFeaturedImage)
                .filter(Objects::nonNull)
                .filter(path -> !path.startsWith("http"))
                .distinct()
                .collect(Collectors.toList());

        if (pathsToConvert.isEmpty()) {
            return result;
        }

        try {
            // Chuyển đổi theo lô tất cả các đường dẫn sang Firebase URLs
            List<String> firebaseUrls = firebaseStorageService.convertPathsToFirebaseUrls(pathsToConvert);

            for (int i = 0; i < pathsToConvert.size(); i++) {
                result.put(pathsToConvert.get(i), firebaseUrls.get(i));
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải theo lô Firebase URLs cho thumbnail", e);
        }

        return result;
    }

    public record RecipeAuthorInfo(UUID authorId, String authorUsername, String authorFullName) {}

    public record GroupEnrichmentData(
            Map<UUID, Map<ReportType, Long>> breakdowns,
            Map<UUID, List<String>> reporters,
            Map<String, String> thumbnailUrls
    ) {
        public static GroupEnrichmentData empty() {
            return new GroupEnrichmentData(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }
    }
}
