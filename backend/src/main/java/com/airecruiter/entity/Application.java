package com.airecruiter.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

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
