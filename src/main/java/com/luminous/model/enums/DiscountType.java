package com.luminous.model.enums;

public enum DiscountType {

    PERCENTAGE("Giảm theo phần trăm"),
    FIXED_AMOUNT("Giảm cố định");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static DiscountType from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return DiscountType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
