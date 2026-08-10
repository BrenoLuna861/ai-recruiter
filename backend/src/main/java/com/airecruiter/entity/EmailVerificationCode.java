package com.airecruiter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Codigo de confirmacao de cadastro.
 *
 * Guarda o SHA-256 do codigo, nao o codigo. Um numero de seis digitos tem apenas
 * um milhao de combinacoes, entao o hash sozinho nao protege contra forca bruta —
 * quem protege e o campo `attempts`, que invalida o codigo apos algumas tentativas
 * erradas, somado a validade curta. O hash serve para o caso de vazamento do
 * banco: sem ele, qualquer pessoa com acesso a uma copia confirmaria contas alheias.
 */
@Entity
@Table(name = "email_verification_codes", indexes = {
        @Index(name = "idx_evc_email", columnList = "email"),
        @Index(name = "idx_evc_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false, length = 191)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Tentativas erradas. Ao atingir o teto, o codigo morre e outro precisa ser pedido. */
    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmailVerificationCode(String codeHash, String email, LocalDateTime expiresAt) {
        this.codeHash = codeHash;
        this.email = email;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
