package com.backend.cookshare.recipe_management.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {
    UUID categoryId;
    String name;
    String slug;
    String description;
    String iconUrl;
    UUID parentId;
    Boolean isActive;
    LocalDateTime createdAt;
}
