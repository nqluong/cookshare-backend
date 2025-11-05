package com.backend.cookshare.authentication.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUploadUrlResponse {

    private String uploadUrl; // Signed URL để upload lên Firebase
    private String publicUrl; // URL công khai của file đã upload
}
