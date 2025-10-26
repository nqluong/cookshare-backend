package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.request.AdminRecipeApprovalRequest;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeUpdateRequest;
import com.backend.cookshare.recipe_management.dto.response.AdminRecipeDetailResponseDTO;
import com.backend.cookshare.recipe_management.dto.response.AdminRecipeListResponseDTO;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminRecipeService {
    
    /**
     * Lấy danh sách tất cả công thức với phân trang và tìm kiếm
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param isPublished Trạng thái xuất bản (null: tất cả, true: đã xuất bản, false: chưa xuất bản)
     * @param isFeatured Có phải công thức nổi bật không (null: tất cả, true: nổi bật, false: không nổi bật)
     * @param status Trạng thái phê duyệt (null: tất cả, PENDING: chờ phê duyệt, APPROVED: đã phê duyệt, REJECTED: đã từ chối)
     * @param pageable Đối tượng phân trang và sắp xếp
     * @return PageResponse chứa danh sách AdminRecipeListResponseDTO
     */
    PageResponse<AdminRecipeListResponseDTO> getAllRecipesWithPagination(
            String search, 
            Boolean isPublished, 
            Boolean isFeatured,
            RecipeStatus status,
            Pageable pageable);
    
    /**
     * Lấy thông tin chi tiết công thức theo ID
     * @param recipeId ID của công thức
     * @return AdminRecipeDetailResponseDTO chứa thông tin chi tiết
     */
    AdminRecipeDetailResponseDTO getRecipeDetailById(UUID recipeId);
    
    /**
     * Cập nhật thông tin công thức bởi admin
     * @param recipeId ID của công thức
     * @param request Thông tin cập nhật
     * @return AdminRecipeDetailResponseDTO đã được cập nhật
     */
    AdminRecipeDetailResponseDTO updateRecipe(UUID recipeId, AdminRecipeUpdateRequest request);
    
    /**
     * Phê duyệt hoặc từ chối công thức
     * @param recipeId ID của công thức
     * @param request Yêu cầu phê duyệt
     */
    void approveRecipe(UUID recipeId, AdminRecipeApprovalRequest request);
    
    /**
     * Xóa công thức (chỉ dành cho admin)
     * @param recipeId ID của công thức
     */
    void deleteRecipe(UUID recipeId);
    
    /**
     * Lấy danh sách công thức theo trạng thái
     * @param status Trạng thái công thức
     * @param pageable Đối tượng phân trang và sắp xếp
     * @return PageResponse chứa danh sách công thức theo trạng thái
     */
    PageResponse<AdminRecipeListResponseDTO> getRecipesByStatus(RecipeStatus status, Pageable pageable);
    
    /**
     * Lấy danh sách công thức chờ phê duyệt
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param pageable Đối tượng phân trang và sắp xếp
     * @return PageResponse chứa danh sách công thức chờ phê duyệt
     */
    PageResponse<AdminRecipeListResponseDTO> getPendingRecipes(String search, Pageable pageable);
    
    /**
     * Lấy danh sách công thức đã được phê duyệt
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param pageable Đối tượng phân trang và sắp xếp
     * @return PageResponse chứa danh sách công thức đã được phê duyệt
     */
    PageResponse<AdminRecipeListResponseDTO> getApprovedRecipes(String search, Pageable pageable);
    
    /**
     * Lấy danh sách công thức bị từ chối
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param pageable Đối tượng phân trang và sắp xếp
     * @return PageResponse chứa danh sách công thức bị từ chối
     */
    PageResponse<AdminRecipeListResponseDTO> getRejectedRecipes(String search, Pageable pageable);
    
    /**
     * Đặt công thức làm nổi bật hoặc bỏ nổi bật
     * @param recipeId ID của công thức
     * @param isFeatured Có phải nổi bật không
     */
    void setFeaturedRecipe(UUID recipeId, Boolean isFeatured);
    
    /**
     * Xuất bản hoặc ẩn công thức
     * @param recipeId ID của công thức
     * @param isPublished Có xuất bản không
     */
    void setPublishedRecipe(UUID recipeId, Boolean isPublished);

}
