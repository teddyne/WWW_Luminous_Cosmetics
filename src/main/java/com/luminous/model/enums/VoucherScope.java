package com.luminous.model.enums;

public enum VoucherScope {

    ALL("Tất cả sản phẩm"),
    CATEGORY("Theo loại sản phẩm"),
    PRODUCT("Sản phẩm cụ thể");

    private final String label;

    VoucherScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static VoucherScope from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return VoucherScope.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
