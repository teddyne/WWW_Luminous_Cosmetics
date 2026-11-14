package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Anh / video kem theo 1 danh gia san pham. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ReviewMedia")
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mediaID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewID", nullable = false)
    private Review review;

    /** IMAGE / VIDEO. */
    @Column(name = "mediaType", nullable = false)
    private String mediaType;

    @Column(name = "mediaUrl", nullable = false)
    private String mediaUrl;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
