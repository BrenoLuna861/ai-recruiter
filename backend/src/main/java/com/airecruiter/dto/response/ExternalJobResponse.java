package com.airecruiter.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Vaga vinda de uma fonte externa (Adzuna, Remotive).
 *
 * Deliberadamente separada de {@link com.airecruiter.entity.Job}: vagas externas
 * nao sao persistidas, nao aceitam candidatura pela plataforma e sempre levam ao
 * anuncio original. Misturar as duas na mesma entidade convidaria a tratar como
 * igual o que nao e.
 */
@Data
@Builder
public class ExternalJobResponse {

    private String id;
    private String title;
    private String company;
    private String location;

    /** Descricao ja truncada — a integra fica no anuncio original. */
    private String description;

    /** URL do anuncio na fonte. E para onde o botao "Ver vaga" aponta. */
    private String url;

    /** "Adzuna" ou "Remotive" — exibido como selo no card. */
    private String source;

    private String category;
    private Boolean remote;

    /** Faixa salarial quando a fonte informa; frequentemente nula. */
    private Double salaryMin;
    private Double salaryMax;

    private LocalDateTime publishedAt;
}
