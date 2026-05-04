package com.airecruiter.service;

import com.airecruiter.entity.ChatLog;
import com.airecruiter.entity.User;
import com.airecruiter.repository.ChatLogRepository;
import com.airecruiter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
}
