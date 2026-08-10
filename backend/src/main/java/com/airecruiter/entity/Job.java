package com.airecruiter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_recruiter", columnList = "recruiter_id"),
    @Index(name = "idx_jobs_active",    columnList = "is_active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * @JsonIgnore e obrigatorio aqui.
     *
     * O JobController devolve a entidade Job diretamente, e o application.yml
     * define open-in-view: false — a sessao do Hibernate fecha antes da
     * serializacao. Sem isto, o Jackson tenta ler este proxy preguicoso, lanca
     * LazyInitializationException e a requisicao vira 500. O sintoma so aparecia
     * quando existia ao menos uma vaga cadastrada; com a lista vazia, nada
     * acontecia.
     *
     * O frontend nao usa este campo. Se um dia precisar do nome do recrutador,
     * o certo e um DTO, nao expor a entidade.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 191)
    private String company;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(length = 191)
    private String location;

    @Column(name = "salary_range", length = 80)
    private String salaryRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", length = 20)
    @Builder.Default
    private JobType jobType = JobType.FULL_TIME;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum JobType {
        FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, REMOTE
    }
}
