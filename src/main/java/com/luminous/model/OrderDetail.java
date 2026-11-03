package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Chi dung o giai doan 1 de tinh "da ban" cho khoi San pham ban chay.
 * Khong map quan he toi Orders o day de tranh phai tao them entity Orders chua can.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "OrderDetail")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderDetailID")
    private Integer id;

    @Column(name = "orderID", nullable = false)
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantID", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unitPrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "totalPrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;
}
