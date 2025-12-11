package com.backend.cookshare.system.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReporterInfo {
    UUID userId;
    String username;
    String avatarUrl;
    String fullName;
}
