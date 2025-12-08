package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

public interface UsernameProjection {
    UUID getUserId();
    String getUsername();
}
