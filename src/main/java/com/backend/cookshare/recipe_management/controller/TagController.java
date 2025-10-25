package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.recipe_management.dto.TagRequest;
import com.backend.cookshare.recipe_management.dto.response.TagResponse;
import com.backend.cookshare.recipe_management.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<TagResponse> createTag(@RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> updateTag(@PathVariable("id") UUID id, @RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable("id") UUID id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTagById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(tagService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {
        return ResponseEntity.ok(tagService.getAll());
    }
}
