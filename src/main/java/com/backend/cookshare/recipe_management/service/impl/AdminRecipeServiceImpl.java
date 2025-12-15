package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.authentication.service.UserService;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeApprovalRequest;
import com.backend.cookshare.recipe_management.dto.request.AdminRecipeUpdateRequest;
import com.backend.cookshare.recipe_management.dto.response.*;
import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.recipe_management.service.AdminRecipeService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.user.service.NotificationService;
import com.backend.cookshare.user.service.ActivityLogService;
import com.backend.cookshare.user.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminRecipeServiceImpl implements AdminRecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeLoaderHelper recipeLoaderHelper;
    private final UserService userService;
    private final PageMapper pageMapper;
    private final FirebaseStorageService firebaseStorageService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final FollowRepository followRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AdminRecipeListResponseDTO> getAllRecipesWithPagination(
            String search, Boolean isPublished, RecipeStatus status, Pageable pageable) {


        Page<Recipe> recipePage = recipeRepository.findAllWithAdminFilters(search, isPublished, status, pageable);

        return pageMapper.toPageResponse(recipePage, this::mapToListResponseDTO);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminRecipeDetailResponseDTO getRecipeDetailById(UUID recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        try {
            RecipeDetailsResult details =
                    recipeLoaderHelper.loadRecipeDetailsForAdmin(recipeId, recipe.getUserId());

            User user = details.user;

            return AdminRecipeDetailResponseDTO.builder()
                    .recipeId(recipe.getRecipeId())
                    .userId(recipe.getUserId())
                    .title(recipe.getTitle())
                    .slug(recipe.getSlug())
                    .description(recipe.getDescription())
                    .prepTime(recipe.getPrepTime())
                    .cookTime(recipe.getCookTime())
                    .servings(recipe.getServings())
                    .difficulty(recipe.getDifficulty())
                    .featuredImage(firebaseStorageService.convertPathToFirebaseUrl(recipe.getFeaturedImage()))
                    .instructions(recipe.getInstructions())
                    .notes(recipe.getNotes())
                    .nutritionInfo(recipe.getNutritionInfo())
                    .viewCount(recipe.getViewCount())
                    .saveCount(recipe.getSaveCount())
                    .likeCount(recipe.getLikeCount())
                    .averageRating(recipe.getAverageRating())
                    .ratingCount(recipe.getRatingCount())
                    .isPublished(recipe.getIsPublished())
                    .status(recipe.getStatus())
                    .metaKeywords(recipe.getMetaKeywords())
                    .seasonalTags(recipe.getSeasonalTags())
                    .createdAt(recipe.getCreatedAt())
                    .updatedAt(recipe.getUpdatedAt())
                    .username(user != null ? user.getUsername() : null)
                    .userFullName(user != null ? user.getFullName() : null)
                    .userEmail(user != null ? user.getEmail() : null)
                    .userAvatarUrl(user != null ? user.getAvatarUrl() : null)
                    .steps(details.steps)
                    .ingredients(details.ingredients)
                    .tags(details.tags)
                    .categories(details.categories)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết công thức cho admin: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminRecipeDetailResponseDTO updateRecipe(UUID recipeId, AdminRecipeUpdateRequest request) {
        log.info("Admin đang cập nhật công thức: {} với thông tin: {}", recipeId, request);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        // Cập nhật thông tin công thức
        if (request.getTitle() != null) recipe.setTitle(request.getTitle());
        if (request.getDescription() != null) recipe.setDescription(request.getDescription());
        if (request.getPrepTime() != null) recipe.setPrepTime(request.getPrepTime());
        if (request.getCookTime() != null) recipe.setCookTime(request.getCookTime());
        if (request.getServings() != null) recipe.setServings(request.getServings());
        if (request.getDifficulty() != null) recipe.setDifficulty(request.getDifficulty());
        if (request.getFeaturedImage() != null) recipe.setFeaturedImage(request.getFeaturedImage());
        if (request.getInstructions() != null) recipe.setInstructions(request.getInstructions());
        if (request.getNotes() != null) recipe.setNotes(request.getNotes());
        if (request.getNutritionInfo() != null) recipe.setNutritionInfo(request.getNutritionInfo());
        if (request.getIsPublished() != null) recipe.setIsPublished(request.getIsPublished());
        if (request.getMetaKeywords() != null) recipe.setMetaKeywords(request.getMetaKeywords());
        if (request.getSeasonalTags() != null) recipe.setSeasonalTags(request.getSeasonalTags());
        if (request.getAverageRating() != null) recipe.setAverageRating(request.getAverageRating());
        if (request.getRatingCount() != null) recipe.setRatingCount(request.getRatingCount());

        recipe.setUpdatedAt(LocalDateTime.now());
        recipeRepository.save(recipe);

        // LOG ACTIVITY: Admin cập nhật recipe
        activityLogService.logRecipeActivity(recipe.getUserId(), recipeId, "UPDATE");

        log.info("Cập nhật công thức thành công: {}", recipeId);
        return getRecipeDetailById(recipeId);
    }

    @Override
    public void approveRecipe(UUID recipeId, AdminRecipeApprovalRequest request) {
        log.info("Admin đang phê duyệt công thức: {} với trạng thái: {}", recipeId, request.getApproved());

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        if (recipe.getStatus() == RecipeStatus.APPROVED && request.getApproved()) {
            log.warn("Công thức {} đã được phê duyệt trước đó", recipeId);
            return;
        }

        if (request.getApproved()) {
            recipe.setStatus(RecipeStatus.APPROVED);
            recipe.setIsPublished(true);
            log.info("Công thức {} đã được phê duyệt", recipeId);

            activityLogService.logRecipeActivity(recipe.getUserId(), recipeId, "APPROVE");

            notificationService.createRecipeApprovedNotification(
                    recipe.getUserId(),
                    recipeId,
                    recipe.getTitle()
            );

            List<UUID> followerIds = followRepository.findAllFollowerIdsByUser(recipe.getUserId());

            if (!followerIds.isEmpty()) {
                User recipeOwner = userService.getUserById(recipe.getUserId())
                        .orElse(null);

                if (recipeOwner != null) {
                    notificationService.createNewRecipeNotificationForFollowers(
                            followerIds,
                            recipe.getUserId(),
                            recipeOwner.getFullName(),
                            recipeId,
                            recipe.getTitle()
                    );
                    log.info("Đã gửi thông báo công thức mới đến {} followers", followerIds.size());
                }
            }

        } else {
            recipe.setStatus(RecipeStatus.REJECTED);
            recipe.setIsPublished(false);
            log.info("Công thức {} đã bị từ chối với lý do: {}", recipeId, request.getRejectionReason());

            // LOG ACTIVITY: Admin từ chối recipe
            activityLogService.logRecipeActivity(recipe.getUserId(), recipeId, "REJECT");
        }

        recipe.setUpdatedAt(LocalDateTime.now());
        recipeRepository.save(recipe);
    }

    @Override
    public void deleteRecipe(UUID recipeId) {
        log.info("Admin đang xóa công thức: {}", recipeId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        notificationService.deleteRecipeNotifications(recipeId);

        activityLogService.logRecipeActivity(recipe.getUserId(), recipeId, "DELETE");

        recipeRepository.delete(recipe);
        log.info("Công thức {} đã được xóa thành công", recipeId);
    }

    @Override
    public PageResponse<AdminRecipeListResponseDTO> getRecipesByStatus(RecipeStatus status, Pageable pageable) {
        log.info("Admin đang lấy danh sách công thức theo trạng thái: {} - trang: {}, kích thước: {}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        Page<Recipe> recipePage = recipeRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        return pageMapper.toPageResponse(recipePage, this::mapToListResponseDTO);
    }

    @Override
    public PageResponse<AdminRecipeListResponseDTO> getPendingRecipes(String search, Pageable pageable) {
        log.info("Admin đang lấy danh sách công thức chờ phê duyệt - trang: {}, kích thước: {}, tìm kiếm: {}",
                pageable.getPageNumber(), pageable.getPageSize(), search);

        return getAllRecipesWithPagination(search, null, RecipeStatus.PENDING, pageable);
    }

    @Override
    public PageResponse<AdminRecipeListResponseDTO> getApprovedRecipes(String search, Pageable pageable) {
        log.info("Admin đang lấy danh sách công thức đã được phê duyệt - trang: {}, kích thước: {}, tìm kiếm: {}",
                pageable.getPageNumber(), pageable.getPageSize(), search);

        return getAllRecipesWithPagination(search, null, RecipeStatus.APPROVED, pageable);
    }

    @Override
    public PageResponse<AdminRecipeListResponseDTO> getRejectedRecipes(String search, Pageable pageable) {
        log.info("Admin đang lấy danh sách công thức bị từ chối - trang: {}, kích thước: {}, tìm kiếm: {}",
                pageable.getPageNumber(), pageable.getPageSize(), search);

        return getAllRecipesWithPagination(search, null, RecipeStatus.REJECTED, pageable);
    }

    @Override
    public void setPublishedRecipe(UUID recipeId, Boolean isPublished) {
        log.info("Admin đang xuất bản công thức {}: {}", recipeId, isPublished);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        if (isPublished && recipe.getStatus() != RecipeStatus.APPROVED) {
            throw new CustomException(ErrorCode.RECIPE_NOT_APPROVED);
        }

        recipe.setIsPublished(isPublished);
        recipe.setUpdatedAt(LocalDateTime.now());
        recipeRepository.save(recipe);

        log.info("Công thức {} đã được xuất bản: {}", recipeId, isPublished);
    }

    private AdminRecipeListResponseDTO mapToListResponseDTO(Recipe recipe) {
        User user = userService.getUserById(recipe.getUserId()).orElse(null);

        return AdminRecipeListResponseDTO.builder()
                .recipeId(recipe.getRecipeId())
                .userId(recipe.getUserId())
                .title(recipe.getTitle())
                .slug(recipe.getSlug())
                .description(recipe.getDescription())
                .prepTime(recipe.getPrepTime())
                .cookTime(recipe.getCookTime())
                .servings(recipe.getServings())
                .difficulty(recipe.getDifficulty())
                .featuredImage(firebaseStorageService.convertPathToFirebaseUrl(recipe.getFeaturedImage()))
                .viewCount(recipe.getViewCount())
                .saveCount(recipe.getSaveCount())
                .likeCount(recipe.getLikeCount())
                .averageRating(recipe.getAverageRating())
                .ratingCount(recipe.getRatingCount())
                .isPublished(recipe.getIsPublished())
                .status(recipe.getStatus())
                .metaKeywords(recipe.getMetaKeywords())
                .seasonalTags(recipe.getSeasonalTags())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .username(user != null ? user.getUsername() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .build();
    }
}