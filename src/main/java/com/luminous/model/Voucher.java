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

    /** Loai hien thi cua the voucher (dung cho mau + nhan HTML/CSS):
     *  shipping = mien phi van chuyen (xanh), percent = giam % (do),
     *  fixed = giam so tien co dinh (cam), special = uu dai dac biet (vang).
     *  "Voucher dac biet" la lua chon hien thi rieng; 3 loai con lai suy ra
     *  tu co che giam that (applicableScope/discountType) de khong lech nhau. */
    public static String badgeKind(String typeName, VoucherApplicableScope scope, DiscountType type) {
        if ("Voucher đặc biệt".equals(typeName)) {
            return "special";
        }
        if (scope == VoucherApplicableScope.SHIPPING) {
            return "shipping";
        }
        return type == DiscountType.PERCENTAGE ? "percent" : "fixed";
    }

    /** Nhan in tren the voucher theo loai: "MIEN PHI VAN CHUYEN",
     *  "GIAM 20%", "GIAM 50K", "UU DAI DAC BIET". */
    public static String badgeLabel(String typeName, VoucherApplicableScope scope,
                                    DiscountType type, BigDecimal value) {
        String kind = badgeKind(typeName, scope, type);
        if ("shipping".equals(kind)) {
            return "MIỄN PHÍ VẬN CHUYỂN";
        }
        if ("percent".equals(kind)) {
            return "GIẢM " + (value == null ? "" : value.stripTrailingZeros().toPlainString()) + "%";
        }
        if ("fixed".equals(kind)) {
            return "GIẢM " + formatShortAmount(value);
        }
        return "ƯU ĐÃI ĐẶC BIỆT";
    }

    /** Rut gon so tien de in len the: 50000 -> "50K", 2500 -> "2.5K", 500 -> "500đ". */
    public static String formatShortAmount(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal v = value.stripTrailingZeros();
        if (v.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return v.divide(BigDecimal.valueOf(1000)).stripTrailingZeros().toPlainString() + "K";
        }
        return v.toPlainString() + "đ";
    }

    public String badgeKind() {
        return badgeKind(voucherType == null ? null : voucherType.getTypeName(),
                applicableScope, discountType);
    }

    public String badgeLabel() {
        return badgeLabel(voucherType == null ? null : voucherType.getTypeName(),
                applicableScope, discountType, discountValue);
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
