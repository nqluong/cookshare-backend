package com.backend.cookshare.admin_report.controller;

import com.backend.cookshare.admin_report.dto.interaction_reponse.*;
import com.backend.cookshare.admin_report.dto.search_response.EngagementByCategoryDTO;
import com.backend.cookshare.admin_report.service.InteractionStatisticsService;
import com.backend.cookshare.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InteractionStatisticsControllerTest {

    @Mock
    private InteractionStatisticsService interactionStatisticsService;

    @InjectMocks
    private InteractionStatisticsController controller;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
    }

    @Test
    void getInteractionOverview_ShouldReturnResponse() {
        InteractionOverviewDTO overviewDTO = new InteractionOverviewDTO();
        when(interactionStatisticsService.getInteractionOverview(startDate, endDate)).thenReturn(overviewDTO);

        ResponseEntity<ApiResponse<InteractionOverviewDTO>> response = controller.getInteractionOverview(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(overviewDTO, response.getBody().getData());
        verify(interactionStatisticsService).getInteractionOverview(startDate, endDate);
    }

    @Test
    void getDetailedInteractionStats_ShouldReturnResponse() {
        DetailedInteractionStatsDTO detailedDTO = new DetailedInteractionStatsDTO();
        when(interactionStatisticsService.getDetailedInteractionStats(startDate, endDate)).thenReturn(detailedDTO);

        ResponseEntity<ApiResponse<DetailedInteractionStatsDTO>> response = controller.getDetailedInteractionStats(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(detailedDTO, response.getBody().getData());
        verify(interactionStatisticsService).getDetailedInteractionStats(startDate, endDate);
    }

    @Test
    void getPeakHoursStats_ShouldReturnResponse() {
        PeakHoursStatsDTO peakDTO = new PeakHoursStatsDTO();
        when(interactionStatisticsService.getPeakHoursStats(startDate, endDate)).thenReturn(peakDTO);

        ResponseEntity<ApiResponse<PeakHoursStatsDTO>> response = controller.getPeakHoursStats(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(peakDTO, response.getBody().getData());
        verify(interactionStatisticsService).getPeakHoursStats(startDate, endDate);
    }

    @Test
    void getTopComments_ShouldReturnResponse() {
        TopCommentsDTO topCommentsDTO = new TopCommentsDTO();
        int limit = 10;
        when(interactionStatisticsService.getTopComments(limit, startDate, endDate)).thenReturn(topCommentsDTO);

        ResponseEntity<ApiResponse<TopCommentsDTO>> response = controller.getTopComments(limit, startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(topCommentsDTO, response.getBody().getData());
        verify(interactionStatisticsService).getTopComments(limit, startDate, endDate);
    }

    @Test
    void getFollowTrends_ShouldReturnResponse() {
        FollowTrendsDTO followDTO = new FollowTrendsDTO();
        String groupBy = "DAY";
        when(interactionStatisticsService.getFollowTrends(startDate, endDate, groupBy)).thenReturn(followDTO);

        ResponseEntity<ApiResponse<FollowTrendsDTO>> response = controller.getFollowTrends(startDate, endDate, groupBy);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(followDTO, response.getBody().getData());
        verify(interactionStatisticsService).getFollowTrends(startDate, endDate, groupBy);
    }

    @Test
    void getEngagementByCategory_ShouldReturnResponse() {
        EngagementByCategoryDTO engagementDTO = new EngagementByCategoryDTO();
        when(interactionStatisticsService.getEngagementByCategory(startDate, endDate)).thenReturn(engagementDTO);

        ResponseEntity<ApiResponse<EngagementByCategoryDTO>> response = controller.getEngagementByCategory(startDate, endDate);

        assertNotNull(response);
        assertEquals(200, response.getBody().getCode());
        assertEquals(engagementDTO, response.getBody().getData());
        verify(interactionStatisticsService).getEngagementByCategory(startDate, endDate);
    }
}
