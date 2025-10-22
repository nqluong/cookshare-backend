package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.response.FacebookTokenResponse;
import com.backend.cookshare.authentication.dto.response.FacebookUserInfo;
import com.backend.cookshare.authentication.dto.response.LoginResponseDTO;
import com.backend.cookshare.authentication.entity.User;

public interface FacebookOAuthService {
    FacebookTokenResponse getAccessToken(String code);

    FacebookUserInfo getUserInfo(String accessToken);

    LoginResponseDTO authenticateFacebookUser(String code);

    User findOrCreateUser(FacebookUserInfo facebookUserInfo);

    String generateUniqueUsername(String email);
}

