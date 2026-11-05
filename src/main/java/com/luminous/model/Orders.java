package com.luminous.model;

import com.luminous.model.enums.OrderStatus;
import com.luminous.model.enums.SalesChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userID", nullable = true)
    private User user;

    @Column(name = "orderCode", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "orderDate", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @Column(name = "recipientName", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipientPhone", nullable = false, length = 15)
    private String recipientPhone;

    @Column(name = "shippingAddress", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shippingFee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discountAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "usedPoints", nullable = false)
    private Integer usedPoints = 0;

    @Column(name = "totalAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "salesChannel", length = 20)
    private SalesChannel salesChannel = SalesChannel.ONLINE;

    @Column(name = "createdBy")
    private Integer createdBy;

    public boolean isStoreSale() {
        return salesChannel == SalesChannel.STORE;
    }

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "cancelReason", length = 500)
    private String cancelReason;

    @Column(name = "confirmedAt")
    private LocalDateTime confirmedAt;

    @Column(name = "shippedAt")
    private LocalDateTime shippedAt;

    @Column(name = "deliveredAt")
    private LocalDateTime deliveredAt;

    @Column(name = "completedAt")
    private LocalDateTime completedAt;

    @Column(name = "cancelledAt")
    private LocalDateTime cancelledAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    public boolean isTerminal() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }
}
