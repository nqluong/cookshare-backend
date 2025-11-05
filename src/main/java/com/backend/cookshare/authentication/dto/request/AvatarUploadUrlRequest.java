package com.backend.cookshare.authentication.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadUrlRequest {

    @NotBlank(message = "Tên file không được để trống")
    private String fileName;

    @NotBlank(message = "Content type không được để trống")
    private String contentType;
}
