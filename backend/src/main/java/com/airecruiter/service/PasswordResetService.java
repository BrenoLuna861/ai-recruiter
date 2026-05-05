package com.airecruiter.service;

import com.airecruiter.entity.PasswordResetToken;
import com.airecruiter.entity.User;
import com.airecruiter.repository.PasswordResetTokenRepository;
import com.airecruiter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void requestPasswordReset(String email) {
        // Sempre retorna sucesso por segurança (não revela se email existe)
        userRepository.findByEmail(email).ifPresent(user -> {
            // Remove tokens antigos do email
            tokenRepository.deleteByEmail(email);

            // Gera novo token
            String token = UUID.randomUUID().toString();
            tokenRepository.save(new PasswordResetToken(token, email));

            // Envia email
            emailService.sendPasswordResetEmail(email, token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token expirado");
        }

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token já utilizado");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Senha redefinida com sucesso para: {}", resetToken.getEmail());
    }
}
