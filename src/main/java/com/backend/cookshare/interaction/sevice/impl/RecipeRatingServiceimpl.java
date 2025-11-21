package com.backend.cookshare.interaction.sevice.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.dto.response.RecipeRatingResponse;
import com.backend.cookshare.interaction.entity.RecipeRating;
import com.backend.cookshare.interaction.mapper.RecipeRatingMapper;
import com.backend.cookshare.interaction.repository.RecipeRatingRespository;
import com.backend.cookshare.interaction.sevice.RecipeRatingService;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecipeRatingServiceimpl implements RecipeRatingService {
    UserRepository userRepository;
    RecipeRepository recipeRepository;
    RecipeRatingRespository recipeRatingRespository;
    RecipeRatingMapper recipeRatingMapper;
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
    @Override
    @Transactional
    public RecipeRatingResponse ratingrecipe(UUID recipeId, Integer rate) {

        User currentUser = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        Optional<RecipeRating> existingRating = recipeRatingRespository
                .findByUserIdAndRecipeId(currentUser.getUserId(), recipeId);

        RecipeRating rating;

        if (existingRating.isPresent()) {
            // update đánh giá
            rating = existingRating.get();
            rating.setRating(rate);
            rating.setUpdatedAt(LocalDateTime.now());
        } else {
            // tạo đánh giá mới
            rating = RecipeRating.builder()
                    .userId(currentUser.getUserId())
                    .recipeId(recipeId)
                    .rating(rate)
                    .review(null)
                    .build();

            recipe.setRatingCount(recipe.getRatingCount() + 1);
        }

        recipeRatingRespository.save(rating);

        // cập nhật avg mới
        BigDecimal avg = recipeRatingRespository.getAverageRatingByRecipeId(recipeId);
        recipe.setAverageRating(avg);
        recipeRepository.save(recipe);

        // trả về response kèm avg + count
        return RecipeRatingResponse.builder()
                .ratingId(rating.getRatingId())
                .userId(rating.getUserId())
                .recipeId(rating.getRecipeId())
                .rating(rating.getRating())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .averageRating(recipe.getAverageRating())
                .ratingCount(recipe.getRatingCount())
                .build();
    }
    @Override
    public Boolean isRecipeRated(UUID recipeId) {
        User currentUser = getCurrentUser();
        return recipeRatingRespository.existsByUserIdAndRecipeId(currentUser.getUserId(), recipeId);
    }
    @Override
    public Integer getMyRating(UUID recipeId){
        User currentUser = getCurrentUser();
        return recipeRatingRespository.findByUserIdAndRecipeId(currentUser.getUserId(), recipeId)
                .map(RecipeRating::getRating)
                .orElse(null);
    }

}
