package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.recipe_management.dto.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;

import java.util.List;
import java.util.UUID;

public interface TagService {
    TagResponse create(TagRequest request);
    TagResponse update(UUID tagId, TagRequest request);
    void delete(UUID tagId);
    TagResponse getById(UUID tagId);
    List<TagResponse> getAll();
}
