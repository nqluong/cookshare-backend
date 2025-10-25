package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.entity.Tag;
import com.backend.cookshare.recipe_management.mapper.TagMapper;
import com.backend.cookshare.recipe_management.repository.TagRepository;
import com.backend.cookshare.recipe_management.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public TagResponse create(TagRequest request) {
        if (tagRepository.existsByNameIgnoreCase(request.getName())) {
            throw new CustomException(ErrorCode.TAG_ALREADY_EXISTS);
        }
        Tag tag = tagMapper.toEntity(request);
        tag.setUsageCount(0);
        return tagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    public TagResponse update(UUID tagId, TagRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
        tagMapper.updateEntity(tag, request);
        return tagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    public void delete(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
        tagRepository.delete(tag);
    }

    @Override
    public TagResponse getById(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND));
        return tagMapper.toResponse(tag);
    }

    @Override
    public List<TagResponse> getAll() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toResponse)
                .toList();
    }
}
