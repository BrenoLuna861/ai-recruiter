package com.airecruiter.repository;

import com.airecruiter.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByEmail(String email);

    /** Quantos pedidos esse e-mail fez na janela — freio anti-spam por conta. */
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    /** Limpeza dos tokens ja expirados/usados. Roda no @Scheduled do service. */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now or t.usedAt is not null")
    int deleteExpiredAndUsed(@Param("now") LocalDateTime now);
}
