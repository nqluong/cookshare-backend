package com.backend.cookshare.admin_report.service.impl;

import com.backend.cookshare.admin_report.dto.search_response.*;
import com.backend.cookshare.admin_report.repository.SearchStatisticsRepository;
import com.backend.cookshare.admin_report.repository.search_projection.*;
import com.backend.cookshare.admin_report.service.SearchStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchStatisticsServiceImpl implements SearchStatisticsService {

    private final SearchStatisticsRepository searchRepository;

    @Override
    public SearchOverviewDTO getSearchOverview(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy tổng quan thống kê tìm kiếm");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy các metrics cơ bản
        Long totalSearches = searchRepository.countTotalSearches(start, end);
        Long uniqueQueries = searchRepository.countUniqueQueries(start, end);
        Long successfulSearches = searchRepository.countSuccessfulSearches(start, end);
        Long failedSearches = totalSearches - successfulSearches;
        Long totalUsers = searchRepository.countUniqueSearchUsers(start, end);

        // Tính tỷ lệ thành công
        BigDecimal successRate = calculatePercentage(successfulSearches, totalSearches);

        // Tính trung bình kết quả mỗi lần tìm kiếm
        BigDecimal avgResults = searchRepository.getAverageResultsPerSearch(start, end);

        // Tính trung bình tìm kiếm mỗi người
        BigDecimal avgSearchesPerUser = totalUsers > 0
                ? BigDecimal.valueOf(totalSearches)
                .divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("Hoàn thành tổng quan: {} tìm kiếm, tỷ lệ thành công {}%",
                totalSearches, successRate);

        return SearchOverviewDTO.builder()
                .totalSearches(totalSearches)
                .uniqueSearchQueries(uniqueQueries)
                .successfulSearches(successfulSearches)
                .failedSearches(failedSearches)
                .successRate(successRate)
                .averageResultsPerSearch(avgResults)
                .totalUsers(totalUsers)
                .averageSearchesPerUser(avgSearchesPerUser)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public PopularKeywordsDTO getPopularKeywords(int limit, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy top {} từ khóa phổ biến", limit);

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy keywords
        List<PopularKeywordProjection> keywordData = searchRepository.getPopularKeywords(limit, start, end);
        List<KeywordStatsDTO> keywords = keywordData.stream()
                .map(this::mapToKeywordStats)
                .collect(Collectors.toList());

        Integer totalUnique = searchRepository.countUniqueQueries(start, end).intValue();

        log.info("Đã lấy {} từ khóa từ tổng {} từ khóa unique", keywords.size(), totalUnique);

        return PopularKeywordsDTO.builder()
                .keywords(keywords)
                .totalUniqueKeywords(totalUnique)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public PopularIngredientsDTO getPopularIngredients(int limit, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy top {} nguyên liệu phổ biến", limit);

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy ingredients
        List<PopularIngredientProjection> ingredientData = searchRepository.getPopularIngredients(limit, start, end);
        List<IngredientSearchStatsDTO> ingredients = ingredientData.stream()
                .map(this::mapToIngredientSearchStats)
                .collect(Collectors.toList());

        log.info("Đã lấy {} nguyên liệu phổ biến", ingredients.size());

        return PopularIngredientsDTO.builder()
                .ingredients(ingredients)
                .totalCount(ingredients.size())
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public PopularCategoriesDTO getPopularCategories(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy danh mục phổ biến");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy category views
        List<CategoryViewProjection> categoryData = searchRepository.getCategoryViewStats(start, end);

        // Tính tổng view để tính view share
        Long totalViews = categoryData.stream()
                .mapToLong(CategoryViewProjection::getViewCount)
                .sum();

        List<CategoryViewStatsDTO> categories = categoryData.stream()
                .map(proj -> mapToCategoryViewStats(proj, totalViews))
                .collect(Collectors.toList());

        log.info("Đã lấy {} danh mục, tổng {} lượt xem", categories.size(), totalViews);

        return PopularCategoriesDTO.builder()
                .categories(categories)
                .totalCategoryViews(totalViews)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public SearchSuccessRateDTO getSearchSuccessRate(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu tính tỷ lệ thành công tìm kiếm");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        Long totalSearches = searchRepository.countTotalSearches(start, end);
        Long successful = searchRepository.countSuccessfulSearches(start, end);
        Long failed = totalSearches - successful;

        BigDecimal successRate = calculatePercentage(successful, totalSearches);
        BigDecimal failureRate = calculatePercentage(failed, totalSearches);

        // Lấy tỷ lệ theo loại
        List<SuccessRateByTypeProjection> byTypeData = searchRepository.getSuccessRateByType(start, end);
        List<SuccessRateByTypeDTO> successByType = byTypeData.stream()
                .map(this::mapToSuccessRateByType)
                .collect(Collectors.toList());

        // Lấy xu hướng
        List<SuccessRateTrendProjection> trendData = searchRepository.getSuccessRateTrend(start, end, "DAY");
        List<SuccessRateTrendDTO> trends = trendData.stream()
                .map(this::mapToSuccessRateTrend)
                .collect(Collectors.toList());

        log.info("Tỷ lệ thành công: {}%, Tỷ lệ thất bại: {}%", successRate, failureRate);

        return SearchSuccessRateDTO.builder()
                .totalSearches(totalSearches)
                .successfulSearches(successful)
                .failedSearches(failed)
                .successRate(successRate)
                .failureRate(failureRate)
                .successByType(successByType)
                .trendData(trends)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public ZeroResultKeywordsDTO getZeroResultKeywords(int limit, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Bắt đầu lấy từ khóa không có kết quả");

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy zero result keywords
        List<ZeroResultKeywordProjection> zeroResultData = searchRepository.getZeroResultKeywords(limit, start, end);
        List<ZeroResultKeywordDTO> keywords = zeroResultData.stream()
                .map(this::mapToZeroResultKeyword)
                .collect(Collectors.toList());

        Long totalSearches = searchRepository.countTotalSearches(start, end);
        Long totalZeroResults = searchRepository.countZeroResultSearches(start, end);

        BigDecimal percentageOfTotal = calculatePercentage(totalZeroResults, totalSearches);

        log.info("Tìm thấy {} từ khóa không có kết quả, chiếm {}%",
                keywords.size(), percentageOfTotal);

        return ZeroResultKeywordsDTO.builder()
                .keywords(keywords)
                .totalCount(keywords.size())
                .percentageOfTotal(percentageOfTotal)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }

    @Override
    public SearchTrendsDTO getSearchTrends(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        log.info("Bắt đầu lấy xu hướng tìm kiếm, nhóm theo: {}", groupBy);

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(3);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        // Lấy trends
        List<SearchTrendProjection> trendData = searchRepository.getSearchTrends(start, end, groupBy);
        List<SearchTrendDataDTO> trends = trendData.stream()
                .map(this::mapToSearchTrend)
                .collect(Collectors.toList());

        // Tính growth rate
        BigDecimal growthRate = calculateGrowthRate(trends);

        // Tìm peak period
        String peakPeriod = trends.stream()
                .max((a, b) -> a.getTotalSearches().compareTo(b.getTotalSearches()))
                .map(t -> t.getDate().toString())
                .orElse("N/A");

        log.info("Xu hướng tìm kiếm - Growth rate: {}%, Peak: {}", growthRate, peakPeriod);

        return SearchTrendsDTO.builder()
                .trendData(trends)
                .growthRate(growthRate)
                .peakPeriod(peakPeriod)
                .periodStart(start)
                .periodEnd(end)
                .build();
    }



    private KeywordStatsDTO mapToKeywordStats(PopularKeywordProjection projection) {
        BigDecimal avgResults = projection.getAvgResults() != null ? projection.getAvgResults() : BigDecimal.ZERO;
        BigDecimal successRate = avgResults.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(100)
                : BigDecimal.ZERO;

        return KeywordStatsDTO.builder()
                .keyword(projection.getSearchQuery())
                .searchCount(projection.getSearchCount())
                .uniqueUsers(projection.getUniqueUsers())
                .averageResults(avgResults)
                .successRate(successRate)
                .lastSearched(projection.getLastSearched())
                .trend("STABLE")
                .build();
    }

    private IngredientSearchStatsDTO mapToIngredientSearchStats(PopularIngredientProjection projection) {
        Long recipeCount = projection.getRecipeCount();
        Long searchCount = projection.getSearchCount();
        Long directSearches = projection.getDirectSearches();

        BigDecimal ratio = recipeCount > 0
                ? BigDecimal.valueOf(searchCount)
                .divide(BigDecimal.valueOf(recipeCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return IngredientSearchStatsDTO.builder()
                .ingredientId(projection.getIngredientId())
                .ingredientName(projection.getIngredientName())
                .searchCount(searchCount)
                .directSearches(directSearches)
                .recipeSearches(searchCount - directSearches)
                .recipeCount(recipeCount)
                .searchToRecipeRatio(ratio)
                .build();
    }

    private CategoryViewStatsDTO mapToCategoryViewStats(CategoryViewProjection projection, Long totalViews) {
        Long viewCount = projection.getViewCount();
        Long recipeCount = projection.getRecipeCount();

        BigDecimal viewShare = totalViews > 0
                ? BigDecimal.valueOf(viewCount)
                .divide(BigDecimal.valueOf(totalViews), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return CategoryViewStatsDTO.builder()
                .categoryId(projection.getCategoryId().toString())
                .categoryName(projection.getCategoryName())
                .viewCount(viewCount)
                .uniqueUsers(projection.getUniqueUsers())
                .recipeCount(recipeCount)
                .averageTimeSpent(BigDecimal.ZERO)
                .clickThroughRate(BigDecimal.ZERO)
                .viewShare(viewShare)
                .build();
    }

    private SuccessRateByTypeDTO mapToSuccessRateByType(SuccessRateByTypeProjection projection) {
        Long total = projection.getTotalSearches();
        Long successful = projection.getSuccessfulSearches();

        return SuccessRateByTypeDTO.builder()
                .searchType(projection.getSearchType())
                .totalSearches(total)
                .successfulSearches(successful)
                .successRate(calculatePercentage(successful, total))
                .build();
    }

    private SuccessRateTrendDTO mapToSuccessRateTrend(SuccessRateTrendProjection projection) {
        Long total = projection.getTotalSearches();
        Long successful = projection.getSuccessfulSearches();

        return SuccessRateTrendDTO.builder()
                .date(projection.getPeriod())
                .totalSearches(total)
                .successfulSearches(successful)
                .successRate(calculatePercentage(successful, total))
                .build();
    }

    private ZeroResultKeywordDTO mapToZeroResultKeyword(ZeroResultKeywordProjection projection) {
        List<String> suggestedActions = generateSuggestedActions(projection.getSearchQuery());

        return ZeroResultKeywordDTO.builder()
                .keyword(projection.getSearchQuery())
                .searchCount(projection.getSearchCount())
                .uniqueUsers(projection.getUniqueUsers())
                .firstSearched(projection.getFirstSearched())
                .lastSearched(projection.getLastSearched())
                .suggestedActions(suggestedActions)
                .build();
    }

    private SearchTrendDataDTO mapToSearchTrend(SearchTrendProjection projection) {
        Long total = projection.getTotalSearches();
        Long successful = projection.getSuccessfulSearches();
        BigDecimal avgResults = projection.getAvgResults() != null ? projection.getAvgResults() : BigDecimal.ZERO;

        return SearchTrendDataDTO.builder()
                .date(projection.getPeriod())
                .totalSearches(total)
                .uniqueUsers(projection.getUniqueUsers())
                .uniqueQueries(projection.getUniqueQueries())
                .averageResultsPerSearch(avgResults)
                .successRate(calculatePercentage(successful, total))
                .build();
    }

    private BigDecimal calculatePercentage(Long part, Long total) {
        if (total == null || total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateGrowthRate(List<SearchTrendDataDTO> trends) {
        if (trends.size() < 2) {
            return BigDecimal.ZERO;
        }

        Long firstPeriod = trends.get(0).getTotalSearches();
        Long lastPeriod = trends.get(trends.size() - 1).getTotalSearches();

        if (firstPeriod == 0) {
            return BigDecimal.valueOf(100);
        }

        return BigDecimal.valueOf(lastPeriod - firstPeriod)
                .divide(BigDecimal.valueOf(firstPeriod), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<String> generateSuggestedActions(String keyword) {
        List<String> actions = new ArrayList<>();

        if (keyword.length() < 3) {
            actions.add("Từ khóa quá ngắn - có thể là lỗi gõ");
        }

        if (keyword.matches(".*[0-9]+.*")) {
            actions.add("Có chứa số - xem xét tạo nội dung với chỉ số dinh dưỡng");
        }

        actions.add("Xem xét tạo công thức mới với từ khóa này");
        actions.add("Kiểm tra chính tả và đồng nghĩa");

        return actions;
    }
}
