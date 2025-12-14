package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.request.CategoryRequest;
import com.backend.cookshare.recipe_management.dto.response.CategoryResponse;
import com.backend.cookshare.recipe_management.service.CategoryService;
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
class CategoryControllerTest {

    private MockMvc mockMvc;

    private final CategoryService categoryService = mock(CategoryService.class);

    @InjectMocks
    private CategoryController categoryController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID categoryId = UUID.randomUUID();

    private CategoryRequest createCategoryRequest() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Món Tráng Miệng");
        request.setDescription("Các món ngọt sau bữa ăn");
        return request;
    }

    private CategoryResponse createCategoryResponse() {
        CategoryResponse response = new CategoryResponse();
        response.setCategoryId(categoryId);
        response.setName("Món Tráng Miệng");
        response.setDescription("Các món ngọt sau bữa ăn");
        return response;
    }

    private List<CategoryResponse> createCategoryList() {
        return Collections.singletonList(createCategoryResponse());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test
    void createCategory_Success() throws Exception {
        CategoryRequest request = createCategoryRequest();
        CategoryResponse response = createCategoryResponse();

        when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Món Tráng Miệng"))
                .andExpect(jsonPath("$.description").value("Các món ngọt sau bữa ăn"));

        verify(categoryService).create(any(CategoryRequest.class));
    }

    @Test
    void updateCategory_Success() throws Exception {
        CategoryRequest request = createCategoryRequest();
        CategoryResponse response = createCategoryResponse();

        when(categoryService.update(eq(categoryId), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Món Tráng Miệng"))
                .andExpect(jsonPath("$.description").value("Các món ngọt sau bữa ăn"));

        verify(categoryService).update(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    void deleteCategory_Success() throws Exception {
        doNothing().when(categoryService).delete(categoryId);

        mockMvc.perform(delete("/api/categories/{categoryId}", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(categoryId);
    }

    @Test
    void getCategoryById_Success() throws Exception {
        CategoryResponse response = createCategoryResponse();

        when(categoryService.getById(categoryId)).thenReturn(response);

        mockMvc.perform(get("/api/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value("Món Tráng Miệng"))
                .andExpect(jsonPath("$.description").value("Các món ngọt sau bữa ăn"));

        verify(categoryService).getById(categoryId);
    }

    @Test
    void getAllCategories_Success() throws Exception {
        List<CategoryResponse> responseList = createCategoryList();

        when(categoryService.getAll()).thenReturn(responseList);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$[0].name").value("Món Tráng Miệng"))
                .andExpect(jsonPath("$[0].description").value("Các món ngọt sau bữa ăn"));

        verify(categoryService).getAll();
    }
}
