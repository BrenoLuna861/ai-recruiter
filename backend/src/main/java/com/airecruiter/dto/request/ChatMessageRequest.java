package com.airecruiter.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {
    @NotBlank
    @Size(max = 4000)
    private String message;

    private String sessionId;
}
