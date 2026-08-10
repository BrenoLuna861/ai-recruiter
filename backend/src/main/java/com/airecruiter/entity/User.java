package com.airecruiter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 191)
    private String email;

    /*
     * @JsonIgnore: esta entidade e serializada em respostas da API (o ranking de
     * candidatos devolve Application, que carrega o User do candidato). Sem isto,
     * o hash bcrypt de cada pessoa iria no JSON. Hash nao e senha, mas e material
     * para ataque offline e nao tem motivo nenhum para sair do servidor.
     */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.CANDIDATE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Se o dono do e-mail confirmou o cadastro com o codigo enviado.
     *
     * O padrao e true de proposito: quem entra pelo Google ja teve o e-mail
     * verificado por eles, e as contas que existiam antes desta funcionalidade
     * nao podem ser trancadas fora do sistema. O cadastro por e-mail e o unico
     * lugar que marca explicitamente como false.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Role {
        CANDIDATE, RECRUITER, ADMIN
    }
}
