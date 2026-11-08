package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Gio hang cua 1 khach hang (moi user toi da 1 gio: userID UNIQUE trong DB).
 * Chi dung cho CUSTOMER/ADMIN da dang nhap (xem SecurityConfig: /cart can quyen).
 * Guest khong co gio DB; bam gio hang se duoc dua ve /login.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Cart")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", nullable = false, unique = true)
    private User user;

    /** DB tu cap nhat (ON UPDATE CURRENT_TIMESTAMP), khong can set tay. */
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
}
