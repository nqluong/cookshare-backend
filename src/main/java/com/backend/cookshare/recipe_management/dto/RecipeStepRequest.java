package com.backend.cookshare.recipe_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeStepRequest {

    @NotNull(message = "stepNumber không được để trống")
    Integer stepNumber;

    @NotBlank(message = "instruction không được để trống")
    String instruction;

    String imageUrl;
    String videoUrl;
    Integer estimatedTime;
    String tips;
}
