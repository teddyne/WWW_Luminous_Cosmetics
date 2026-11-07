package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Review")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewID")
    private Integer id;

    @Column(name = "userID", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderDetailID", nullable = false)
    private OrderDetail orderDetail;

    // DB: rating TINYINT (1..5, co CHECK). Java phai dung Byte (8-bit, khop TINYINT),
    // neu de Integer thi Hibernate doi INTEGER va validate se bao:
    // "wrong column type ... found [tinyint], but expecting [integer]".
    // Khong co code nao khac dung truc tiep truong nay (chi co AVG(rating) trong
    // ReviewRepository, van tra Double binh thuong) nen doi kieu o day la du.
    @Column(name = "rating", nullable = false, columnDefinition = "TINYINT")
    private Byte rating; // 1..5, DB co CHECK

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "isVisible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "isAnonymous", nullable = false)
    private Boolean isAnonymous = false;
}
