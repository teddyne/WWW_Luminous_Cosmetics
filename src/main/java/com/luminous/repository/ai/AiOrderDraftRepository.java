package com.luminous.repository.ai;

import com.luminous.model.AiOrderDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface AiOrderDraftRepository extends JpaRepository<AiOrderDraft, Long> {

    Optional<AiOrderDraft> findByIdAndSessionId(Long id, String sessionId);

    Optional<AiOrderDraft> findByIdAndUserId(Long id, Integer userId);

    @Modifying
    @Query("UPDATE AiOrderDraft d SET d.status = 'EXPIRED' WHERE d.status = 'DRAFT' AND d.expiresAt <= ?1")
    int markExpired(LocalDateTime now);
}
