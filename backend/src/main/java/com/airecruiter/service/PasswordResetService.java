package com.airecruiter.service;

import com.airecruiter.config.PasswordResetRateLimiter;
import com.airecruiter.entity.PasswordResetToken;
import com.airecruiter.entity.User;
import com.airecruiter.exception.PasswordResetException;
import com.airecruiter.repository.PasswordResetTokenRepository;
import com.airecruiter.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRateLimiter rateLimiter;
    private final TransactionTemplate txTemplate;

    @Value("${password-reset.ttl-minutes:15}")
    private int ttlMinutes;

    @Value("${password-reset.max-per-email-window:3}")
    private int maxPerEmailWindow;

    private static final SecureRandom RANDOM = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder,
                                PasswordResetRateLimiter rateLimiter,
                                PlatformTransactionManager txManager) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    // ------------------------------------------------------------------
    // 1. Solicitar recuperacao
    // ------------------------------------------------------------------

    /**
     * NAO e @Transactional de proposito: o envio do e-mail e uma chamada de rede
     * lenta e falivel, e prender a transacao aberta durante ela seguraria conexao
     * do pool. A escrita no banco acontece em uma transacao curta (txTemplate) e
     * o e-mail sai depois, ja commitado — assim nunca mandamos link de um token
     * que sofreu rollback.
     *
     * Tambem nao lanca excecao em nenhum caminho: qualquer erro diferenciado
     * (usuario nao existe / falha no Resend) viraria um oraculo de enumeracao de
     * contas. O controller responde 200 sempre.
     */
    public void requestPasswordReset(String rawEmail, String requestIp) {
        String email = normalize(rawEmail);
        if (email == null) return;

        if (!rateLimiter.tryAcquire("ip:" + requestIp)) {
            log.warn("Rate limit de forgot-password atingido para IP {}", requestIp);
            return;
        }

        Optional<User> maybeUser = txTemplate.execute(status -> userRepository.findByEmail(email));
        if (maybeUser == null || maybeUser.isEmpty()) {
            // E-mail nao cadastrado: silencio. Mesma resposta que o caminho feliz.
            log.info("forgot-password para e-mail inexistente (resposta generica enviada)");
            return;
        }

        User user = maybeUser.get();
        if (!user.isActive()) {
            log.info("forgot-password para conta inativa: id={}", user.getId());
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(ttlMinutes);
        long recentes = tokenRepository.countByEmailAndCreatedAtAfter(email, since);
        if (recentes >= maxPerEmailWindow) {
            log.warn("Muitos pedidos de reset para o mesmo e-mail em {} min (id={})", ttlMinutes, user.getId());
            return;
        }

        String token = generateToken();
        String tokenHash = sha256Hex(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);

        txTemplate.executeWithoutResult(status -> {
            // Invalida links anteriores: so o mais recente vale.
            tokenRepository.deleteByEmail(email);
            tokenRepository.save(new PasswordResetToken(tokenHash, email, expiresAt, requestIp));
        });

        try {
            emailService.sendPasswordResetEmail(email, token, ttlMinutes);
        } catch (Exception e) {
            // Falha de envio NAO pode virar 500 — isso revelaria que a conta existe.
            log.error("Falha ao enviar e-mail de reset (id={}): {}", user.getId(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 2. Validar token (a tela chama antes de mostrar o formulario)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        return findUsableToken(token).isPresent();
    }

    // ------------------------------------------------------------------
    // 3. Redefinir a senha
    // ------------------------------------------------------------------

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = findUsableToken(token)
                .orElseThrow(PasswordResetException::invalidToken);

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(PasswordResetException::invalidToken);

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new PasswordResetException("A nova senha deve ser diferente da atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Uso unico: marca este e apaga os demais da conta.
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
        tokenRepository.deleteByEmail(resetToken.getEmail());

        log.info("Senha redefinida com sucesso (id={})", user.getId());
    }

    // ------------------------------------------------------------------
    // Limpeza
    // ------------------------------------------------------------------

    /** Todo dia as 3h: remove tokens expirados/usados. @EnableScheduling ja esta ativo. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int removidos = tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
        if (removidos > 0) log.info("Limpeza de tokens de reset: {} removidos", removidos);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Optional<PasswordResetToken> findUsableToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return tokenRepository.findByTokenHash(sha256Hex(token))
                .filter(t -> !t.isUsed() && !t.isExpired());
    }

    private String normalize(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase();
        return e.isEmpty() ? null : e;
    }

    /** 32 bytes de entropia (256 bits) em base64url — inbrutavel na pratica. */
    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
