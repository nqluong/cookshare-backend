package com.backend.cookshare.interaction.entity.sevice.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.interaction.entity.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.entity.mapper.RecipeLikeMapper;
import com.backend.cookshare.interaction.entity.repository.RecipeLikeRepository;
import com.backend.cookshare.interaction.entity.sevice.RecipeLikeService;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecipeLikeServiceImpl implements RecipeLikeService {
    RecipeLikeRepository recipeLikeRepository;
    UserRepository userRepository;
    RecipeRepository recipeRepository;
    RecipeLikeMapper recipeLikeMapper;

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
                .build();
        recipeLike = recipeLikeRepository.save(recipeLike);
        recipe.setLikeCount(recipe.getLikeCount() + 1);
        recipeRepository.save(recipe);
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

        recipeLikeRepository.delete(recipeLike);
        recipe.setLikeCount(Math.max(0, recipe.getLikeCount() - 1));
        recipeRepository.save(recipe);
    }

    @Override
    public Boolean isRecipeLiked(UUID recipeId) {
        User currentUser = getCurrentUser();
        return recipeLikeRepository.existsByUserIdAndRecipeId(currentUser.getUserId(), recipeId);
    }
}