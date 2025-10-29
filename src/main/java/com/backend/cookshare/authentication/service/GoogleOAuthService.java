package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.response.GoogleTokenResponse;
import com.backend.cookshare.authentication.dto.response.GoogleUserInfo;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;

public interface GoogleOAuthService {
    GoogleTokenResponse getAccessToken(String code);

    GoogleUserInfo getUserInfo(String accessToken);

    LoginResponseDTO authenticateGoogleUser(String code);

    User findOrCreateUser(GoogleUserInfo googleUserInfo);

    String generateUniqueUsername(String email);
}
