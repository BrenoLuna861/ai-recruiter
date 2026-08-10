package com.airecruiter.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Resumo de uma conversa, para a lista lateral do chat.
 *
 * Só o suficiente para a pessoa reconhecer e escolher — as mensagens em si são
 * buscadas ao abrir. Devolver todas as conversas com todo o conteúdo tornaria a
 * lista pesada sem necessidade.
 */
@Data
@Builder
public class ChatSessionResponse {

    private String sessionId;

    /** Primeira pergunta da conversa, truncada. É o que identifica o assunto. */
    private String title;

    private int messageCount;

    private LocalDateTime lastMessageAt;
}
