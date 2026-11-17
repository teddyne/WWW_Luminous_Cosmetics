package com.luminous.service.ai;

import com.luminous.model.AiChatMessage;
import com.luminous.repository.ai.AiChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatHistoryService {

    public record ChatTurn(String role, String content) {
    }

    private static final int MAX_CONTENT_LENGTH = 4000;

    private final AiChatMessageRepository repository;

    @Transactional
    public void save(Integer userId, String sessionId, String role, String content) {
        if (content == null || content.isBlank() || sessionId == null) {
            return;
        }
        String trimmed = content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH)
                : content;
        repository.save(new AiChatMessage(userId, sessionId, role, trimmed));
    }

    @Transactional(readOnly = true)
    public List<ChatTurn> history(Integer userId, String sessionId, int limit) {
        List<AiChatMessage> rows = userId != null
                ? repository.findTop30ByUserIdOrderByCreatedAtDesc(userId)
                : repository.findTop30BySessionIdOrderByCreatedAtDesc(sessionId);
        List<ChatTurn> turns = new ArrayList<>();
        for (AiChatMessage row : rows) {
            turns.add(new ChatTurn(row.getRole(), row.getContent()));
        }
        Collections.reverse(turns);
        if (turns.size() > limit) {
            return turns.subList(turns.size() - limit, turns.size());
        }
        return turns;
    }

    @Transactional
    public void clear(Integer userId, String sessionId) {
        if (userId != null) {
            repository.deleteByUserId(userId);
        } else if (sessionId != null) {
            repository.deleteBySessionId(sessionId);
        }
    }
}
