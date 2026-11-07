package com.luminous.model;

import com.luminous.model.enums.DiscountType;
import com.luminous.model.enums.VoucherApplicableScope;
import com.luminous.model.enums.VoucherScope;
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
@Table(name = "Voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucherID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucherTypeID", nullable = false)
    private VoucherType voucherType;

    @Column(name = "voucherCode", nullable = false, unique = true, length = 50)
    private String voucherCode;

    @Column(name = "voucherName", nullable = false, length = 100)
    private String voucherName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discountType", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discountValue", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "maxDiscountAmount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "minOrderValue", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "usageLimit", nullable = false)
    private Integer usageLimit;

    @Column(name = "usedCount", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "startDate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "endDate", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicableScope", nullable = false, length = 50)
    private VoucherApplicableScope applicableScope = VoucherApplicableScope.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "scopeType", nullable = false, length = 20)
    private VoucherScope scopeType = VoucherScope.ALL;

    @Column(name = "imgUrl", length = 500)
    private String imgUrl;

    @Column(name = "isActive", nullable = false)
    private Boolean isActive = true;

    public boolean isPercentage() {
        return discountType == DiscountType.PERCENTAGE;
    }

    public String displayStatus(LocalDateTime now) {
        if (!Boolean.TRUE.equals(isActive)) {
            return "Tạm dừng";
        }
        if (now.isAfter(endDate)) {
            return "Hết hạn";
        }
        if (now.isBefore(startDate)) {
            return "Sắp diễn ra";
        }
        return "Đang chạy";
    }
}
