package com.backend.cookshare.recipe_management.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeStepResponse {
    Integer stepNumber;
    String instruction;
    String imageUrl;
    String videoUrl;
    Integer estimatedTime;
    String tips;
}
