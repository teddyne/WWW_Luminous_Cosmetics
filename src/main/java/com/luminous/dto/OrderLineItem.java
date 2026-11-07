package com.luminous.dto;

import java.math.BigDecimal;

public record OrderLineItem(
        String productName,
        String variantLabel,
        String imageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {

    public String unitPriceText() {
        return Vnd.format(unitPrice);
    }

    public String totalPriceText() {
        return Vnd.format(totalPrice);
    }
}
