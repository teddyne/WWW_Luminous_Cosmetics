package com.luminous.model.enums;

public enum PaymentMethod {

    COD("Thanh toán khi nhận hàng (COD)"),
    MOMO("Ví MoMo"),
    VNPAY("VNPay"),
    BANK_TRANSFER("Chuyển khoản ngân hàng");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PaymentMethod from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
