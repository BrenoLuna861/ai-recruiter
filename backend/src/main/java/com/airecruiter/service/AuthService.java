package com.airecruiter.service;

import com.airecruiter.dto.request.LoginRequest;
import com.airecruiter.dto.request.RegisterRequest;
import com.airecruiter.dto.response.AuthResponse;
import com.airecruiter.entity.User;
import com.airecruiter.exception.EmailNotVerifiedException;
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
    private final EmailVerificationService emailVerificationService;

    /**
     * Cria a conta e dispara o codigo de confirmacao.
     *
     * NAO devolve token: a conta so vira utilizavel depois da confirmacao. Emitir
     * o JWT aqui tornaria o passo opcional na pratica, ja que o app funcionaria
     * sem nunca confirmar.
     */
    @Transactional
    public void register(RegisterRequest req, String requestIp) {
        String email = req.getEmail() == null ? null : req.getEmail().trim().toLowerCase();

        if (email == null || userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está em uso.");
        }

        User user = User.builder()
            .name(req.getName())
            .email(email)
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(req.getRole() != null ? req.getRole() : User.Role.CANDIDATE)
            .active(true)
            .emailVerified(false)
            .build();

        userRepository.save(user);
        emailVerificationService.enviarCodigo(email, requestIp);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        User user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // A checagem fica aqui, e nao no UserDetailsServiceImpl, para o frontend
        // conseguir distinguir "senha errada" de "conta nao confirmada" e levar a
        // pessoa para a tela certa.
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getEmail());
        }

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
