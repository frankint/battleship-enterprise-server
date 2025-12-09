package com.frankint.battleship.infrastructure.security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simple REST API testing
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/ws/**").permitAll() // Allow Login/Register & WebSocket handshake
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css").permitAll() // Allow Frontend
                        .anyRequest().authenticated() // Block everything else
                )
                .httpBasic(basic -> {}); // Use HTTP Basic Auth (Simple header: Authorization: Basic Base64(user:pass))

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // The industry standard for hashing
    }
}