package com.backend.cookshare.system.service.impl;

import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.system.dto.response.ReportDetailInGroupResponse;
import com.backend.cookshare.system.dto.response.ReportGroupDetailResponse;
import com.backend.cookshare.system.dto.response.ReportGroupResponse;
import com.backend.cookshare.system.enums.ReportActionType;
import com.backend.cookshare.system.enums.ReportStatus;
import com.backend.cookshare.system.enums.ReportType;
import com.backend.cookshare.system.repository.ReportGroupRepository;
import com.backend.cookshare.system.repository.projection.ReportDetailWithContextProjection;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader;
import com.backend.cookshare.system.service.loader.ReportGroupDataLoader.GroupEnrichmentData;
import com.backend.cookshare.system.service.mapper.ReportGroupMapper;
import com.backend.cookshare.system.service.score.ReportGroupScoreCalculator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportGroupServiceImplTest {

    @Mock
    private ReportGroupRepository groupRepository;

    @Mock
    private ReportGroupDataLoader dataLoader;

    @Mock
    private ReportGroupMapper groupMapper;

    @Mock
    private ReportGroupScoreCalculator scoreCalculator;

    @Mock
    private FirebaseStorageService firebaseStorageService;

    @InjectMocks
    private ReportGroupServiceImpl reportGroupService;

    private List<ReportGroupResponse> mockGroups;
    private Page<ReportGroupResponse> emptyPage;
    private Page<ReportGroupResponse> nonEmptyPage;

    @BeforeEach
    void setUp() {
        mockGroups = List.of(
                ReportGroupResponse.builder().recipeId(UUID.randomUUID()).reportCount(5L).build(),
                ReportGroupResponse.builder().recipeId(UUID.randomUUID()).reportCount(3L).build()
        );

        emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        nonEmptyPage = new PageImpl<>(mockGroups, PageRequest.of(0, 10), 2);
    }

    @Test
    @DisplayName("getGroupedReports - no filters - returns enriched page")
    void getGroupedReports_noFilters_success() {
        when(groupRepository.findGroupedReports(any(Pageable.class))).thenReturn(nonEmptyPage);
        when(dataLoader.loadEnrichmentData(eq(mockGroups))).thenReturn(mock(GroupEnrichmentData.class));
        when(groupMapper.enrichAndSortGroups(eq(mockGroups), any())).thenReturn(mockGroups);

        PageResponse<ReportGroupResponse> expectedResponse = PageResponse.<ReportGroupResponse>builder()
                .content(mockGroups)
                .page(0)
                .size(10)
                .totalElements(2)
                .totalPages(1)
                .build();

        when(groupMapper.buildPageResponse(anyInt(), anyInt(), any(), any()))
                .thenReturn(expectedResponse);

        PageResponse<ReportGroupResponse> response = reportGroupService.getGroupedReports(0, 10, null, null, null);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        verify(groupRepository).findGroupedReports(any(Pageable.class));
        verify(dataLoader).loadEnrichmentData(mockGroups);
        verify(groupMapper).enrichAndSortGroups(eq(mockGroups), any());
        verify(groupMapper).buildPageResponse(anyInt(), anyInt(), any(), any());  // ← Sửa thành dùng matcher
    }

    @Test
    @DisplayName("getGroupedReports - with filters - uses filtered query")
    void getGroupedReports_withFilters_success() {
        when(groupRepository.findGroupedReportsWithFilters(any(Pageable.class), eq(ReportStatus.PENDING), eq(ReportActionType.USER_BANNED), eq(ReportType.SPAM)))
                .thenReturn(nonEmptyPage);
        when(dataLoader.loadEnrichmentData(any())).thenReturn(mock(GroupEnrichmentData.class));
        when(groupMapper.enrichAndSortGroups(any(), any())).thenReturn(mockGroups);
        when(groupMapper.buildPageResponse(anyInt(), anyInt(), any(), any()))
                .thenReturn(PageResponse.<ReportGroupResponse>builder()
                        .content(mockGroups)
                        .size(10)
                        .page(0)
                        .totalElements(2)
                        .totalPages(1)
                        .build());


        reportGroupService.getGroupedReports(0, 10, ReportStatus.PENDING, ReportActionType.USER_BANNED, ReportType.SPAM);

        verify(groupRepository).findGroupedReportsWithFilters(any(Pageable.class), eq(ReportStatus.PENDING), eq(ReportActionType.USER_BANNED), eq(ReportType.SPAM));
        verify(groupRepository, never()).findGroupedReports(any());
    }

    @Test
    @DisplayName("getGroupedReports - empty result - returns empty page response")
    void getGroupedReports_emptyResult() {
        when(groupRepository.findGroupedReports(any(Pageable.class))).thenReturn(emptyPage);

        // Sửa: stub buildEmptyPageResponse thay vì buildPageResponse
        PageResponse<ReportGroupResponse> emptyResponse = PageResponse.<ReportGroupResponse>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(groupMapper.buildEmptyPageResponse(eq(0), eq(10), eq(emptyPage)))
                .thenReturn(emptyResponse);

        PageResponse<ReportGroupResponse> response = reportGroupService.getGroupedReports(0, 10, null, null, null);

        // Bây giờ response không null nữa
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0L, response.getTotalElements());
        assertEquals(0, response.getTotalPages());

        verify(groupMapper).buildEmptyPageResponse(0, 10, emptyPage);
        verify(groupMapper, never()).buildPageResponse(anyInt(), anyInt(), any(), any());
        verify(dataLoader, never()).loadEnrichmentData(any());
    }

    @Test
    @DisplayName("getGroupDetail - not found throws CustomException")
    void getGroupDetail_notFound() {
        UUID recipeId = UUID.randomUUID();
        when(groupRepository.findReportDetailsWithContext(recipeId)).thenReturn(List.of());

        CustomException ex = assertThrows(CustomException.class,
                () -> reportGroupService.getGroupDetail(recipeId));

        assertEquals(ErrorCode.REPORT_NOT_FOUND, ex.getErrorCode());
    }

    @ParameterizedTest
    @MethodSource("provideReportDetails")
    @DisplayName("getGroupDetail - success with full data enrichment")
    void getGroupDetail_success(List<ReportDetailWithContextProjection> projections,
                                Map<ReportType, Long> expectedBreakdown,
                                double expectedScore,
                                ReportType expectedMostSevere,
                                boolean expectedExceedsThreshold) {
        UUID recipeId = UUID.randomUUID();

        when(groupRepository.findReportDetailsWithContext(recipeId)).thenReturn(projections);
        when(scoreCalculator.calculateWeightedScore(expectedBreakdown)).thenReturn(expectedScore);
        when(scoreCalculator.findMostSevereType(expectedBreakdown)).thenReturn(expectedMostSevere);
        when(scoreCalculator.getThreshold()).thenReturn(10.0);
        when(firebaseStorageService.convertPathToFirebaseUrl(anyString()))
                .thenAnswer(inv -> "https://firebase/" + inv.getArgument(0));

        ReportGroupDetailResponse response = reportGroupService.getGroupDetail(recipeId);

        assertNotNull(response);
        assertEquals(recipeId, response.getRecipeId());
        assertEquals(projections.size(), response.getReportCount());
        assertEquals(expectedScore, response.getWeightedScore());
        assertEquals(expectedMostSevere, response.getMostSevereType());
        assertEquals(expectedExceedsThreshold, response.getExceedsThreshold());
        assertEquals(expectedBreakdown, response.getReportTypeBreakdown());
        assertEquals(projections.size(), response.getReports().size());

        // Verify avatar/thumbnail conversion
        verify(firebaseStorageService, atLeast(1)).convertPathToFirebaseUrl(anyString());
    }

    static Stream<Arguments> provideReportDetails() {
        ReportDetailWithContextProjection p1 = mock(ReportDetailWithContextProjection.class);
        when(p1.getReportId()).thenReturn(UUID.randomUUID());
        when(p1.getReporterId()).thenReturn(UUID.randomUUID());
        when(p1.getReporterUsername()).thenReturn("reporter1");
        when(p1.getReporterFullName()).thenReturn("Reporter One");
//        when(p1.getReporterAvatar()).thenReturn("avatar1.jpg");
        when(p1.getReportType()).thenReturn(ReportType.SPAM.name());
        when(p1.getReason()).thenReturn("Spam reason");
        when(p1.getDescription()).thenReturn("Desc");
        when(p1.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());

        ReportDetailWithContextProjection p2 = mock(ReportDetailWithContextProjection.class);
        when(p2.getReportId()).thenReturn(UUID.randomUUID());
        when(p2.getReporterId()).thenReturn(UUID.randomUUID());
        when(p2.getReporterUsername()).thenReturn("reporter2");
        when(p2.getReporterFullName()).thenReturn("Reporter Two");
//        when(p2.getReporterAvatar()).thenReturn("avatar2.jpg");
        when(p2.getReportType()).thenReturn(ReportType.INAPPROPRIATE.name());
        when(p2.getReason()).thenReturn("Inappropriate");
        when(p2.getDescription()).thenReturn("Bad content");
        when(p2.getCreatedAt()).thenReturn(java.time.LocalDateTime.now());

        // Common fields
        List<ReportDetailWithContextProjection> list = List.of(p1, p2);
        for (ReportDetailWithContextProjection p : list) {
            when(p.getRecipeTitle()).thenReturn("Bad Recipe");
            when(p.getRecipeFeaturedImage()).thenReturn("thumb.jpg");
            when(p.getAuthorId()).thenReturn(UUID.randomUUID());
            when(p.getAuthorUsername()).thenReturn("author");
            when(p.getAuthorFullName()).thenReturn("Author Name");
//            when(p.getAuthorAvatar()).thenReturn("author_avatar.jpg");
        }

        Map<ReportType, Long> breakdown = Map.of(
                ReportType.SPAM, 1L,
                ReportType.INAPPROPRIATE, 1L
        );

        return Stream.of(
                Arguments.of(list, breakdown, 15.0, ReportType.INAPPROPRIATE, true),
                Arguments.of(List.of(p1), Map.of(ReportType.SPAM, 1L), 5.0, ReportType.SPAM, false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideFirebaseUrlCases")
    @DisplayName("convertToFirebaseUrl - handles all cases")
    void convertToFirebaseUrl_variousInputs(String input, String expectedOutput, int expectedCallCount) throws Exception {
        // Stub động: chỉ gọi khi cần, và trả về URL có prefix
        lenient().when(firebaseStorageService.convertPathToFirebaseUrl(anyString()))
                .thenAnswer(inv -> "https://firebase/" + inv.getArgument(0));

        Method method = ReportGroupServiceImpl.class
                .getDeclaredMethod("convertToFirebaseUrl", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(reportGroupService, input);

        assertEquals(expectedOutput, result);
        verify(firebaseStorageService, times(expectedCallCount)).convertPathToFirebaseUrl(anyString());
    }

    static Stream<Arguments> provideFirebaseUrlCases() {
        return Stream.of(
                Arguments.of(null, null, 0),
                Arguments.of("", null, 0),
                Arguments.of("   ", null, 0),
                Arguments.of("local.jpg", "https://firebase/local.jpg", 1),
                Arguments.of("https://already.url/image.jpg", "https://already.url/image.jpg", 0)
        );
    }
}