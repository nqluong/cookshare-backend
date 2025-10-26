package com.backend.cookshare.recipe_management.dto.request;

import lombok.Data;

@Data
public class TagRequest {
    private String name;
    private String slug;
    private String color;
    private Boolean isTrending;
}
