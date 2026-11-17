package com.luminous.model.enums;

public enum FlashSaleStatus {

    DRAFT("Nháp"),
    PUBLISHED("Đã đăng"),
    PAUSED("Tạm dừng");

    private final String label;

    FlashSaleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
