package com.policymesh.auth.service;

import com.policymesh.auth.dto.AuthResponse;
import com.policymesh.auth.dto.LoginRequest;
import com.policymesh.auth.dto.RegisterRequest;
import com.policymesh.auth.dto.UserResponse;
import com.policymesh.auth.entity.User;
import com.policymesh.auth.repository.UserRepository;
import com.policymesh.auth.security.JwtService;
import com.policymesh.common.exception.AuthenticationFailedException;
import com.policymesh.common.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, user.getEmail(), user.getRole().name(), jwtService.getExpirationMs());
    }
}
