package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;
import com.backend.cookshare.recipe_management.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    Category toEntity(CategoryRequest request);

    CategoryResponse toResponse(Category category);

    void updateEntity(@MappingTarget Category category, CategoryRequest request);
}
