package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.request.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TagMapper {
    Tag toEntity(TagRequest request);
    TagResponse toResponse(Tag tag);
    void updateEntity(@MappingTarget Tag tag, TagRequest request);
}
