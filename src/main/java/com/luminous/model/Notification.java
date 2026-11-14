package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/** Thong bao cho khach: dat don, doi trang thai don, cong diem, danh gia... */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationID")
    private Integer id;

    @Column(name = "userID", nullable = false)
    private Integer userId;

    /** ORDER / POINTS / REVIEW / RETURN / SYSTEM. */
    @Column(name = "notificationType", nullable = false)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String message;

    /** Duong dan mo khi bam vao thong bao, vi du /account/orders. */
    @Column(name = "actionUrl")
    private String link;

    @Column(name = "isRead", nullable = false)
    private Boolean isRead = false;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
