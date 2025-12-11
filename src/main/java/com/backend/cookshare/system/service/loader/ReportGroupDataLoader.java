package com.backend.cookshare.system.service.loader;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.ReportTypeCount;
import lombok.RequiredArgsConstructor;
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


    /**
     * Tải tất cả dữ liệu bổ sung cho các nhóm song song.
     *
     * @param groups Danh sách các nhóm cần tải dữ liệu
     * @return Dữ liệu tổng hợp chứa các phân loại, người báo cáo và URL avatar
     */
    public GroupEnrichmentData loadEnrichmentData(List<ReportGroupResponse> groups) {
        if (groups.isEmpty()) {
            return GroupEnrichmentData.empty();
        }

        CompletableFuture<Map<TargetKey, Map<ReportType, Long>>> breakdownsFuture =
                CompletableFuture.supplyAsync(() -> batchLoadReportTypeBreakdowns(groups), asyncExecutor);

        CompletableFuture<Map<TargetKey, List<String>>> reportersFuture =
                CompletableFuture.supplyAsync(() -> batchLoadTopReporters(groups), asyncExecutor);

        CompletableFuture<Map<String, String>> avatarUrlsFuture =
                CompletableFuture.supplyAsync(() -> batchLoadAvatarUrls(groups), asyncExecutor);

        CompletableFuture.allOf(breakdownsFuture, reportersFuture, avatarUrlsFuture).join();

        return new GroupEnrichmentData(
                breakdownsFuture.join(),
                reportersFuture.join(),
                avatarUrlsFuture.join()
        );
    }


    public Map<TargetKey, Map<ReportType, Long>> batchLoadReportTypeBreakdowns(
            List<ReportGroupResponse> groups) {

        Map<TargetKey, Map<ReportType, Long>> result = new HashMap<>();

        for (ReportGroupResponse group : groups) {
            TargetKey key = new TargetKey(group.getTargetType(), group.getTargetId());

            List<ReportTypeCount> counts =
                    groupRepository.countReportTypesByTarget(group.getTargetType(), group.getTargetId());

            Map<ReportType, Long> breakdown = counts.stream()
                    .collect(Collectors.toMap(
                            ReportTypeCount::getType,
                            ReportTypeCount::getCount
                    ));

            result.put(key, breakdown);
        }

        return result;
    }

    /**
     * Tải theo lô các người báo cáo hàng đầu cho tất cả các nhóm.
     */
    public Map<TargetKey, List<String>> batchLoadTopReporters(List<ReportGroupResponse> groups) {
        Map<TargetKey, List<String>> result = new HashMap<>();

        for (ReportGroupResponse group : groups) {
            TargetKey key = new TargetKey(group.getTargetType(), group.getTargetId());

            List<Report> reports = groupRepository.findReportsByTarget(
                    group.getTargetType(),
                    group.getTargetId()
            );

            // Lấy ID người báo cáo (giới hạn 3 người)
            Set<UUID> reporterIds = reports.stream()
                    .limit(3)
                    .map(Report::getReporterId)
                    .collect(Collectors.toSet());

            // Tải tên người dùng
            Map<UUID, String> usernames = loadReporterUsernames(reporterIds);

            List<String> topReporters = reports.stream()
                    .limit(3)
                    .map(r -> "@" + usernames.getOrDefault(r.getReporterId(), "unknown"))
                    .collect(Collectors.toList());

            result.put(key, topReporters);
        }

        return result;
    }

    /**
     * Tải theo lô các URL avatar/thumbnail từ Firebase.
     */
    public Map<String, String> batchLoadAvatarUrls(List<ReportGroupResponse> groups) {
        Map<String, String> result = new HashMap<>();

        if (!firebaseStorageService.isInitialized()) {
            log.warn("Firebase chưa khởi tạo, bỏ qua việc tải avatar");
            return result;
        }

        // Thu thập tất cả các đường dẫn cục bộ cần chuyển đổi
        List<String> pathsToConvert = groups.stream()
                .map(ReportGroupResponse::getAvatarUrl)
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
            log.error("Lỗi khi tải theo lô Firebase URLs", e);
        }

        return result;
    }

    /**
     * Tải tên người dùng báo cáo theo IDs.
     */
    public Map<UUID, String> loadReporterUsernames(Set<UUID> reporterIds) {
        if (reporterIds == null || reporterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return reportQueryRepository.findUsernamesByIds(new ArrayList<>(reporterIds))
                .stream()
                .collect(Collectors.toMap(
                        projection -> projection.getUserId(),
                        projection -> projection.getUsername()
                ));
    }

    /**
     * Tải tiêu đề mục tiêu (tiêu đề công thức hoặc tên người dùng).
     */
    public String loadTargetInfo(String targetType, UUID targetId) {
        if ("RECIPE".equals(targetType)) {
            return reportQueryRepository.findRecipeTitleById(targetId)
                    .orElse("Unknown Recipe");
        } else {
            return reportQueryRepository.findUsernameById(targetId)
                    .orElse("Unknown User");
        }
    }

    /**
     * Tải tất cả báo cáo cho một mục tiêu cụ thể.
     */
    public List<Report> loadReportsByTarget(String targetType, UUID targetId) {
        return groupRepository.findReportsByTarget(targetType, targetId);
    }

    /**
     * Lớp key để xác định mục tiêu.
     */
    public static class TargetKey {
        private final String targetType;
        private final UUID targetId;

        public TargetKey(String targetType, UUID targetId) {
            this.targetType = targetType;
            this.targetId = targetId;
        }

        public String getTargetType() {
            return targetType;
        }

        public UUID getTargetId() {
            return targetId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TargetKey targetKey = (TargetKey) o;
            return Objects.equals(targetType, targetKey.targetType) &&
                    Objects.equals(targetId, targetKey.targetId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetType, targetId);
        }
    }


    public record GroupEnrichmentData(
            Map<TargetKey, Map<ReportType, Long>> breakdowns,
            Map<TargetKey, List<String>> reporters,
            Map<String, String> avatarUrls
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
