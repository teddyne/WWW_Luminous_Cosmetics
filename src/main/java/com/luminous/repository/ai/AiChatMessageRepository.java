package com.luminous.repository.ai;

import com.luminous.model.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop30BySessionIdOrderByCreatedAtDesc(String sessionId);

    List<AiChatMessage> findTop30ByUserIdOrderByCreatedAtDesc(Integer userId);

    void deleteByUserId(Integer userId);

    void deleteBySessionId(String sessionId);
}
