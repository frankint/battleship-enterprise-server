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
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/ws/**").permitAll()
                        .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, authException) -> {
                            // Send 401 directly without the 'WWW-Authenticate' header
                            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                        }
                ));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // The industry standard for hashing
    }
}