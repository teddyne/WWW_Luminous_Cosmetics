package com.luminous.dto;

import com.luminous.model.enums.OrderStatus;
import com.luminous.model.enums.SalesChannel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderRow(
        Integer id,
        String orderCode,
        String customerName,
        String recipientPhone,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        OrderStatus status,
        String statusLabel,
        int itemCount,
        SalesChannel salesChannel) {

    public String totalText() {
        return Vnd.format(totalAmount);
    }

    public String channelLabel() {
        return salesChannel == null ? "Bán online" : salesChannel.label();
    }
}
