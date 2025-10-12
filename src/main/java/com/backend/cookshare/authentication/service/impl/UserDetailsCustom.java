package com.backend.cookshare.authentication.service.impl;

import com.backend.cookshare.authentication.service.UserService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService {
    private final UserService userService;

    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.backend.cookshare.authentication.entity.User user = this.userService.getUserByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.emptyList()
        );
    }
}
