package com.luminous.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ma giam gia da dung cho 1 don hang (bang OrderVoucher). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "OrderVoucher")
public class OrderVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderVoucherID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucherID", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderID", nullable = false)
    private Orders order;

    @Column(name = "discountAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "appliedAt", nullable = false, updatable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();
}
