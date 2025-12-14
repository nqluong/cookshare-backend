package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.entity.Tag;
import com.backend.cookshare.recipe_management.mapper.TagMapper;
import com.backend.cookshare.recipe_management.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagServiceImpl Tests")
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    private final UUID existingId = UUID.randomUUID();

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Tạo tag thành công")
        void create_success() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Món Việt");
            request.setColor("#FF0000");
            request.setIsTrending(true);

            Tag entity = new Tag();
            Tag savedEntity = new Tag();
            savedEntity.setTagId(existingId);
            savedEntity.setName("Món Việt");
            savedEntity.setSlug("mon-viet");
            savedEntity.setColor("#FF0000");
            savedEntity.setIsTrending(true);
            savedEntity.setUsageCount(0);
            savedEntity.setCreatedAt(LocalDateTime.now());

            TagResponse expectedResponse = TagResponse.builder()
                    .tagId(existingId)
                    .name("Món Việt")
                    .slug("mon-viet")
                    .color("#FF0000")
                    .usageCount(0)
                    .isTrending(true)
                    .createdAt(savedEntity.getCreatedAt())
                    .build();

            when(tagRepository.existsByNameIgnoreCase("Món Việt")).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedEntity);
            when(tagMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

            // When
            TagResponse response = tagService.create(request);

            // Then
            assertThat(response)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(expectedResponse);

            verify(tagRepository).existsByNameIgnoreCase("Món Việt");
            verify(tagMapper).toEntity(request);

            // Capture để kiểm tra slug và usageCount
            ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(tagCaptor.capture());
            Tag capturedTag = tagCaptor.getValue();

            assertThat(capturedTag.getSlug()).isNotNull();
            assertThat(capturedTag.getUsageCount()).isEqualTo(0);
            assertThat(capturedTag.getCreatedAt()).isNotNull();

            verify(tagMapper).toResponse(savedEntity);
        }

        @Test
        @DisplayName("Tạo tag thất bại - tên đã tồn tại (không phân biệt hoa thường)")
        void create_tagAlreadyExists() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("mÓN việt");

            when(tagRepository.existsByNameIgnoreCase("mÓN việt")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> tagService.create(request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException cex = (CustomException) ex;
                        assertThat(cex.getErrorCode()).isEqualTo(ErrorCode.TAG_ALREADY_EXISTS);
                        assertThat(cex.getMessage()).isEqualTo("Thẻ đã tồn tại");
                    });

            verify(tagRepository).existsByNameIgnoreCase("mÓN việt");
            verify(tagRepository, never()).save(any());
            verifyNoInteractions(tagMapper);
        }

        @Test
        @DisplayName("Tạo tag với tên có ký tự đặc biệt - slug được tạo đúng")
        void create_withSpecialCharacters() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Low-Carb & Sugar-Free!");
            request.setColor("#00FF00");

            Tag entity = new Tag();
            Tag savedEntity = new Tag();
            savedEntity.setSlug("low-carb-sugar-free");

            TagResponse response = TagResponse.builder()
                    .slug("low-carb-sugar-free")
                    .build();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedEntity);
            when(tagMapper.toResponse(savedEntity)).thenReturn(response);

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).matches("[a-z0-9-]+");
            assertThat(slug).doesNotContain("&", "!", " ");
        }

        @Test
        @DisplayName("Tạo tag với tên viết hoa - slug được lowercase")
        void create_uppercaseName() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("VEGAN RECIPES");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).isEqualTo(slug.toLowerCase());
        }

        @Test
        @DisplayName("Tạo tag với nhiều khoảng trắng - slug collapse spaces")
        void create_multipleSpaces() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Quick    Easy    Recipes");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).doesNotContain("  ");
        }

        @Test
        @DisplayName("Tạo tag - xóa dấu gạch ngang đầu cuối")
        void create_trimHyphens() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("---Budget Friendly+++");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).doesNotStartWith("-").doesNotEndWith("-");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("Cập nhật tag thành công - slug tự động thay đổi khi name thay đổi")
        void update_success() {
            // Given
            Tag existingTag = new Tag();
            existingTag.setTagId(existingId);
            existingTag.setName("Old Name");
            existingTag.setSlug("old-name");

            TagRequest request = new TagRequest();
            request.setName("New Name Updated");
            request.setColor("#00FF00");
            request.setIsTrending(false);

            TagResponse expectedResponse = TagResponse.builder()
                    .tagId(existingId)
                    .name("New Name Updated")
                    .slug("new-name-updated")
                    .color("#00FF00")
                    .isTrending(false)
                    .build();

            when(tagRepository.findById(existingId)).thenReturn(Optional.of(existingTag));
            doAnswer(invocation -> {
                existingTag.setName("New Name Updated");
                return null;
            }).when(tagMapper).updateEntity(existingTag, request);
            when(tagRepository.save(any(Tag.class))).thenReturn(existingTag);
            when(tagMapper.toResponse(any(Tag.class))).thenReturn(expectedResponse);

            // When
            TagResponse response = tagService.update(existingId, request);

            // Then
            assertThat(response).isEqualTo(expectedResponse);

            verify(tagRepository).findById(existingId);
            verify(tagMapper).updateEntity(existingTag, request);

            // Capture để kiểm tra slug được regenerate
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            Tag savedTag = captor.getValue();
            assertThat(savedTag.getSlug()).isNotNull();

            verify(tagMapper).toResponse(any(Tag.class));
        }

        @Test
        @DisplayName("Cập nhật thất bại - tag không tồn tại")
        void update_tagNotFound() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Anything");

            when(tagRepository.findById(existingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.update(existingId, request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException cex = (CustomException) ex;
                        assertThat(cex.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_FOUND);
                        assertThat(cex.getMessage()).contains("Không tìm thấy thẻ");
                    });

            verify(tagRepository).findById(existingId);
            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cập nhật tag - preserve usage count")
        void update_preserveUsageCount() {
            // Given
            Tag existingTag = new Tag();
            existingTag.setTagId(existingId);
            existingTag.setName("Old Name");
            existingTag.setUsageCount(10);

            TagRequest request = new TagRequest();
            request.setName("Updated Name");

            when(tagRepository.findById(existingId)).thenReturn(Optional.of(existingTag));
            doAnswer(invocation -> {
                existingTag.setName("Updated Name");
                return null;
            }).when(tagMapper).updateEntity(existingTag, request);
            when(tagRepository.save(any(Tag.class))).thenReturn(existingTag);
            when(tagMapper.toResponse(any(Tag.class))).thenReturn(TagResponse.builder().build());

            // When
            tagService.update(existingId, request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            assertThat(captor.getValue().getUsageCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("Cập nhật tag với ký tự đặc biệt - regenerate slug")
        void update_withSpecialCharacters() {
            // Given
            Tag existingTag = new Tag();
            existingTag.setTagId(existingId);
            existingTag.setName("Old Name");

            TagRequest request = new TagRequest();
            request.setName("Kid-Friendly & Fun!");

            when(tagRepository.findById(existingId)).thenReturn(Optional.of(existingTag));
            doAnswer(invocation -> {
                existingTag.setName("Kid-Friendly & Fun!");
                return null;
            }).when(tagMapper).updateEntity(existingTag, request);
            when(tagRepository.save(any(Tag.class))).thenReturn(existingTag);
            when(tagMapper.toResponse(any(Tag.class))).thenReturn(TagResponse.builder().build());

            // When
            tagService.update(existingId, request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).doesNotContain("&", "!");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Xóa tag thành công")
        void delete_success() {
            // Given
            when(tagRepository.existsById(existingId)).thenReturn(true);

            // When
            tagService.delete(existingId);

            // Then
            verify(tagRepository).existsById(existingId);
            verify(tagRepository).deleteById(existingId);
        }

        @Test
        @DisplayName("Xóa thất bại - tag không tồn tại")
        void delete_notFound() {
            // Given
            when(tagRepository.existsById(existingId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> tagService.delete(existingId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException cex = (CustomException) ex;
                        assertThat(cex.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_FOUND);
                        assertThat(cex.getMessage()).contains("Không tìm thấy thẻ để xóa");
                    });

            verify(tagRepository).existsById(existingId);
            verify(tagRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Xóa tag - verify ID đúng")
        void delete_correctId() {
            // Given
            UUID specificId = UUID.randomUUID();
            when(tagRepository.existsById(specificId)).thenReturn(true);

            // When
            tagService.delete(specificId);

            // Then
            verify(tagRepository).deleteById(specificId);
        }
    }

    @Nested
    @DisplayName("getById() và getAll()")
    class Get {

        @Test
        @DisplayName("Lấy tag theo ID thành công")
        void getById_success() {
            // Given
            Tag tag = new Tag();
            tag.setTagId(existingId);
            tag.setName("Test Tag");

            TagResponse responseDto = TagResponse.builder()
                    .tagId(existingId)
                    .name("Test Tag")
                    .build();

            when(tagRepository.findById(existingId)).thenReturn(Optional.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(responseDto);

            // When
            TagResponse result = tagService.getById(existingId);

            // Then
            assertThat(result).isEqualTo(responseDto);
            verify(tagRepository).findById(existingId);
            verify(tagMapper).toResponse(tag);
        }

        @Test
        @DisplayName("Lấy tag theo ID thất bại - không tìm thấy")
        void getById_notFound() {
            // Given
            when(tagRepository.findById(existingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tagService.getById(existingId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> {
                        CustomException cex = (CustomException) ex;
                        assertThat(cex.getErrorCode()).isEqualTo(ErrorCode.TAG_NOT_FOUND);
                        assertThat(cex.getMessage()).contains("Không tìm thấy thẻ");
                    });

            verify(tagRepository).findById(existingId);
            verifyNoInteractions(tagMapper);
        }

        @Test
        @DisplayName("Lấy tag với usage count")
        void getById_withUsageCount() {
            // Given
            Tag tag = new Tag();
            tag.setTagId(existingId);
            tag.setName("Popular Tag");
            tag.setUsageCount(25);

            TagResponse responseDto = TagResponse.builder()
                    .tagId(existingId)
                    .name("Popular Tag")
                    .usageCount(25)
                    .build();

            when(tagRepository.findById(existingId)).thenReturn(Optional.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(responseDto);

            // When
            TagResponse result = tagService.getById(existingId);

            // Then
            assertThat(result.getUsageCount()).isEqualTo(25);
        }

        @Test
        @DisplayName("Lấy tất cả tags")
        void getAll() {
            // Given
            Tag tag1 = new Tag();
            tag1.setTagId(UUID.randomUUID());
            tag1.setName("Tag1");

            Tag tag2 = new Tag();
            tag2.setTagId(UUID.randomUUID());
            tag2.setName("Tag2");

            TagResponse resp1 = TagResponse.builder().name("Tag1").build();
            TagResponse resp2 = TagResponse.builder().name("Tag2").build();

            when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));
            when(tagMapper.toResponse(tag1)).thenReturn(resp1);
            when(tagMapper.toResponse(tag2)).thenReturn(resp2);

            // When
            List<TagResponse> result = tagService.getAll();

            // Then
            assertThat(result).containsExactly(resp1, resp2);
            verify(tagRepository).findAll();
            verify(tagMapper, times(2)).toResponse(any(Tag.class));
        }

        @Test
        @DisplayName("Lấy tất cả tags - empty list")
        void getAll_empty() {
            // Given
            when(tagRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<TagResponse> result = tagService.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(tagRepository).findAll();
            verifyNoInteractions(tagMapper);
        }

        @Test
        @DisplayName("Lấy tất cả tags - single item")
        void getAll_singleItem() {
            // Given
            Tag tag = new Tag();
            tag.setName("Single Tag");

            TagResponse response = TagResponse.builder().name("Single Tag").build();

            when(tagRepository.findAll()).thenReturn(List.of(tag));
            when(tagMapper.toResponse(tag)).thenReturn(response);

            // When
            List<TagResponse> result = tagService.getAll();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Single Tag");
        }

        @Test
        @DisplayName("Lấy tất cả tags - preserve order")
        void getAll_preserveOrder() {
            // Given
            Tag tag1 = new Tag();
            tag1.setName("First");
            Tag tag2 = new Tag();
            tag2.setName("Second");
            Tag tag3 = new Tag();
            tag3.setName("Third");

            TagResponse resp1 = TagResponse.builder().name("First").build();
            TagResponse resp2 = TagResponse.builder().name("Second").build();
            TagResponse resp3 = TagResponse.builder().name("Third").build();

            when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2, tag3));
            when(tagMapper.toResponse(tag1)).thenReturn(resp1);
            when(tagMapper.toResponse(tag2)).thenReturn(resp2);
            when(tagMapper.toResponse(tag3)).thenReturn(resp3);

            // When
            List<TagResponse> result = tagService.getAll();

            // Then
            assertThat(result.get(0).getName()).isEqualTo("First");
            assertThat(result.get(1).getName()).isEqualTo("Second");
            assertThat(result.get(2).getName()).isEqualTo("Third");
        }
    }

    @Nested
    @DisplayName("Slug Generation Edge Cases")
    class SlugEdgeCases {

        @Test
        @DisplayName("Slug với số")
        void slugWithNumbers() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Under 30 Minutes");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).contains("30");
            assertThat(slug).matches("[a-z0-9-]+");
        }

        @Test
        @DisplayName("Slug với dấu chấm và phẩy")
        void slugWithDotsAndCommas() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Dinner, Lunch. Breakfast");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).doesNotContain(",", ".");
        }

        @Test
        @DisplayName("Slug chỉ có ký tự đặc biệt")
        void slugOnlySpecialChars() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("!@#$%^&*()");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            // Should be empty or only valid characters
            assertThat(slug).matches("[a-z0-9-]*");
        }

        @Test
        @DisplayName("Slug với dấu ngoặc")
        void slugWithBrackets() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Recipe (Quick) [Easy]");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).doesNotContain("(", ")", "[", "]");
        }

        @Test
        @DisplayName("Slug với underscore")
        void slugWithUnderscore() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Meal_Prep_Sunday");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).matches("[a-z0-9-]+");
        }

        @Test
        @DisplayName("Slug với dấu nháy")
        void slugWithQuotes() {
            // Given
            TagRequest request = new TagRequest();
            request.setName("Chef's Special \"Best\" Recipe");

            Tag entity = new Tag();

            when(tagRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(tagMapper.toEntity(request)).thenReturn(entity);
            when(tagRepository.save(any(Tag.class))).thenReturn(entity);
            when(tagMapper.toResponse(entity)).thenReturn(TagResponse.builder().build());

            // When
            tagService.create(request);

            // Then
            ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
            verify(tagRepository).save(captor.capture());

            String slug = captor.getValue().getSlug();
            assertThat(slug).doesNotContain("'", "\"");
        }
    }
}