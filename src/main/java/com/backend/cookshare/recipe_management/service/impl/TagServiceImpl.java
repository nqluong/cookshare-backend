package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.entity.Tag;
import com.backend.cookshare.recipe_management.mapper.TagMapper;
import com.backend.cookshare.recipe_management.repository.TagRepository;
import com.backend.cookshare.recipe_management.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public TagResponse create(TagRequest request) {
        // Kiểm tra trùng tên
        if (tagRepository.existsByNameIgnoreCase(request.getName())) {
            throw new CustomException(ErrorCode.TAG_ALREADY_EXISTS, "Thẻ đã tồn tại");
        }

        Tag tag = tagMapper.toEntity(request);
        tag.setSlug(generateSlug(request.getName()));
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUsageCount(0);

        return tagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public TagResponse update(UUID tagId, TagRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND, "Không tìm thấy thẻ"));

        tagMapper.updateEntity(tag, request);
        tag.setSlug(generateSlug(tag.getName()));

        return tagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public void delete(UUID tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new CustomException(ErrorCode.TAG_NOT_FOUND, "Không tìm thấy thẻ để xóa");
        }
        tagRepository.deleteById(tagId);
    }

    @Override
    public TagResponse getById(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.TAG_NOT_FOUND, "Không tìm thấy thẻ"));
        return tagMapper.toResponse(tag);
    }

    @Override
    public List<TagResponse> getAll() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
