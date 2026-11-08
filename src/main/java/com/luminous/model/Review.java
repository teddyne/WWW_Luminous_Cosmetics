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
