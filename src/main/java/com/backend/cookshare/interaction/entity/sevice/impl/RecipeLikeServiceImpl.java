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
    @Override
    public RecipeLikeResponse likerecipe(UUID recipeId) {
        var context= SecurityContextHolder.getContext();
        String username=context.getAuthentication().getName();
        Optional<User> user=userRepository.findByUsername(username);
        UUID userId =user.get().getUserId();
        Recipe recipe= recipeRepository.findById(recipeId).orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));
        RecipeLike recipeLike= RecipeLike.builder()
                .userId(userId)
                .recipeId(recipeId)
                .build();
        recipeLike= recipeLikeRepository.save(recipeLike);
        recipe.setLikeCount(recipe.getLikeCount() +1 );
        recipeRepository.save(recipe);
        return recipeLikeMapper.toRecipeLikeResponse(recipeLike);
    }
    @Override
    public Boolean isRecipeLiked(UUID recipeId) {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        Optional<User> user = userRepository.findByUsername(username);
        UUID userId = user.get().getUserId();
        return recipeLikeRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }
    @Override
    public void unlikerecipe(UUID recipeId) {
        var context= SecurityContextHolder.getContext();
        String username=context.getAuthentication().getName();
        Optional<User> user=userRepository.findByUsername(username);
        UUID userId =user.get().getUserId();
        Recipe recipe= recipeRepository.findById(recipeId).orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));
        RecipeLike recipeLike = recipeLikeRepository
                .findByUserIdAndRecipeId(userId, recipeId);
        recipeLikeRepository.delete(recipeLike);
        recipe.setLikeCount(recipe.getLikeCount() -1 );
        recipeRepository.save(recipe);
    }

}
