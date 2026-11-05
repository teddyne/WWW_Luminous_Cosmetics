package com.luminous.dto;

import com.luminous.model.enums.DiscountType;
import com.luminous.model.enums.VoucherScope;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherRow(
        Integer id,
        String voucherCode,
        String voucherName,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        int usageLimit,
        int usedCount,
        LocalDateTime startDate,
        LocalDateTime endDate,
        VoucherScope scopeType,
        boolean active,
        String displayStatus) {

    public String discountText() {
        if (discountValue == null) {
            return "—";
        }
        if (discountType == DiscountType.PERCENTAGE) {
            return discountValue.stripTrailingZeros().toPlainString() + "%";
        }
        return Vnd.format(discountValue);
    }
}
