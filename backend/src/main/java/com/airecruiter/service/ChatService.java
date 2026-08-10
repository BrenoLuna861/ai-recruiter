package com.airecruiter.service;

import com.airecruiter.entity.ChatLog;
import com.airecruiter.entity.User;
import com.airecruiter.repository.ChatLogRepository;
import com.airecruiter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.airecruiter.dto.response.ChatSessionResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLogRepository chatLogRepository;
    private final UserRepository userRepository;
    private final AnthropicService anthropicService;

    public Map<String, String> sendMessage(String userMessage, String sessionId, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String sid = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();

        // Save user message
        chatLogRepository.save(ChatLog.builder()
            .sessionId(sid).userId(user.getId())
            .role("user").content(userMessage).createdAt(LocalDateTime.now()).build());

        // Build history for context
        List<Map<String, String>> history = chatLogRepository
            .findBySessionIdOrderByCreatedAtAsc(sid).stream()
            .limit(20) // last 20 messages for context window
            .map(log -> Map.of("role", log.getRole(), "content", log.getContent()))
            .collect(Collectors.toList());

        String response = anthropicService.chat(userMessage, history, user.getRole().name());

        // Save assistant response
        chatLogRepository.save(ChatLog.builder()
            .sessionId(sid).userId(user.getId())
            .role("assistant").content(response).createdAt(LocalDateTime.now()).build());

        return Map.of("response", response, "sessionId", sid);
    }

    public List<ChatLog> getHistory(String sessionId, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return chatLogRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream().filter(log -> log.getUserId().equals(user.getId())).collect(Collectors.toList());
    }

    /**
     * Conversas do usuario, da mais recente para a mais antiga.
     *
     * O agrupamento acontece em Java e nao numa agregacao do Mongo: o volume por
     * usuario e pequeno (dezenas de mensagens), e uma agregacao aqui seria mais
     * codigo para manter sem ganho perceptivel.
     */
    public List<ChatSessionResponse> listSessions(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Vem em ordem decrescente; invertemos para que, ao agrupar, a primeira
        // mensagem vista de cada sessao seja de fato a primeira da conversa.
        List<ChatLog> logs = chatLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Map<String, List<ChatLog>> porSessao = new LinkedHashMap<>();
        for (ChatLog log : logs) {
            porSessao.computeIfAbsent(log.getSessionId(), k -> new ArrayList<>()).add(log);
        }

        List<ChatSessionResponse> sessoes = new ArrayList<>();
        for (Map.Entry<String, List<ChatLog>> e : porSessao.entrySet()) {
            List<ChatLog> mensagens = e.getValue();

            String titulo = mensagens.stream()
                .filter(m -> "user".equals(m.getRole()))
                .reduce((primeira, segunda) -> segunda)   // a mais antiga da lista decrescente
                .map(m -> resumir(m.getContent()))
                .orElse("Conversa sem título");

            sessoes.add(ChatSessionResponse.builder()
                .sessionId(e.getKey())
                .title(titulo)
                .messageCount(mensagens.size())
                .lastMessageAt(mensagens.get(0).getCreatedAt())
                .build());
        }

        sessoes.sort(Comparator.comparing(
            ChatSessionResponse::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return sessoes;
    }

    /** O userId no filtro impede apagar conversa alheia adivinhando o sessionId. */
    public void deleteSession(String sessionId, String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        chatLogRepository.deleteBySessionIdAndUserId(sessionId, user.getId());
    }

    public void deleteAllSessions(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        chatLogRepository.deleteByUserId(user.getId());
    }

    /** Título da conversa: a primeira pergunta, cortada em limite de palavra. */
    private String resumir(String texto) {
        if (texto == null || texto.isBlank()) return "Conversa sem título";
        String limpo = texto.replaceAll("\\s+", " ").trim();
        if (limpo.length() <= 60) return limpo;
        int corte = limpo.lastIndexOf(' ', 60);
        return limpo.substring(0, corte > 30 ? corte : 60) + "...";
    }
}
