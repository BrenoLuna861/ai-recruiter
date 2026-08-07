package com.airecruiter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications",
    uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "candidate_id"}),
    indexes = {
        @Index(name = "idx_app_job",       columnList = "job_id"),
        @Index(name = "idx_app_candidate", columnList = "candidate_id"),
        @Index(name = "idx_app_status",    columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * O endpoint de ranking devolve esta entidade direto, e com open-in-view:
     * false a sessao ja esta fechada na hora de serializar. Qualquer relacao
     * preguicosa que o Jackson tocar lanca LazyInitializationException e a
     * requisicao vira 500.
     *
     * job e resume nao sao usados pela tela — o recrutador ja sabe de qual vaga
     * e o ranking — entao ficam fora da resposta. candidate e EAGER porque a
     * tela mostra o nome da pessoa; o passwordHash dele esta protegido por
     * @JsonIgnore na propria entidade User.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, REVIEWED, SHORTLISTED, REJECTED, HIRED
    }
}
