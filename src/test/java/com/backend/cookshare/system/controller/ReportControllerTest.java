//package com.backend.cookshare.system.controller;
//
//import com.backend.cookshare.authentication.util.SecurityUtil;
//import com.backend.cookshare.common.dto.ApiResponse;
//import com.backend.cookshare.common.dto.PageResponse;
//import com.backend.cookshare.common.exception.CustomException;
//import com.backend.cookshare.common.exception.ErrorCode;
//import com.backend.cookshare.system.dto.request.CreateReportRequest;
//import com.backend.cookshare.system.dto.request.ReportFilterRequest;
//import com.backend.cookshare.system.dto.request.ReviewReportRequest;
//import com.backend.cookshare.system.dto.response.*;
//import com.backend.cookshare.system.enums.ReportStatus;
//import com.backend.cookshare.system.enums.ReportType;
//import com.backend.cookshare.system.repository.ReportQueryRepository;
//import com.backend.cookshare.system.service.ReportGroupService;
//import com.backend.cookshare.system.service.ReportService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@ExtendWith(MockitoExtension.class)
//class ReportControllerTest {
//
//    @Mock
//    private ReportService reportService;
//
//    @Mock
//    private SecurityUtil securityUtil;
//
//    @Mock
//    private ReportQueryRepository reportQueryRepository;
//
//    @Mock
//    private ReportGroupService reportGroupService;
//
//    @InjectMocks
//    private ReportController reportController;
//
//    private MockMvc mockMvc;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    private final UUID TEST_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
//    private final UUID RECIPE_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
//    private final String USERNAME = "testuser";
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
//    }
//
//    // Xóa method setupMockMvc() thừa này đi
//    // private void setupMockMvc() { ... } ← XÓA HOÀN TOÀN
//
//    private ReportResponse createSampleReportResponse() {
//        return ReportResponse.builder()
//                .reportId(TEST_UUID)  // sửa lại cho đúng với response thực tế (id, không phải reportId)
//                .reportType(ReportType.SPAM)
//                .status(ReportStatus.PENDING)
//                .build();
//    }
//
//    private PageResponse<ReportResponse> createPageResponse() {
//        return PageResponse.<ReportResponse>builder()
//                .content(java.util.List.of(createSampleReportResponse()))
//                .page(0)
//                .size(20)
//                .totalElements(1L)
//                .totalPages(1)
//                .build();
//    }
//
//    @Test
//    @WithMockUser(roles = {"USER"})
//    void createReport_success() throws Exception {
//        CreateReportRequest request = new CreateReportRequest();
//        ReportResponse response = createSampleReportResponse();
//
//        when(reportService.createReport(any(CreateReportRequest.class))).thenReturn(response);
//
//        mockMvc.perform(post("/api/reports")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Tạo báo cáo thành công"))
//                .andExpect(jsonPath("$.data.id").value(TEST_UUID.toString()));
//    }
//
//    @Test
//    @WithMockUser(roles = {"ADMIN"})
//    void getGroupedReports_success() throws Exception {
//        PageResponse<ReportGroupResponse> response = PageResponse.<ReportGroupResponse>builder()
//                .content(java.util.List.of(new ReportGroupResponse()))
//                .page(0).size(20).totalElements(1L).totalPages(1).build();
//
//        when(reportGroupService.getGroupedReports(eq(0), eq(20), isNull(), isNull(), isNull()))
//                .thenReturn(response);
//
//        mockMvc.perform(get("/api/admin/reports/grouped"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Lấy danh sách báo cáo nhóm thành công"));
//    }
//
//    @Test
//    @WithMockUser(roles = {"ADMIN"})
//    void getGroupDetail_success() throws Exception {
//        ReportGroupDetailResponse response = new ReportGroupDetailResponse();
//
//        when(reportGroupService.getGroupDetail(RECIPE_ID)).thenReturn(response);
//
//        mockMvc.perform(get("/api/admin/reports/grouped/recipe/{recipeId}", RECIPE_ID))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    @WithMockUser(roles = {"ADMIN"})
//    void batchReviewByRecipe_success() throws Exception {
//        ReviewReportRequest request = new ReviewReportRequest();
//        BatchReviewResponse response = new BatchReviewResponse();
//
//        when(reportService.batchReviewByRecipe(eq(RECIPE_ID), any(ReviewReportRequest.class)))
//                .thenReturn(response);
//
//        mockMvc.perform(post("/api/admin/reports/grouped/recipe/{recipeId}/review", RECIPE_ID)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    @WithMockUser(username = USERNAME, roles = {"USER"})
//    void getMyReports_success() throws Exception {
//        when(securityUtil.getCurrentUserLogin()).thenReturn(Optional.of(USERNAME));
//        when(reportQueryRepository.findUserIdByUsername(USERNAME)).thenReturn(Optional.of(TEST_UUID));
//        when(reportService.getReports(any(ReportFilterRequest.class))).thenReturn(createPageResponse());
//
//        mockMvc.perform(get("/api/my-reports"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    @Test
//    void getMyReports_userNotFound_throwsException() throws Exception {
//        // Tự set SecurityContext vì @WithMockUser không hoạt động
//        org.springframework.security.core.context.SecurityContextHolder
//                .getContext()
//                .setAuthentication(
//                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
//                                USERNAME, null, java.util.Collections.singletonList(
//                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")
//                        )
//                        )
//                );
//
//        when(securityUtil.getCurrentUserLogin()).thenReturn(Optional.of(USERNAME));
//        when(reportQueryRepository.findUserIdByUsername(USERNAME)).thenReturn(Optional.empty());
//
//        mockMvc.perform(get("/api/my-reports"))
//                .andExpect(status().isNotFound());
//
//        // Clear sau test (tốt nhất)
//        SecurityContextHolder.clearContext();
//    }
//
//
//}