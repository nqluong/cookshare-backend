package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.request.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.entity.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagMapperTest {

    private final TagMapper mapper =
            Mappers.getMapper(TagMapper.class);

    // =========================
    // toEntity
    // =========================
    @Test
    void toEntity_fullMapping() {
        TagRequest request = new TagRequest();
        request.setName("Healthy");

        Tag entity = mapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("Healthy", entity.getName());
    }

    @Test
    void toEntity_nullInput() {
        Tag entity = mapper.toEntity(null);
        assertNull(entity);
    }

    // =========================
    // toResponse
    // =========================
    @Test
    void toResponse_fullMapping() {
        Tag tag = new Tag();
        tag.setTagId(UUID.randomUUID());
        tag.setName("Vegan");

        TagResponse response = mapper.toResponse(tag);

        assertNotNull(response);
        assertEquals("Vegan", response.getName());
    }

    @Test
    void toResponse_nullInput() {
        TagResponse response = mapper.toResponse(null);
        assertNull(response);
    }

    // =========================
    // updateEntity
    // =========================
    @Test
    void updateEntity_updateFields() {
        Tag tag = new Tag();
        tag.setTagId(UUID.randomUUID());
        tag.setName("Old");

        TagRequest request = new TagRequest();
        request.setName("New");

        mapper.updateEntity(tag, request);

        assertEquals("New", tag.getName());
    }

    @Test
    void updateEntity_nullRequest() {
        Tag tag = new Tag();
        tag.setTagId(UUID.randomUUID());
        tag.setName("Original");

        mapper.updateEntity(tag, null);

        // Không bị thay đổi
        assertEquals("Original", tag.getName());
    }
}
