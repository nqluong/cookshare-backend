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
public class TagResponse {
    UUID tagId;
    String name;
    String slug;
    String color;
    Integer usageCount;
    Boolean isTrending;
    LocalDateTime createdAt;
}
