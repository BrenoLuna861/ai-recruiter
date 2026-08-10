package com.airecruiter.repository;

import com.airecruiter.entity.ChatLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLogRepository extends MongoRepository<ChatLog, String> {
    List<ChatLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /** Base do histórico: todas as mensagens do usuário, das mais recentes para as antigas. */
    List<ChatLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** O userId no filtro impede apagar a conversa de outra pessoa adivinhando o sessionId. */
    void deleteBySessionIdAndUserId(String sessionId, Long userId);

    void deleteByUserId(Long userId);
}
