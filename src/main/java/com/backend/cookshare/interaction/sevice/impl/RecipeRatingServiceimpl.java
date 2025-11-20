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

        // Tìm xem user đã rate chưa
        Optional<RecipeRating> existingRating = recipeRatingRespository
                .findByUserIdAndRecipeId(currentUser.getUserId(), recipeId);
        log.info("rate: {}", existingRating);
        RecipeRating rating;
        if (existingRating.isPresent()) {
            // ĐÃ RATE → CẬP NHẬT
            rating = existingRating.get();
            rating.setRating(rate);
            rating.setUpdatedAt(LocalDateTime.now());
        } else {
            // CHƯA RATE → TẠO MỚI
            rating = RecipeRating.builder()
                    .userId(currentUser.getUserId())
                    .recipeId(recipeId)
                    .rating(rate)
                    .review(null)
                    .build();
        }

        rating = recipeRatingRespository.save(rating);

        // Cập nhật lại average rating
        BigDecimal avg = recipeRatingRespository.getAverageRatingByRecipeId(recipeId);
        recipe.setAverageRating(avg);

        if (existingRating.isEmpty()) {
            recipe.setRatingCount(recipe.getRatingCount() + 1);
        }
        recipeRepository.save(recipe);
        log.info("recipe: {}",recipe);
        return recipeRatingMapper.toRecipeRatingResponse(rating);
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
