package com.airecruiter.dto.response;

import com.airecruiter.entity.Resume;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data @Builder
public class ResumeResponse {
    private Long id;
    private String title;
    private String fileType;
    private Integer overallScore;
    private Integer skillsScore;
    private Integer experienceScore;
    private Integer formatScore;
    private Integer atsScore;
    private Resume.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String analysis;

    /** Análise qualitativa completa em JSON, vinda do MySQL (independe do MongoDB). */
    private String analysisJson;

    /** Pesos usados no cálculo da nota geral — a tela exibe a fórmula ao usuário. */
    private Map<String, Integer> scoreWeights;

    private String content; // Texto extraído do currículo original
}
