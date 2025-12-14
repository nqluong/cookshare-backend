package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.authentication.service.TokenBlacklistService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeApprovalRequest;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeUpdateRequest;
import com.backend.cookshare.recipe_management.dto.response.AdminRecipeDetailResponseDTO;
import com.backend.cookshare.recipe_management.dto.response.AdminRecipeListResponseDTO;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.service.AdminRecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WithMockUser(roles = "ADMIN")
class AdminRecipeControllerTest {

    private MockMvc mockMvc;

    private final AdminRecipeService adminRecipeService = mock(AdminRecipeService.class);

    @InjectMocks
    private AdminRecipeController adminRecipeController;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private final UUID recipeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminRecipeController).build();
    }
    private PageResponse<AdminRecipeListResponseDTO> createPageResponse() {
        AdminRecipeListResponseDTO item = new AdminRecipeListResponseDTO();
        List<AdminRecipeListResponseDTO> content = Collections.singletonList(item);

        PageResponse<AdminRecipeListResponseDTO> response = new PageResponse<>();
        response.setContent(content);
        response.setPage(0);
        response.setSize(10);
        response.setTotalElements(1L);
        response.setTotalPages(1);

        return response;
    }

    // Helper: tạo chi tiết công thức giả
    private AdminRecipeDetailResponseDTO createDetailResponse() {
        return new AdminRecipeDetailResponseDTO(); // Nếu cần, có thể set các field
    }
    @Test
    void getAllRecipes_DefaultParams_Success() throws Exception {
        when(adminRecipeService.getAllRecipesWithPagination(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(createPageResponse());

        mockMvc.perform(get("/api/admin/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy danh sách công thức thành công"));
    }

    @Test
    void getRecipeDetail_Success() throws Exception {
        when(adminRecipeService.getRecipeDetailById(recipeId))
                .thenReturn(createDetailResponse());

        mockMvc.perform(get("/api/admin/recipes/{id}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy thông tin chi tiết công thức thành công"));
    }

    @Test
    void updateRecipe_Success() throws Exception {
        AdminRecipeUpdateRequest request = new AdminRecipeUpdateRequest();
        when(adminRecipeService.updateRecipe(eq(recipeId), any()))
                .thenReturn(createDetailResponse());

        mockMvc.perform(put("/api/admin/recipes/{id}", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật công thức thành công"));
    }

    @Test
    void approveRecipe_Approve_Success() throws Exception {
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(true);

        doNothing().when(adminRecipeService).approveRecipe(eq(recipeId), any());

        mockMvc.perform(put("/api/admin/recipes/{id}/approve", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Phê duyệt công thức thành công"));
    }

    @Test
    void approveRecipe_Reject_Success() throws Exception {
        AdminRecipeApprovalRequest request = new AdminRecipeApprovalRequest();
        request.setApproved(false);

        doNothing().when(adminRecipeService).approveRecipe(eq(recipeId), any());

        mockMvc.perform(put("/api/admin/recipes/{id}/approve", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Từ chối công thức thành công"));
    }

    @Test
    void deleteRecipe_Success() throws Exception {
        doNothing().when(adminRecipeService).deleteRecipe(recipeId);

        mockMvc.perform(delete("/api/admin/recipes/{id}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa công thức thành công"));
    }

    @Test
    void getRecipesByStatus_Success() throws Exception {
        when(adminRecipeService.getRecipesByStatus(eq(RecipeStatus.PENDING), any(Pageable.class)))
                .thenReturn(createPageResponse());

        mockMvc.perform(get("/api/admin/recipes/status/PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void getPendingRecipes_WithSearch_Success() throws Exception {
        when(adminRecipeService.getPendingRecipes(eq("soup"), any(Pageable.class)))
                .thenReturn(createPageResponse());

        mockMvc.perform(get("/api/admin/recipes/pending")
                        .param("search", "soup")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    void getPendingRecipes_NoSearch_DefaultDesc_Success() throws Exception {
        when(adminRecipeService.getPendingRecipes(eq(null), any(Pageable.class)))
                .thenReturn(createPageResponse());

        mockMvc.perform(get("/api/admin/recipes/pending"))
                .andExpect(status().isOk());
    }

    @Test
    void getApprovedRecipes_Success() throws Exception {
        when(adminRecipeService.getApprovedRecipes(anyString(), any(Pageable.class)))
                .thenReturn(createPageResponse());

        mockMvc.perform(get("/api/admin/recipes/approved"))
                .andExpect(status().isOk());
    }

    @Test
    void getRejectedRecipes_Success() throws Exception {
        when(adminRecipeService.getRejectedRecipes(anyString(), any(Pageable.class)))
                .thenReturn(createPageResponse());

        mockMvc.perform(get("/api/admin/recipes/rejected"))
                .andExpect(status().isOk());
    }

    @Test
    void setFeaturedRecipe_MakeFeatured_Success() throws Exception {
        doNothing().when(adminRecipeService).setFeaturedRecipe(eq(recipeId), eq(true));

        mockMvc.perform(put("/api/admin/recipes/{id}/featured", recipeId)
                        .param("isFeatured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt công thức làm nổi bật thành công"));
    }

    @Test
    void setFeaturedRecipe_RemoveFeatured_Success() throws Exception {
        doNothing().when(adminRecipeService).setFeaturedRecipe(eq(recipeId), eq(false));

        mockMvc.perform(put("/api/admin/recipes/{id}/featured", recipeId)
                        .param("isFeatured", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bỏ nổi bật công thức thành công"));
    }

    @Test
    void setPublishedRecipe_Publish_Success() throws Exception {
        doNothing().when(adminRecipeService).setPublishedRecipe(eq(recipeId), eq(true));

        mockMvc.perform(put("/api/admin/recipes/{id}/published", recipeId)
                        .param("isPublished", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xuất bản công thức thành công"));
    }

    @Test
    void setPublishedRecipe_Unpublish_Success() throws Exception {
        doNothing().when(adminRecipeService).setPublishedRecipe(eq(recipeId), eq(false));

        mockMvc.perform(put("/api/admin/recipes/{id}/published", recipeId)
                        .param("isPublished", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ẩn công thức thành công"));
    }
}