package com.backend.cookshare.user.config;

import com.backend.cookshare.authentication.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final SecurityUtil securityUtil;

    // JwtChannelInterceptor.java
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("STOMP CONNECT frame received! Session: {}", accessor.getSessionId());

            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            log.info("Headers: {}", accessor.toNativeHeaderMap());

            if (authHeaders == null || authHeaders.isEmpty()) {
                log.error("Missing Authorization header");
                sendError(accessor, channel, "Missing Authorization header");
                return null;
            }

            String token = authHeaders.get(0);
            if (!token.startsWith("Bearer ")) {
                log.error("Invalid header: {}", token);
                sendError(accessor, channel, "Invalid Authorization format");
                return null;
            }

            try {
                String jwtToken = token.substring(7);
                Jwt jwt = securityUtil.checkValidAccessToken(jwtToken);
                String username = jwt.getSubject();
                String role = jwt.getClaimAsString("role");

                log.info("Authenticated user: {} (Role: {})", username, role);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                accessor.setUser(auth);

            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
                sendError(accessor, channel, "Invalid or expired token");
                return null;
            }
        }

        return message;
    }

    private void sendError(StompHeaderAccessor accessor, MessageChannel channel, String msg) {
        Message<byte[]> error = MessageBuilder
                .withPayload(msg.getBytes())
                .setHeader("message", msg)
                .build();

        channel.send(error);
    }
}