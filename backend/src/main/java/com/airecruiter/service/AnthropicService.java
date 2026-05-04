package com.airecruiter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnthropicService {

    private final WebClient webClient;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens}")
    private int maxTokens;

    public AnthropicService(@Value("${anthropic.api-key}") String apiKey,
                            @Value("${anthropic.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    public String analyzeResume(String resumeContent) {
        String prompt = """
            Você é o Alex, um agente de IA especialista em recrutamento e análise de currículos.
            Analise o currículo a seguir e forneça uma análise completa e estruturada em português.
            
            CURRÍCULO:
            %s
            
            Forneça sua análise no seguinte formato JSON (responda APENAS o JSON, sem markdown):
            {
              "overallScore": <0-100>,
              "skillsScore": <0-100>,
              "experienceScore": <0-100>,
              "formatScore": <0-100>,
              "atsScore": <0-100>,
              "summary": "<resumo executivo da análise>",
              "strengths": ["<ponto forte 1>", "<ponto forte 2>", "<ponto forte 3>"],
              "weaknesses": ["<ponto fraco 1>", "<ponto fraco 2>"],
              "suggestions": ["<sugestão 1>", "<sugestão 2>", "<sugestão 3>"],
              "keywordsFound": ["<keyword 1>", "<keyword 2>"],
              "keywordsMissing": ["<keyword faltando 1>", "<keyword faltando 2>"],
              "rewrittenSummary": "<resumo profissional reescrito e otimizado para ATS>",
              "fullAnalysis": "<análise detalhada completa em texto corrido>"
            }
            """.formatted(resumeContent);

        return callClaude(prompt);
    }

    public String chat(String userMessage, List<Map<String, String>> history, String userRole) {
        String systemPrompt = """
            Você é o Alex, um agente de IA especialista em recrutamento e desenvolvimento de carreira.
            Você trabalha como headhunter virtual inteligente na plataforma AI Recruiter.
            
            Comportamento:
            - Para CANDIDATOS: Oriente sobre carreira, otimização de currículo e preparação para entrevistas.
            - Para RECRUTADORES: Ajude a encontrar candidatos ideais, criar descrições de vagas e avaliar perfis.
            - Seja direto, profissional mas acessível. Responda em português.
            - Papel do usuário atual: %s
            """.formatted(userRole);

        List<Map<String, Object>> messages = new java.util.ArrayList<>();
        for (Map<String, String> h : history) {
            messages.add(Map.of("role", h.get("role"), "content", h.get("content")));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "messages", messages
        );

        return webClient.post()
            .uri("/v1/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .map(resp -> {
                var content = (List<?>) resp.get("content");
                if (content != null && !content.isEmpty()) {
                    var block = (Map<?, ?>) content.get(0);
                    return (String) block.get("text");
                }
                return "Desculpe, não consegui gerar uma resposta.";
            })
            .onErrorResume(e -> {
                log.error("Anthropic API error: {}", e.getMessage());
                return Mono.just("Erro ao conectar com o agente de IA. Tente novamente.");
            })
            .block();
    }

    public String matchResumeToJob(String resumeContent, String jobDescription) {
        String prompt = """
            Analise a compatibilidade entre este currículo e esta vaga. Responda em JSON:
            
            CURRÍCULO: %s
            
            VAGA: %s
            
            JSON esperado:
            {
              "matchScore": <0-100>,
              "analysis": "<análise da compatibilidade>",
              "pros": ["<ponto positivo 1>", "<ponto positivo 2>"],
              "cons": ["<ponto negativo 1>"],
              "recommendation": "<recomendação para o recrutador>"
            }
            """.formatted(resumeContent, jobDescription);

        return callClaude(prompt);
    }

    private String callClaude(String prompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
            .uri("/v1/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .map(resp -> {
                var content = (List<?>) resp.get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) ((Map<?, ?>) content.get(0)).get("text");
                }
                return "{}";
            })
            .onErrorResume(e -> {
                log.error("Anthropic API error: {}", e.getMessage());
                return Mono.just("{}");
            })
            .block();
    }
}
