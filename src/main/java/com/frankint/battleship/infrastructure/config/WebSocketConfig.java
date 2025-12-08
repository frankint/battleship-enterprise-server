package com.frankint.battleship.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. Enable a simple memory-based message broker to send messages back to the client
        // Prefixes for messages destined for the client (Server -> Client)
        config.enableSimpleBroker("/topic", "/queue");

        // 2. Prefix for messages bound for methods annotated with @MessageMapping (Client -> Server)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. Register the endpoint where the connection is established
        // Clients will connect to http://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for development (CORS)
                .withSockJS(); // Enable SockJS fallback options (crucial for older browsers/proxies)
    }
}