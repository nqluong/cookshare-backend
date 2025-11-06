package com.backend.cookshare.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ✅ THÊM logging
        log.info("🔧 Configuring message broker...");

        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

        log.info("✅ Message broker configured: /topic, /queue, /app, /user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",      // Expo dev
                        "exp://*",                 // Expo Go
                        "http://192.168.*.*",      // LAN
                        "http://10.*.*.*"          // iOS simulator / Android emulator
                )
                .withSockJS();

        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "exp://*",
                        "http://192.168.*.*",
                        "http://10.*.*.*"
                )
                .withSockJS();
    }
}