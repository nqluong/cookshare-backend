package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.request.IngredientRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.service.IngredientService;
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
class IngredientControllerTest {

    private MockMvc mockMvc;

    private final IngredientService ingredientService = mock(IngredientService.class);

    @InjectMocks
    private IngredientController ingredientController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID ingredientId = UUID.randomUUID();

    private IngredientRequest createIngredientRequest() {
        IngredientRequest request = new IngredientRequest();
        request.setName("Đường");
        request.setQuantity("200g");
        request.setUnit("gram");
        return request;
    }

    private RecipeIngredientResponse createIngredientResponse() {
        RecipeIngredientResponse response = new RecipeIngredientResponse();
        response.setIngredientId(ingredientId);
        response.setName("Đường");
        response.setQuantity("200g");
        response.setUnit("gram");
        return response;
    }

    private List<RecipeIngredientResponse> createIngredientList() {
        return Collections.singletonList(createIngredientResponse());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ingredientController).build();
    }

    @Test
    void createIngredient_Success() throws Exception {
        IngredientRequest request = createIngredientRequest();
        RecipeIngredientResponse response = createIngredientResponse();

        when(ingredientService.createIngredient(any(IngredientRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientId").value(ingredientId.toString()))
                .andExpect(jsonPath("$.name").value("Đường"))
                .andExpect(jsonPath("$.quantity").value("200g"))
                .andExpect(jsonPath("$.unit").value("gram"));

        verify(ingredientService).createIngredient(any(IngredientRequest.class));
    }

    @Test
    void getIngredientById_Success() throws Exception {
        RecipeIngredientResponse response = createIngredientResponse();
        when(ingredientService.getIngredientById(ingredientId)).thenReturn(response);

        mockMvc.perform(get("/api/ingredients/{id}", ingredientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientId").value(ingredientId.toString()))
                .andExpect(jsonPath("$.name").value("Đường"))
                .andExpect(jsonPath("$.quantity").value("200g"))
                .andExpect(jsonPath("$.unit").value("gram"));

        verify(ingredientService).getIngredientById(ingredientId);
    }

    @Test
    void updateIngredient_Success() throws Exception {
        IngredientRequest request = createIngredientRequest();
        RecipeIngredientResponse response = createIngredientResponse();

        when(ingredientService.updateIngredient(eq(ingredientId), any(IngredientRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/ingredients/{id}", ingredientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientId").value(ingredientId.toString()))
                .andExpect(jsonPath("$.name").value("Đường"))
                .andExpect(jsonPath("$.quantity").value("200g"))
                .andExpect(jsonPath("$.unit").value("gram"));

        verify(ingredientService).updateIngredient(eq(ingredientId), any(IngredientRequest.class));
    }

    @Test
    void deleteIngredient_Success() throws Exception {
        doNothing().when(ingredientService).deleteIngredient(ingredientId);

        mockMvc.perform(delete("/api/ingredients/{id}", ingredientId))
                .andExpect(status().isNoContent());

        verify(ingredientService).deleteIngredient(ingredientId);
    }

    @Test
    void getAllIngredients_Success() throws Exception {
        List<RecipeIngredientResponse> responseList = createIngredientList();
        when(ingredientService.getAllIngredients()).thenReturn(responseList);

        mockMvc.perform(get("/api/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingredientId").value(ingredientId.toString()))
                .andExpect(jsonPath("$[0].name").value("Đường"))
                .andExpect(jsonPath("$[0].quantity").value("200g"))
                .andExpect(jsonPath("$[0].unit").value("gram"));

        verify(ingredientService).getAllIngredients();
    }
}
