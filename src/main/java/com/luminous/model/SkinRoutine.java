package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bang MOI (migration 002): 1 chu trinh dang hoat dong cho 1 user.
 * routineJson chi luu khung lich, vi du:
 * [{"time":"MORNING","categoryId":2,"variantId":5},
 *  {"time":"EVENING","categoryId":1,"variantId":2}]
 * Ten, gia, anh, thanh phan luon doc lai tu Product/ProductVariant that
 * trong DB khi hien thi, khong luu cung vao JSON de gia doi thi lich
 * van hien gia moi. Doi san pham = doi variantId o 1 buoc trong JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "SkinRoutine")
public class SkinRoutine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routineID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", nullable = false, unique = true)
    private User user;

    @Column(name = "quizID", nullable = false)
    private Integer quizId;

    @Column(name = "skinType", nullable = false, length = 50)
    private String skinType;

    /** Ma muc tieu cach nhau dau phay, vi du "ACNE,HYDRATE". */
    @Column(name = "goals", length = 255)
    private String goals;

    /** TIET_KIEM / CAN_BANG / CAO_CAP / KHONG_GIOI_HAN. */
    @Column(name = "budget", length = 20)
    private String budget;

    @Column(name = "routineJson", columnDefinition = "TEXT")
    private String routineJson;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
}
