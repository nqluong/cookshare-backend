package com.backend.cookshare.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCollectionRequest {
    @NotBlank(message = "Tên bộ sưu tập không được để trống")
    @Size(min = 1, max = 255, message = "Tên bộ sưu tập phải từ 1-255 ký tự")
    String name;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    String description;

    @Builder.Default
    Boolean isPublic = true;

    String coverImage;
}