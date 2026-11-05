package com.luminous.model.enums;

import java.util.List;

public enum OrderStatus {

    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static OrderStatus from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<OrderStatus> next() {
        return switch (this) {
            case PENDING -> List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> List.of(SHIPPING, CANCELLED);
            case SHIPPING -> List.of(DELIVERED, CANCELLED);
            case DELIVERED -> List.of(COMPLETED);
            case COMPLETED -> List.of();
            case CANCELLED -> List.of(PENDING);
        };
    }
}
