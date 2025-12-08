package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.authentication.enums.UserRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportedUserInfo {
    UUID userId;
    String username;
    String email;
    String avatarUrl;
    UserRole role;
    Boolean isActive;
}
