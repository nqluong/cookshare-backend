package com.backend.cookshare.recipe_management.dto.response;

import com.backend.cookshare.authentication.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeDetailsResult {
    public List<RecipeStepResponse> steps;
    public List<RecipeIngredientResponse> ingredients;
    public List<TagResponse> tags;
    public List<CategoryResponse> categories;
    public String fullName;
    public User user;
}
