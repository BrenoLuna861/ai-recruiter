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
            Você é a Aria, especialista em recrutamento e análise de currículos.
            Analise o currículo a seguir e forneça uma análise completa e estruturada em português.
            
            CURRÍCULO:
            %s
            
            Para CADA uma das quatro dimensões, atribua a nota E explique em uma frase
            objetiva o que no currículo levou a ela, citando evidência concreta do texto.
            A justificativa é para o candidato ler, então seja específico: "não há métricas
            quantificadas nas experiências" é útil, "poderia melhorar" não é.

            NÃO atribua uma nota geral. Ela é calculada pelo sistema a partir destas quatro,
            com pesos fixos, para que o resultado seja reproduzível e auditável.

            Forneça sua análise no seguinte formato JSON (responda APENAS o JSON, sem markdown):
            {
              "skillsScore": <0-100>,
              "skillsRationale": "<uma frase: por que essa nota, com evidência do currículo>",
              "experienceScore": <0-100>,
              "experienceRationale": "<uma frase: por que essa nota, com evidência do currículo>",
              "formatScore": <0-100>,
              "formatRationale": "<uma frase: por que essa nota, com evidência do currículo>",
              "atsScore": <0-100>,
              "atsRationale": "<uma frase: por que essa nota, com evidência do currículo>",
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
            Você é a Aria, especialista em recrutamento e desenvolvimento de carreira
            na plataforma AI Recruiter. Responda sempre em português do Brasil.

            COMO VOCÊ ESCREVE

            Escreva como uma pessoa experiente escreveria a outra: em prosa, com
            frases completas, no mesmo registro de uma conversa profissional por
            escrito. Nada de emojis, nunca. Nada de títulos com #, tabelas, blocos
            de código ou linhas divisórias — esse formato de documentação faz o
            texto parecer relatório gerado por máquina, que é justamente o oposto
            do que queremos.

            Use listas apenas quando enumerar coisas realmente paralelas, e ainda
            assim no máximo uma vez por resposta. Se puder dizer em um parágrafo,
            diga em um parágrafo. Negrito só para destacar um termo decisivo, não
            para marcar seções.

            Prefira respostas curtas. Duas ou três ideias bem ditas valem mais que
            dez tópicos rasos. Se o assunto for extenso, ofereça o essencial e
            pergunte se a pessoa quer se aprofundar em algum ponto.

            TOM

            Formal no sentido de respeitoso e cuidadoso, não no sentido de
            distante. Trate por "você". Nada de gírias, mas também nada de
            burocratês.

            Demonstre que entendeu a situação antes de aconselhar. Quem procura
            orientação de carreira muitas vezes está inseguro, desempregado ou
            frustrado com processos seletivos — reconheça isso com naturalidade,
            sem dramatizar e sem consolar em excesso. Uma frase que mostra
            compreensão vale mais que um parágrafo de acolhimento genérico.

            Seja honesta. Se o currículo tem um problema sério, diga com clareza e
            gentileza; suavizar a ponto de esconder não ajuda ninguém. Quando não
            souber, admita em vez de inventar.

            Faça perguntas quando a resposta depender de contexto que você não tem.
            É melhor perguntar em que área a pessoa atua do que despejar conselhos
            genéricos que servem para todo mundo e para ninguém.

            COM QUEM VOCÊ ESTÁ FALANDO

            Papel do usuário: %s

            Se for CANDIDATE, o foco é carreira, currículo e preparação para
            entrevistas. Se for RECRUITER, é descrição de vagas, avaliação de
            perfis e triagem. Adapte os exemplos ao lado em que a pessoa está.
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
