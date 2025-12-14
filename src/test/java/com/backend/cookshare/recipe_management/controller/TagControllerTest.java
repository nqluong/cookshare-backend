package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.request.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    private MockMvc mockMvc;

    private final TagService tagService = mock(TagService.class);

    @InjectMocks
    private TagController tagController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID tagId = UUID.randomUUID();

    private TagRequest createTagRequest() {
        TagRequest request = new TagRequest();
        request.setName("Ngon rẻ");
        request.setSlug("ngon-re");
        return request;
    }

    private TagResponse createTagResponse() {
        TagResponse response = new TagResponse();
        response.setTagId(tagId);
        response.setName("Ngon rẻ");
        response.setSlug("ngon-re");
        return response;
    }

    private List<TagResponse> createTagList() {
        return Collections.singletonList(createTagResponse());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tagController).build();
    }

    @Test
    void createTag_Success() throws Exception {
        TagRequest request = createTagRequest();
        TagResponse response = createTagResponse();

        when(tagService.create(any(TagRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value("Ngon rẻ"))
                .andExpect(jsonPath("$.slug").value("ngon-re"));

        verify(tagService).create(any(TagRequest.class));
    }

    @Test
    void updateTag_Success() throws Exception {
        TagRequest request = createTagRequest();
        TagResponse response = createTagResponse();

        when(tagService.update(eq(tagId), any(TagRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tags/{id}", tagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value("Ngon rẻ"))
                .andExpect(jsonPath("$.slug").value("ngon-re"));

        verify(tagService).update(eq(tagId), any(TagRequest.class));
    }

    @Test
    void deleteTag_Success() throws Exception {
        doNothing().when(tagService).delete(tagId);

        mockMvc.perform(delete("/api/tags/{id}", tagId))
                .andExpect(status().isNoContent());

        verify(tagService).delete(tagId);
    }

    @Test
    void getTagById_Success() throws Exception {
        TagResponse response = createTagResponse();

        when(tagService.getById(tagId)).thenReturn(response);

        mockMvc.perform(get("/api/tags/{id}", tagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tagId").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value("Ngon rẻ"))
                .andExpect(jsonPath("$.slug").value("ngon-re"));

        verify(tagService).getById(tagId);
    }

    @Test
    void getAllTags_Success() throws Exception {
        List<TagResponse> responseList = createTagList();
        when(tagService.getAll()).thenReturn(responseList);

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tagId").value(tagId.toString()))
                .andExpect(jsonPath("$[0].name").value("Ngon rẻ"))
                .andExpect(jsonPath("$[0].slug").value("ngon-re"));

        verify(tagService).getAll();
    }
}
