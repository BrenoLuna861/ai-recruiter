package com.airecruiter.service;

import com.airecruiter.dto.request.GoogleAuthRequest;
import com.airecruiter.dto.response.AuthResponse;
import com.airecruiter.entity.User;
import com.airecruiter.repository.UserRepository;
import com.airecruiter.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        if (googleClientId == null || googleClientId.isBlank()) {
            log.warn("google.oauth.client-id nao configurado. Login com Google ficara desabilitado.");
            return;
        }
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        log.info("Google ID token verifier inicializado.");
    }

    @Transactional
    public AuthResponse authenticate(GoogleAuthRequest req) {
        if (verifier == null) {
            throw new IllegalStateException("Login com Google nao configurado neste servidor.");
        }
        Payload payload = verify(req.getCredential());

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token Google sem email.");
        }
        Boolean emailVerified = payload.getEmailVerified();
        if (emailVerified != null && !emailVerified) {
            throw new IllegalArgumentException("Email do Google nao verificado.");
        }

        String rawName = (String) payload.get("name");
        final String displayName = (rawName == null || rawName.isBlank()) ? email : rawName;

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User.Role role = req.getRole() != null ? req.getRole() : User.Role.CANDIDATE;
            String randomPwd = UUID.randomUUID().toString() + UUID.randomUUID();
            User created = User.builder()
                    .name(displayName)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(randomPwd))
                    .role(role)
                    .active(true)
                    // O Google ja verificou este e-mail; pedir codigo de novo seria
                    // atrito sem ganho de seguranca.
                    .emailVerified(true)
                    .build();
            return userRepository.save(created);
        });

        return buildResponse(user);
    }

    private Payload verify(String credential) {
        try {
            GoogleIdToken token = verifier.verify(credential);
            if (token == null) {
                throw new IllegalArgumentException("Token Google invalido.");
            }
            return token.getPayload();
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new IllegalArgumentException("Falha ao validar token Google.", e);
        }
    }

    private AuthResponse buildResponse(User user) {
        UserDetails ud = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(ud, Map.of(
                "role", user.getRole().name(),
                "name", user.getName(),
                "userId", user.getId()
        ));
        String refresh = jwtService.generateRefreshToken(ud);
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refresh)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
