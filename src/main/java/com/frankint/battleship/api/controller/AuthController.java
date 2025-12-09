package com.frankint.battleship.api.controller;

import com.frankint.battleship.infrastructure.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomUserDetailsService authService;

    record AuthRequest(String username, String password) {}
    record GuestResponse(String username, String password) {}

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        authService.register(request.username(), request.password());
        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/guest")
    public ResponseEntity<GuestResponse> createGuest() {
        String username = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        String password = UUID.randomUUID().toString();

        // Register them like a normal user so Spring Security accepts them
        authService.register(username, password);

        return ResponseEntity.ok(new GuestResponse(username, password));
    }
}