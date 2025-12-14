package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    RecipeService recipeService;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    RecipeController recipeController;

    MockMvc mockMvcMultipart;
    MockMvc mockMvcJson;
    @BeforeEach
    void setUpMultipart() {
        mockMvcMultipart = MockMvcBuilders
                .standaloneSetup(recipeController)
                .build();
    }
    @BeforeEach
    void setUpJson() {
        mockMvcJson = MockMvcBuilders
                .standaloneSetup(recipeController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }


    // ================= CREATE =================

    @Test
    void createRecipe_success() throws Exception {
        RecipeRequest request = new RecipeRequest();
        RecipeResponse response = RecipeResponse.builder().build();

        when(objectMapper.readValue(anyString(), eq(RecipeRequest.class)))
                .thenReturn(request);

        when(recipeService.createRecipeWithFiles(any(), any(), any()))
                .thenReturn(response);

        MockMultipartFile data = new MockMultipartFile(
                "data", "", "application/json",
                "{}".getBytes() // nội dung không quan trọng vì đã mock ObjectMapper
        );

        MockMultipartFile image = new MockMultipartFile(
                "image", "img.png", "image/png", "img".getBytes()
        );

        mockMvcMultipart.perform(multipart("/api/recipes")
                        .file(data)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        verify(objectMapper).readValue(anyString(), eq(RecipeRequest.class));
        verify(recipeService).createRecipeWithFiles(any(), any(), any());
    }

    // ================= UPDATE =================

    @Test
    void updateRecipe_success() throws Exception {
        UUID id = UUID.randomUUID();

        RecipeRequest request = new RecipeRequest();
        RecipeResponse response = RecipeResponse.builder().build();

        // mock ObjectMapper
        when(objectMapper.readValue(anyString(), eq(RecipeRequest.class)))
                .thenReturn(request);

        // mock service
        when(recipeService.updateRecipe(eq(id), any(), any(), any()))
                .thenReturn(response);

        MockMultipartFile data = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{}".getBytes() // ⚠️ BẮT BUỘC: dữ liệu thật
        );

        mockMvcMultipart.perform(
                        multipart("/api/recipes/{id}", id)
                                .file(data)
                                .with(req -> {
                                    req.setMethod("PUT"); // ⚠️ multipart mặc định POST
                                    return req;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andExpect(status().isOk());

        verify(objectMapper).readValue(anyString(), eq(RecipeRequest.class));
        verify(recipeService).updateRecipe(eq(id), any(), any(), any());
    }


    // ================= GET =================

    @Test
    void getRecipeById_success() throws Exception {
        UUID id = UUID.randomUUID();

        when(recipeService.getRecipeById(id))
                .thenReturn(RecipeResponse.builder().build());

        mockMvcMultipart.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk());

        verify(recipeService).getRecipeById(id);
    }

//    @Test
//    void getAllRecipes_success() throws Exception {
//        RecipeResponse response = RecipeResponse.builder()
//                .recipeId(UUID.randomUUID())
//                .title("Test Recipe")
//                .slug("test-recipe")
//                .description("Test description")
//                .ingredients(new ArrayList<>())
//                .steps(new ArrayList<>())
//                .tags(new ArrayList<>())
//                .categories(new ArrayList<>())
//                .build();
//
//        // Create a mutable list for the Page content
//        List<RecipeResponse> content = new ArrayList<>();
//        content.add(response);
//
//        Page<RecipeResponse> page = new PageImpl<>(content);
//
//        when(recipeService.getAllRecipes(any()))
//                .thenReturn(page);
//
//        mockMvcJson.perform(get("/api/recipes")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andExpect(status().isOk());
//
//        verify(recipeService).getAllRecipes(any());
//    }


    @Test
    void getAllRecipesByUser_success() throws Exception {
        UUID userId = UUID.randomUUID();

        when(recipeService.getAllRecipesByUserId(userId))
                .thenReturn(List.of(RecipeResponse.builder().build()));

        mockMvcMultipart.perform(get("/api/recipes/user/{userId}", userId))
                .andExpect(status().isOk());

        verify(recipeService).getAllRecipesByUserId(userId);
    }

    // ================= DELETE =================

    @Test
    void deleteRecipe_success() throws Exception {
        UUID id = UUID.randomUUID();

        doNothing().when(recipeService).deleteRecipe(id);

        mockMvcMultipart.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent());

        verify(recipeService).deleteRecipe(id);
    }
}
