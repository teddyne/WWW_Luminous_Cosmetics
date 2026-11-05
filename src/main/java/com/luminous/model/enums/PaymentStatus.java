package com.luminous.model.enums;

public enum PaymentStatus {

    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PaymentStatus from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
