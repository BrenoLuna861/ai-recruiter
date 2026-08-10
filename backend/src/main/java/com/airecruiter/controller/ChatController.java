package com.airecruiter.controller;

import com.airecruiter.dto.request.ChatMessageRequest;
import com.airecruiter.dto.response.ChatSessionResponse;
import com.airecruiter.entity.ChatLog;
import com.airecruiter.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> sendMessage(
        @Valid @RequestBody ChatMessageRequest req,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
            chatService.sendMessage(req.getMessage(), req.getSessionId(), userDetails.getUsername())
        );
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatLog>> history(@PathVariable String sessionId,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.getHistory(sessionId, userDetails.getUsername()));
    }

    /** Conversas anteriores, para a lista lateral. */
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> sessions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.listSessions(userDetails.getUsername()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        chatService.deleteSession(sessionId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal UserDetails userDetails) {
        chatService.deleteAllSessions(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
