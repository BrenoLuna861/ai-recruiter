package com.airecruiter.service;

import com.airecruiter.config.SlidingWindowRateLimiter;
import com.airecruiter.dto.response.ExternalJobResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Busca vagas em fontes publicas.
 *
 * Duas fontes, por ordem de preferencia:
 *
 * 1. Adzuna — agrega portais brasileiros, mas exige app_id/app_key e o plano
 *    gratuito da 1.000 chamadas por MES. Dai o cache: sem ele, alguem recarregando
 *    a pagina algumas dezenas de vezes queima a cota do mes inteiro.
 *
 * 2. Remotive — feed aberto de vagas remotas, sem cadastro. Serve de alternativa
 *    quando as chaves da Adzuna nao estao configuradas, para a tela nunca aparecer
 *    vazia so por falta de credencial.
 */
@Slf4j
@Service
public class ExternalJobService {

    @Value("${jobs.adzuna.app-id:}")
    private String adzunaAppId;

    @Value("${jobs.adzuna.app-key:}")
    private String adzunaAppKey;

    @Value("${jobs.adzuna.country:br}")
    private String adzunaCountry;

    @Value("${jobs.cache-minutes:30}")
    private int cacheMinutes;

    /** Buscas por IP/hora que chegam a consultar a API externa (as servidas pelo cache nao contam). */
    @Value("${jobs.max-buscas-por-hora:20}")
    private int maxBuscasPorHora;

    private final SlidingWindowRateLimiter rateLimiter;

    public ExternalJobService(SlidingWindowRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    private final WebClient webClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build();

    private record CacheEntry(List<ExternalJobResponse> vagas, Instant gravadoEm) {}
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------

    public List<ExternalJobResponse> buscar(String termo, String local, boolean apenasRemotas, int pagina, String ip) {
        String chave = String.join("|", nz(termo), nz(local), String.valueOf(apenasRemotas), String.valueOf(pagina));

        CacheEntry cacheado = cache.get(chave);
        if (cacheado != null && cacheado.gravadoEm().isAfter(Instant.now().minusSeconds(cacheMinutes * 60L))) {
            return cacheado.vagas();
        }

        // Chegou aqui = vai bater na API externa de verdade. O cache sozinho nao
        // protege a cota: variando o termo a cada requisicao, o cache nunca acerta
        // e a cota mensal de 1.000 chamadas se esgota em minutos. O limite so conta
        // as chamadas que furam o cache, entao navegacao normal nunca esbarra nele.
        if (!rateLimiter.tryAcquire("vagas:ip:" + ip, maxBuscasPorHora, 60)) {
            log.warn("Rate limit de busca de vagas atingido para o IP {}", ip);
            return cacheado != null ? cacheado.vagas() : List.of();
        }

        List<ExternalJobResponse> vagas;
        if (adzunaConfigurada()) {
            vagas = buscarNaAdzuna(termo, local, pagina);
        } else {
            log.info("Chaves da Adzuna ausentes — usando o Remotive como fonte alternativa");
            vagas = buscarNoRemotive(termo);
        }

        if (apenasRemotas) {
            vagas = vagas.stream().filter(v -> Boolean.TRUE.equals(v.getRemote())).toList();
        }

        cache.put(chave, new CacheEntry(vagas, Instant.now()));
        return vagas;
    }

    public boolean adzunaConfigurada() {
        return adzunaAppId != null && !adzunaAppId.isBlank()
            && adzunaAppKey != null && !adzunaAppKey.isBlank();
    }

    // ------------------------------------------------------------------
    // Adzuna
    // ------------------------------------------------------------------

    private List<ExternalJobResponse> buscarNaAdzuna(String termo, String local, int pagina) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.adzuna.com/v1/api/jobs/" + adzunaCountry + "/search/" + Math.max(1, pagina))
                .queryParam("app_id", adzunaAppId)
                .queryParam("app_key", adzunaAppKey)
                .queryParam("results_per_page", 20)
                .queryParam("what", nz(termo))
                .queryParam("where", nz(local))
                .queryParam("content-type", "application/json")
                .build().toUriString();

        try {
            JsonNode raiz = webClient.get().uri(url)
                    .retrieve().bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            List<ExternalJobResponse> vagas = new ArrayList<>();
            if (raiz == null || !raiz.has("results")) return vagas;

            for (JsonNode n : raiz.get("results")) {
                String descricao = texto(n, "description");
                vagas.add(ExternalJobResponse.builder()
                        .id("adzuna-" + texto(n, "id"))
                        .title(texto(n, "title"))
                        .company(n.path("company").path("display_name").asText(null))
                        .location(n.path("location").path("display_name").asText(null))
                        .description(descricao)
                        .url(texto(n, "redirect_url"))
                        .source("Adzuna")
                        .category(n.path("category").path("label").asText(null))
                        .remote(pareceRemota(texto(n, "title") + " " + descricao))
                        .salaryMin(n.hasNonNull("salary_min") ? n.get("salary_min").asDouble() : null)
                        .salaryMax(n.hasNonNull("salary_max") ? n.get("salary_max").asDouble() : null)
                        .publishedAt(dataIso(texto(n, "created")))
                        .build());
            }
            return vagas;

        } catch (Exception e) {
            log.error("Falha ao consultar a Adzuna: {}", e.getMessage());
            // Nao propaga: a tela mostra as vagas internas e um aviso, em vez de quebrar.
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Remotive
    // ------------------------------------------------------------------

    private List<ExternalJobResponse> buscarNoRemotive(String termo) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://remotive.com/api/remote-jobs")
                .queryParam("limit", 30)
                .queryParam("search", nz(termo))
                .build().toUriString();

        try {
            JsonNode raiz = webClient.get().uri(url)
                    .retrieve().bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            List<ExternalJobResponse> vagas = new ArrayList<>();
            if (raiz == null || !raiz.has("jobs")) return vagas;

            for (JsonNode n : raiz.get("jobs")) {
                vagas.add(ExternalJobResponse.builder()
                        .id("remotive-" + texto(n, "id"))
                        .title(texto(n, "title"))
                        .company(texto(n, "company_name"))
                        .location(texto(n, "candidate_required_location"))
                        .description(limparHtml(texto(n, "description")))
                        .url(texto(n, "url"))
                        .source("Remotive")
                        .category(texto(n, "category"))
                        .remote(true)
                        .publishedAt(dataIso(texto(n, "publication_date")))
                        .build());
            }
            return vagas;

        } catch (Exception e) {
            log.error("Falha ao consultar o Remotive: {}", e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String nz(String s) { return s == null ? "" : s.trim(); }

    private String texto(JsonNode n, String campo) {
        return n.hasNonNull(campo) ? n.get(campo).asText() : null;
    }

    /** O Remotive devolve a descricao em HTML; o card precisa de texto puro. */
    private String limparHtml(String html) {
        if (html == null) return null;
        String limpo = html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
        return limpo.length() > 400 ? limpo.substring(0, 400) + "..." : limpo;
    }

    /** A Adzuna nao tem campo de trabalho remoto, entao inferimos pelo texto. */
    private Boolean pareceRemota(String texto) {
        if (texto == null) return false;
        String t = texto.toLowerCase();
        return t.contains("remoto") || t.contains("remote") || t.contains("home office") || t.contains("teletrabalho");
    }

    private LocalDateTime dataIso(String valor) {
        if (valor == null) return null;
        try {
            return OffsetDateTime.parse(valor).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(valor);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
