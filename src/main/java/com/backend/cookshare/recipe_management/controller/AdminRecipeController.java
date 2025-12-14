package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.request.AdminRecipeApprovalRequest;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeUpdateRequest;
import com.backend.cookshare.recipe_management.dto.response.AdminRecipeDetailResponseDTO;
import com.backend.cookshare.recipe_management.dto.response.AdminRecipeListResponseDTO;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.service.AdminRecipeService;
import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/admin/recipes")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminRecipeController {

    private final AdminRecipeService adminRecipeService;

    /**
     * GET /api/admin/recipes - Lấy danh sách tất cả công thức với phân trang và tìm kiếm
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param isPublished Trạng thái xuất bản (null: tất cả, true: đã xuất bản, false: chưa xuất bản)
     * @param isFeatured Có phải công thức nổi bật không (null: tất cả, true: nổi bật, false: không nổi bật)
     * @param status Trạng thái phê duyệt (null: tất cả, PENDING: chờ phê duyệt, APPROVED: đã phê duyệt, REJECTED: đã từ chối)
     * @param page Số trang (mặc định: 0)
     * @param size Kích thước trang (mặc định: 10)
     * @param sortBy Trường sắp xếp (mặc định: createdAt)
     * @param sortDir Hướng sắp xếp (mặc định: desc)
     * @return Danh sách công thức có phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminRecipeListResponseDTO>>> getAllRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) RecipeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Admin đang lấy danh sách công thức - trang: {}, kích thước: {}, tìm kiếm: {}, xuất bản: {}, nổi bật: {}, trạng thái: {}", 
                page, size, search, isPublished, isFeatured, status);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<AdminRecipeListResponseDTO> pageResponse = adminRecipeService.getAllRecipesWithPagination(
                search, isPublished, status, pageable);

        ApiResponse<PageResponse<AdminRecipeListResponseDTO>> response = ApiResponse.<PageResponse<AdminRecipeListResponseDTO>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức thành công")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/recipes/{id} - Lấy thông tin chi tiết công thức
     * @param id ID của công thức
     * @return Thông tin chi tiết công thức
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminRecipeDetailResponseDTO>> getRecipeDetail(@PathVariable UUID id) {
        log.info("Admin đang lấy thông tin chi tiết công thức với recipeId: {}", id);

        AdminRecipeDetailResponseDTO recipeDetail = adminRecipeService.getRecipeDetailById(id);

        ApiResponse<AdminRecipeDetailResponseDTO> response = ApiResponse.<AdminRecipeDetailResponseDTO>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy thông tin chi tiết công thức thành công")
                .data(recipeDetail)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/recipes/{id} - Cập nhật thông tin công thức
     * @param id ID của công thức
     * @param request Thông tin cập nhật
     * @return Thông tin công thức đã được cập nhật
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminRecipeDetailResponseDTO>> updateRecipe(
            @PathVariable UUID id,
            @RequestBody AdminRecipeUpdateRequest request) {
        
        log.info("Admin đang cập nhật công thức: {}", id);
        
        AdminRecipeDetailResponseDTO updatedRecipe = adminRecipeService.updateRecipe(id, request);

        ApiResponse<AdminRecipeDetailResponseDTO> response = ApiResponse.<AdminRecipeDetailResponseDTO>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Cập nhật công thức thành công")
                .data(updatedRecipe)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/recipes/{id}/approve - Phê duyệt hoặc từ chối công thức
     * @param id ID của công thức
     * @param request Yêu cầu phê duyệt
     * @return Phản hồi thành công
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRecipe(
            @PathVariable UUID id,
            @RequestBody AdminRecipeApprovalRequest request) {
        
        log.info("Admin đang phê duyệt công thức: {} với trạng thái: {}", id, request.getApproved());
        
        adminRecipeService.approveRecipe(id, request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message(request.getApproved() ? "Phê duyệt công thức thành công" : "Từ chối công thức thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/admin/recipes/{id} - Xóa công thức
     * @param id ID của công thức
     * @return Phản hồi thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipe(@PathVariable UUID id) {
        log.info("Admin đang xóa công thức: {}", id);
        
        adminRecipeService.deleteRecipe(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Xóa công thức thành công")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/recipes/status/{status} - Lấy danh sách công thức theo trạng thái
     * @param status Trạng thái công thức (PENDING, APPROVED, REJECTED)
     * @param page Số trang (mặc định: 0)
     * @param size Kích thước trang (mặc định: 10)
     * @return Danh sách công thức theo trạng thái
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PageResponse<AdminRecipeListResponseDTO>>> getRecipesByStatus(
            @PathVariable RecipeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Admin đang lấy danh sách công thức theo trạng thái: {} - trang: {}, kích thước: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<AdminRecipeListResponseDTO> pageResponse = adminRecipeService.getRecipesByStatus(status, pageable);

        ApiResponse<PageResponse<AdminRecipeListResponseDTO>> response = ApiResponse.<PageResponse<AdminRecipeListResponseDTO>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức theo trạng thái thành công")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/recipes/pending - Lấy danh sách công thức chờ phê duyệt
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param page Số trang (mặc định: 0)
     * @param size Kích thước trang (mặc định: 10)
     * @param sortBy Trường sắp xếp (mặc định: createdAt)
     * @param sortDir Hướng sắp xếp (mặc định: desc)
     * @return Danh sách công thức chờ phê duyệt
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<PageResponse<AdminRecipeListResponseDTO>>> getPendingRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Admin đang lấy danh sách công thức chờ phê duyệt - trang: {}, kích thước: {}, tìm kiếm: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<AdminRecipeListResponseDTO> pageResponse = adminRecipeService.getPendingRecipes(search, pageable);

        ApiResponse<PageResponse<AdminRecipeListResponseDTO>> response = ApiResponse.<PageResponse<AdminRecipeListResponseDTO>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức chờ phê duyệt thành công")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/recipes/approved - Lấy danh sách công thức đã được phê duyệt
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param page Số trang (mặc định: 0)
     * @param size Kích thước trang (mặc định: 10)
     * @param sortBy Trường sắp xếp (mặc định: createdAt)
     * @param sortDir Hướng sắp xếp (mặc định: desc)
     * @return Danh sách công thức đã được phê duyệt
     */
    @GetMapping("/approved")
    public ResponseEntity<ApiResponse<PageResponse<AdminRecipeListResponseDTO>>> getApprovedRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Admin đang lấy danh sách công thức đã được phê duyệt - trang: {}, kích thước: {}, tìm kiếm: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<AdminRecipeListResponseDTO> pageResponse = adminRecipeService.getApprovedRecipes(search, pageable);

        ApiResponse<PageResponse<AdminRecipeListResponseDTO>> response = ApiResponse.<PageResponse<AdminRecipeListResponseDTO>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức đã được phê duyệt thành công")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/recipes/rejected - Lấy danh sách công thức bị từ chối
     * @param search Từ khóa tìm kiếm (có thể null)
     * @param page Số trang (mặc định: 0)
     * @param size Kích thước trang (mặc định: 10)
     * @param sortBy Trường sắp xếp (mặc định: createdAt)
     * @param sortDir Hướng sắp xếp (mặc định: desc)
     * @return Danh sách công thức bị từ chối
     */
    @GetMapping("/rejected")
    public ResponseEntity<ApiResponse<PageResponse<AdminRecipeListResponseDTO>>> getRejectedRecipes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Admin đang lấy danh sách công thức bị từ chối - trang: {}, kích thước: {}, tìm kiếm: {}", page, size, search);

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<AdminRecipeListResponseDTO> pageResponse = adminRecipeService.getRejectedRecipes(search, pageable);

        ApiResponse<PageResponse<AdminRecipeListResponseDTO>> response = ApiResponse.<PageResponse<AdminRecipeListResponseDTO>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức bị từ chối thành công")
                .data(pageResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/admin/recipes/{id}/published - Xuất bản hoặc ẩn công thức
     * @param id ID của công thức
     * @param isPublished Có xuất bản không
     * @return Phản hồi thành công
     */
    @PutMapping("/{id}/published")
    public ResponseEntity<ApiResponse<Void>> setPublishedRecipe(
            @PathVariable UUID id,
            @RequestParam Boolean isPublished) {
        
        log.info("Admin đang xuất bản công thức {}: {}", id, isPublished);
        
        adminRecipeService.setPublishedRecipe(id, isPublished);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message(isPublished ? "Xuất bản công thức thành công" : "Ẩn công thức thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}
