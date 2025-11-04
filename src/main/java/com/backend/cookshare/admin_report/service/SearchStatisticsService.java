package com.backend.cookshare.admin_report.service;

import com.backend.cookshare.admin_report.dto.search_response.*;

import java.time.LocalDateTime;

public interface SearchStatisticsService {
    /**
     * Lấy tổng quan thống kê tìm kiếm
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return SearchOverviewDTO
     */
    SearchOverviewDTO getSearchOverview(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy từ khóa tìm kiếm phổ biến nhất
     * @param limit Số lượng từ khóa
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return PopularKeywordsDTO
     */
    PopularKeywordsDTO getPopularKeywords(int limit, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy nguyên liệu được tìm kiếm nhiều nhất
     * @param limit Số lượng nguyên liệu
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return PopularIngredientsDTO
     */
    PopularIngredientsDTO getPopularIngredients(int limit, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy danh mục được xem nhiều nhất
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return PopularCategoriesDTO
     */
    PopularCategoriesDTO getPopularCategories(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy tỷ lệ tìm kiếm thành công
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return SearchSuccessRateDTO
     */
    SearchSuccessRateDTO getSearchSuccessRate(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy từ khóa không có kết quả
     * @param limit Số lượng từ khóa
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return ZeroResultKeywordsDTO
     */
    ZeroResultKeywordsDTO getZeroResultKeywords(int limit, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy xu hướng tìm kiếm theo thời gian
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param groupBy Nhóm theo (DAY, WEEK, MONTH)
     * @return SearchTrendsDTO
     */
    SearchTrendsDTO getSearchTrends(LocalDateTime startDate, LocalDateTime endDate, String groupBy);
}
