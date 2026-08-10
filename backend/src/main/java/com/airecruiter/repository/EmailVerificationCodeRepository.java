package com.airecruiter.repository;

import com.airecruiter.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    /** O codigo vigente de um e-mail: o mais recente que ainda nao foi usado. */
    Optional<EmailVerificationCode> findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    @Modifying
    @Query("delete from EmailVerificationCode c where c.expiresAt < :now or c.usedAt is not null")
    int deleteExpiredAndUsed(@Param("now") LocalDateTime now);
}
