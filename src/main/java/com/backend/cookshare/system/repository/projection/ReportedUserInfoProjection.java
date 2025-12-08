package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

public interface ReportedUserInfoProjection {
    UUID getUserId();
    String getUsername();
    String getEmail();
    String getAvatarUrl();
    String getRole();
    Boolean getIsActive();
}
