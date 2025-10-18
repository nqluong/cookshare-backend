package com.backend.cookshare.user.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCollectionRequest {
    @Size(min = 1, max = 255, message = "Tên bộ sưu tập phải từ 1-255 ký tự")
    String name;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    String description;

    Boolean isPublic;

    String coverImage;
}