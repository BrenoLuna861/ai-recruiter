package com.airecruiter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Token de recuperacao de senha.
 *
 * IMPORTANTE: o token em claro NUNCA e persistido. O banco guarda apenas o
 * SHA-256 dele (tokenHash). Se o dump do banco vazar, ninguem consegue
 * reconstruir os links de reset — mesmo raciocinio do hash de senha.
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_email", columnList = "email"),
        @Index(name = "idx_prt_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 do token, em hex minusculo (64 chars). */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 191)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** null = nao usado. Guardamos o instante para trilha de auditoria. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PasswordResetToken(String tokenHash, String email, LocalDateTime expiresAt, String requestIp) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
