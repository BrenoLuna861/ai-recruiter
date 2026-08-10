package com.airecruiter.service;

import com.airecruiter.config.SlidingWindowRateLimiter;
import com.airecruiter.entity.EmailVerificationCode;
import com.airecruiter.entity.User;
import com.airecruiter.repository.EmailVerificationCodeRepository;
import com.airecruiter.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
public class EmailVerificationService {

    private final EmailVerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SlidingWindowRateLimiter rateLimiter;
    private final TransactionTemplate txTemplate;

    @Value("${email-verification.ttl-minutes:30}")
    private int ttlMinutes;

    /** Tentativas erradas antes de o codigo ser descartado. */
    @Value("${email-verification.max-attempts:5}")
    private int maxAttempts;

    /** Quantos codigos o mesmo e-mail pode pedir dentro da janela do ttl. */
    @Value("${email-verification.max-per-window:3}")
    private int maxPorJanela;

    private static final SecureRandom RANDOM = new SecureRandom();

    public EmailVerificationService(EmailVerificationCodeRepository codeRepository,
                                    UserRepository userRepository,
                                    EmailService emailService,
                                    SlidingWindowRateLimiter rateLimiter,
                                    PlatformTransactionManager txManager) {
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    // ------------------------------------------------------------------

    /**
     * Gera e envia um codigo novo.
     *
     * Nao e @Transactional: o envio do e-mail e chamada de rede, e prender a
     * transacao durante ela seguraria conexao do pool. A escrita acontece em
     * transacao curta e o e-mail sai depois, ja commitado.
     */
    public void enviarCodigo(String rawEmail, String requestIp) {
        String email = normalize(rawEmail);
        if (email == null) return;

        if (!rateLimiter.tryAcquire("verify:ip:" + requestIp, 10, 60)) {
            log.warn("Rate limit de envio de codigo atingido para o IP {}", requestIp);
            return;
        }

        Optional<User> maybeUser = txTemplate.execute(s -> userRepository.findByEmail(email));
        if (maybeUser == null || maybeUser.isEmpty()) return;
        if (maybeUser.get().isEmailVerified()) return;

        LocalDateTime desde = LocalDateTime.now().minusMinutes(ttlMinutes);
        if (codeRepository.countByEmailAndCreatedAtAfter(email, desde) >= maxPorJanela) {
            log.warn("Muitos pedidos de codigo para o mesmo e-mail em {} min", ttlMinutes);
            return;
        }

        String codigo = gerarCodigo();
        LocalDateTime expira = LocalDateTime.now().plusMinutes(ttlMinutes);

        txTemplate.executeWithoutResult(s -> {
            // Só o código mais recente vale.
            codeRepository.deleteByEmail(email);
            codeRepository.save(new EmailVerificationCode(sha256Hex(codigo), email, expira));
        });

        try {
            emailService.sendVerificationEmail(email, codigo, ttlMinutes);
        } catch (Exception e) {
            log.error("Falha ao enviar codigo de confirmacao: {}", e.getMessage());
        }
    }

    /**
     * @return true se o codigo confere e a conta foi confirmada.
     *
     * Ao contrario da recuperacao de senha, aqui a mensagem PODE ser especifica:
     * quem esta confirmando ja provou conhecer o e-mail no cadastro, entao nao ha
     * enumeracao de contas a proteger, e um erro claro ajuda quem digitou errado.
     */
    @Transactional
    public boolean confirmar(String rawEmail, String codigo) {
        String email = normalize(rawEmail);
        if (email == null || codigo == null || codigo.isBlank()) return false;

        EmailVerificationCode registro = codeRepository
                .findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElse(null);

        if (registro == null || registro.isExpired() || registro.getAttempts() >= maxAttempts) {
            return false;
        }

        if (!registro.getCodeHash().equals(sha256Hex(codigo.trim()))) {
            // Contar a tentativa e o que impede forca bruta num numero de 6 digitos.
            registro.setAttempts(registro.getAttempts() + 1);
            codeRepository.save(registro);
            return false;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        user.setEmailVerified(true);
        userRepository.save(user);

        registro.setUsedAt(LocalDateTime.now());
        codeRepository.save(registro);
        codeRepository.deleteByEmail(email);

        log.info("Conta confirmada (id={})", user.getId());
        return true;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void limparExpirados() {
        int removidos = codeRepository.deleteExpiredAndUsed(LocalDateTime.now());
        if (removidos > 0) log.info("Limpeza de codigos de confirmacao: {} removidos", removidos);
    }

    // ------------------------------------------------------------------

    private String normalize(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase();
        return e.isEmpty() ? null : e;
    }

    /** Seis digitos, com zeros a esquerda preservados. */
    private String gerarCodigo() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String sha256Hex(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
