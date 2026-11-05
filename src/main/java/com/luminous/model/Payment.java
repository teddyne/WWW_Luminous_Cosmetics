package com.luminous.model;

import com.luminous.model.enums.PaymentMethod;
import com.luminous.model.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentID")
    private Integer id;

    @Column(name = "orderID", nullable = false)
    private Integer orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "paymentMethod", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "paymentStatus", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transactionCode", length = 100)
    private String transactionCode;

    @Column(name = "paidAt")
    private LocalDateTime paidAt;

    @Column(name = "cashGiven", precision = 12, scale = 2)
    private BigDecimal cashGiven;

    @Column(name = "changeAmount", precision = 12, scale = 2)
    private BigDecimal changeAmount;
}
