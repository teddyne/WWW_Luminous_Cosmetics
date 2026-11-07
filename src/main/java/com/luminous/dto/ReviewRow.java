package com.luminous.dto;

import java.time.LocalDateTime;

public record ReviewRow(
        Integer id,
        String customerName,
        String productName,
        String productImage,
        Byte rating,
        String comment,
        LocalDateTime createdAt,
        boolean visible,
        boolean anonymous) {
}
