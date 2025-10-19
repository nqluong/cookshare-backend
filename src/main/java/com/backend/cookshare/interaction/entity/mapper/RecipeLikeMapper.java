package com.backend.cookshare.interaction.entity.mapper;

import com.backend.cookshare.interaction.entity.RecipeLike;
import com.backend.cookshare.interaction.entity.dto.response.RecipeLikeResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecipeLikeMapper {
    RecipeLikeResponse toRecipeLikeResponse(RecipeLike recipeLike);
}
