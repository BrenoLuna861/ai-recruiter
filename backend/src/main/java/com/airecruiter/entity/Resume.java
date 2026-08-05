package com.airecruiter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes", indexes = {
    @Index(name = "idx_resumes_user",   columnList = "user_id"),
    @Index(name = "idx_resumes_status", columnList = "status"),
    @Index(name = "idx_resumes_active", columnList = "is_active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_type", length = 100)
    private String fileType;

    // FIX: LONGTEXT suporta até 4GB — necessário para PDFs extensos
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "skills_score")
    private Integer skillsScore;

    @Column(name = "experience_score")
    private Integer experienceScore;

    @Column(name = "format_score")
    private Integer formatScore;

    @Column(name = "ats_score")
    private Integer atsScore;

    @Column(name = "analysis_mongo_id", length = 24)
    private String analysisMongoId;

    /**
     * Analise qualitativa completa (pontos fortes, fracos, sugestoes, justificativas
     * das notas) em JSON.
     *
     * Antes isso vivia SO no MongoDB. Quando o Mongo ficava indisponivel, o curriculo
     * era salvo com as notas e sem os textos, e a tela exibia "Nao foi possivel
     * carregar os pontos fortes" sem nenhum erro visivel. Guardar aqui torna o Mongo
     * opcional: ele continua util para consulta analitica, mas a tela nao depende dele.
     */
    @Lob
    @Column(name = "analysis_json", columnDefinition = "LONGTEXT")
    private String analysisJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, ANALYZING, DONE, ERROR
    }
}
