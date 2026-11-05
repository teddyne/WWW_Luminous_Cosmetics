package com.luminous.dto;

import java.math.BigDecimal;

public record TopProductRow(
        Integer id,
        String name,
        String imageUrl,
        long soldCount,
        BigDecimal revenue) {

    public String revenueText() {
        return Vnd.format(revenue);
    }
}
