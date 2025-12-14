package com.backend.cookshare.recipe_management.enums;


public enum RecipeStatus {
    PENDING("PENDING", "Chờ phê duyệt"),
    APPROVED("APPROVED", "Đã phê duyệt"),
    REJECTED("REJECTED", "Đã từ chối"),
    BAN("BAN", "Bị cấm");

    private final String code;
    private final String description;

    RecipeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
