package com.backend.cookshare.user.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CollectionUserDto {
    UUID collectionId;
    UUID userId;
    String name;
    String description;
    Boolean isPublic;
    String coverImage;
    Integer recipeCount;
    Integer viewCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}