package com.backend.cookshare.admin_report.service.impl;

import com.backend.cookshare.admin_report.dto.interaction_reponse.*;
import com.backend.cookshare.admin_report.dto.search_response.EngagementByCategoryDTO;
import com.backend.cookshare.admin_report.repository.InteractionStatisticsRepository;
import com.backend.cookshare.admin_report.repository.interaction_projection.*;
import com.backend.cookshare.admin_report.service.InteractionStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionStatisticsServiceImpl implements InteractionStatisticsService {

    private final InteractionStatisticsRepository interactionRepository;

    @Override
    public InteractionOverviewDTO getInteractionOverview(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy tổng quan thống kê tương tác");

        // Thiết lập khoảng thời gian mặc định nếu null
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy tổng số tương tác từ database
        Long totalLikes = interactionRepository.countTotalLikes(start, end);
        Long totalComments = interactionRepository.countTotalComments(start, end);
        Long totalSaves = interactionRepository.countTotalSaves(start, end);
        Long totalRecipes = interactionRepository.countPublishedRecipes(start, end);
        Long totalViews = interactionRepository.countTotalViews(start, end);

        // Tính engagement rate: (likes + comments + saves) / views * 100
        BigDecimal engagementRate = BigDecimal.ZERO;
        if (totalViews > 0) {
            BigDecimal totalInteractions = BigDecimal.valueOf(totalLikes + totalComments + totalSaves);
            engagementRate = totalInteractions
                    .divide(BigDecimal.valueOf(totalViews), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Tính trung bình tương tác mỗi công thức
        BigDecimal avgLikes = calculateAverage(totalLikes, totalRecipes);
        BigDecimal avgComments = calculateAverage(totalComments, totalRecipes);
        BigDecimal avgSaves = calculateAverage(totalSaves, totalRecipes);

        log.info("Hoàn thành lấy tổng quan: {} likes, {} comments, {} saves",
                totalLikes, totalComments, totalSaves);

        return InteractionOverviewDTO.builder()
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .totalSaves(totalSaves)
                .totalRecipes(totalRecipes)
                .engagementRate(engagementRate)
                .averageLikesPerRecipe(avgLikes)
                .averageCommentsPerRecipe(avgComments)
                .averageSavesPerRecipe(avgSaves)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public DetailedInteractionStatsDTO getDetailedInteractionStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy thống kê tương tác chi tiết");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy các giá trị trung bình
        BigDecimal avgLikes = interactionRepository.getAverageLikesPerRecipe(start, end);
        BigDecimal avgComments = interactionRepository.getAverageCommentsPerRecipe(start, end);
        BigDecimal avgSaves = interactionRepository.getAverageSavesPerRecipe(start, end);

        // Lấy các giá trị trung vị
        BigDecimal medianLikes = interactionRepository.getMedianLikesPerRecipe(start, end);
        BigDecimal medianComments = interactionRepository.getMedianCommentsPerRecipe(start, end);
        BigDecimal medianSaves = interactionRepository.getMedianSavesPerRecipe(start, end);

        // Lấy giá trị lớn nhất
        Long maxLikes = interactionRepository.getMaxLikesOnRecipe(start, end);
        Long maxComments = interactionRepository.getMaxCommentsOnRecipe(start, end);
        Long maxSaves = interactionRepository.getMaxSavesOnRecipe(start, end);

        // Lấy phân phối tương tác
        InteractionDistribution likeDistribution = mapToInteractionDistribution(
                interactionRepository.getInteractionDistribution(start, end, "LIKE"));
        InteractionDistribution commentDistribution = mapToInteractionDistribution(
                interactionRepository.getInteractionDistribution(start, end, "COMMENT"));
        InteractionDistribution saveDistribution = mapToInteractionDistribution(
                interactionRepository.getInteractionDistribution(start, end, "SAVE"));

        log.info("Hoàn thành thống kê chi tiết");

        return DetailedInteractionStatsDTO.builder()
                .averageLikesPerRecipe(avgLikes)
                .averageCommentsPerRecipe(avgComments)
                .averageSavesPerRecipe(avgSaves)
                .medianLikesPerRecipe(medianLikes)
                .medianCommentsPerRecipe(medianComments)
                .medianSavesPerRecipe(medianSaves)
                .maxLikesOnRecipe(maxLikes)
                .maxCommentsOnRecipe(maxComments)
                .maxSavesOnRecipe(maxSaves)
                .likeDistribution(likeDistribution)
                .commentDistribution(commentDistribution)
                .saveDistribution(saveDistribution)
                .build();
    }

    @Override
    public PeakHoursStatsDTO getPeakHoursStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy thống kê giờ cao điểm");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy thống kê theo giờ
        List<HourlyInteractionProjection> hourlyData = interactionRepository.getInteractionsByHour(start, end);
        List<HourlyInteractionDTO> hourlyStats = hourlyData.stream()
                .map(this::mapToHourlyInteraction)
                .collect(Collectors.toList());

        // Lấy thống kê theo ngày trong tuần
        List<DailyInteractionProjection> dailyData = interactionRepository.getInteractionsByDayOfWeek(start, end);
        List<DailyInteractionDTO> dailyStats = dailyData.stream()
                .map(this::mapToDailyInteraction)
                .collect(Collectors.toList());

        // Tìm giờ và ngày có nhiều tương tác nhất
        Integer peakHour = hourlyStats.stream()
                .max((a, b) -> a.getTotalInteractions().compareTo(b.getTotalInteractions()))
                .map(HourlyInteractionDTO::getHour)
                .orElse(null);

        String peakDay = dailyStats.stream()
                .max((a, b) -> a.getTotalInteractions().compareTo(b.getTotalInteractions()))
                .map(DailyInteractionDTO::getDayOfWeek)
                .orElse(null);

        log.info("Giờ cao điểm: {}, Ngày cao điểm: {}", peakHour, peakDay);

        return PeakHoursStatsDTO.builder()
                .hourlyStats(hourlyStats)
                .dailyStats(dailyStats)
                .peakHour(peakHour)
                .peakDayOfWeek(peakDay)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public TopCommentsDTO getTopComments(int limit, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy top {} bình luận", limit);

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy top comments
        List<TopCommentProjection> topCommentsData = interactionRepository.getTopComments(limit, start, end);
        List<CommentDetailDTO> topComments = topCommentsData.stream()
                .map(this::mapToCommentDetail)
                .collect(Collectors.toList());

        log.info("Đã lấy {} bình luận hàng đầu", topComments.size());

        return TopCommentsDTO.builder()
                .topComments(topComments)
                .totalCount(topComments.size())
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public FollowTrendsDTO getFollowTrends(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        log.info("Bắt đầu lấy xu hướng follow, nhóm theo: {}", groupBy);

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(3);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy dữ liệu xu hướng theo thời gian
        List<FollowTrendProjection> trendData = interactionRepository.getFollowTrendsByPeriod(start, end, groupBy);
        List<FollowTrendDataDTO> trends = trendData.stream()
                .map(this::mapToFollowTrendData)
                .collect(Collectors.toList());

        // Tính tổng số follow mới
        Long totalNewFollows = trends.stream()
                .mapToLong(FollowTrendDataDTO::getNewFollows)
                .sum();

        // Tính tăng trưởng
        Long netGrowth = totalNewFollows;
        BigDecimal growthRate = calculateGrowthRate(trends);

        log.info("Tổng follow mới: {}, Tăng trưởng: {}", totalNewFollows, growthRate);

        return FollowTrendsDTO.builder()
                .trendData(trends)
                .totalNewFollows(totalNewFollows)
                .totalUnfollows(0L)
                .netFollowGrowth(netGrowth)
                .followGrowthRate(growthRate)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public EngagementByCategoryDTO getEngagementByCategory(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy engagement rate theo danh mục");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy engagement theo category
        List<CategoryEngagementProjection> categoryData = interactionRepository.getEngagementByCategory(start, end);
        List<CategoryEngagementDTO> categoryEngagements = categoryData.stream()
                .map(this::mapToCategoryEngagement)
                .collect(Collectors.toList());

        // Tính tổng engagement rate
        BigDecimal overallEngagement = categoryEngagements.stream()
                .map(CategoryEngagementDTO::getEngagementRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, categoryEngagements.size())), 2, RoundingMode.HALF_UP);

        log.info("Đã tính engagement cho {} danh mục", categoryEngagements.size());

        return EngagementByCategoryDTO.builder()
                .categoryEngagements(categoryEngagements)
                .overallEngagementRate(overallEngagement)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    private BigDecimal calculateAverage(Long total, Long count) {
        if (count == null || count == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private InteractionDistribution mapToInteractionDistribution(InteractionDistributionProjection projection) {
        return InteractionDistribution.builder()
                .count0to10(projection.getRange0To10())
                .count11to50(projection.getRange11To50())
                .count51to100(projection.getRange51To100())
                .count101to500(projection.getRange101To500())
                .countOver500(projection.getRangeOver500())
                .build();
    }

    private HourlyInteractionDTO mapToHourlyInteraction(HourlyInteractionProjection projection) {
        return HourlyInteractionDTO.builder()
                .hour(projection.getHour())
                .likes(projection.getLikes())
                .comments(projection.getComments())
                .saves(projection.getSaves())
                .totalInteractions(projection.getTotal())
                .build();
    }

    private DailyInteractionDTO mapToDailyInteraction(DailyInteractionProjection projection) {
        return DailyInteractionDTO.builder()
                .dayNumber(projection.getDayOfWeek())
                .dayOfWeek(getDayName(projection.getDayOfWeek()))
                .likes(projection.getLikes())
                .comments(projection.getComments())
                .saves(projection.getSaves())
                .totalInteractions(projection.getTotal())
                .build();
    }

    private CommentDetailDTO mapToCommentDetail(TopCommentProjection projection) {
        return CommentDetailDTO.builder()
                .commentId(projection.getCommentId())
                .content(projection.getContent())
                .recipeId(projection.getRecipeId())
                .recipeTitle(projection.getRecipeTitle())
                .userId(projection.getUserId())
                .username(projection.getUsername())
                .userAvatar(projection.getAvatarUrl())
                .likeCount(projection.getLikeCount())
                .createdAt(projection.getCreatedAt())
                .build();
    }

    private FollowTrendDataDTO mapToFollowTrendData(FollowTrendProjection projection) {
        return FollowTrendDataDTO.builder()
                .date(projection.getPeriodDate())
                .newFollows(projection.getNewFollows())
                .cumulativeFollows(projection.getCumulativeFollows())
                .build();
    }

    private CategoryEngagementDTO mapToCategoryEngagement(CategoryEngagementProjection projection) {
        Long views = projection.getTotalViews();
        Long likes = projection.getTotalLikes();
        Long comments = projection.getTotalComments();
        Long saves = projection.getTotalSaves();
        Long recipeCount = projection.getRecipeCount();

        BigDecimal engagementRate = views > 0
                ? BigDecimal.valueOf(likes + comments + saves)
                .divide(BigDecimal.valueOf(views), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return CategoryEngagementDTO.builder()
                .categoryId(projection.getCategoryId())
                .categoryName(projection.getCategoryName())
                .recipeCount(recipeCount)
                .totalViews(views)
                .totalLikes(likes)
                .totalComments(comments)
                .totalSaves(saves)
                .engagementRate(engagementRate)
                .averageViewsPerRecipe(calculateAverage(views, recipeCount))
                .averageLikesPerRecipe(calculateAverage(likes, recipeCount))
                .averageCommentsPerRecipe(calculateAverage(comments, recipeCount))
                .averageSavesPerRecipe(calculateAverage(saves, recipeCount))
                .build();
    }


    private BigDecimal calculateGrowthRate(List<FollowTrendDataDTO> trends) {
        if (trends.size() < 2) {
            return BigDecimal.ZERO;
        }

        Long firstPeriod = trends.get(0).getNewFollows();
        Long lastPeriod = trends.get(trends.size() - 1).getNewFollows();

        if (firstPeriod == 0) {
            return BigDecimal.valueOf(100);
        }

        return BigDecimal.valueOf(lastPeriod - firstPeriod)
                .divide(BigDecimal.valueOf(firstPeriod), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private String getDayName(int dayNumber) {
        String[] days = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        return dayNumber >= 1 && dayNumber <= 7 ? days[dayNumber - 1] : "Không xác định";
    }
}
