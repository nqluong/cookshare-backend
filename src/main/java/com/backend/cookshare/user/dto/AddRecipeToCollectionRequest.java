package com.backend.cookshare.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddRecipeToCollectionRequest {
    @NotNull(message = "Recipe ID không được để trống")
    UUID recipeId;
}