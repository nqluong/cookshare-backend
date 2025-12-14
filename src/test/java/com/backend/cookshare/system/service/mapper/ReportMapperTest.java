package com.backend.cookshare.system.service.mapper;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.system.dto.response.ReportResponse;
import com.backend.cookshare.system.dto.response.ReporterInfo;
import com.backend.cookshare.system.dto.response.TopReportedItem;
import com.backend.cookshare.system.entity.Report;
import com.backend.cookshare.system.repository.ReportQueryRepository;
import com.backend.cookshare.system.repository.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import static org.mockito.ArgumentMatchers.eq;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportMapperTest {

    @Mock
    ReportQueryRepository reportQueryRepository;

    @Mock
    PageMapper pageMapper;

    @Mock
    FirebaseStorageService firebaseStorageService;

    Executor executor = Runnable::run;

    ReportMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReportMapper(reportQueryRepository, pageMapper, firebaseStorageService, executor);
    }

    // ==================== toResponse(Report) ====================

    @Test
    void toResponse_report_fullMapping() {
        UUID reporterId = UUID.randomUUID();
        UUID reportedId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        Report report = new Report();
        report.setReportId(UUID.randomUUID());
        report.setReporterId(reporterId);
        report.setReportedId(reportedId);
        report.setRecipeId(recipeId);
        report.setReviewedBy(reviewerId);

        ReporterInfo reporter = ReporterInfo.builder()
                .userId(reporterId)
                .username("reporter")
                .avatarUrl("ava.png")
                .build();

        ReporterInfo reviewer = ReporterInfo.builder()
                .userId(reviewerId)
                .username("admin")
                .avatarUrl("admin.png")
                .build();

        when(reportQueryRepository.findReporterInfoByIds(any()))
                .thenReturn(List.of(reporter, reviewer));

        ReportedUserInfoProjection reportedUser = mock(ReportedUserInfoProjection.class);
        when(reportedUser.getUserId()).thenReturn(reportedId);
        when(reportedUser.getUsername()).thenReturn("badUser");
        when(reportedUser.getEmail()).thenReturn("bad@mail.com");
        when(reportedUser.getAvatarUrl()).thenReturn("bad.png");
        when(reportedUser.getRole()).thenReturn("USER");
        when(reportedUser.getIsActive()).thenReturn(true);

        ReportedRecipeInfoProjection recipe = mock(ReportedRecipeInfoProjection.class);
        when(recipe.getRecipeId()).thenReturn(recipeId);
        when(recipe.getTitle()).thenReturn("Recipe");
        when(recipe.getSlug()).thenReturn("recipe");
        when(recipe.getFeaturedImage()).thenReturn("img.png");
        when(recipe.getStatus())
                .thenReturn(RecipeStatus.APPROVED.name());
        when(recipe.getIsPublished()).thenReturn(true);
        when(recipe.getViewCount()).thenReturn(100);
        when(recipe.getUserId()).thenReturn(UUID.randomUUID());
        when(recipe.getAuthorUsername()).thenReturn("author");


        when(reportQueryRepository.findReportedUserInfoByIds(any()))
                .thenReturn(List.of(reportedUser));

        when(reportQueryRepository.findReportedRecipeInfoByIds(any()))
                .thenReturn(List.of(recipe));

        ReportResponse res = mapper.toResponse(report);

        assertNotNull(res.getReporter());
        assertNotNull(res.getReportedUser());
        assertNotNull(res.getReportedRecipe());
        assertNotNull(res.getReviewer());
    }

    // ==================== toResponse(Projection) ====================
//
//    @Test
//    void toResponse_projection() {
//        ReportProjection proj = mock(ReportProjection.class);
//        UUID reporterId = UUID.randomUUID();
//
//        when(proj.getReporterId()).thenReturn(reporterId);
//
//        ReporterInfo reporter = ReporterInfo.builder()
//                .userId(reporterId)
//                .username("u1")
//                .build();
//
//        when(reportQueryRepository.findReporterInfoByIds(any()))
//                .thenReturn(List.of(reporter));
//
//        ReportResponse res = mapper.toResponse(proj);
//
////        assertEquals("u1", res.getReporter().getUsername());
//    }

    // ==================== toPageResponse ====================

//    @Test
//    void toPageResponse_fullBatchFlow() {
//        UUID reporterId = UUID.randomUUID();
//
//        ReportProjection proj = mock(ReportProjection.class);
//        when(proj.getReporterId()).thenReturn(reporterId);
//
//        ReporterInfo reporter = ReporterInfo.builder()
//                .userId(reporterId)
//                .username("pageUser")
//                .build();
//
//        when(reportQueryRepository.findReporterInfoByIds(any()))
//                .thenReturn(List.of(reporter));
//
//        Page<ReportProjection> page = mock(Page.class);
//        when(page.getContent()).thenReturn(List.of(proj));
//
//        when(pageMapper.toPageResponse(anyList(), eq(page)))
//                .thenReturn(new PageResponse<>());
//
//        PageResponse<ReportResponse> res = mapper.toPageResponse(page);
//
//        assertNotNull(res);
//    }

    @Test
    void toPageResponse_emptyPage() {
        Page<ReportProjection> page = mock(Page.class);
        when(page.getContent()).thenReturn(Collections.emptyList());

        when(pageMapper.toPageResponse(anyList(), eq(page)))
                .thenReturn(new PageResponse<>());

        PageResponse<ReportResponse> res = mapper.toPageResponse(page);

        assertNotNull(res);
    }

    // ==================== Top Reported ====================

    @Test
    void toTopReportedUsers_ok() {
        UUID userId = UUID.randomUUID();

        TopReportedProjection proj = mock(TopReportedProjection.class);
        when(proj.getItemId()).thenReturn(userId);
        when(proj.getReportCount()).thenReturn(5L);

        UsernameProjection username = mock(UsernameProjection.class);
        when(username.getUserId()).thenReturn(userId);
        when(username.getUsername()).thenReturn("user");

        when(reportQueryRepository.findUsernamesByIds(any()))
                .thenReturn(List.of(username));

        List<TopReportedItem> res =
                mapper.toTopReportedUsers(List.of(proj));

        assertEquals("user", res.get(0).getItemName());
    }

    @Test
    void toTopReportedRecipes_ok() {
        UUID recipeId = UUID.randomUUID();

        TopReportedProjection proj = mock(TopReportedProjection.class);
        when(proj.getItemId()).thenReturn(recipeId);
        when(proj.getReportCount()).thenReturn(3L);

        ReportedRecipeInfoProjection recipe = mock(ReportedRecipeInfoProjection.class);
        when(recipe.getRecipeId()).thenReturn(recipeId);
        when(recipe.getTitle()).thenReturn("R1");

        when(reportQueryRepository.findReportedRecipeInfoByIds(any()))
                .thenReturn(List.of(recipe));

        List<TopReportedItem> res =
                mapper.toTopReportedRecipes(List.of(proj));

        assertEquals("R1", res.get(0).getItemName());
    }

    @Test
    void toTopReported_emptyInput() {
        assertTrue(mapper.toTopReportedUsers(null).isEmpty());
        assertTrue(mapper.toTopReportedRecipes(Collections.emptyList()).isEmpty());
    }
}

