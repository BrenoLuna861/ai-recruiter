package com.airecruiter.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "chat_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatLog {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    @Field("user_id")
    private Long userId;

    @Field("role")
    private String role; // "user" | "assistant"

    @Field("content")
    private String content;

    @Field("created_at")
    private LocalDateTime createdAt;
}
