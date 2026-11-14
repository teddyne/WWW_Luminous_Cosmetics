package com.luminous.model.enums;

import java.util.List;

public enum OrderStatus {

    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    RETURNED("Trả hàng");

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
            // DELIVERED khong duoc chuyen tay sang COMPLETED nua: don chi
            // hoan thanh khi khach xac nhan da nhan, hoac job tu dong hoan
            // thanh sau 3 ngay (OrderAutoCompleteService). Admin chi co the
            // chuyen DELIVERED sang RETURNED.
            case DELIVERED -> List.of(RETURNED);
            case COMPLETED -> List.of(RETURNED);
            case CANCELLED -> List.of(PENDING);
            case RETURNED -> List.of();
        };
    }
}
