package com.luminous.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Khoa chong dat hang trung: 1 lan bam "Dat hang" chi tao 1 don.
 * Khoa do client sinh (UUID moi khi mo trang checkout), luu truoc khi tao
 * don; request lap lai cung khoa se tra ve don da tao thay vi tao don moi.
 * Cot DB dat ten idempotencyKey (chu "key" la tu khoa du tru cua MariaDB).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "IdempotencyKey")
public class IdempotencyKey {

    @Id
    @Column(name = "idempotencyKey", nullable = false, length = 64)
    private String key;

    @Column(name = "userID", nullable = false)
    private Integer userId;

    @Column(name = "orderCode", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public IdempotencyKey(String key, Integer userId, String orderCode) {
        this.key = key;
        this.userId = userId;
        this.orderCode = orderCode;
    }
}
