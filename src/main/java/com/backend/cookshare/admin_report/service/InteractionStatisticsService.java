package com.backend.cookshare.admin_report.service;

import com.backend.cookshare.admin_report.dto.interaction_reponse.*;
import com.backend.cookshare.admin_report.dto.search_response.EngagementByCategoryDTO;

import java.time.LocalDateTime;

public interface InteractionStatisticsService {
    /**
     * Lấy tổng quan thống kê tương tác
     * @param startDate Ngày bắt đầu (null = không giới hạn)
     * @param endDate Ngày kết thúc (null = hiện tại)
     * @return InteractionOverviewDTO
     */
    InteractionOverviewDTO getInteractionOverview(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy thống kê tương tác chi tiết
     * Bao gồm trung bình, trung vị và phân phối
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return DetailedInteractionStatsDTO
     */
    DetailedInteractionStatsDTO getDetailedInteractionStats(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy thống kê giờ cao điểm tương tác
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return PeakHoursStatsDTO
     */
    PeakHoursStatsDTO getPeakHoursStats(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy top bình luận được like nhiều nhất
     * @param limit Số lượng bình luận cần lấy
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return TopCommentsDTO
     */
    TopCommentsDTO getTopComments(int limit, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Lấy xu hướng follow/unfollow theo thời gian
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param groupBy Nhóm theo (DAY, WEEK, MONTH)
     * @return FollowTrendsDTO
     */
    FollowTrendsDTO getFollowTrends(LocalDateTime startDate, LocalDateTime endDate, String groupBy);

    /**
     * Lấy engagement rate theo danh mục
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return EngagementByCategoryDTO
     */
    EngagementByCategoryDTO getEngagementByCategory(LocalDateTime startDate, LocalDateTime endDate);
}
