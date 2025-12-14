package com.backend.cookshare.system.service.mapper;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader.GroupEnrichmentData;
import com.backend.cookshare.system.service.score.ReportGroupScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportGroupMapperTest {

    @Mock
    ReportGroupScoreCalculator scoreCalculator;

    @InjectMocks
    ReportGroupMapper mapper;

    UUID recipeId;
    ReportGroupResponse group;

    @BeforeEach
    void setup() {
        recipeId = UUID.randomUUID();

        group = new ReportGroupResponse();
        group.setRecipeId(recipeId);
        group.setReportCount(5L);
        group.setRecipeFeaturedImage("img1.png");
    }

    /* ================= buildEmptyPageResponse ================= */

    @Test
    void buildEmptyPageResponse_success() {
        Page<ReportGroupResponse> page = new PageImpl<>(List.of(group));

        PageResponse<ReportGroupResponse> res =
                mapper.buildEmptyPageResponse(1, 10, page);

        assertNotNull(res);
        assertEquals(1, res.getPage());
        assertEquals(10, res.getSize());
        assertEquals(page.getTotalPages(), res.getTotalPages());
        assertEquals(page.getTotalElements(), res.getTotalElements());
        assertTrue(res.getContent().isEmpty());
    }

    /* ================= buildPageResponse ================= */

    @Test
    void buildPageResponse_success() {
        Page<ReportGroupResponse> page = new PageImpl<>(List.of(group));
        List<ReportGroupResponse> enriched = List.of(group);

        PageResponse<ReportGroupResponse> res =
                mapper.buildPageResponse(0, 5, page, enriched);

        assertEquals(1, res.getContent().size());
        assertEquals(group, res.getContent().get(0));
    }

    /* ================= enrichGroupData ================= */

    @Test
    void enrichGroupData_fullData_thumbnailConverted() {
        Map<ReportType, Long> breakdown =
                Map.of(ReportType.SPAM, 3L);

        GroupEnrichmentData data = new GroupEnrichmentData(
                Map.of(recipeId, breakdown),
                Map.of(recipeId, List.of("user1", "user2")),
                Map.of("img1.png", "http://cdn/img1.png")
        );

        when(scoreCalculator.calculateWeightedScore(breakdown)).thenReturn(10.0);
        when(scoreCalculator.findMostSevereType(breakdown)).thenReturn(ReportType.SPAM);
        when(scoreCalculator.exceedsThreshold(10.0)).thenReturn(true);
        when(scoreCalculator.determinePriority(10.0, 5)).thenReturn("HIGH");

        ReportGroupResponse res = mapper.enrichGroupData(group, data);

        assertEquals(10.0, res.getWeightedScore());
        assertEquals(ReportType.SPAM, res.getMostSevereType());
        assertTrue(res.getExceedsThreshold());
        assertEquals("HIGH", res.getPriority());
        assertEquals(2, res.getTopReporters().size());
        assertEquals("http://cdn/img1.png", res.getRecipeFeaturedImage());
    }

    @Test
    void enrichGroupData_noBreakdown_noThumbnail() {
        group.setRecipeFeaturedImage(null);

        GroupEnrichmentData data = new GroupEnrichmentData(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        when(scoreCalculator.calculateWeightedScore(any())).thenReturn(0.0);
        when(scoreCalculator.findMostSevereType(any())).thenReturn(null);
        when(scoreCalculator.exceedsThreshold(0.0)).thenReturn(false);
        when(scoreCalculator.determinePriority(0.0, 5)).thenReturn("LOW");

        ReportGroupResponse res = mapper.enrichGroupData(group, data);

        assertEquals(0.0, res.getWeightedScore());
        assertNull(res.getMostSevereType());
        assertFalse(res.getExceedsThreshold());
        assertEquals("LOW", res.getPriority());
        assertTrue(res.getTopReporters().isEmpty());
        assertNull(res.getRecipeFeaturedImage());
    }

    /* ================= enrichAndSortGroups ================= */

    @Test
    void enrichAndSortGroups_sortedByPriorityAndScore() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ReportGroupResponse g1 = new ReportGroupResponse();
        g1.setRecipeId(id1);
        g1.setReportCount(5L);

        ReportGroupResponse g2 = new ReportGroupResponse();
        g2.setRecipeId(id2);
        g2.setReportCount(5L);

        Map<ReportType, Long> highBreakdown = Map.of(ReportType.SPAM, 10L);
        Map<ReportType, Long> lowBreakdown  = Map.of(ReportType.SPAM, 1L);

        when(scoreCalculator.calculateWeightedScore(highBreakdown))
                .thenReturn(20.0); // HIGH priority
        when(scoreCalculator.calculateWeightedScore(lowBreakdown))
                .thenReturn(5.0);  // LOW priority

        when(scoreCalculator.determinePriority(20.0, 5L))
                .thenReturn("HIGH");
        when(scoreCalculator.determinePriority(5.0, 5L))
                .thenReturn("LOW");

        when(scoreCalculator.getPriorityOrder("HIGH")).thenReturn(2);
        when(scoreCalculator.getPriorityOrder("LOW")).thenReturn(1);

        List<ReportGroupResponse> res =
                mapper.enrichAndSortGroups(
                        List.of(g2, g1),
                        new GroupEnrichmentData(
                                Map.of(
                                        id1, highBreakdown,
                                        id2, lowBreakdown
                                ),
                                Collections.emptyMap(),
                                Collections.emptyMap()
                        )
                );

        assertEquals("HIGH", res.get(0).getPriority());
        assertEquals("LOW", res.get(1).getPriority());
    }


    @Test
    void enrichAndSortGroups_samePriority_sortedByScore() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        ReportGroupResponse g1 = new ReportGroupResponse();
        g1.setRecipeId(id1);
        g1.setReportCount(5L);

        ReportGroupResponse g2 = new ReportGroupResponse();
        g2.setRecipeId(id2);
        g2.setReportCount(5L);

        Map<ReportType, Long> b1 = Map.of(ReportType.SPAM, 1L);
        Map<ReportType, Long> b2 = Map.of(ReportType.SPAM, 5L);

        when(scoreCalculator.calculateWeightedScore(b1)).thenReturn(10.0);
        when(scoreCalculator.calculateWeightedScore(b2)).thenReturn(20.0);
        when(scoreCalculator.determinePriority(anyDouble(), anyLong()))
                .thenReturn("MEDIUM");
        when(scoreCalculator.getPriorityOrder("MEDIUM"))
                .thenReturn(1);

        List<ReportGroupResponse> res =
                mapper.enrichAndSortGroups(
                        List.of(g1, g2),
                        new GroupEnrichmentData(
                                Map.of(id1, b1, id2, b2),
                                Collections.emptyMap(),
                                Collections.emptyMap()
                        )
                );

        assertEquals(id2, res.get(0).getRecipeId()); // score cao hơn đứng trước
    }


}
