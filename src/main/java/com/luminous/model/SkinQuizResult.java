package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Khop bang SkinQuizResult trong luminous_db.sql.
 * LUU Y: khong map quan he toi SkinQuiz bang @ManyToOne, chi giu quizId
 * dang so nguyen. Ly do: bang SkinQuiz co cot questions kieu JSON cua
 * MariaDB, map thanh entity de bi loi validate schema; phan doc cau hoi
 * nam o SkinQuizDataLoader (native query, chi doc).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "SkinQuizResult")
public class SkinQuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resultID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", nullable = false)
    private User user;

    @Column(name = "quizID", nullable = false)
    private Integer quizId;

    @Column(name = "skinType", nullable = false, length = 50)
    private String skinType; // OILY / DRY / COMBINATION / NORMAL / SENSITIVE

    @Column(name = "skincareAdvice", columnDefinition = "TEXT")
    private String skincareAdvice;

    /** Danh sach variantID dang JSON, vi du [5, 6, 9]. */
    @Column(name = "suitableProducts", columnDefinition = "TEXT")
    private String suitableProducts;

    @Column(name = "completedAt", nullable = false)
    private LocalDateTime completedAt;
}
