package com.luminous.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_document_vector")
@Getter
@Setter
@NoArgsConstructor
public class AiDocumentVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false, unique = true, length = 100)
    private String docId;

    @Column(name = "doc_type", nullable = false, length = 20)
    private String docType;

    @Column(name = "ref_id")
    private Integer refId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AiDocumentVector(String docId, String docType, Integer refId, String content, String embedding) {
        this.docId = docId;
        this.docType = docType;
        this.refId = refId;
        this.content = content;
        this.embedding = embedding;
    }
}
