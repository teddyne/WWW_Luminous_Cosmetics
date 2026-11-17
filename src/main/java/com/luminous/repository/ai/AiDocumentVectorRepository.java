package com.luminous.repository.ai;

import com.luminous.model.AiDocumentVector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiDocumentVectorRepository extends JpaRepository<AiDocumentVector, Long> {

    Optional<AiDocumentVector> findByDocId(String docId);
}
