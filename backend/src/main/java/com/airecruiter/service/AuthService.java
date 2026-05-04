package com.airecruiter.service;

import com.airecruiter.dto.request.LoginRequest;
import com.airecruiter.dto.request.RegisterRequest;
import com.airecruiter.dto.response.AuthResponse;
import com.airecruiter.entity.User;
import com.airecruiter.repository.UserRepository;
import com.airecruiter.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(req.getRole() != null ? req.getRole() : User.Role.CANDIDATE)
            .active(true)
            .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, Map.of(
            "role", user.getRole().name(),
            "name", user.getName(),
            "userId", user.getId()
        ));
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build())
            .build();
    }
}
