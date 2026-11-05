package com.luminous.model.enums;

public enum VoucherApplicableScope {

    ALL("Toàn bộ đơn hàng"),
    SHIPPING("Miễn phí vận chuyển");

    private final String label;

    VoucherApplicableScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static VoucherApplicableScope from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return VoucherApplicableScope.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
