package com.luminous.model.enums;

public enum CategoryStatus {

    ACTIVE("Đang bán"),
    INACTIVE("Tạm dừng");

    private final String label;

    CategoryStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static CategoryStatus from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return CategoryStatus.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
