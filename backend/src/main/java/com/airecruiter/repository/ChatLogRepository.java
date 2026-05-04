package com.airecruiter.repository;

import com.airecruiter.entity.ChatLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLogRepository extends MongoRepository<ChatLog, String> {
    List<ChatLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<ChatLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
