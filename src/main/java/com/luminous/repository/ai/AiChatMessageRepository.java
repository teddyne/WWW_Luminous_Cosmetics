package com.luminous.repository.ai;

import com.luminous.model.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop30BySessionIdOrderByCreatedAtDesc(String sessionId);

    List<AiChatMessage> findTop30ByUserIdOrderByCreatedAtDesc(Integer userId);

    long countByUserIdAndRoleAndCreatedAtAfter(Integer userId, String role, LocalDateTime after);

    long countBySessionIdAndRoleAndCreatedAtAfter(String sessionId, String role, LocalDateTime after);

    void deleteByUserId(Integer userId);

    void deleteBySessionId(String sessionId);
}
