package com.luminous.model.enums;

public enum SalesChannel {

    STORE("Bán tại cửa hàng"),
    ONLINE("Bán online");

    private final String label;

    SalesChannel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static SalesChannel from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return SalesChannel.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
