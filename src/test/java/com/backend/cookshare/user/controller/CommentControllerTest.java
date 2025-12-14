package com.backend.cookshare.user.controller;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.user.dto.CommentRequest;
import com.backend.cookshare.user.dto.CommentResponse;
import com.backend.cookshare.user.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentController commentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final UUID commentId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    private User createUser() {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("nguyenduy");
        return user;
    }

    private CommentRequest createCommentRequest() {
        CommentRequest request = new CommentRequest();
        request.setContent("Nội dung comment");
        return request;
    }

    private CommentResponse createCommentResponse() {
        CommentResponse response = new CommentResponse();
        response.setCommentId(commentId);
        response.setContent("Nội dung comment");
        response.setUserId(userId);
        response.setReplies(Collections.emptyList());
        return response;
    }

    private Page<CommentResponse> createCommentPage() {
        return new PageImpl<>(Collections.singletonList(createCommentResponse()), PageRequest.of(0, 10), 1);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
    }

    @Test
    void getRecipeComments_Success() throws Exception {
        Page<CommentResponse> page = createCommentPage();
        when(commentService.getRecipeComments(eq(recipeId), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/comments/recipe/{recipeId}", recipeId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(commentId.toString()))
                .andExpect(jsonPath("$.content[0].content").value("Nội dung comment"));

        verify(commentService).getRecipeComments(eq(recipeId), anyInt(), anyInt());
    }

    @Test
    void getCommentReplies_Success() throws Exception {
        List<CommentResponse> replies = Collections.singletonList(createCommentResponse());
        when(commentService.getCommentReplies(commentId)).thenReturn(replies);

        mockMvc.perform(get("/comments/{commentId}/replies", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(commentId.toString()));

        verify(commentService).getCommentReplies(commentId);
    }

    @Test
    void createComment_Success() throws Exception {
        CommentRequest request = createCommentRequest();
        CommentResponse response = createCommentResponse();
        User user = createUser();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(commentService.createComment(any(CommentRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value("Nội dung comment"));

        verify(commentService).createComment(any(CommentRequest.class), eq(userId));
    }

    @Test
    void updateComment_Success() throws Exception {
        CommentRequest request = createCommentRequest();
        CommentResponse response = createCommentResponse();
        User user = createUser();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(commentService.updateComment(eq(commentId), any(CommentRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(put("/comments/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value("Nội dung comment"));

        verify(commentService).updateComment(eq(commentId), any(CommentRequest.class), eq(userId));
    }

    @Test
    void deleteComment_Success() throws Exception {
        User user = createUser();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        doNothing().when(commentService).deleteComment(commentId, userId);

        mockMvc.perform(delete("/comments/{commentId}", commentId)
                        .principal(auth))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(commentId, userId);
    }
}
