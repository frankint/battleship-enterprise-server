package com.frankint.battleship.infrastructure.security;

import com.frankint.battleship.infrastructure.persistence.entity.UserEntity;
import com.frankint.battleship.infrastructure.persistence.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. Used by Spring Security to verify login
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return new User(entity.getUsername(), entity.getPassword(), Collections.emptyList());
    }

    // 2. Used by our Controller to register new users
    public void register(String username, String rawPassword) {
        if (userRepository.existsById(username)) {
            throw new IllegalArgumentException("Username taken");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}