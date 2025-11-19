package com.backend.cookshare.interaction.sevice.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.interaction.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.mapper.RecipeLikeMapper;
import com.backend.cookshare.interaction.repository.RecipeLikeRepository;
import com.backend.cookshare.interaction.sevice.RecipeLikeService;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecipeLikeServiceImpl implements RecipeLikeService {
    RecipeLikeRepository recipeLikeRepository;
    UserRepository userRepository;
    RecipeRepository recipeRepository;
    RecipeLikeMapper recipeLikeMapper;
    FirebaseStorageService firebaseStorageService;
    NotificationService notificationService;
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public RecipeLikeResponse likerecipe(UUID recipeId) {
        User currentUser = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        // Kiểm tra like trùng lặp
        if (recipeLikeRepository.existsByUserIdAndRecipeId(currentUser.getUserId(), recipeId)) {
            throw new CustomException(ErrorCode.RECIPE_ALREADY_LIKED);
        }

        RecipeLike recipeLike = RecipeLike.builder()
                .userId(currentUser.getUserId())
                .recipeId(recipeId)
                .createdAt(LocalDateTime.now())
                .build();
        recipeLike = recipeLikeRepository.save(recipeLike);
        recipe.setLikeCount(recipe.getLikeCount() + 1);
        recipeRepository.save(recipe);
        notificationService.createLikeNotification(recipeRepository.findUserIdByRecipeId(recipeId), currentUser.getUserId(), recipeId);
        return recipeLikeMapper.toRecipeLikeResponse(recipeLike);
    }

    @Override
    @Transactional
    public void unlikerecipe(UUID recipeId) {
        User currentUser = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        RecipeLike recipeLike = recipeLikeRepository.findByUserIdAndRecipeId(currentUser.getUserId(), recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_LIKED));

//        notificationService.deleteLikeNotification(recipeRepository.findUserIdByRecipeId(recipeId), currentUser.getUserId(), recipeId);
        recipeLikeRepository.delete(recipeLike);
        recipe.setLikeCount(Math.max(0, recipe.getLikeCount() - 1));
        recipeRepository.save(recipe);
    }
    @Override
    @Transactional
    public PageResponse<RecipeLikeResponse> getallRecipeLiked(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<RecipeLike> likedRecipes = recipeLikeRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable);

        Page<RecipeLikeResponse> responsePage = likedRecipes.map(like -> {
            Recipe recipe = recipeRepository.findById(like.getRecipeId()).orElse(null);
            if (recipe == null) return null;
            var summary = recipeLikeMapper.toRecipeSummary(recipe);
            if (summary != null && summary.getFeaturedImage() != null) {
                summary.setFeaturedImage(firebaseStorageService.convertPathToFirebaseUrl(summary.getFeaturedImage()));
            }
            return recipeLikeMapper.toRecipeResponse(like, summary);
        });

        // Filter out null values
        List<RecipeLikeResponse> filteredContent = responsePage.getContent().stream()
                .filter(response -> response != null)
                .collect(Collectors.toList());

        return PageResponse.<RecipeLikeResponse>builder()
                .page(page)
                .size(size)
                .totalPages(responsePage.getTotalPages())
                .totalElements(responsePage.getTotalElements())
                .content(filteredContent)
                .build();
    }
    @Override
    public Boolean isRecipeLiked(UUID recipeId) {
        User currentUser = getCurrentUser();
        return recipeLikeRepository.existsByUserIdAndRecipeId(currentUser.getUserId(), recipeId);
    }
    @Override
    public Map<UUID, Boolean> checkMultipleLikes(List<UUID> recipeIds) {
        User currentUser = getCurrentUser();

        // Query 1 lần duy nhất cho tất cả recipes
        List<RecipeLike> likes = recipeLikeRepository
                .findAllByUserIdAndRecipeIdIn(currentUser.getUserId(), recipeIds);

        // Convert thành Map để tra cứu nhanh
        Set<UUID> likedRecipeIds = likes.stream()
                .map(RecipeLike::getRecipeId)
                .collect(Collectors.toSet());

        // Trả về Map với tất cả recipeIds
        return recipeIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        likedRecipeIds::contains
                ));
    }
}