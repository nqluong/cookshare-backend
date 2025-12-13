package com.backend.cookshare.admin_report.service.impl;

import com.backend.cookshare.admin_report.dto.interaction_reponse.*;
import com.backend.cookshare.admin_report.dto.search_response.EngagementByCategoryDTO;
import com.backend.cookshare.admin_report.repository.InteractionStatisticsRepository;
import com.backend.cookshare.admin_report.repository.interaction_projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InteractionStatisticsServiceImplTest {

    @Mock
    private InteractionStatisticsRepository interactionRepository;

    @InjectMocks
    private InteractionStatisticsServiceImpl service;

    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        start = LocalDateTime.now().minusDays(7);
        end = LocalDateTime.now();
    }

    // ---------------------------------------------------------
    // 1) Test getInteractionOverview()
    // ---------------------------------------------------------
    @Test
    void testGetInteractionOverview() {
        when(interactionRepository.countTotalLikes(any(), any())).thenReturn(100L);
        when(interactionRepository.countTotalComments(any(), any())).thenReturn(50L);
        when(interactionRepository.countTotalSaves(any(), any())).thenReturn(25L);
        when(interactionRepository.countPublishedRecipes(any(), any())).thenReturn(5L);
        when(interactionRepository.countTotalViews(any(), any())).thenReturn(1000L);

        InteractionOverviewDTO dto = service.getInteractionOverview(start, end);

        assertEquals(100L, dto.getTotalLikes());
        assertEquals(50L, dto.getTotalComments());
        assertEquals(25L, dto.getTotalSaves());
        assertEquals(5L, dto.getTotalRecipes());
        assertTrue(dto.getEngagementRate().compareTo(BigDecimal.ZERO) > 0);

        verify(interactionRepository, times(1))
                .countTotalLikes(any(), any());
    }

    // ---------------------------------------------------------
    // 2) Test getDetailedInteractionStats()
    // ---------------------------------------------------------
    @Test
    void testGetDetailedInteractionStats() {

        when(interactionRepository.getAverageLikesPerRecipe(any(), any())).thenReturn(BigDecimal.TEN);
        when(interactionRepository.getAverageCommentsPerRecipe(any(), any())).thenReturn(BigDecimal.ONE);
        when(interactionRepository.getAverageSavesPerRecipe(any(), any())).thenReturn(BigDecimal.ZERO);

        when(interactionRepository.getMedianLikesPerRecipe(any(), any())).thenReturn(BigDecimal.valueOf(5));
        when(interactionRepository.getMedianCommentsPerRecipe(any(), any())).thenReturn(BigDecimal.valueOf(3));
        when(interactionRepository.getMedianSavesPerRecipe(any(), any())).thenReturn(BigDecimal.valueOf(2));

        when(interactionRepository.getMaxLikesOnRecipe(any(), any())).thenReturn(20L);
        when(interactionRepository.getMaxCommentsOnRecipe(any(), any())).thenReturn(10L);
        when(interactionRepository.getMaxSavesOnRecipe(any(), any())).thenReturn(7L);

        // Mock range distribution
        InteractionDistributionProjection mockDist = mock(InteractionDistributionProjection.class);
        when(mockDist.getRange0To10()).thenReturn(10L);
        when(mockDist.getRange11To50()).thenReturn(5L);
        when(mockDist.getRange51To100()).thenReturn(2L);
        when(mockDist.getRange101To500()).thenReturn(1L);
        when(mockDist.getRangeOver500()).thenReturn(0L);

        when(interactionRepository.getInteractionDistribution(any(), any(), eq("LIKE"))).thenReturn(mockDist);
        when(interactionRepository.getInteractionDistribution(any(), any(), eq("COMMENT"))).thenReturn(mockDist);
        when(interactionRepository.getInteractionDistribution(any(), any(), eq("SAVE"))).thenReturn(mockDist);

        DetailedInteractionStatsDTO dto = service.getDetailedInteractionStats(start, end);

        assertNotNull(dto.getLikeDistribution());
        assertEquals(10L, dto.getLikeDistribution().getCount0to10());
        assertEquals(BigDecimal.TEN, dto.getAverageLikesPerRecipe());
        assertEquals(BigDecimal.valueOf(5), dto.getMedianLikesPerRecipe());
    }


    // ---------------------------------------------------------
    // 3) Test getPeakHoursStats()
    // ---------------------------------------------------------
    @Test
    void testGetPeakHoursStats() {

        // Mock hourly
        HourlyInteractionProjection hp = mock(HourlyInteractionProjection.class);
        when(hp.getHour()).thenReturn(12);
        when(hp.getLikes()).thenReturn(10L);
        when(hp.getComments()).thenReturn(5L);
        when(hp.getSaves()).thenReturn(2L);
        when(hp.getTotal()).thenReturn(17L);

        when(interactionRepository.getInteractionsByHour(any(), any()))
                .thenReturn(List.of(hp));

        // Mock daily
        DailyInteractionProjection dp = mock(DailyInteractionProjection.class);
        when(dp.getDayOfWeek()).thenReturn(3);
        when(dp.getLikes()).thenReturn(10L);
        when(dp.getComments()).thenReturn(10L);
        when(dp.getSaves()).thenReturn(5L);
        when(dp.getTotal()).thenReturn(25L);

        when(interactionRepository.getInteractionsByDayOfWeek(any(), any()))
                .thenReturn(List.of(dp));

        PeakHoursStatsDTO dto = service.getPeakHoursStats(start, end);

        assertEquals(12, dto.getPeakHour());
        assertEquals("Thứ 4", dto.getPeakDayOfWeek());
    }

    // ---------------------------------------------------------
    // 4) Test getTopComments()
    // ---------------------------------------------------------
    @Test
    void testGetTopComments() {

        TopCommentProjection item = mock(TopCommentProjection.class);
        when(item.getCommentId()).thenReturn(UUID.randomUUID());
        when(item.getContent()).thenReturn("Nice!");
        when(item.getRecipeId()).thenReturn(UUID.randomUUID());
        when(item.getRecipeTitle()).thenReturn("Test Recipe");
        when(item.getUserId()).thenReturn(UUID.randomUUID());
        when(item.getUsername()).thenReturn("user1");
        when(item.getAvatarUrl()).thenReturn("avatar.png");
        when(item.getLikeCount()).thenReturn(12L);
        when(item.getCreatedAt()).thenReturn(LocalDateTime.now());

        when(interactionRepository.getTopComments(anyInt(), any(), any()))
                .thenReturn(List.of(item));

        TopCommentsDTO dto = service.getTopComments(5, start, end);

        assertEquals(1, dto.getTotalCount());
        assertEquals("Nice!", dto.getTopComments().get(0).getContent());
    }

    // ---------------------------------------------------------
    // 5) Test getFollowTrends()
    // ---------------------------------------------------------
    @Test
    void testGetFollowTrends() {

        FollowTrendProjection p1 = mock(FollowTrendProjection.class);
        when(p1.getPeriodDate()).thenReturn(start.plusDays(1));
        when(p1.getNewFollows()).thenReturn(10L);
        when(p1.getCumulativeFollows()).thenReturn(100L);

        FollowTrendProjection p2 = mock(FollowTrendProjection.class);
        when(p2.getPeriodDate()).thenReturn(start.plusDays(2));
        when(p2.getNewFollows()).thenReturn(20L);
        when(p2.getCumulativeFollows()).thenReturn(120L);

        when(interactionRepository.getFollowTrendsByPeriod(any(), any(), any()))
                .thenReturn(List.of(p1, p2));

        FollowTrendsDTO dto = service.getFollowTrends(start, end, "DAY");

        assertEquals(30L, dto.getTotalNewFollows());
        assertTrue(dto.getFollowGrowthRate().compareTo(BigDecimal.ZERO) > 0);
    }

    // ---------------------------------------------------------
    // 6) Test getEngagementByCategory()
    // ---------------------------------------------------------
    @Test
    void testGetEngagementByCategory() {

        CategoryEngagementProjection cp = mock(CategoryEngagementProjection.class);
        when(cp.getCategoryId()).thenReturn(UUID.randomUUID());
        when(cp.getCategoryName()).thenReturn("Dessert");
        when(cp.getRecipeCount()).thenReturn(2L);
        when(cp.getTotalViews()).thenReturn(100L);
        when(cp.getTotalLikes()).thenReturn(20L);
        when(cp.getTotalComments()).thenReturn(10L);
        when(cp.getTotalSaves()).thenReturn(5L);

        when(interactionRepository.getEngagementByCategory(any(), any()))
                .thenReturn(List.of(cp));

        EngagementByCategoryDTO dto = service.getEngagementByCategory(start, end);

        assertEquals(1, dto.getCategoryEngagements().size());
        assertTrue(dto.getOverallEngagementRate().compareTo(BigDecimal.ZERO) > 0);
    }
}
